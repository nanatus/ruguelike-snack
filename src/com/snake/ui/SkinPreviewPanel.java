package com.snake.ui;

import com.snake.ConfigStore;
import com.snake.GameConfig;
import com.snake.GamePanel;
import com.snake.Navigator;
import com.snake.Skin;
import com.snake.Skins;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * 皮肤预览。
 *
 * 关键点：预览里放的是一只真实的 {@link GamePanel}（demo 模式），
 * 所以看到的就是游戏里实际的样子，不用为预览另写一套绘制代码，
 * 以后改了渲染这里也自动跟着对。
 */
public final class SkinPreviewPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int PREVIEW_W = 460;
    private static final int PREVIEW_H = 300;

    private final Navigator nav;
    private final GamePanel preview;
    private final JLabel nameLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel descLabel = new JLabel("", SwingConstants.CENTER);
    private final UiKit.FlatButton prevBtn;
    private final UiKit.FlatButton nextBtn;
    private final UiKit.FlatButton applyBtn;
    private int index;

    /** 模态对话框模式下，关闭动作由外部（对话框自己）接管。 */
    private final boolean modal;
    /** 「使用这套」和 Esc 之后要做什么：独立页面切回主菜单，对话框则 dispose。 */
    private Runnable onClose;

    public SkinPreviewPanel(Navigator nav, Skin skin) {
        this(nav, skin, false);
    }

    private SkinPreviewPanel(Navigator nav, Skin skin, boolean modal) {
        this.nav = nav;
        this.modal = modal;

        // 预览固定用一个中等尺寸的经典棋盘：既有边界感，又不会因为太长看不清蛇。
        GameConfig c = new GameConfig();
        c.mode = GameConfig.Mode.CLASSIC;
        c.cols = 23;
        c.rows = 15;
        c.wrapWalls = true;
        c.tickMs = 140;
        c.foodCount = 2;
        this.preview = new GamePanel(ConfigStore.get().background());
        this.prevBtn = UiKit.button(skin, "上一套", 14, false);
        this.nextBtn = UiKit.button(skin, "下一套", 14, false);
        this.applyBtn = UiKit.button(skin, "使用这套", 14, true);
        this.onClose = new Runnable() {
            @Override
            public void run() {
                nav.show(Navigator.CARD_MENU);
            }
        };

        build(skin);
        select(Skins.all().indexOf(skin));
        preview.configure(c, skin);
        preview.startDemo(true);
        wireVisibility();
    }

    private void build(Skin skin) {
        setOpaque(true);
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel stage = new JPanel(new BorderLayout());
        stage.setOpaque(false);
        stage.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(skin.panelBorder),
                BorderFactory.createEmptyBorder(1, 1, 1, 1)));
        preview.setPreferredSize(new Dimension(PREVIEW_W, PREVIEW_H));
        preview.setFocusable(false);
        stage.add(preview, BorderLayout.CENTER);
        add(stage, BorderLayout.CENTER);

        JPanel south = new JPanel();
        south.setOpaque(false);
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        nameLabel.setFont(UiKit.font(19, UiKit.FONT_BOLD_));
        descLabel.setFont(UiKit.font(13, UiKit.FONT_PLAIN_));
        south.add(nameLabel);
        south.add(Box.createVerticalStrut(4));
        south.add(descLabel);
        south.add(Box.createVerticalStrut(12));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        btns.setOpaque(false);
        btns.add(prevBtn);
        btns.add(nextBtn);
        btns.add(applyBtn);
        btns.setAlignmentX(CENTER_ALIGNMENT);
        south.add(btns);
        add(south, BorderLayout.SOUTH);

        prevBtn.addActionListener(e -> step(-1));
        nextBtn.addActionListener(e -> step(1));
        applyBtn.addActionListener(e -> doApply());

        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                    case KeyEvent.VK_A:
                        step(-1);
                        e.consume();
                        break;
                    case KeyEvent.VK_RIGHT:
                    case KeyEvent.VK_D:
                        step(1);
                        e.consume();
                        break;
                    case KeyEvent.VK_ENTER:
                    case KeyEvent.VK_SPACE:
                        doApply();
                        e.consume();
                        break;
                    case KeyEvent.VK_ESCAPE:
                        back();
                        e.consume();
                        break;
                    default:
                        break;
                }
            }
        });
    }

    private void doApply() {
        nav.applySkin(Skins.all().get(index).id);
        onClose.run();
    }

    private void back() {
        onClose.run();
    }

    private void step(int delta) {
        int n = Skins.all().size();
        select(((index + delta) % n + n) % n);
    }

    private void select(int i) {
        int n = Skins.all().size();
        index = ((i % n) + n) % n;
        Skin s = Skins.all().get(index);
        nameLabel.setText(s.name);
        descLabel.setText(s.description);
        preview.setSkin(s);
        repaint();
    }

    public void applySkin(Skin s) {
        setBackground(s.bg);
        nameLabel.setForeground(s.text);
        descLabel.setForeground(s.textDim);
        prevBtn.applySkin(s);
        nextBtn.applySkin(s);
        applyBtn.applySkin(s);
        repaint();
    }

    /**
     * 切走时停掉预览的定时器，别在看不见的地方耗 CPU。
     * 用 ComponentListener 而不是覆写 componentHidden：JPanel 本身没有那两个方法。
     */
    private void wireVisibility() {
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                if (!modal) {
                    preview.startDemo(true);
                }
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                preview.stop();
            }
        });
    }

    /**
     * 局中打开皮肤预览用的模态版本。
     * 不能走卡片：那样会把游戏页面换掉，这一局就找不回来了。
     */
    public static void showDialog(Navigator nav, Skin skin) {
        JDialog dlg = new JDialog(nav.window(), "皮肤预览", JDialog.ModalityType.APPLICATION_MODAL);
        final SkinPreviewPanel p = new SkinPreviewPanel(nav, skin, true);
        p.onClose = new Runnable() {
            @Override
            public void run() {
                dlg.dispose();
            }
        };
        dlg.setContentPane(p);
        UiKit.place(dlg, PREVIEW_W + 80, PREVIEW_H + 150);
        dlg.setVisible(true);
        p.preview.stop();
    }
}
