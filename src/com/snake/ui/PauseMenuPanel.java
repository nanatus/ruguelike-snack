package com.snake.ui;

import com.snake.ConfigStore;
import com.snake.GameConfig;
import com.snake.GameScreen;
import com.snake.Skin;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

/**
 * ESC 暂停菜单：一块不透明圆角卡片浮在棋盘上，底下压一层暗色遮罩。
 *
 * 为什么是「卡片」而不是整屏半透明按钮：棋盘本身的颜色很跳，
 * 半透明按钮压在上面等于没有对比度，卡片把棋盘挡掉一块才看得清。
 *
 * 布局：标题 / 分数 / 模式一行说明，下面一列等宽按钮，最下面是音效和速度。
 * 所有坐标都在 {@link #doLayout()} 里算，不套 GridBag + BoxLayout：
 * 那两层嵌套会把按钮一起撑到最长文字的宽度（模式说明有三十多个字），
 * 按钮就被拉成一条条空板。
 *
 * 菜单打开时键盘归这里独占：所有按钮 focusable=false，焦点落在本面板上，
 * 方向键不会被棋盘抢走，蛇也不会在看不见的时候转向。
 */
public final class PauseMenuPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /** 卡片四周留白，卡片本身也再留一圈内边距。 */
    private static final int MARGIN = 22;
    private static final int CARD_PAD_X = 28;
    private static final int CARD_PAD_Y = 22;
    private static final int BTN_W = 280;
    private static final int BTN_GAP = 8;
    private static final int CARD_RADIUS = 18;

    private final GameScreen screen;
    private Skin skin;
    /** 卡片矩形由 doLayout 算出来，paintComponent 直接拿来画。 */
    private final Rectangle card = new Rectangle();

    private final JLabel infoLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel scoreLine = new JLabel("", SwingConstants.CENTER);
    private final JLabel modeLine = new JLabel("", SwingConstants.CENTER);
    private final JLabel keyHint = new JLabel("↑↓ 选择 · 回车 确认 · Esc 继续", SwingConstants.CENTER);

    private final UiKit.FlatButton resumeBtn;
    private final UiKit.FlatButton restartBtn;
    private final UiKit.FlatButton saveMenuBtn;
    private final UiKit.FlatButton abandonBtn;
    private final UiKit.FlatButton settingsBtn;
    private final UiKit.FlatButton skinBtn;
    private final UiKit.FlatButton quitAppBtn;

    private final JCheckBox soundBox = new JCheckBox("音效");
    /** 左边慢右边快，滑块值就是 TICK_LEVELS 的下标。 */
    private final JSlider speedSlider = new JSlider(
            JSlider.HORIZONTAL, 0, GameConfig.TICK_LEVELS.length - 1, 3);
    private final JLabel speedLabel = new JLabel("普通");
    private final JLabel speedTitle = new JLabel("速度");
    /** 音效和速度这一行整体定位，内部仍用 FlowLayout（尺寸给成 preferredSize，结果确定）。 */
    private final JPanel quick = new JPanel();

    private final UiKit.Nav nav = new UiKit.Nav();
    private final String[] modeText = new String[2];
    /** 局是否还在进行中；已结算完就只留「返回」和「退出程序」。 */
    private boolean live = true;
    private boolean speedTuning;

    public PauseMenuPanel(GameScreen screen, Skin skin) {
        this.screen = screen;
        this.skin = skin;

        // 主操作给大一号，剩下的统一一档，整列才对得齐。
        resumeBtn = act("继续游戏", screen::resumeFromMenu, 18, true);
        restartBtn = act("重新开始", this::doRestart, 17, false);
        saveMenuBtn = act("保存并返回主菜单", screen::saveAndGoMenu, 17, false);
        abandonBtn = act("不保存退出", this::doAbandon, 17, false);
        settingsBtn = act("设置…", screen::openSettingsFromPause, 17, false);
        skinBtn = act("皮肤预览", screen::skinPreviewFromPause, 17, false);
        quitAppBtn = act("退出程序", this::doQuitApp, 17, false);

        build();
        wireKeys();
        applySkin(skin);
    }

    private UiKit.FlatButton act(String text, Runnable r, int size, boolean primary) {
        UiKit.FlatButton b = UiKit.button(skin, text, size, primary);
        // 焦点要留在面板上，否则方向键会被按钮的焦点遍历吃掉。
        b.setFocusable(false);
        UiKit.pad(b, 11, 16, 11, 16);
        b.addActionListener(e -> r.run());
        return b;
    }

    private void doRestart() {
        if (UiKit.confirm(this, "重新开始", "当前进度会丢失，确定重新开始吗？")) {
            screen.restart();
        }
    }

    private void doAbandon() {
        if (UiKit.confirm(this, "不保存退出",
                "本局进度不会保留，也不会出现在「继续游戏」里。确定吗？")) {
            screen.abandonAndGoMenu();
        }
    }

    private void doQuitApp() {
        if (UiKit.confirm(this, "退出程序", "本局进度会保存到「继续游戏」。确定退出吗？")) {
            screen.saveAndExitApp();
        }
    }

    private void build() {
        setOpaque(false);
        setLayout(null);

        infoLabel.setFont(UiKit.font(26, Font.BOLD));
        scoreLine.setFont(UiKit.font(15, Font.PLAIN));
        modeLine.setFont(UiKit.font(13, Font.PLAIN));
        keyHint.setFont(UiKit.font(12, Font.PLAIN));
        speedTitle.setFont(UiKit.font(14, Font.PLAIN));
        speedLabel.setFont(UiKit.font(14, Font.PLAIN));

        for (JComponent c : new JComponent[]{
                infoLabel, scoreLine, modeLine, keyHint, resumeBtn, restartBtn,
                saveMenuBtn, abandonBtn, settingsBtn, skinBtn, quitAppBtn, quick}) {
            add(c);
        }

        quick.setOpaque(false);
        quick.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 10, 0));
        speedSlider.setPreferredSize(new Dimension(170, 24));
        speedSlider.setMaximumSize(new Dimension(200, 28));
        speedSlider.setFocusable(false);
        quick.add(soundBox);
        quick.add(speedTitle);
        quick.add(speedSlider);
        quick.add(speedLabel);

        soundBox.addActionListener(e -> ConfigStore.get().setSoundOn(soundBox.isSelected()));
        speedSlider.addChangeListener(e -> {
            int ms = GameConfig.TICK_LEVELS[speedSlider.getValue()];
            speedLabel.setText(GameConfig.speedName(ms));
            if (speedTuning) {
                screen.applyLiveSpeed(ms);
            }
        });

        for (UiKit.FlatButton b : allButtons()) {
            nav.add(b);
        }
    }

    private UiKit.FlatButton[] allButtons() {
        return new UiKit.FlatButton[]{
                resumeBtn, restartBtn, saveMenuBtn, abandonBtn,
                settingsBtn, skinBtn, quitAppBtn};
    }

    private void wireKeys() {
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int k = e.getKeyCode();
                switch (k) {
                    case KeyEvent.VK_ESCAPE:
                    case KeyEvent.VK_SPACE:
                        screen.resumeFromMenu();
                        break;
                    case KeyEvent.VK_UP:
                    case KeyEvent.VK_W:
                        step(-1);
                        break;
                    case KeyEvent.VK_DOWN:
                    case KeyEvent.VK_S:
                        step(1);
                        break;
                    case KeyEvent.VK_ENTER:
                        nav.activate();
                        break;
                    default:
                        return;
                }
                e.consume();
            }
        });
    }

    /** 上下移动时跳过禁用的按钮，停在灰色项上回车没反应是很烦的。 */
    private void step(int delta) {
        int n = nav.size();
        for (int i = 0; i < n; i++) {
            nav.step(delta);
            JButton c = nav.current();
            if (c != null && c.isEnabled()) {
                return;
            }
        }
    }

    /**
     * 焦点给面板自己。
     *
     * 按钮全是 focusable=false，{@link UiKit.Nav} 内部对按钮调
     * requestFocusInWindow 必然无效，所以必须在这里显式要一次面板的焦点。
     */
    public void focusFirst() {
        if (!isVisible()) {
            return;
        }
        if (nav.current() == null || !nav.current().isEnabled()) {
            step(0);
        } else {
            // 第一次打开时 Nav 只记了 index、没套样式，这里补一次，
            // 否则「继续游戏」会一直画成未选中的灰底。
            nav.reselect();
        }
        requestFocusInWindow();
    }

    public void update(int score, int length, GameConfig config, String skinName,
                       long elapsedSec, boolean live) {
        this.live = live;
        infoLabel.setText(live ? "暂 停" : "本局已结束");
        scoreLine.setText("分数 " + score + "  ·  长度 " + length
                + "  ·  本局 " + fmtTime(elapsedSec));
        // 模式说明最长，先存原文，doLayout 里再按卡片宽度截断。
        modeText[0] = config.summary() + " · " + skinName;
        modeLine.setText(modeText[0]);
        resumeBtn.setText("继续游戏");

        int idx = GameConfig.speedIndexOfOrDefault(config.tickMs);
        if (speedSlider.getValue() != idx) {
            speedSlider.setValue(idx);
        }
        speedLabel.setText(GameConfig.speedName(config.tickMs));
        soundBox.setSelected(ConfigStore.get().soundOn());

        // 已结算完的这一局没有「继续」可言，但重开和回主菜单仍然要给，
        // 否则玩家只能杀进程才能离开这个画面。
        restartBtn.setEnabled(true);
        resumeBtn.setEnabled(live && !screen.panel().isGameOver());
        // 已结算时把「保存并返回」变成纯粹的「返回主菜单」：
        // 这时 finishRun 已经是空操作，不会重复计分或写存档。
        saveMenuBtn.setText(live ? "保存并返回主菜单" : "返回主菜单");
        saveMenuBtn.setEnabled(true);

        abandonBtn.setEnabled(live);
        settingsBtn.setEnabled(live);
        skinBtn.setEnabled(live);
        speedSlider.setEnabled(live);
        soundBox.setEnabled(live);
        // 局中调速要立刻生效（不落盘）；结算后滑块是灰的，这条路径也就走不到。
        speedTuning = live;

        revalidate();
        repaint();
    }

    public static String fmtTime(long sec) {
        long s = Math.max(0, sec);
        long h = s / 3600;
        long m = (s % 3600) / 60;
        if (h > 0) {
            return String.format("%d:%02d:%02d", h, m, s % 60);
        }
        return String.format("%02d:%02d", m, s);
    }

    public void applySkin(Skin s) {
        this.skin = s;
        infoLabel.setForeground(s.text);
        scoreLine.setForeground(s.text);
        modeLine.setForeground(s.textDim);
        keyHint.setForeground(s.textDim);
        soundBox.setForeground(s.text);
        speedTitle.setForeground(s.textDim);
        speedLabel.setForeground(s.text);
        speedSlider.setForeground(s.accent);
        speedSlider.setOpaque(false);
        UiKit.styleCheck(s, soundBox);
        for (UiKit.FlatButton b : allButtons()) {
            b.applySkin(s);
        }
        revalidate();
        repaint();
    }

    // ---- 布局 ----

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(Math.max(BTN_W, quick.getPreferredSize().width) + 2 * CARD_PAD_X,
                520);
    }

    @Override
    public void doLayout() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        UiKit.FlatButton[] btns = allButtons();

        int availW = Math.max(0, w - 2 * MARGIN);
        int btnH = 0;
        for (UiKit.FlatButton b : btns) {
            btnH = Math.max(btnH, b.getPreferredSize().height);
        }
        int titleH = infoLabel.getPreferredSize().height;
        int scoreH = scoreLine.getPreferredSize().height;
        int modeH = modeLine.getPreferredSize().height;
        int hintH = keyHint.getPreferredSize().height;
        Dimension quickD = quick.getPreferredSize();

        // 卡片内边距里的内容宽度：按钮固定宽，另外三项谁宽听谁的，
        // 再用可用宽度封顶，免得小窗口下卡片被撑出去。
        int contentW = Math.max(BTN_W, quickD.width);
        contentW = Math.min(availW, Math.max(contentW, 0));
        int cardW = contentW + 2 * CARD_PAD_X;
        cardW = Math.min(cardW, w);

        // 纵向：间距不够时先收按钮间距，再收各段留白，最后才允许卡片被截。
        int gap = BTN_GAP;
        int pad = 18;
        int cardH = fit(btns.length, btnH, gap, pad, titleH, scoreH, modeH, hintH, quickD.height);
        if (cardH + 2 * MARGIN > h) {
            gap = 2;
            pad = 10;
            cardH = fit(btns.length, btnH, gap, pad, titleH, scoreH, modeH, hintH, quickD.height);
        }
        int cardH2 = cardH + 2 * CARD_PAD_Y;
        int cardX = (w - cardW) / 2;
        int cardY = Math.max(0, (h - cardH2) / 2);
        card.setBounds(cardX, cardY, cardW, cardH2);

        int cx = cardX + CARD_PAD_X;
        int cw = cardW - 2 * CARD_PAD_X;
        int y = cardY + CARD_PAD_Y;

        infoLabel.setBounds(cx, y, cw, titleH);
        y += titleH + 4;
        scoreLine.setBounds(cx, y, cw, scoreH);
        y += scoreH + 2;
        // 模式说明太长就截断，不能反过来把卡片撑成一条横幅。
        fitText(modeLine, cw);
        modeLine.setBounds(cx, y, cw, modeH);
        y += modeH + pad;

        int btnX = cx + (cw - BTN_W) / 2;
        for (UiKit.FlatButton b : btns) {
            b.setBounds(btnX, y, BTN_W, btnH);
            y += btnH + gap;
        }
        y -= gap;

        y += pad;
        quick.setBounds(cx, y, cw, quickD.height);
        y += quickD.height + 6;
        fitText(keyHint, cw);
        keyHint.setBounds(cx, Math.min(y, cardY + cardH2 - hintH), cw, hintH);
    }

    private static int fit(int n, int btnH, int gap, int pad,
                           int titleH, int scoreH, int modeH, int hintH, int quickH) {
        return titleH + 4 + scoreH + 2 + modeH + pad
                + n * btnH + (n - 1) * gap
                + pad + quickH + 6 + hintH;
    }

    /** 标签太长就截成省略号，宁可少几个字也不要顶出卡片。 */
    private void fitText(JLabel label, int maxW) {
        String t = label.getText();
        if (t == null || t.isEmpty() || maxW <= 0) {
            return;
        }
        FontMetrics fm = label.getFontMetrics(label.getFont());
        if (fm.stringWidth(t) <= maxW) {
            return;
        }
        StringBuilder sb = new StringBuilder(t);
        while (sb.length() > 1 && fm.stringWidth(sb + "…") > maxW) {
            sb.setLength(sb.length() - 1);
        }
        label.setText(sb + "…");
    }

    // ---- 绘制 ----

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(skin == null ? new Color(0, 0, 0, 170) : skin.overlay);
        g2.fillRect(0, 0, getWidth(), getHeight());
        if (card.width > 0 && card.height > 0) {
            // 一层偏移的暗色当投影，卡片才不会像贴纸一样浮在棋盘上。
            g2.setColor(new Color(0, 0, 0, 70));
            g2.fillRoundRect(card.x + 3, card.y + 5, card.width, card.height,
                    CARD_RADIUS, CARD_RADIUS);
            g2.setColor(skin.panelBg);
            g2.fillRoundRect(card.x, card.y, card.width, card.height,
                    CARD_RADIUS, CARD_RADIUS);
            g2.setColor(skin.accent);
            g2.drawRoundRect(card.x, card.y, card.width - 1, card.height - 1,
                    CARD_RADIUS, CARD_RADIUS);
        }
        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public Color getBackground() {
        return skin == null ? super.getBackground() : skin.bg;
    }
}
