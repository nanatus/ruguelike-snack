package com.snake;

import com.snake.ui.PauseMenuPanel;
import com.snake.ui.PigDialoguePanel;
import com.snake.ui.ShopPanel;
import com.snake.ui.Toast;
import com.snake.ui.UiKit;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * 游戏窗口：顶部状态栏 + 中部游戏区，ESC 弹出暂停菜单。
 *
 * 这个类同时负责「局与局之间」的事：写入存档、计分、判定成就。
 * 所有退出路径（返回主菜单、退出程序、点窗口 X、正常死亡）都收敛到
 * {@link #finishRun} 一个方法，避免漏掉某一条而丢存档。
 */
public final class GameScreen extends JPanel implements GamePanel.Listener {

    private static final long serialVersionUID = 1L;

    private final Navigator nav;
    private final ConfigStore store;
    /** 游戏区和暂停菜单叠在一起，这样暂停时底下还能看见蛇。 */
    private final JPanel root = new JPanel(new java.awt.GridBagLayout());
    private final GamePanel panel;
    private final PauseMenuPanel pauseMenu;
    private final PigDialoguePanel dialoguePanel;
    private final ShopPanel shopPanel;
    private final Toast toast;
    private final JPanel bar = new JPanel(new BorderLayout(16, 0));

    private final JLabel scoreLabel = new JLabel();
    private final JLabel lengthLabel = new JLabel();
    private final JLabel modeLabel = new JLabel();
    private final JLabel hintLabel = new JLabel();

    private GameConfig config = new GameConfig();
    /** 本局已写入榜单的分数，续局时用来避免重复记录。 */
    private int recordedScore = -1;
    private boolean resumed;
    private boolean menuOpen;
    private boolean dialogueOpen;
    private boolean shopOpen;
    /** 这一局是否已经结算过，防止死亡后再退出重复结算。 */
    private boolean settled;

    public GameScreen(Navigator nav, Skin skin) {
        this.nav = nav;
        this.store = ConfigStore.get();
        this.panel = new GamePanel(store.background());
        this.pauseMenu = new PauseMenuPanel(this, skin);
        this.dialoguePanel = new PigDialoguePanel(skin);
        this.shopPanel = new ShopPanel(skin);
        this.toast = new Toast(skin);

        dialoguePanel.setOnFinished(this::finishDialogue);
        shopPanel.setOnClose(this::closeShop);

        setLayout(new BorderLayout());
        buildStatusBar(skin);

        root.setBackground(skin.bg);
        root.setOpaque(true);
        java.awt.GridBagConstraints gc = new java.awt.GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.weighty = 1;
        gc.fill = java.awt.GridBagConstraints.BOTH;
        root.add(panel, gc);
        root.add(pauseMenu, gc);
        root.add(dialoguePanel, gc);
        root.add(shopPanel, gc);
        // 必须显式压到最上层。Swing 的绘制顺序是「下标从大到小」，
        // 也就是 add 得越早画得越晚、越靠上：照上面的写法棋盘会盖住整个浮层。
        // setComponentZOrder(c, 0) 的 0 就是最上层，不依赖 add 顺序的记法。
        root.setComponentZOrder(pauseMenu, 0);
        pauseMenu.setVisible(false);
        dialoguePanel.setVisible(false);
        shopPanel.setVisible(false);

        add(bar, BorderLayout.NORTH);
        add(root, BorderLayout.CENTER);

        panel.setListener(this);

        // 切到别的卡片时暂停：玩家在主菜单翻榜单，这一局不该在背后继续跑。
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                if (!menuOpen && !dialogueOpen && !shopOpen) {
                    panel.requestFocusInWindow();
                }
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                if (dialogueOpen) {
                    panel.unlockShop();
                }
                hideOverlays();
                panel.setPaused(true);
            }
        });
        applySkin(skin);
    }

    private void buildStatusBar(Skin skin) {
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, skin.barEdge),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));

        scoreLabel.setFont(UiKit.font(18, Font.BOLD));

        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 0));
        center.setOpaque(false);
        for (JLabel l : new JLabel[]{lengthLabel, modeLabel}) {
            l.setFont(UiKit.font(14, Font.PLAIN));
            center.add(l);
        }

        hintLabel.setFont(UiKit.font(14, Font.PLAIN));
        hintLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);

        bar.add(scoreLabel, BorderLayout.WEST);
        bar.add(center, BorderLayout.CENTER);
        bar.add(hintLabel, BorderLayout.EAST);
    }

    public void applySkin(Skin s) {
        setBackground(s.bg);
        bar.setBackground(s.barBg);
        root.setBackground(s.bg);
        pauseMenu.applySkin(s);
        dialoguePanel.applySkin(s);
        shopPanel.applySkin(s);
        toast.applySkin(s);
        panel.setSkin(s);
        scoreLabel.setForeground(s.score);
        lengthLabel.setForeground(s.hint);
        modeLabel.setForeground(s.hint);
        hintLabel.setForeground(s.hint);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, s.barEdge),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));
        repaint();
    }

    // ---- 开局 ----

    public void start(GameConfig cfg, Skin skin) {
        hideOverlays();
        config = cfg.deepCopy();
        recordedScore = -1;
        resumed = false;
        settled = false;
        panel.setSkin(skin);
        panel.configure(config, skin);
        menuOpen = false;
        pauseMenu.setVisible(false);
        refreshLabels();
        panel.requestFocusInWindow();
    }

    public void resume(SaveState save) {
        hideOverlays();
        config = save.config.deepCopy();
        recordedScore = save.recordedScore;
        resumed = true;
        settled = false;
        panel.applySaveState(save);
        // 恢复后直接摆出暂停菜单：玩家是从主菜单点进来的，
        // 不给反应时间就开始跑，必然撞死。
        menuOpen = true;
        pauseMenu.setVisible(true);
        root.setComponentZOrder(pauseMenu, 0);
        refreshLabels();
        focusPauseMenu();

        if (save.config.challenge == GameConfig.Challenge.DAILY
                && save.config.seed != ChallengeFactory.todayEpochDay()) {
            showToast("今日挑战已过期，已按普通对局继续");
        } else {
            showToast("已恢复上次的对局 · 分数 " + save.score);
        }
    }

    // ---- GamePanel.Listener ----

    @Override
    public void onScoreChanged(int score, int length) {
        refreshLabels();
    }

    @Override
    public void onPauseRequested(boolean paused) {
        if (paused && !menuOpen && !dialogueOpen && !shopOpen) {
            openMenu();
        }
    }

    @Override
    public void onPigEncounter() {
        if (dialogueOpen || shopOpen || menuOpen || settled) {
            return;
        }
        dialogueOpen = true;
        panel.setPaused(true);
        root.setComponentZOrder(dialoguePanel, 0);
        dialoguePanel.open();
        refreshLabels();
        focusDialogue();
    }

    @Override
    public void onShopRequested() {
        if (shopOpen) {
            closeShop();
            return;
        }
        if (dialogueOpen || menuOpen || settled || panel.isGameOver()) {
            return;
        }
        if (!panel.isShopUnlocked()) {
            showToast("商店尚未解锁 · 先找到猪头");
            return;
        }
        shopOpen = true;
        panel.setPaused(true);
        root.setComponentZOrder(shopPanel, 0);
        shopPanel.open(panel.length());
        focusShop();
    }

    @Override
    public void onEscape() {
        if (shopOpen) {
            closeShop();
            return;
        }
        if (dialogueOpen) {
            return;
        }
        if (menuOpen) {
            resumeFromMenu();
        } else {
            openMenu();
        }
    }

    @Override
    public void onGameOver(int score, int length, boolean filled) {
        // 正常死亡：结算并清掉存档，玩家不该从一局死局续上。
        finishRun(true);
        int rank = store.highScores().rank(score);
        if (rank > 0 && rank <= 3) {
            showToast("新纪录！榜单第 " + rank + " 名");
        }
    }

    // ---- 暂停菜单 ----

    public void openMenu() {
        if (menuOpen || dialogueOpen || shopOpen) {
            return;
        }
        menuOpen = true;
        panel.setPaused(true);
        root.setComponentZOrder(pauseMenu, 0);
        pauseMenu.setVisible(true);
        refreshLabels();
        focusPauseMenu();
    }

    /**
     * 焦点要等浮层真正显示之后再抢。
     *
     * setVisible(true) 只是标了个脏，同一轮事件里 requestFocusInWindow 会因为
     * 组件还没有 showing 而静默失败，焦点就留在棋盘上——方向键接着被
     * GamePanel 吞掉，菜单里的键盘导航整个失效。
     */
    private void focusPauseMenu() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                pauseMenu.focusFirst();
            }
        });
    }

    private void focusDialogue() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                dialoguePanel.requestFocusInWindow();
            }
        });
    }

    private void focusShop() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                shopPanel.requestFocusInWindow();
            }
        });
    }

    private void finishDialogue() {
        if (!dialogueOpen) {
            return;
        }
        dialogueOpen = false;
        dialoguePanel.setVisible(false);
        panel.unlockShop();
        panel.setPaused(false);
        panel.requestFocusInWindow();
        refreshLabels();
        showToast("群友商店已解锁 · 按 B 打开");
    }

    private void closeShop() {
        if (!shopOpen) {
            return;
        }
        shopOpen = false;
        shopPanel.setVisible(false);
        panel.setPaused(false);
        panel.requestFocusInWindow();
        refreshLabels();
    }

    private void hideOverlays() {
        dialogueOpen = false;
        shopOpen = false;
        dialoguePanel.setVisible(false);
        shopPanel.setVisible(false);
    }

    public void resumeFromMenu() {
        menuOpen = false;
        pauseMenu.setVisible(false);
        panel.setPaused(false);
        panel.requestFocusInWindow();
    }

    public boolean isMenuOpen() {
        return menuOpen;
    }

    public boolean isDialogueOpen() {
        return dialogueOpen;
    }

    public boolean isShopOpen() {
        return shopOpen;
    }

    public boolean isRunning() {
        return !menuOpen && !settled && !panel.isGameOver();
    }

    public GamePanel panel() {
        return panel;
    }

    public GameConfig currentConfig() {
        return config;
    }

    public ConfigStore store() {
        return store;
    }

    public Navigator navigator() {
        return nav;
    }

    // ---- 退出与存档 ----

    /** 局中返回主菜单：计分并写存档。 */
    public void saveAndGoMenu() {
        finishRun(false);
        panel.stop();
        nav.show(Navigator.CARD_MENU);
    }

    /** 局中直接退出程序。 */
    public void saveAndExitApp() {
        finishRun(false);
        panel.stop();
        System.exit(0);
    }

    /** 局中放弃这一局。 */
    public void abandonAndGoMenu() {
        finishRun(false);
        store.clearSave();
        panel.stop();
        nav.show(Navigator.CARD_MENU);
    }

    public void restart() {
        hideOverlays();
        recordedScore = -1;
        resumed = false;
        settled = false;
        menuOpen = false;
        pauseMenu.setVisible(false);
        panel.reset();
        refreshLabels();
        panel.requestFocusInWindow();
    }

    /** 窗口失去焦点时自动暂停：切出去时不可能还指望玩家在操作。 */
    public void onWindowDeactivated() {
        if (menuOpen || dialogueOpen || shopOpen || !panel.isShowing() || settled) {
            return;
        }
        openMenu();
    }

    /** 窗口真正关闭前调用：局中先结算并存档，再停掉定时器。 */
    public void onWindowClosing() {
        if (dialogueOpen) {
            // 关窗来不及看完对话也直接解锁，避免猪头消失后商店永久锁死。
            dialogueOpen = false;
            dialoguePanel.setVisible(false);
            panel.unlockShop();
        }
        if (isRunning()) {
            finishRun(false);
        }
        panel.stop();
    }

    /**
     * 收尾一局。所有退出路径都走这里。
     *
     * @param dead true 表示正常死亡（清存档），false 表示局中退出（留存档）
     */
    private void finishRun(boolean dead) {
        if (settled) {
            return;
        }
        settled = true;
        int score = panel.score();
        int length = panel.length();
        boolean filled = panel.isFilled();
        boolean classic = config.mode == GameConfig.Mode.CLASSIC;

        // 局中退出要计分，但续局后再退出会记第二次同一局，
        // 所以只有严格超过 recordedScore 才追加。
        if (score > 0 && score > recordedScore) {
            ScoreRecord rec = new ScoreRecord(score, length, config, panel.skin().name,
                    panel.elapsedSec(), System.currentTimeMillis(), !dead);
            if (store.highScores().add(rec)) {
                store.saveScores();
            }
            recordedScore = score;
        }

        store.recordRun(score, length);
        if (config.challenge == GameConfig.Challenge.DAILY) {
            store.touchDaily(ChallengeFactory.todayEpochDay());
        }
        checkAchievements(score, length, filled, classic);

        if (dead) {
            store.clearSave();
        } else {
            store.writeSave(panel.captureSaveState(recordedScore, resumed));
        }
    }

    private void checkAchievements(int score, int length, boolean filled, boolean classic) {
        Achievements.Progress p = new Achievements.Progress();
        p.lastScore = score;
        p.lastLength = length;
        p.maxLengthThisRun = config.maxLength;
        p.lastClassic = classic;
        p.lastFilled = filled;
        p.gamesPlayed = store.gamesPlayed();
        p.totalScore = store.totalScore();
        p.skinsUsed = store.skinsUsed();
        p.dailyStreak = store.dailyStreak();
        p.resumed = resumed;
        List<Achievement> fresh = store.achievements().check(p);
        store.saveAchievements();
        for (Achievement a : fresh) {
            showToast("成就解锁 · " + a.name);
        }
    }

    /** 局中打开设置对话框，对话框关掉后这一局照旧。 */
    public void openSettingsFromPause() {
        nav.openSettings();
        panel.setSkin(ConfigStore.get().skin());
        refreshLabels();
    }

    /** 局中预览皮肤：必须是模态的，否则会把游戏页面换掉，这一局就找不回来了。 */
    public void skinPreviewFromPause() {
        com.snake.ui.SkinPreviewPanel.showDialog(nav, ConfigStore.get().skin());
        panel.setSkin(ConfigStore.get().skin());
        refreshLabels();
    }

    /** 暂停菜单里的调速：立刻生效，不落盘（局内微调不该改掉玩家的默认设置）。 */
    public void applyLiveSpeed(int tickMs) {
        config.tickMs = tickMs;
        panel.setTickInterval(tickMs);
        refreshLabels();
    }

    public void showToast(String text) {
        toast.show(this, text);
    }

    private void refreshLabels() {
        scoreLabel.setText("分数 " + panel.score());
        lengthLabel.setText("长度 " + panel.length() + " / " + config.maxLength
                + (panel.isFilled() ? "（已满）" : ""));
        modeLabel.setText(config.summary() + " · " + panel.skin().name);
        String hint = "方向键 / WASD 移动 · Esc 菜单 · 空格 暂停";
        if (panel.isShopUnlocked()) {
            hint += " · B 商店";
        }
        hintLabel.setText(hint);
        pauseMenu.update(panel.score(), panel.length(), config,
                panel.skin().name, panel.elapsedSec(), !settled);
    }

    @Override
    public String toString() {
        return "GameScreen[" + config.summary() + "]";
    }

    /** 状态栏底色，供换肤时复查用。 */
    public Color barColor() {
        return bar.getBackground();
    }
}
