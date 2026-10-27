package com.snake.ui;

import com.snake.GameConfig;
import com.snake.Navigator;
import com.snake.Skin;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

/**
 * 「新的游戏」页：改完参数直接开局。
 *
 * 表单本体复用 {@link ConfigForm}，和设置对话框里的是同一份代码，
 * 两处规则永远一致。
 */
public final class SetupPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final Navigator nav;
    private final GameConfig config;
    private final ConfigForm form;
    private final JLabel summary = new JLabel("", SwingConstants.CENTER);
    private final UiKit.FlatButton startBtn;
    private final UiKit.FlatButton resetBtn;

    public SetupPanel(Navigator nav) {
        this.nav = nav;
        Skin skin = nav.skin();
        this.config = nav.config();
        this.form = new ConfigForm(config, skin);
        this.startBtn = UiKit.button(skin, "开始游戏", 16, true);
        this.resetBtn = UiKit.button(skin, "恢复默认", 14, false);

        build();
        refreshSummary();
        applySkin(skin);
    }

    private void build() {
        setOpaque(true);
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UiKit.sectionTitle(nav.skin(), "新的游戏"), BorderLayout.WEST);
        header.add(resetBtn, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        JScrollPane scroll = new JScrollPane(form);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        center.add(scroll, BorderLayout.CENTER);
        center.add(summary, BorderLayout.SOUTH);
        add(center, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setOpaque(false);
        JLabel hint = new JLabel("Esc 返回");
        hint.setFont(UiKit.font(12, UiKit.FONT_PLAIN_));
        hint.setForeground(nav.skin().textDim);
        footer.add(hint);
        footer.add(startBtn);
        add(footer, BorderLayout.SOUTH);

        // 配置是被就地修改的，所以任何改动后都要重刷摘要
        form.addChangeListener(e -> refreshSummary());
        startBtn.addActionListener(e -> {
            config.challenge = GameConfig.Challenge.NONE;
            config.seed = 0L;
            nav.startNewGame(config.deepCopy());
        });
        resetBtn.addActionListener(e -> {
            GameConfig d = new GameConfig();
            config.mode = d.mode;
            config.cols = d.cols;
            config.rows = d.rows;
            config.wrapWalls = d.wrapWalls;
            config.tickMs = d.tickMs;
            config.foodCount = d.foodCount;
            config.maxLength = d.maxLength;
            form.readFromConfig();
            refreshSummary();
        });

        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    nav.show(Navigator.CARD_MENU);
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    startBtn.doClick();
                    e.consume();
                }
            }
        });
    }

    private void refreshSummary() {
        summary.setText(config.summary());
    }

    public void applySkin(Skin s) {
        setBackground(s.bg);
        form.applySkin(s);
        startBtn.applySkin(s);
        resetBtn.applySkin(s);
        summary.setForeground(s.accent);
        for (java.awt.Component c : getComponents()) {
            if (c instanceof JPanel) {
                JPanel p = (JPanel) c;
                p.setOpaque(false);
                for (java.awt.Component ch : p.getComponents()) {
                    if (ch instanceof JLabel) {
                        ((JLabel) ch).setForeground(s.textDim);
                    }
                }
            }
        }
        repaint();
    }
}
