package com.snake.ui;

import com.snake.ConfigStore;
import com.snake.Navigator;
import com.snake.Skin;
import com.snake.Skins;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 设置 → 外观 → 皮肤。选中即刻生效，不需要额外的「应用」按钮——
 * 换皮肤马上能看到效果，玩家才判断得准。
 *
 * 想看蛇动起来的样子就去「皮肤预览」页，这里是快速切换。
 */
public final class SkinPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final Navigator nav;
    private Skin skin;

    private final JComboBox<String> box = new JComboBox<String>();
    private final JLabel desc = new JLabel();
    private final JLabel title = new JLabel("皮肤");
    private final JLabel tip = new JLabel("选中即生效。想看实际效果请到主菜单的「皮肤预览」。");
    private final Swatches swatches = new Swatches();
    private final UiKit.FlatButton prev;
    private final UiKit.FlatButton next;
    private int index;

    public SkinPanel(Navigator nav, Skin skin) {
        this.nav = nav;
        this.skin = skin;
        this.prev = UiKit.button(skin, "上一套", 13, false);
        this.next = UiKit.button(skin, "下一套", 13, false);
        build();
        applySkin(skin);
    }

    private void build() {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        for (Skin s : Skins.all()) {
            box.addItem(s.name);
        }
        box.setMaximumSize(new Dimension(220, 28));
        box.addActionListener(e -> {
            if (index != box.getSelectedIndex()) {
                select(box.getSelectedIndex());
            }
        });

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);
        title.setFont(UiKit.font(14, UiKit.FONT_PLAIN_));
        top.add(title);
        top.add(box);
        top.add(prev);
        top.add(next);
        add(top);
        add(Box.createVerticalStrut(6));

        desc.setFont(UiKit.font(13, UiKit.FONT_PLAIN_));
        add(desc);
        add(Box.createVerticalStrut(8));
        swatches.setPreferredSize(new Dimension(200, 26));
        swatches.setMaximumSize(new Dimension(240, 26));
        add(swatches);
        add(Box.createVerticalStrut(8));
        tip.setFont(UiKit.font(12, UiKit.FONT_PLAIN_));
        add(tip);

        prev.addActionListener(e -> step(-1));
        next.addActionListener(e -> step(1));
        select(indexOf(ConfigStore.get().skin().id));
    }

    private static int indexOf(String id) {
        for (int i = 0; i < Skins.all().size(); i++) {
            if (Skins.all().get(i).id.equals(id)) {
                return i;
            }
        }
        return 0;
    }

    private void step(int delta) {
        int n = Skins.all().size();
        select(((index + delta) % n + n) % n);
    }

    private void select(int i) {
        index = Math.max(0, Math.min(Skins.all().size() - 1, i));
        if (box.getSelectedIndex() != index) {
            box.setSelectedIndex(index);
        }
        Skin s = Skins.all().get(index);
        desc.setText(s.description);
        swatches.setSkin(s);
        nav.applySkin(s.id);
    }

    public void applySkin(Skin s) {
        this.skin = s;
        title.setForeground(s.text);
        desc.setForeground(s.textDim);
        tip.setForeground(s.textDim);
        prev.applySkin(s);
        next.applySkin(s);
        box.setForeground(s.text);
        swatches.setSkin(s);
        repaint();
    }

    /** 色板预览。 */
    private static final class Swatches extends JComponent {
        private static final long serialVersionUID = 1L;
        private Skin skin;

        Swatches() {
            setOpaque(true);
        }

        void setSkin(Skin s) {
            this.skin = s;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            if (skin == null) {
                g2.dispose();
                return;
            }
            Color[] cols = skin.swatch();
            int x = 0;
            for (Color c : cols) {
                g2.setColor(c);
                g2.fillRoundRect(x, 0, 24, getHeight(), 6, 6);
                g2.setColor(new Color(0, 0, 0, 90));
                g2.drawRoundRect(x, 0, 23, getHeight() - 1, 6, 6);
                x += 28;
            }
            g2.dispose();
        }
    }
}
