package com.snake.ui;

import com.snake.Skin;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JPanel;

/**
 * 群友商店浮层。B 打开，B/Esc 关闭；打开期间游戏暂停。
 * 当前版本只有货币展示和空货架，后续商品直接在这里加卡片即可。
 */
public final class ShopPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int CARD_RADIUS = 18;
    private static final int CARD_W = 460;
    private static final int CARD_H = 300;

    private final Rectangle card = new Rectangle();
    private final Font titleFont = UiKit.font(28, Font.BOLD);
    private final Font bodyFont = UiKit.font(15, Font.PLAIN);
    private final Font emptyFont = UiKit.font(17, Font.BOLD);
    private final Font hintFont = UiKit.font(13, Font.PLAIN);

    private Skin skin;
    private int length;
    private Runnable onClose;

    public ShopPanel(Skin skin) {
        this.skin = skin;
        setOpaque(false);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        setVisible(false);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int k = e.getKeyCode();
                if (k == KeyEvent.VK_B || k == KeyEvent.VK_ESCAPE) {
                    if (onClose != null) {
                        onClose.run();
                    }
                }
                e.consume();
            }
        });
    }

    public void setOnClose(Runnable r) {
        this.onClose = r;
    }

    public void applySkin(Skin s) {
        this.skin = s;
        repaint();
    }

    public void open(int currentLength) {
        this.length = currentLength;
        setVisible(true);
        if (getParent() != null) {
            getParent().doLayout();
        }
        doLayout();
        repaint();
    }

    public int length() {
        return length;
    }

    @Override
    public void doLayout() {
        int w = getWidth();
        int h = getHeight();
        int cw = Math.min(CARD_W, Math.max(0, w - 44));
        int ch = Math.min(CARD_H, Math.max(0, h - 44));
        card.setBounds((w - cw) / 2, (h - ch) / 2, cw, ch);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(skin == null ? new Color(0, 0, 0, 170) : skin.overlay);
        g2.fillRect(0, 0, getWidth(), getHeight());
        if (card.width > 0 && card.height > 0) {
            g2.setColor(new Color(0, 0, 0, 70));
            g2.fillRoundRect(card.x + 3, card.y + 5, card.width, card.height,
                    CARD_RADIUS, CARD_RADIUS);
            g2.setColor(skin.panelBg);
            g2.fillRoundRect(card.x, card.y, card.width, card.height,
                    CARD_RADIUS, CARD_RADIUS);
            g2.setColor(skin.accent);
            g2.drawRoundRect(card.x, card.y, card.width - 1, card.height - 1,
                    CARD_RADIUS, CARD_RADIUS);

            int x = card.x + 34;
            int y = card.y + 54;
            g2.setFont(titleFont);
            g2.setColor(skin.text);
            g2.drawString("群友商店", x, y);

            g2.setFont(bodyFont);
            g2.setColor(skin.accent);
            g2.drawString("可用长度：" + length + " 节", x, y + 38);

            g2.setColor(skin.textDim);
            g2.drawLine(x, y + 56, card.x + card.width - 34, y + 56);

            g2.setFont(emptyFont);
            g2.setColor(skin.text);
            g2.drawString("货架空空如也", x, y + 110);
            g2.setFont(bodyFont);
            g2.setColor(skin.textDim);
            g2.drawString("群友还在准备商品…", x, y + 140);

            g2.setFont(hintFont);
            g2.setColor(skin.textDim);
            String hint = "B / Esc 关闭";
            int hintW = g2.getFontMetrics().stringWidth(hint);
            g2.drawString(hint, card.x + card.width - 34 - hintW,
                    card.y + card.height - 24);
        }
        g2.dispose();
    }

    @Override
    public Color getBackground() {
        return skin == null ? super.getBackground() : skin.bg;
    }
}
