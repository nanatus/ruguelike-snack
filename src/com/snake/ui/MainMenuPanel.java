package com.snake.ui;

import com.snake.BackgroundImage;
import com.snake.ConfigStore;
import com.snake.Navigator;
import com.snake.SaveState;
import com.snake.Skin;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * 主菜单。
 *
 * 布局：正中间一列主选项（22pt），左下角一组次要选项（18pt，正好小两号），
 * 右下角一行累计统计，顶部是游戏名。背景只有玩家导入的图片，没有动态演示。
 *
 * 所有子组件的坐标都在 {@link #doLayout()} 里算，不套 GridBag/BoxLayout：
 * 这两层嵌套容器算出来的宽度会随字体和平台变化而飘，按钮容易被挤扁或者撑满，
 * 这里直接按 preferredSize 摆，位置和大小都是确定的。
 *
 * 按钮全部 focusable=false，键盘统一由本面板处理，
 * 否则方向键会被按钮的焦点遍历 Action 吃掉，菜单就不好走了。
 */
public final class MainMenuPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final Navigator nav;
    private final JPanel layer = new JPanel(null);
    private final ImageLayer imageLayer = new ImageLayer();
    private final JComponent scrim = new JComponent() {
        private static final long serialVersionUID = 1L;
    };

    private final JLabel title = new JLabel("贪吃蛇", SwingConstants.CENTER);
    private final JLabel subtitle =
            new JLabel("经典 · 无尽 · 每日挑战", SwingConstants.CENTER);
    private final JLabel hint = new JLabel("↑↓ 选择 · ←→ 切换分组 · 回车 确认 · ESC 退出",
            SwingConstants.CENTER);

    private final UiKit.FlatButton continueBtn;
    private final JLabel continueInfo;
    private final UiKit.FlatButton newGameBtn;
    private final UiKit.FlatButton skinBtn;
    private final UiKit.FlatButton settingsBtn;
    private final UiKit.FlatButton quitBtn;
    private final UiKit.FlatButton dailyBtn;
    private final UiKit.FlatButton achvBtn;
    private final UiKit.FlatButton scoreBtn;
    private final JLabel statsLabel;

    private final UiKit.Nav mainNav = new UiKit.Nav();
    private final UiKit.Nav sideNav = new UiKit.Nav();
    private boolean sideActive;

    private Skin skin;

    public MainMenuPanel(Navigator nav) {
        this.nav = nav;
        this.skin = nav.skin();

        continueBtn = menuBtn("继续游戏", () -> doContinue());
        newGameBtn = menuBtn("新的游戏", () -> nav.show(Navigator.CARD_SETUP));
        skinBtn = menuBtn("皮肤预览", () -> nav.show(Navigator.CARD_SKINS));
        settingsBtn = menuBtn("设置", () -> nav.openSettings());
        quitBtn = menuBtn("退出", () -> confirmQuit());

        dailyBtn = sideBtn("每日挑战", () -> nav.show(Navigator.CARD_DAILY));
        achvBtn = sideBtn("成就", () -> nav.show(Navigator.CARD_ACHIEVEMENTS));
        scoreBtn = sideBtn("排行榜", () -> nav.show(Navigator.CARD_SCORES));

        continueInfo = UiKit.dim(skin, "", 13);
        statsLabel = UiKit.dim(skin, "", 12);

        setLayout(new BorderLayout());
        setBackground(skin.bg);
        layer.setOpaque(false);
        layer.add(imageLayer);
        layer.add(scrim);
        for (JComponent c : new JComponent[]{
            title, subtitle, hint, statsLabel, continueInfo,
            continueBtn, newGameBtn, skinBtn, settingsBtn, quitBtn,
            dailyBtn, achvBtn, scoreBtn}) {
            layer.add(c);
        }
        add(layer, BorderLayout.CENTER);

        wireKeys();
        applySkin(skin);
        refresh();
    }

    private UiKit.FlatButton menuBtn(String text, Runnable action) {
        UiKit.FlatButton b = UiKit.menuButton(skin, text);
        b.setFocusable(false);
        b.addActionListener(e -> action.run());
        return b;
    }

    private UiKit.FlatButton sideBtn(String text, Runnable action) {
        UiKit.FlatButton b = UiKit.smallMenuButton(skin, text);
        b.setFocusable(false);
        b.addActionListener(e -> {
            sideActive = true;
            syncSelection();
            action.run();
        });
        return b;
    }

    private void wireKeys() {
        UiKit.FlatButton[] main = {continueBtn, newGameBtn, skinBtn, settingsBtn, quitBtn};
        for (int i = 0; i < main.length; i++) {
            mainNav.add(main[i]);
            final int idx = i;
            main[i].addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    sideActive = false;
                    mainNav.focus(idx);
                }
            });
        }
        UiKit.FlatButton[] side = {dailyBtn, achvBtn, scoreBtn};
        for (int i = 0; i < side.length; i++) {
            sideNav.add(side[i]);
            final int idx = i;
            side[i].addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    sideActive = true;
                    sideNav.focus(idx);
                }
            });
        }

        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!handleKey(e.getKeyCode())) {
                    return;
                }
                e.consume();
            }
        });
    }

    private boolean handleKey(int k) {
        switch (k) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                sideActive = false;
                mainNav.step(-1);
                return true;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                sideActive = false;
                mainNav.step(1);
                return true;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                sideActive = true;
                sideNav.step(-1);
                return true;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                sideActive = true;
                sideNav.step(1);
                return true;
            case KeyEvent.VK_ENTER:
            case KeyEvent.VK_SPACE:
                (sideActive ? sideNav : mainNav).activate();
                return true;
            case KeyEvent.VK_B:
                nav.show(Navigator.CARD_SETUP);
                return true;
            case KeyEvent.VK_K:
                nav.show(Navigator.CARD_SKINS);
                return true;
            case KeyEvent.VK_T:
                nav.openSettings();
                return true;
            case KeyEvent.VK_G:
                nav.show(Navigator.CARD_DAILY);
                return true;
            case KeyEvent.VK_L:
                nav.show(Navigator.CARD_SCORES);
                return true;
            case KeyEvent.VK_C:
                nav.show(Navigator.CARD_ACHIEVEMENTS);
                return true;
            case KeyEvent.VK_ESCAPE:
                confirmQuit();
                return true;
            default:
                return false;
        }
    }

    private void confirmQuit() {
        if (UiKit.confirm(this, "退出", "确定要退出贪吃蛇吗？")) {
            System.exit(0);
        }
    }

    private void doContinue() {
        SaveState s = ConfigStore.get().loadSave();
        if (s == null) {
            // hasSave() 为真但解析失败（存档被截断或版本不符）时也会走到这里。
            ConfigStore.get().clearSave();
            UiKit.error(this, "没有存档", "存档已损坏或版本不符，已清除。");
            refresh();
            return;
        }
        nav.resumeGame(s);
    }

    private void syncSelection() {
        JButton cur = sideActive ? sideNav.current() : mainNav.current();
        for (int i = 0; i < mainNav.size(); i++) {
            JButton b = mainNav.get(i);
            b.setSelected(!sideActive && b == cur);
        }
        for (int i = 0; i < sideNav.size(); i++) {
            JButton b = sideNav.get(i);
            b.setSelected(sideActive && b == cur);
        }
    }

    /** 主菜单每次显示都要刷新：存档有无、统计数字、背景图有没有都会变。 */
    public void refresh() {
        ConfigStore store = ConfigStore.get();
        SaveState save = store.loadSave();
        continueBtn.setVisible(save != null);
        continueInfo.setVisible(save != null);
        if (save != null) {
            continueInfo.setText(save.summary());
            continueBtn.setToolTipText(save.tooltip());
        } else {
            continueInfo.setText("");
            continueBtn.setToolTipText(null);
        }
        statsLabel.setText("累计 " + store.gamesPlayed() + " 局 · 最高 " + store.bestScore()
                + " · 最长 " + store.bestLength() + " 节");
        refreshBackground();
        revalidate();
        repaint();
        mainNav.focus(continueBtn.isVisible() ? 0 : 1);
        syncSelection();
    }

    /** 背景只有导入的图片这一种来源，没有别的了。 */
    private void refreshBackground() {
        com.snake.BackgroundSettings bg = ConfigStore.get().background();
        boolean image = bg.enabledInMenu && bg.hasImage();
        imageLayer.setVisible(image);
        // 遮罩只压在图片上，不然纯色背景会被自己压暗一层。
        scrim.setVisible(image);
        // 暗化程度跟游戏区用同一个设置，玩家调滑块两边一起变。
        applyScrim(bg, skin);
    }

    private void applyScrim(com.snake.BackgroundSettings bg, Skin s) {
        int alpha = 255 - (int) (bg.dimPercent * 2.55);
        scrim.setBackground(new Color(s.bg.getRed(), s.bg.getGreen(), s.bg.getBlue(),
                Math.max(0, Math.min(255, alpha))));
    }

    public void applySkin(Skin s) {
        this.skin = s;
        setBackground(s.bg);
        imageLayer.applySkin(s);
        applyScrim(ConfigStore.get().background(), s);
        scrim.setOpaque(true);
        for (UiKit.FlatButton b : new UiKit.FlatButton[]{
                continueBtn, newGameBtn, skinBtn, settingsBtn, quitBtn,
                dailyBtn, achvBtn, scoreBtn}) {
            b.applySkin(s);
        }
        title.setFont(UiKit.font(40, java.awt.Font.BOLD));
        title.setForeground(s.text);
        subtitle.setFont(UiKit.font(15, java.awt.Font.PLAIN));
        subtitle.setForeground(s.textDim);
        hint.setFont(UiKit.font(13, java.awt.Font.PLAIN));
        hint.setForeground(s.textDim);
        continueInfo.setForeground(s.textDim);
        statsLabel.setForeground(s.textDim);
        // 字体变了 preferredSize 就变了，坐标必须重算。
        revalidate();
        repaint();
    }

    // ---- 布局 ----

    private static final int SIDE = 28;
    /** 主选项至少这么宽：四个 22pt 汉字只有 130px，一列窄条在大窗口里很寒酸。 */
    private static final int MIN_MAIN_W = 240;

    @Override
    public void doLayout() {
        int w = getWidth();
        int h = getHeight();
        layer.setBounds(0, 0, w, h);
        super.doLayout();

        imageLayer.setBounds(0, 0, w, h);
        scrim.setBounds(0, 0, w, h);

        UiKit.FlatButton[] main = visibleMain();

        // 底部那一条（次要选项 / 统计 / 快捷键提示）先量出来，
        // 中间这一竖块只能在剩下的空间里居中，不然小窗口下会和左下角撞上。
        int sideH = maxHeight(sideButtons());
        Dimension st = statsLabel.getPreferredSize();
        Dimension hd = hint.getPreferredSize();
        int bottomH = Math.max(sideH, Math.max(st.height, hd.height)) + SIDE;
        int avail = Math.max(0, h - bottomH - SIDE);

        int titleH = title.getPreferredSize().height;
        int subH = subtitle.getPreferredSize().height;
        int infoH = continueInfo.isVisible() ? continueInfo.getPreferredSize().height : 0;
        int btnW = MIN_MAIN_W;
        int btnH = 0;
        for (UiKit.FlatButton b : main) {
            btnW = Math.max(btnW, b.getPreferredSize().width);
            btnH += b.getPreferredSize().height;
        }

        int solid = titleH + subH + 6 + 18 + infoH + btnH;
        int gap = main.length > 1 ? (avail - solid) / (main.length - 1) : 0;
        gap = Math.max(2, Math.min(10, gap));
        int blockH = solid + gap * Math.max(0, main.length - 1);

        int y = Math.max(SIDE, SIDE + (avail - blockH) / 2);

        title.setBounds(0, y, w, titleH);
        y += titleH + 6;
        subtitle.setBounds(0, y, w, subH);
        y += subH + 18;

        // (w - btnW) / 2 在奇数宽度下会差一个像素，这里按中心点算，保证真的居中。
        int btnX = w / 2 - btnW / 2;
        for (UiKit.FlatButton b : main) {
            int bh = b.getPreferredSize().height;
            b.setBounds(btnX, y, btnW, bh);
            y += bh + gap;
        }
        if (continueInfo.isVisible()) {
            continueInfo.setBounds(0, y - gap, w, infoH);
        }

        // 左下角：次要选项一行排开。
        int sx = SIDE;
        int sy = h - SIDE - sideH;
        for (UiKit.FlatButton b : sideButtons()) {
            if (!b.isVisible()) {
                continue;
            }
            Dimension d = b.getPreferredSize();
            b.setBounds(sx, sy, d.width, d.height);
            sx += d.width + 10;
        }
        int leftEnd = sx - 10;

        // 右下角统计。
        statsLabel.setBounds(w - SIDE - st.width, h - SIDE - st.height, st.width, st.height);
        int rightStart = w - SIDE - st.width;

        // 快捷键提示只在左右两块之间还有地方时才摆；窗口太窄就收起来，
        // 宁可少一行说明，也不能压在按钮或者统计数字上面。
        int room = rightStart - leftEnd;
        hint.setVisible(room >= hd.width);
        if (hint.isVisible()) {
            hint.setBounds(leftEnd + (room - hd.width) / 2, h - SIDE - hd.height,
                    hd.width, hd.height);
        }
    }

    private UiKit.FlatButton[] visibleMain() {
        java.util.List<UiKit.FlatButton> list = new java.util.ArrayList<UiKit.FlatButton>();
        for (UiKit.FlatButton b : mainButtons()) {
            if (b.isVisible()) {
                list.add(b);
            }
        }
        return list.toArray(new UiKit.FlatButton[0]);
    }

    private UiKit.FlatButton[] mainButtons() {
        return new UiKit.FlatButton[]{continueBtn, newGameBtn, skinBtn, settingsBtn, quitBtn};
    }

    private UiKit.FlatButton[] sideButtons() {
        return new UiKit.FlatButton[]{dailyBtn, achvBtn, scoreBtn};
    }

    private static int maxHeight(JComponent[] comps) {
        int max = 0;
        for (JComponent c : comps) {
            if (c.isVisible()) {
                max = Math.max(max, c.getPreferredSize().height);
            }
        }
        return max;
    }

    /** 键盘焦点给面板自己：按钮全部 focusable=false，方向键才不会被吃掉。 */
    public JComponent firstFocusable() {
        return this;
    }

    /** 画玩家导入的背景图。没有图片时不显示，省掉一次无谓的缩放。 */
    private final class ImageLayer extends JComponent {
        private static final long serialVersionUID = 1L;

        ImageLayer() {
            setOpaque(true);
        }

        void applySkin(Skin s) {
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            com.snake.BackgroundSettings bg = ConfigStore.get().background();
            int w = getWidth();
            int h = getHeight();
            BufferedImage img = BackgroundImage.scaled(
                    Math.max(1, w), Math.max(1, h), bg.scale, skin.bg);
            if (img != null) {
                g2.drawImage(img, 0, 0, null);
            } else {
                g2.setColor(skin.bg);
                g2.fillRect(0, 0, w, h);
            }
            g2.dispose();
        }
    }
}
