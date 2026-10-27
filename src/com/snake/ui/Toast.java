package com.snake.ui;

import com.snake.Skin;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.Timer;

/**
 * 右上角滑入的短提示，几秒后自动淡出。
 * 成就解锁、破纪录之类的信息用它：不打断操作，也不用玩家点掉。
 *
 * 挂在根面板的玻璃层上，所以不用操心被 BorderLayout 摆到哪去。
 */
public final class Toast extends JComponent {

    private static final long serialVersionUID = 1L;

    private static final int VISIBLE_MS = 2600;
    private static final int FADE_MS = 420;
    private static final int TICK_MS = 50;
    private static final int MAX_VISIBLE = 4;
    private static final int PADDING = 16;
    private static final int STACK_GAP = 8;
    private static final int BOX_H = 34;

    private final Queue<String> queue = new ArrayDeque<String>();
    /** 当前正在显示的一条，最新的在最后。 */
    private final List<String> shown = new ArrayList<String>();
    private long currentSince;
    private Timer ticker;
    private JRootPane owner;
    private Skin skin;

    public Toast(Skin skin) {
        this.skin = skin;
        setOpaque(false);
        setFocusable(false);
    }

    public void applySkin(Skin s) {
        this.skin = s;
        repaint();
    }

    public void show(JComponent host, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        JRootPane rp = host.getRootPane();
        if (rp == null) {
            return;
        }
        if (owner != rp) {
            queue.clear();
            shown.clear();
            if (owner != null && owner.getGlassPane() == this) {
                owner.setGlassPane(new JComponent() {
                    private static final long serialVersionUID = 1L;
                });
            }
            owner = rp;
            rp.setGlassPane(this);
        }
        queue.add(text);
        if (ticker == null) {
            startTicking();
        } else if (shown.isEmpty()) {
            currentSince = System.currentTimeMillis();
        }
        repaint();
    }

    private void startTicking() {
        ticker = new Timer(TICK_MS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tick();
            }
        });
        currentSince = System.currentTimeMillis();
        ticker.start();
    }

    private void tick() {
        if (shown.isEmpty()) {
            if (queue.isEmpty()) {
                stop();
            } else {
                shown.add(queue.poll());
            }
            repaint();
            return;
        }
        long age = System.currentTimeMillis() - currentSince;
        if (age >= VISIBLE_MS + FADE_MS) {
            shown.remove(0);
            if (shown.size() < MAX_VISIBLE && !queue.isEmpty()) {
                shown.add(queue.poll());
            }
            // 最后一条也要留出完整的展示时间再淡出
            currentSince = System.currentTimeMillis();
            repaint();
        }
    }

    private void stop() {
        if (ticker != null) {
            ticker.stop();
            ticker = null;
        }
        shown.clear();
        queue.clear();
        if (owner != null && owner.getGlassPane() == this) {
            owner.setGlassPane(new JComponent() {
                private static final long serialVersionUID = 1L;
            });
        }
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        Font f = UiKit.font(14, Font.BOLD);
        int w = 0;
        for (String s : shown) {
            w = Math.max(w, textWidth(s, f));
        }
        return new Dimension(Math.max(1, w + 32), 1);
    }

    @Override
    public void doLayout() {
        setSize(getWidth(), getHeight());
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (shown.isEmpty()) {
            return;
        }
        Font f = UiKit.font(14, Font.BOLD);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        long age = System.currentTimeMillis() - currentSince;
        float alpha = age > VISIBLE_MS ? 1f - (age - VISIBLE_MS) / (float) FADE_MS : 1f;
        alpha = Math.max(0f, Math.min(1f, alpha));
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        int boxY = PADDING;
        for (int i = 0; i < shown.size(); i++) {
            String s = shown.get(i);
            int w = textWidth(s, f) + 30;
            int x = getWidth() - PADDING - w;
            int yy = boxY + i * (BOX_H + STACK_GAP);
            g2.setColor(new Color(skin.panelBg.getRed(), skin.panelBg.getGreen(),
                    skin.panelBg.getBlue(), 236));
            g2.fillRoundRect(x, yy, w, BOX_H, 12, 12);
            g2.setColor(skin.accent);
            g2.drawRoundRect(x, yy, w - 1, BOX_H - 1, 12, 12);
            g2.setColor(skin.text);
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(s, x + 15, yy + (BOX_H + fm.getAscent() - fm.getDescent()) / 2);
        }
        g2.dispose();
    }

    private int textWidth(String s, Font f) {
        // 玻璃层还没显示时 fontMetrics 可能取不到，用临时度量兜底
        javax.swing.JLabel probe = new javax.swing.JLabel(s);
        probe.setFont(f);
        java.awt.FontMetrics fm = isShowing() ? getFontMetrics(f) : probe.getFontMetrics(probe.getFont());
        return fm.stringWidth(s);
    }

    /** 提示不吃鼠标事件，否则会挡住游戏区的点击。 */
    @Override
    public boolean contains(int x, int y) {
        return false;
    }
}
