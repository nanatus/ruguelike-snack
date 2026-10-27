package com.snake.ui;

import com.snake.ConfigStore;
import com.snake.GameConfig;
import com.snake.Navigator;
import com.snake.Skin;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * 设置对话框。四个标签页：游戏、外观、音频、玩法说明。
 *
 * 主菜单和暂停菜单开的是同一个对话框，所以局中改设置不会丢当前进度。
 *
 * 关于「确定 / 取消」：游戏页是暂存区，点确定才写盘；
 * 外观页的皮肤、背景和音频页则是**即时生效**——换肤和调背景需要立刻看到效果，
 * 强行等确定才生效反而更难用。这两类行为在界面上有文字说明。
 */
public final class SettingsDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final Navigator nav;
    private final GameConfig config;
    private final ConfigForm gameForm;
    private final SkinPanel skinPanel;
    private final BackgroundForm backgroundForm;
    private final AudioForm audioForm;
    private final HelpPanel helpPanel;
    private Skin skin;

    private UiKit.FlatButton okBtn;
    private UiKit.FlatButton cancelBtn;
    private UiKit.FlatButton resetBtn;

    private SettingsDialog(Navigator nav, Skin skin) {
        super(nav.window(), "设置", ModalityType.APPLICATION_MODAL);
        this.nav = nav;
        this.skin = skin;
        // 游戏页改的是副本，确定之后才写回真实配置。
        this.config = nav.config().deepCopy();
        this.gameForm = new ConfigForm(config, skin);
        this.skinPanel = new SkinPanel(nav, skin);
        this.backgroundForm = new BackgroundForm(skin);
        this.audioForm = new AudioForm(skin);
        this.helpPanel = new HelpPanel(skin);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(skin.panelBg);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(skin.panelBg);
        tabs.setForeground(skin.text);
        tabs.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        tabs.addTab("游戏", wrap(gameForm));
        tabs.addTab("外观", wrap(stack(skinPanel, backgroundForm)));
        tabs.addTab("音频", wrap(audioForm));
        tabs.addTab("玩法说明", wrap(helpPanel));
        content.add(tabs, BorderLayout.CENTER);
        content.add(buildButtons(), BorderLayout.SOUTH);
        setContentPane(content);

        getRootPane().setDefaultButton(okBtn);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    // Esc 等同于「放弃游戏页改动」，外观和音频本来就已生效，不受影响
                    dispose();
                    e.consume();
                }
            }
        });
        UiKit.place(this, 640, 580);
    }

    private JComponent wrap(JComponent c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(skin.panelBg);
        p.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        p.add(c, BorderLayout.NORTH);
        return p;
    }

    private static JComponent stack(JComponent a, JComponent b) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        a.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(a);
        p.add(Box.createVerticalStrut(16));
        p.add(b);
        return p;
    }

    private JPanel buildButtons() {
        okBtn = UiKit.button(skin, "确定", 14, true);
        cancelBtn = UiKit.button(skin, "取消", 14, false);
        resetBtn = UiKit.button(skin, "恢复默认", 14, false);

        okBtn.addActionListener(e -> {
            commit();
            dispose();
        });
        cancelBtn.addActionListener(e -> dispose());
        resetBtn.addActionListener(e -> {
            if (!UiKit.confirm(this, "恢复默认",
                    "会把设置、皮肤、背景、榜单和成就全部清空，确定吗？")) {
                return;
            }
            ConfigStore.get().resetAll();
            nav.applySkin(ConfigStore.get().skin().id);
            dispose();
        });

        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        p.setBackground(skin.panelBg);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, skin.panelBorder),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));
        p.add(resetBtn);
        p.add(cancelBtn);
        p.add(okBtn);
        return p;
    }

    /** 把「游戏」页的暂存配置写回并落盘。 */
    private void commit() {
        ConfigStore store = ConfigStore.get();
        GameConfig live = store.config();
        live.mode = config.mode;
        live.cols = config.cols;
        live.rows = config.rows;
        live.wrapWalls = config.wrapWalls;
        live.tickMs = config.tickMs;
        live.foodCount = config.foodCount;
        live.maxLength = config.maxLength;
        live.sanitize();
        store.saveConfig();
        // 背景与音频在表单里已经即时生效，这里再存一次兜底
        store.saveBackground();
    }

    public void applySkin(Skin s) {
        this.skin = s;
        gameForm.applySkin(s);
        skinPanel.applySkin(s);
        backgroundForm.applySkin(s);
        audioForm.applySkin(s);
        helpPanel.applySkin(s);
        if (okBtn != null) {
            okBtn.applySkin(s);
            cancelBtn.applySkin(s);
            resetBtn.applySkin(s);
        }
        getContentPane().setBackground(s.panelBg);
    }

    public static void show(Navigator nav, Skin skin) {
        SettingsDialog dlg = new SettingsDialog(nav, skin);
        dlg.setVisible(true);
    }
}
