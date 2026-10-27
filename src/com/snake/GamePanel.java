package com.snake;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * 游戏主面板：状态推进、键盘输入、相机、绘制都在这里。
 *
 * 支持两种地图：
 * - 经典有限：固定 cols×rows 棋盘，撞墙死或穿墙（可配），相机把棋盘摆在正中。
 * - 无尽无限：没有边界，相机永远把蛇头放在画面中央，食物按可见区域自动补充。
 *
 * 皮肤预览页与主菜单背景都复用这个类跑 demo 模式，所以演示画的就是真实渲染效果。
 */
public class GamePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /** 单个格子的像素边长。 */
    public static final int CELL = 20;
    /** 初始长度（节）。 */
    private static final int INITIAL_LENGTH = 3;
    /** 食物生成的基准搜索半径（格）。 */
    private static final int FOOD_RADIUS_X = 14;
    private static final int FOOD_RADIUS_Y = 10;
    private static final int FOOD_SPAWN_ATTEMPTS = 80;
    private static final int FOOD_SPIRAL_LIMIT = 200;
    /** 无尽模式按可见面积补食物：每 40 个可见格 1 个。 */
    private static final int FOOD_CELLS_PER_FOOD = 40;

    /**
     * 面板与外层窗口之间的回调。全部给默认实现，
     * 这样只需要处理其中一两个方法时不必写空方法。
     */
    public interface Listener {
        default void onScoreChanged(int score, int length) {
        }

        default void onPauseRequested(boolean paused) {
        }

        default void onPigEncounter() {
        }

        default void onShopRequested() {
        }

        default void onEscape() {
        }

        default void onGameOver(int score, int length, boolean filled) {
        }
    }

    private GameConfig config = new GameConfig();
    private Skin skin = Skins.defaultSkin();
    private final BackgroundSettings background;

    private final List<Point> body = new ArrayList<Point>();
    private final List<Point> foods = new ArrayList<Point>();
    /** 待处理的方向输入，最多缓存两个，避免一帧内连续急转撞到自己。 */
    private final Deque<Direction> pending = new ArrayDeque<Direction>(2);
    private final Random random = new Random();

    private Timer timer;
    private Listener listener = new Listener() {
    };

    private Direction dir = Direction.RIGHT;
    private int score;
    private boolean gameOver;
    private boolean paused;
    private boolean demo;
    /** 本局是否已经生成过猪头。 */
    private boolean pigDropped;
    /** 当前猪头所在格；没有猪头时为 null。 */
    private Point pigPos;
    /** 当前存档是否已经解锁商店。 */
    private boolean shopUnlocked;
    private long startedAt = System.currentTimeMillis();

    private int originX;
    private int originY;
    /**
     * 相机锚点（蛇头的世界坐标），必须是 long：
     * 无尽模式里蛇可以一直朝一个方向走，坐标迟早超出 int 范围，
     * 一旦这里被截断，屏幕坐标就会整片跳掉。
     */
    private long headX;
    private long headY;

    public GamePanel() {
        this(ConfigStore.get().background());
    }

    public GamePanel(BackgroundSettings background) {
        this.background = background;
        setOpaque(true);
        setFocusable(true);
        timer = new Timer(config.tickMs, e -> update());
        setPreferredSize(computePreferredSize());
        reset();
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKey(e);
            }
        });
    }

    // ---- 配置与外观 ----

    /** 换一局：应用配置、重建计时器间隔、重开并启动。 */
    public void configure(GameConfig cfg, Skin s) {
        this.config = cfg.sanitize();
        this.skin = s;
        this.demo = false;
        timer.setDelay(this.config.tickMs);
        setPreferredSize(computePreferredSize());
        reset();
        // 计时器必须在这里启动：调用方（GameScreen）只管发配置，
        // 指望外面记得再调一次 start() 迟早会漏。
        timer.start();
    }

    public void setSkin(Skin s) {
        this.skin = s;
        repaint();
    }

    public Skin skin() {
        return skin;
    }

    public GameConfig config() {
        return config;
    }

    /** 设置或皮肤变了之后调用，重新按当前尺寸对齐相机。 */
    public void refreshAppearance() {
        updateOrigin();
        repaint();
    }

    /** 暂停菜单里调速用：只改间隔，不重开局。 */
    public void setTickInterval(int ms) {
        if (GameConfig.speedIndexOf(ms) < 0) {
            return;
        }
        config.tickMs = ms;
        timer.setDelay(ms);
    }

    private Dimension computePreferredSize() {
        if (config.mode == GameConfig.Mode.CLASSIC) {
            return new Dimension(config.cols * CELL + 40, config.rows * CELL + 40);
        }
        return new Dimension(660, 500);
    }

    // ---- 状态查询 ----

    public int score() {
        return score;
    }

    public int length() {
        return body.size();
    }

    public int maxLength() {
        return config.maxLength;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean isPaused() {
        return paused;
    }

    /** 是否正由演示 AI 代打。停掉计时器就等同于不在演示了。 */
    public boolean isDemo() {
        return demo && timer.isRunning();
    }

    public boolean isFilled() {
        return body.size() >= config.maxLength;
    }

    public boolean isShopUnlocked() {
        return shopUnlocked;
    }

    /** 对话结束：仅当前存档解锁商店，并确保猪头不会再次出现。 */
    public void unlockShop() {
        shopUnlocked = true;
        pigDropped = true;
        pigPos = null;
        repaint();
    }

    public Point pigPosition() {
        return pigPos;
    }

    public long elapsedSec() {
        return Math.max(0, (System.currentTimeMillis() - startedAt) / 1000);
    }

    public void setListener(Listener l) {
        this.listener = l == null ? new Listener() {
        } : l;
    }

    public void setPaused(boolean p) {
        if (this.paused != p) {
            this.paused = p;
            listener.onPauseRequested(p);
        }
    }

    public void togglePaused() {
        setPaused(!paused);
    }

    /** 窗口关闭时停掉定时器，避免 EDT 残留。 */
    public void stop() {
        timer.stop();
    }

    /** 主菜单背景用：自动开一局并让 AI 代打。 */
    public void startDemo(boolean on) {
        if (on) {
            GameConfig c = new GameConfig();
            c.mode = GameConfig.Mode.CLASSIC;
            c.cols = 25;
            c.rows = 19;
            c.wrapWalls = true;
            c.tickMs = 130;
            c.foodCount = 2;
            c.maxLength = 30;
            configure(c, skin);
            // configure() 会把 demo 复位，所以要等它跑完再打开。
            this.demo = true;
            timer.start();
        } else {
            this.demo = false;
            stop();
        }
    }

    // ---- 生命周期 ----

    /** 重开一局。 */
    public void reset() {
        body.clear();
        foods.clear();
        pending.clear();
        if (config.mode == GameConfig.Mode.CLASSIC) {
            long sx = config.cols / 2;
            long sy = config.rows / 2;
            for (int i = 0; i < INITIAL_LENGTH; i++) {
                body.add(new Point(sx - i, sy));
            }
        } else {
            for (int i = 0; i < INITIAL_LENGTH; i++) {
                body.add(new Point(-i, 0));
            }
        }
        dir = Direction.RIGHT;
        score = 0;
        gameOver = false;
        paused = false;
        pigDropped = false;
        pigPos = null;
        shopUnlocked = false;
        startedAt = System.currentTimeMillis();
        syncCamera();
        fillFoods();
        notifyScore();
        repaint();
    }

    /** 从存档恢复一局（主菜单「继续游戏」）。 */
    public void applySaveState(SaveState s) {
        config = s.config.sanitize();
        skin = s.skin();
        demo = false;
        timer.setDelay(config.tickMs);
        setPreferredSize(computePreferredSize());

        body.clear();
        body.addAll(s.body);
        foods.clear();
        foods.addAll(s.foods);
        pending.clear();
        pending.addAll(s.pending);

        dir = s.dir;
        score = s.score;
        gameOver = false;
        shopUnlocked = s.shopUnlocked;
        pigDropped = s.pigDropped;
        pigPos = s.pigPos;
        startedAt = s.startedAt;
        // 计时器要跑起来，但先暂停：玩家是从菜单点进来的，
        // 不给反应时间直接开跑必然猝死。之后由「继续游戏」解除暂停。
        timer.start();
        setPaused(true);
        syncCamera();
        if (config.mode == GameConfig.Mode.ENDLESS) {
            fillFoods();
        }
        if (shopUnlocked) {
            pigDropped = true;
            pigPos = null;
        } else {
            if (pigPos != null) {
                pigDropped = true;
            } else if (pigDropped) {
                // 旧版本或异常存档：猪头标记在、位置却没了，允许重新生成，避免商店永久锁死。
                pigDropped = false;
            }
            if (pigPos != null && (!insideBoard(pigPos)
                    || body.contains(pigPos) || foods.contains(pigPos))) {
                pigPos = null;
                pigDropped = false;
            }
            if (!pigDropped && body.size() >= config.maxLength) {
                dropPig();
            }
            keepPigInView();
        }
        notifyScore();
        repaint();
    }

    /** 把当前局面打包成存档。 */
    public SaveState captureSaveState(int recordedScore, boolean resumed) {
        SaveState s = new SaveState();
        s.config = config.deepCopy();
        s.skinId = skin.id;
        s.dir = dir;
        s.body = new ArrayList<Point>(body);
        s.foods = new ArrayList<Point>(foods);
        s.pending = new ArrayList<Direction>(pending);
        s.shopUnlocked = shopUnlocked;
        s.pigDropped = pigDropped;
        s.pigPos = pigPos;
        s.score = score;
        s.startedAt = startedAt;
        s.savedAt = System.currentTimeMillis();
        s.recordedScore = recordedScore;
        s.resumed = resumed;
        return s;
    }

    // ---- 输入 ----

    private void handleKey(KeyEvent e) {
        int key = e.getKeyCode();
        if (demo) {
            e.consume();
            return;
        }
        // ESC 必须先于 consume 处理：所有按键都会在这里被吞掉，
        // 如果不在最前面特判，外层容器永远收不到 ESC。
        if (key == KeyEvent.VK_ESCAPE) {
            listener.onEscape();
            e.consume();
            return;
        }
        if (gameOver) {
            if (key == KeyEvent.VK_SPACE || key == KeyEvent.VK_ENTER || key == KeyEvent.VK_R) {
                reset();
            }
            e.consume();
            return;
        }
        // B 放在 paused 之前：商店/剧情层显示时焦点可能还留在棋盘上，
        // 这样 B 仍然能交给外层关闭商店。
        if (key == KeyEvent.VK_B) {
            listener.onShopRequested();
            e.consume();
            return;
        }
        // paused 必须排在空格之前。
        // 暂停菜单浮在棋盘上时，如果空格还能走到 togglePaused，
        // 棋盘会在菜单底下重新跑起来，而菜单还开着，玩家完全看不见。
        if (paused) {
            e.consume();
            return;
        }
        if (key == KeyEvent.VK_SPACE) {
            togglePaused();
            e.consume();
            return;
        }
        Direction wanted = toDirection(key);
        if (wanted == null) {
            e.consume();
            return;
        }
        if (pending.size() < 2) {
            Direction base = pending.isEmpty() ? dir : pending.peek();
            if (wanted != base.opposite()) {
                pending.addLast(wanted);
            }
        }
        e.consume();
    }

    private static Direction toDirection(int key) {
        switch (key) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                return Direction.UP;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                return Direction.DOWN;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                return Direction.LEFT;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                return Direction.RIGHT;
            default:
                return null;
        }
    }

    // ---- 推进 ----

    private void update() {
        // demo 面板只在可见时跑，切走的卡片不会白白吃 CPU。
        if (demo && !isShowing()) {
            return;
        }
        if (gameOver || paused) {
            return;
        }
        if (demo) {
            demoStep();
            return;
        }
        if (!pending.isEmpty()) {
            dir = pending.poll();
        }
        step();
    }

    private void step() {
        Point next = body.get(0).translate(dir);
        Point newHead = enterBoard(next);
        if (newHead == null) {
            die();
            return;
        }
        boolean growing = foods.contains(newHead);
        boolean pigHit = pigPos != null && pigPos.equals(newHead);
        // 吃食物时尾巴不会移走，所以整条蛇都要参与碰撞检测。
        int limit = growing ? body.size() : body.size() - 1;
        for (int i = 0; i < limit; i++) {
            if (body.get(i).equals(newHead)) {
                die();
                return;
            }
        }

        body.add(0, newHead);
        if (growing) {
            foods.remove(newHead);
            score++;
            if (body.size() > config.maxLength) {
                body.remove(body.size() - 1);
            }
            notifyScore();
        } else {
            body.remove(body.size() - 1);
        }
        if (pigHit) {
            // 猪头只是剧情触发器：不加分、不变长，碰到后立即消失。
            pigPos = null;
            pigDropped = true;
        }
        syncCamera();
        if (growing || config.mode == GameConfig.Mode.ENDLESS) {
            fillFoods();
        }
        if (!demo && !shopUnlocked && !pigDropped
                && body.size() >= config.maxLength) {
            dropPig();
        }
        keepPigInView();
        repaint();
        if (pigHit && !demo) {
            listener.onPigEncounter();
        }
    }

    /**
     * 处理边界。
     *
     * <p>{@link Point} 是不可变的，所以这里返回落到棋盘内的新坐标，
     * 而不是就地改参数。穿墙取模后可能出现 y 恰好等于 rows（撞到下边界再绕回去），
     * 所以取模后再夹一次，保证永远落在 0..rows-1。</p>
     *
     * @return 合法坐标；越界且不穿墙时返回 null 表示撞墙死亡
     */
    private Point enterBoard(Point p) {
        if (config.mode != GameConfig.Mode.CLASSIC) {
            return p;
        }
        if (config.wrapWalls) {
            long x = Math.floorMod(p.x, config.cols);
            long y = Math.floorMod(p.y, config.rows);
            return new Point(x, y);
        }
        return (p.x >= 0 && p.y >= 0 && p.x < config.cols && p.y < config.rows) ? p : null;
    }

    private void die() {
        gameOver = true;
        repaint();
        listener.onGameOver(score, body.size(), isFilled());
    }

    private void notifyScore() {
        listener.onScoreChanged(score, body.size());
    }

    // ---- 食物 ----

    private void fillFoods() {
        if (body.isEmpty()) {
            return;
        }
        if (config.mode == GameConfig.Mode.ENDLESS) {
            if (getWidth() <= 0 || getHeight() <= 0) {
                return;
            }
            pruneFoodsAwayFromView();
            int target = endlessFoodTarget();
            while (foods.size() < target) {
                if (!spawnFood()) {
                    break;
                }
            }
            return;
        }
        while (foods.size() < config.foodCount && body.size() < config.maxLength) {
            if (!spawnFood()) {
                break;
            }
        }
    }

    /**
     * 以蛇头附近为目标生成一个食物。
     *
     * 经典模式在蛇头周围随机尝试（范围随食物数量收缩，避免几个食物挤在一小块），
     * 失败后沿方形螺旋向外找，所有候选都限制在棋盘内。
     * 无尽模式把范围放大到整个视野，随机点必须仍然落在可见区域内。
     *
     * @return 是否成功生成
     */
    private boolean spawnFood() {
        Point head = body.get(0);
        boolean endless = config.mode == GameConfig.Mode.ENDLESS;
        int shrink = endless ? 1
                : (int) Math.max(1, Math.round(Math.sqrt(Math.max(1, config.foodCount))));
        int rx = endless ? viewCols() / 2 + 2 : Math.max(2, foodRadiusX() / shrink);
        int ry = endless ? viewRows() / 2 + 2 : Math.max(2, foodRadiusY() / shrink);

        long x0 = head.x - rx;
        long x1 = head.x + rx;
        long y0 = head.y - ry;
        long y1 = head.y + ry;
        if (config.mode == GameConfig.Mode.CLASSIC) {
            x0 = Math.max(0, x0);
            y0 = Math.max(0, y0);
            x1 = Math.min(config.cols - 1L, x1);
            y1 = Math.min(config.rows - 1L, y1);
            if (x1 < x0 || y1 < y0) {
                x0 = 0;
                y0 = 0;
                x1 = config.cols - 1L;
                y1 = config.rows - 1L;
            }
        }
        int spanX = (int) Math.min(Integer.MAX_VALUE, x1 - x0 + 1);
        int spanY = (int) Math.min(Integer.MAX_VALUE, y1 - y0 + 1);

        for (int i = 0; i < FOOD_SPAWN_ATTEMPTS; i++) {
            Point p = new Point(x0 + random.nextInt(spanX), y0 + random.nextInt(spanY));
            if (isFree(p) && (!endless || inView(p))) {
                foods.add(p);
                return true;
            }
        }
        return spawnFoodSpiral(head);
    }

    private boolean spawnFoodSpiral(Point head) {
        long limit = config.mode == GameConfig.Mode.ENDLESS
                ? Math.max(viewCols(), viewRows()) / 2 + 2
                : FOOD_SPIRAL_LIMIT - 1;
        for (long r = 1; r <= limit; r++) {
            for (long dy = -r; dy <= r; dy++) {
                if (trySpiral(head, head.x - r, head.y + dy)) {
                    return true;
                }
                if (trySpiral(head, head.x + r, head.y + dy)) {
                    return true;
                }
            }
            for (long dx = -r + 1; dx <= r - 1; dx++) {
                if (trySpiral(head, head.x + dx, head.y - r)) {
                    return true;
                }
                if (trySpiral(head, head.x + dx, head.y + r)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean trySpiral(Point head, long x, long y) {
        if (config.mode == GameConfig.Mode.CLASSIC && enterBoard(new Point(x, y)) == null) {
            return false;
        }
        Point p = new Point(x, y);
        if (config.mode == GameConfig.Mode.ENDLESS && !inView(p)) {
            return false;
        }
        if (isFree(p)) {
            foods.add(p);
            return true;
        }
        return false;
    }

    /** 无尽模式的同屏目标：每 {@link #FOOD_CELLS_PER_FOOD} 个可见格一个食物，不设硬上限。 */
    private int endlessFoodTarget() {
        return Math.max(1, viewCols() * viewRows() / FOOD_CELLS_PER_FOOD);
    }

    private int viewCols() {
        return Math.max(1, (getWidth() + CELL - 1) / CELL);
    }

    private int viewRows() {
        return Math.max(1, (getHeight() + CELL - 1) / CELL);
    }

    /** 格子与面板矩形哪怕只交叠一点也算可见，贴边食物不会被误清。 */
    private boolean inView(Point p) {
        int sx = screenX(p);
        int sy = screenY(p);
        return sx < getWidth() && sx + CELL > 0 && sy < getHeight() && sy + CELL > 0;
    }

    private void pruneFoodsAwayFromView() {
        foods.removeIf(p -> !inView(p));
    }

    private boolean isFree(Point p) {
        for (Point seg : body) {
            if (seg.equals(p)) {
                return false;
            }
        }
        for (Point f : foods) {
            if (f.equals(p)) {
                return false;
            }
        }
        return pigPos == null || !pigPos.equals(p);
    }

    // ---- 猪头 ----

    /** 达到长度上限时生成一次猪头，优先放在蛇头前方视野内。 */
    private void dropPig() {
        Point old = pigPos;
        pigPos = null;
        if (findPigCell()) {
            pigDropped = true;
        } else {
            pigPos = old;
        }
    }

    /** 先找正前方，找不到再沿螺旋找；所有候选都必须在视野内。 */
    private boolean findPigCell() {
        if (body.isEmpty()) {
            return false;
        }
        Point head = body.get(0);
        int ahead = Math.max(3, Math.min(7, viewCols() / 4));
        for (int d = ahead; d >= 3; d--) {
            Point p = head;
            for (int i = 0; i < d; i++) {
                p = p.translate(dir);
            }
            if (pigCellFree(p)) {
                pigPos = p;
                return true;
            }
        }
        long limit = config.mode == GameConfig.Mode.ENDLESS
                ? Math.max(viewCols(), viewRows()) / 2 + 2
                : Math.max(config.cols, config.rows);
        for (long r = 1; r <= limit; r++) {
            for (long dy = -r; dy <= r; dy++) {
                if (tryPigCell(head.x - r, head.y + dy)
                        || tryPigCell(head.x + r, head.y + dy)) {
                    return true;
                }
            }
            for (long dx = -r + 1; dx <= r - 1; dx++) {
                if (tryPigCell(head.x + dx, head.y - r)
                        || tryPigCell(head.x + dx, head.y + r)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean tryPigCell(long x, long y) {
        Point p = new Point(x, y);
        if (!pigCellFree(p)) {
            return false;
        }
        pigPos = p;
        return true;
    }

    private boolean pigCellFree(Point p) {
        return insideBoard(p) && inView(p) && isFree(p);
    }

    private boolean insideBoard(Point p) {
        return config.mode != GameConfig.Mode.CLASSIC
                || (p.x >= 0 && p.y >= 0 && p.x < config.cols && p.y < config.rows);
    }

    /** 猪头被相机移出画面时，把它挪回前方视野，仍然是同一个猪头。 */
    private void keepPigInView() {
        if (pigPos == null || inView(pigPos)) {
            return;
        }
        Point old = pigPos;
        pigPos = null;
        if (!findPigCell()) {
            pigPos = old;
        }
    }

    /** 经典模式的食物半径跟着棋盘尺寸收缩，小棋盘里也不会生成到边界外。 */
    private int foodRadiusX() {
        return Math.max(1, Math.min(FOOD_RADIUS_X, config.cols / 2));
    }

    private int foodRadiusY() {
        return Math.max(1, Math.min(FOOD_RADIUS_Y, config.rows / 2));
    }

    // ---- demo 自动演示 ----

    /**
     * 极简 AI：优先走不会立刻死、且能靠近最近食物的方向；
     * 同分时看落点周围有多少空格，并加一点随机抖动让它不像机器。
     */
    private void demoStep() {
        if (foods.isEmpty()) {
            reset();
            return;
        }
        Point head = body.get(0);
        Point target = foods.get(0);
        for (Point f : foods) {
            if (chebyshev(head, f) < chebyshev(head, target)) {
                target = f;
            }
        }

        Direction best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Direction d : Direction.values()) {
            if (d == dir.opposite()) {
                continue;
            }
            Point nh = head.translate(d);
            if (config.mode == GameConfig.Mode.CLASSIC && enterBoard(nh) == null) {
                continue;
            }
            if (body.contains(nh)) {
                continue;
            }
            int score = -chebyshev(nh, target) * 8 + openNeighbors(nh) * 3 + random.nextInt(3);
            if (score > bestScore) {
                bestScore = score;
                best = d;
            }
        }
        if (best == null) {
            reset();
            return;
        }
        dir = best;
        step();
        if (gameOver) {
            // 演示蛇撞死了就悄悄重开，玩家看到的是背景一直在动。
            reset();
        }
    }

    private static int chebyshev(Point a, Point b) {
        return (int) Math.max(Math.abs(a.x - b.x), Math.abs(a.y - b.y));
    }

    private int openNeighbors(Point p) {
        int n = 0;
        for (Direction d : Direction.values()) {
            Point q = p.translate(d);
            if (config.mode == GameConfig.Mode.CLASSIC && enterBoard(q) == null) {
                continue;
            }
            if (!body.contains(q)) {
                n++;
            }
        }
        return n;
    }

    // ---- 相机 ----

    private void syncCamera() {
        Point head = body.get(0);
        headX = head.x;
        headY = head.y;
        updateOrigin();
    }

    /** 经典模式把棋盘摆正中；无尽模式把蛇头摆在画面中央（origin 不参与计算）。 */
    private void updateOrigin() {
        if (config.mode == GameConfig.Mode.CLASSIC) {
            originX = (getWidth() - config.cols * CELL) / 2;
            originY = (getHeight() - config.rows * CELL) / 2;
        } else {
            // 无尽模式的绘制全部以蛇头为锚点算相对偏移，用不到 origin。
            originX = 0;
            originY = 0;
        }
    }

    /** 蛇头所在格的左上角在屏幕上的像素位置。 */
    private int centerX() {
        return getWidth() / 2 - CELL / 2;
    }

    private int centerY() {
        return getHeight() / 2 - CELL / 2;
    }

    @Override
    public void doLayout() {
        super.doLayout();
        updateOrigin();
        if (config.mode == GameConfig.Mode.ENDLESS && !gameOver
                && getWidth() > 0 && getHeight() > 0) {
            fillFoods();
        }
        if (!demo && !gameOver && !shopUnlocked && !pigDropped
                && body.size() >= config.maxLength) {
            dropPig();
        }
        keepPigInView();
    }

    /**
     * 世界坐标 → 屏幕像素。
     *
     * 无尽模式下一律用「相对蛇头的偏移」来算：
     * (p.x - headX) 最多也就几十格，乘 CELL 远在 int 范围内，
     * 而直接算 headX * CELL 在坐标很大时会静默溢出。
     */
    private int screenX(Point p) {
        if (config.mode == GameConfig.Mode.CLASSIC) {
            return originX + (int) (p.x * CELL);
        }
        return centerX() + (int) ((p.x - headX) * CELL);
    }

    private int screenY(Point p) {
        if (config.mode == GameConfig.Mode.CLASSIC) {
            return originY + (int) (p.y * CELL);
        }
        return centerY() + (int) ((p.y - headY) * CELL);
    }

    // ---- 绘制 ----

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        boolean custom = background.enabledInGame && background.hasImage();
        if (custom) {
            drawBackground(g2);
        } else {
            g2.setColor(skin.bg);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
        if (background.showGrid) {
            drawGrid(g2, custom);
        }
        drawFood(g2);
        drawPig(g2);
        drawSnake(g2);
        if (config.mode == GameConfig.Mode.CLASSIC) {
            drawWall(g2);
        }
        if (gameOver && !demo) {
            drawGameOver(g2);
        }
        g2.dispose();
    }

    private void drawBackground(Graphics2D g2) {
        BufferedImage img = BackgroundImage.scaled(
                Math.max(1, getWidth()), Math.max(1, getHeight()),
                background.scale, skin.bg);
        if (img != null) {
            g2.drawImage(img, 0, 0, null);
        } else {
            g2.setColor(skin.bg);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
        if (background.dimPercent > 0) {
            g2.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, background.dimPercent / 100f));
            g2.setColor(skin.bg);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setComposite(AlphaComposite.SrcOver);
        }
    }

    private void drawGrid(Graphics2D g2, boolean dimmed) {
        g2.setStroke(new BasicStroke(1f));
        if (dimmed) {
            // 有背景图时网格只留淡淡的纹理，不然会盖住画面。
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
        }
        if (config.mode == GameConfig.Mode.CLASSIC) {
            for (int g = 0; g <= config.cols; g++) {
                int x = originX + g * CELL;
                g2.setColor(isAxis(g) ? skin.gridAxis : skin.grid);
                g2.drawLine(x, originY, x, originY + config.rows * CELL);
            }
            for (int g = 0; g <= config.rows; g++) {
                int y = originY + g * CELL;
                g2.setColor(isAxis(g) ? skin.gridAxis : skin.grid);
                g2.drawLine(originX, y, originX + config.cols * CELL, y);
            }        } else {
            // 无尽模式：网格线按真实世界坐标每 10 格加深一次，
            // 所以要按「左边界那一格是世界第几格」来推，而不是从头画起。
            int spanX = getWidth() / CELL + 2;
            int spanY = getHeight() / CELL + 2;
            long leftCell = headX - (centerX() + CELL) / CELL;
            long topCell = headY - (centerY() + CELL) / CELL;
            for (int i = 0; i <= spanX; i++) {
                long g = leftCell + i;
                int x = centerX() + (int) ((g - headX) * CELL);
                g2.setColor(isAxis(g) ? skin.gridAxis : skin.grid);
                g2.drawLine(x, 0, x, getHeight());
            }
            for (int j = 0; j <= spanY; j++) {
                long g = topCell + j;
                int y = centerY() + (int) ((g - headY) * CELL);
                g2.setColor(isAxis(g) ? skin.gridAxis : skin.grid);
                g2.drawLine(0, y, getWidth(), y);
            }
        }
        if (dimmed) {
            g2.setComposite(AlphaComposite.SrcOver);
        }
    }

    /** 每 10 格加深一色，纯粹是给玩家一个方位参考，不影响真实坐标。 */
    private static boolean isAxis(long cell) {
        long m = cell % 10;
        if (m < 0) {
            m += 10;
        }
        return m == 0;
    }

    private void drawWall(Graphics2D g2) {
        g2.setColor(skin.wall);
        g2.setStroke(new BasicStroke(2f));
        int x = originX;
        int y = originY;
        int w = config.cols * CELL;
        int h = config.rows * CELL;
        g2.drawRect(x - 1, y - 1, w + 2, h + 2);
    }

    private void drawFood(Graphics2D g2) {
        g2.setColor(skin.food);
        for (Point f : foods) {
            g2.fillOval(screenX(f) + 3, screenY(f) + 3, CELL - 6, CELL - 6);
        }
    }

    private void drawPig(Graphics2D g2) {
        if (pigPos == null) {
            return;
        }
        int x = screenX(pigPos);
        int y = screenY(pigPos);
        PigArt.paint(g2, x + 1, y + 1, CELL - 2);
    }

    private void drawSnake(Graphics2D g2) {
        int n = body.size();
        for (int i = n - 1; i >= 0; i--) {
            Point p = body.get(i);
            int x = screenX(p);
            int y = screenY(p);
            if (i == 0) {
                g2.setColor(skin.head);
                g2.fillRoundRect(x + 1, y + 1, CELL - 2, CELL - 2, 8, 8);
                g2.setColor(skin.headEdge);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(x + 1, y + 1, CELL - 2, CELL - 2, 8, 8);
            } else {
                float t = (float) i / Math.max(1, n - 1);
                g2.setColor(blend(skin.bodyFrom, skin.bodyTo, t));
                g2.fillRoundRect(x + 2, y + 2, CELL - 4, CELL - 4, 5, 5);
            }
        }
    }

    private static Color blend(Color from, Color to, float t) {
        float k = Math.max(0f, Math.min(1f, t));
        return new Color(
                (int) (from.getRed() + (to.getRed() - from.getRed()) * k),
                (int) (from.getGreen() + (to.getGreen() - from.getGreen()) * k),
                (int) (from.getBlue() + (to.getBlue() - from.getBlue()) * k));
    }

    private void drawGameOver(Graphics2D g2) {
        int w = getWidth();
        int h = getHeight();
        g2.setColor(skin.overlay);
        g2.fillRect(0, 0, w, h);

        g2.setColor(skin.text);
        g2.setFont(new Font("SansSerif", Font.BOLD, 34));
        drawCentered(g2, "游戏结束", h / 2 - 60);

        g2.setColor(skin.textDim);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 18));
        drawCentered(g2, "最终分数：" + score, h / 2 - 12);
        drawCentered(g2, "按 空格 重新开始", h / 2 + 22);
    }

    private void drawCentered(Graphics2D g2, String text, int y) {
        int x = (getWidth() - g2.getFontMetrics().stringWidth(text)) / 2;
        g2.drawString(text, x, y);
    }
}
