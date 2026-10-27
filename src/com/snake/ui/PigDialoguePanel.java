package com.snake.ui;

import com.snake.PigArt;
import com.snake.Skin;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;

/**
 * 吃到猪头后的逐句对话层。点击任意位置推进一句，最后一句再点击结束。
 * 对话期间棋盘被遮罩盖住并暂停，避免蛇在剧情播放时继续跑。
 */
public final class PigDialoguePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    public static final String[] LINES = {
            "哪来的橡皮软糖?",
            "正好有点饿了吃了解解馋.",
            "不想被吃掉?",
            "那好吧我就不吃了",
            "不过我有一些妙妙小工具,你想使用的话就需要给我你身体的一部分交换,嘿嘿.",
            "接下来你只要摁下B键就能找到我了.",
            "对了,还没自我介绍,我姓群,单名一个友."
    };

    private static final int MARGIN = 22;
    private static final int CARD_RADIUS = 18;

    private final Rectangle card = new Rectangle();
    private final Font speakerFont = UiKit.font(18, Font.BOLD);
    private final Font textFont = UiKit.font(20, Font.PLAIN);
    private final Font hintFont = UiKit.font(13, Font.PLAIN);

    private Skin skin;
    private int index;
    private Runnable onFinished;

    public PigDialoguePanel(Skin skin) {
        this.skin = skin;
        setOpaque(false);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setVisible(false);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                advance();
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // 剧情期间不把按键漏给棋盘；B、Esc 等也要等对话结束后再生效。
                e.consume();
            }
        });
    }

    public void setOnFinished(Runnable r) {
        this.onFinished = r;
    }

    public void applySkin(Skin s) {
        this.skin = s;
        repaint();
    }

    public void open() {
        index = 0;
        setVisible(true);
        // 隐藏期间布局管理器未必给过尺寸，显示后先补一次父布局再算卡片。
        if (getParent() != null) {
            getParent().doLayout();
        }
        doLayout();
        repaint();
    }

    public void advance() {
        if (!isVisible()) {
            return;
        }
        if (index < LINES.length - 1) {
            index++;
            repaint();
            return;
        }
        setVisible(false);
        if (onFinished != null) {
            onFinished.run();
        }
    }

    public int currentIndex() {
        return index;
    }

    public String currentLine() {
        return LINES[Math.max(0, Math.min(index, LINES.length - 1))];
    }

    @Override
    public void doLayout() {
        int w = getWidth();
        int h = getHeight();
        int cardW = Math.max(0, Math.min(w - 2 * MARGIN, 820));
        int cardH = Math.max(0, Math.min(h - 2 * MARGIN, 460));
        card.setBounds((w - cardW) / 2, (h - cardH) / 2, cardW, cardH);
    }

    private int pigSize() {
        int byHeight = (int) (card.height * 0.48f);
        int byWidth = (int) (card.width * 0.34f);
        return Math.max(100, Math.min(300, Math.min(byHeight, byWidth)));
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

            int pig = pigSize();
            int pigX = card.x + card.width - pig - 20;
            int pigY = card.y + card.height - pig - 18;
            PigArt.paint(g2, pigX, pigY, pig);

            int textX = card.x + 34;
            int textW = Math.max(80, pigX - textX - 24);

            g2.setFont(speakerFont);
            g2.setColor(skin.accent);
            g2.drawString("群友", textX, card.y + 48);

            g2.setFont(textFont);
            g2.setColor(skin.text);
            int lineH = 30;
            drawWrapped(g2, LINES[index], textX, card.y + 92, textW, lineH);

            g2.setFont(hintFont);
            g2.setColor(skin.textDim);
            String progress = (index + 1) + " / " + LINES.length;
            g2.drawString(progress, textX, card.y + card.height - 44);
            String hint = index < LINES.length - 1 ? "点击继续" : "点击结束对话";
            g2.drawString(hint, textX, card.y + card.height - 20);
        }
        g2.dispose();
    }

    private void drawWrapped(Graphics2D g2, String text, int x, int y,
                             int maxW, int lineH) {
        FontMetrics fm = g2.getFontMetrics();
        int start = 0;
        int lineY = y;
        while (start < text.length()) {
            int end = start + 1;
            while (end < text.length()
                    && fm.stringWidth(text.substring(start, end + 1)) <= maxW) {
                end++;
            }
            g2.drawString(text.substring(start, end), x, lineY);
            start = end;
            lineY += lineH;
        }
    }

    @Override
    public Color getBackground() {
        return skin == null ? super.getBackground() : skin.bg;
    }
}
