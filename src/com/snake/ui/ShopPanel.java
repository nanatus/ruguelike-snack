package com.snake.ui;

import com.snake.GamePanel;
import com.snake.ShopItem;
import com.snake.Skin;
import java.awt.Color;
import java.awt.Font;
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
 * 群友商店浮层。B 打开，B/Esc 关闭；打开期间游戏暂停。
 *
 * 三件商品都能无限购买，只受蛇身余额、速度范围和上限硬顶限制。
 * 购买逻辑本身在 {@link GamePanel#buy(ShopItem)}，这里只负责展示和输入。
 */
public final class ShopPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int CARD_RADIUS = 18;
    private static final int CARD_W = 560;
    private static final int CARD_H = 420;

    private static final int ROW_H = 76;
    private static final int ROW_GAP = 10;
    private static final int ROWS_TOP = 100;
    private static final int BUTTON_W = 96;
    private static final int BUTTON_H = 32;

    private static final ShopItem[] ITEMS = ShopItem.values();

    private final Rectangle card = new Rectangle();
    private final Rectangle[] rowRects = new Rectangle[ITEMS.length];
    private final Rectangle[] buyRects = new Rectangle[ITEMS.length];

    private final Font titleFont = UiKit.font(28, Font.BOLD);
    private final Font bodyFont = UiKit.font(15, Font.PLAIN);
    private final Font itemNameFont = UiKit.font(17, Font.BOLD);
    private final Font smallFont = UiKit.font(13, Font.PLAIN);
    private final Font buttonFont = UiKit.font(13, Font.BOLD);
    private final Font hintFont = UiKit.font(12, Font.PLAIN);

    private Skin skin;
    private GamePanel game;
    private Runnable onClose;
    private Runnable onPurchased;

    private int selected;
    private String message = "";
    private boolean messageError;

    public ShopPanel(Skin skin) {
        this.skin = skin;
        for (int i = 0; i < ITEMS.length; i++) {
            rowRects[i] = new Rectangle();
            buyRects[i] = new Rectangle();
        }
        setOpaque(false);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        setVisible(false);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKey(e);
                e.consume();
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleClick(e.getX(), e.getY());
            }
        });
    }

    public void setOnClose(Runnable r) {
        this.onClose = r;
    }

    /** 购买成功后通知外层刷新状态栏。 */
    public void setOnPurchased(Runnable r) {
        this.onPurchased = r;
    }

    public void applySkin(Skin s) {
        this.skin = s;
        repaint();
    }

    /** 打开商店，并绑定这一局的购买模型。 */
    public void open(GamePanel panel) {
        this.game = panel;
        selected = 0;
        message = "";
        messageError = false;
        setVisible(true);
        if (getParent() != null) {
            getParent().doLayout();
        }
        doLayout();
        repaint();
    }

    /** 当前绑定的对局面板；未打开时为 null。 */
    public GamePanel game() {
        return game;
    }

    // ---- 输入 ----

    private void handleKey(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_B || k == KeyEvent.VK_ESCAPE) {
            if (onClose != null) {
                onClose.run();
            }
            return;
        }
        if (k == KeyEvent.VK_UP || k == KeyEvent.VK_W) {
            selected = (selected + ITEMS.length - 1) % ITEMS.length;
            repaint();
            return;
        }
        if (k == KeyEvent.VK_DOWN || k == KeyEvent.VK_S) {
            selected = (selected + 1) % ITEMS.length;
            repaint();
            return;
        }
        if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) {
            buy(ITEMS[selected]);
            return;
        }
        if (k >= KeyEvent.VK_1 && k <= KeyEvent.VK_3) {
            int index = k - KeyEvent.VK_1;
            selected = index;
            buy(ITEMS[index]);
        }
    }

    private void handleClick(int x, int y) {
        requestFocusInWindow();
        for (int i = 0; i < ITEMS.length; i++) {
            if (buyRects[i].contains(x, y)) {
                selected = i;
                buy(ITEMS[i]);
                return;
            }
            if (rowRects[i].contains(x, y)) {
                selected = i;
                message = "";
                messageError = false;
                repaint();
                return;
            }
        }
    }

    private void buy(ShopItem item) {
        if (game == null) {
            return;
        }
        GamePanel.PurchaseResult result = game.buy(item);
        switch (result) {
            case OK:
                message = "已购买" + item.name + " · 当前速度 " + game.speedMs()
                        + " 毫秒/步 · 上限 " + game.maxLength() + " 节";
                messageError = false;
                if (onPurchased != null) {
                    onPurchased.run();
                }
                break;
            case NOT_ENOUGH_LENGTH:
                message = "长度不足 · 付款后至少要保留 1 节";
                messageError = true;
                break;
            case SPEED_LIMIT:
                message = "已经到速度极限了（" + GamePanel.MIN_SPEED_MS + "～"
                        + GamePanel.MAX_SPEED_MS + " 毫秒/步）";
                messageError = true;
                break;
            default:
                message = "长度上限已经到 " + GamePanel.MAX_LENGTH_HARD_CAP + " 节";
                messageError = true;
                break;
        }
        repaint();
    }

    // ---- 布局与绘制 ----

    @Override
    public void doLayout() {
        int w = getWidth();
        int h = getHeight();
        int cw = Math.min(CARD_W, Math.max(0, w - 44));
        int ch = Math.min(CARD_H, Math.max(0, h - 44));
        card.setBounds((w - cw) / 2, (h - ch) / 2, cw, ch);

        int contentX = card.x + 30;
        int contentW = card.width - 60;
        int rowW = Math.max(0, contentW);
        for (int i = 0; i < ITEMS.length; i++) {
            int rowY = card.y + ROWS_TOP + i * (ROW_H + ROW_GAP);
            rowRects[i].setBounds(contentX, rowY, rowW, ROW_H);
            int bx = contentX + rowW - BUTTON_W - 16;
            int by = rowY + (ROW_H - BUTTON_H) / 2;
            buyRects[i].setBounds(bx, by, BUTTON_W, BUTTON_H);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
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

            int x = card.x + 30;
            g2.setFont(titleFont);
            g2.setColor(skin.text);
            g2.drawString("群友商店", x, card.y + 46);

            int length = game == null ? 0 : game.length();
            int speed = game == null ? 120 : game.speedMs();
            int maxLength = game == null ? 0 : game.maxLength();

            g2.setFont(bodyFont);
            g2.setColor(skin.accent);
            String balance = "可用长度 " + length + " 节";
            int balanceW = g2.getFontMetrics().stringWidth(balance);
            g2.drawString(balance, card.x + card.width - 30 - balanceW, card.y + 40);

            g2.setFont(smallFont);
            g2.setColor(skin.textDim);
            g2.drawString("当前速度 " + speed + " 毫秒/步 · 当前上限 " + maxLength + " 节",
                    x, card.y + 68);
            g2.drawLine(x, card.y + 84, card.x + card.width - 30, card.y + 84);

            for (int i = 0; i < ITEMS.length; i++) {
                paintRow(g2, i);
            }

            int rowsBottom = card.y + ROWS_TOP + ITEMS.length * ROW_H
                    + (ITEMS.length - 1) * ROW_GAP;
            g2.setFont(smallFont);
            g2.setColor(messageError ? skin.text : skin.accent);
            if (message != null && !message.isEmpty()) {
                g2.drawString(message, x, rowsBottom + 26);
            }

            g2.setFont(hintFont);
            g2.setColor(skin.textDim);
            String hint = "↑↓ 选择 · 回车/空格 购买 · 1/2/3 快捷购买 · B/Esc 关闭";
            int hintW = g2.getFontMetrics().stringWidth(hint);
            g2.drawString(hint, card.x + card.width - 30 - hintW,
                    card.y + card.height - 18);
        }
        g2.dispose();
    }

    private void paintRow(Graphics2D g2, int index) {
        ShopItem item = ITEMS[index];
        Rectangle row = rowRects[index];
        Rectangle button = buyRects[index];
        boolean active = index == selected;
        boolean affordable = game != null && game.length() > item.price;

        g2.setColor(active ? withAlpha(skin.accent, 34) : withAlpha(skin.panelBorder, 70));
        g2.fillRoundRect(row.x, row.y, row.width, row.height, 12, 12);
        g2.setColor(active ? skin.accent : skin.panelBorder);
        g2.drawRoundRect(row.x, row.y, row.width - 1, row.height - 1, 12, 12);

        int textX = row.x + 16;
        g2.setFont(itemNameFont);
        g2.setColor(skin.text);
        g2.drawString(item.name, textX, row.y + 26);

        g2.setFont(smallFont);
        g2.setColor(skin.textDim);
        g2.drawString(item.description, textX, row.y + 47);

        String status = status(item, affordable);
        g2.setColor(affordable && status.equals("可购买") ? skin.accent : skin.textDim);
        g2.drawString(status, textX, row.y + 66);

        boolean enabled = affordable && status.equals("可购买");
        g2.setColor(enabled ? skin.accent : withAlpha(skin.panelBorder, 160));
        g2.fillRoundRect(button.x, button.y, button.width, button.height, 10, 10);
        g2.setFont(buttonFont);
        g2.setColor(enabled ? skin.panelBg : skin.textDim);
        String label = "购买 " + item.price + " 节";
        int labelW = g2.getFontMetrics().stringWidth(label);
        g2.drawString(label, button.x + (button.width - labelW) / 2,
                button.y + (button.height + g2.getFontMetrics().getAscent()) / 2 - 3);
    }

    private String status(ShopItem item, boolean affordable) {
        if (game == null) {
            return "未就绪";
        }
        switch (item) {
            case SPEED_UP:
                if (game.speedMs() <= GamePanel.MIN_SPEED_MS) {
                    return "已到最快";
                }
                break;
            case SPEED_DOWN:
                if (game.speedMs() >= GamePanel.MAX_SPEED_MS) {
                    return "已到最慢";
                }
                break;
            default:
                if (game.maxLength() >= GamePanel.MAX_LENGTH_HARD_CAP) {
                    return "已达上限";
                }
                break;
        }
        if (!affordable) {
            return "长度不足";
        }
        return "可购买";
    }

    private static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    @Override
    public Color getBackground() {
        return skin == null ? super.getBackground() : skin.bg;
    }
}
