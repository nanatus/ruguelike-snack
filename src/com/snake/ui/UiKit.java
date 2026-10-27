package com.snake.ui;

import com.snake.Skin;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;

/**
 * 界面外观工厂。所有面板都从这里取字体和按钮，
 * 这样换皮肤时不会出现某个页面漏改颜色的情况。
 */
public final class UiKit {

    /** 主菜单主选项字号（中文"二号"）。 */
    public static final int MENU_BIG = 22;
    /** 主菜单左下角次要选项字号（"小二号"，正好小两号）。 */
    public static final int MENU_SMALL = 18;

    public static final String FONT = "SansSerif";
    public static final int FONT_PLAIN_ = java.awt.Font.PLAIN;
    public static final int FONT_BOLD_ = java.awt.Font.BOLD;

    /** 标记控件已经被 styleCheck/styleRadio 处理过，避免重复挂监听器。 */
    private static final String KEY_STYLED = "ui.styled";
    /** 悬停前记住的普通文字色，用于鼠标移出后还原。 */
    private static final String KEY_BASE_FG = "ui.baseFg";

    private UiKit() {
    }

    public static Font font(int size, int style) {
        return new Font(FONT, style, size);
    }

    public static JLabel label(Skin skin, String text, int size, int style) {
        JLabel l = new JLabel(text);
        l.setFont(font(size, style));
        l.setForeground(skin.text);
        return l;
    }

    public static JLabel dim(Skin skin, String text, int size) {
        JLabel l = label(skin, text, size, Font.PLAIN);
        l.setForeground(skin.textDim);
        return l;
    }

    public static JLabel sectionTitle(Skin skin, String text) {
        JLabel l = label(skin, text, 15, Font.BOLD);
        l.setForeground(skin.accent);
        return l;
    }

    /** 带边框的卡片容器。 */
    public static JPanel card(Skin skin) {
        JPanel p = new JPanel();
        p.setOpaque(true);
        p.setBackground(skin.panelBg);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(skin.panelBorder, 1),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        return p;
    }

    // ---- 按钮 ----

    public static FlatButton button(Skin skin, String text, int size, boolean primary) {
        return new FlatButton(skin, text, size, primary, false);
    }

    /** 主菜单里的选项按钮。 */
    public static FlatButton menuButton(Skin skin, String text) {
        return new FlatButton(skin, text, MENU_BIG, false, true);
    }

    /** 主菜单左下角的次要选项。 */
    public static FlatButton smallMenuButton(Skin skin, String text) {
        return new FlatButton(skin, text, MENU_SMALL, false, true);
    }

    public static void styleRadio(Skin skin, JRadioButton b) {
        styleSelectable(skin, b);
        b.setFont(font(14, Font.PLAIN));
    }

    public static void styleCheck(Skin skin, JCheckBox b) {
        styleSelectable(skin, b);
        b.setFont(font(14, Font.PLAIN));
    }

    /** 复选框数组的糖，因为换肤处基本都在遍历一组框。 */
    public static void styleChecks(Skin skin, AbstractButton... boxes) {
        for (AbstractButton b : boxes) {
            styleSelectable(skin, b);
        }
    }

    private static void styleSelectable(Skin skin, JComponent c) {
        c.setOpaque(false);
        c.setForeground(skin.text);
        if (c instanceof AbstractButton) {
            AbstractButton b = (AbstractButton) c;
            b.setFocusPainted(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            // 换肤会反复调用这个方法，必须只挂一次监听器，否则监听器会越堆越多。
            if (Boolean.TRUE.equals(b.getClientProperty(KEY_STYLED))) {
                b.setForeground(skin.text);
                return;
            }
            b.putClientProperty(KEY_STYLED, Boolean.TRUE);
            // 悬停结束时要把文字色还原成「当前皮肤」的颜色，
            // 所以每次进入时记下最新的普通色，而不是把第一次的皮肤记死。
            b.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    b.putClientProperty(KEY_BASE_FG, skin.text);
                    b.setForeground(skin.accent);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    Object base = b.getClientProperty(KEY_BASE_FG);
                    b.setForeground(base instanceof Color ? (Color) base : skin.text);
                }
            });
        }
    }

    // ---- 键盘导航 ----

    /**
     * 一组可键盘操作的按钮。上下键移动，回车/空格触发。
     * 主菜单的中间组和左下角组各用一个，左右键在两组之间切。
     */
    public static final class Nav {
        private final List<JButton> items = new ArrayList<JButton>();
        private int index = -1;

        public void add(JButton b) {
            items.add(b);
            if (index < 0) {
                index = 0;
            }
        }

        public boolean isEmpty() {
            return items.isEmpty();
        }

        public int size() {
            return items.size();
        }

        public JButton current() {
            return index < 0 || index >= items.size() ? null : items.get(index);
        }

        /** 取第 i 个按钮，界面上用来同步「当前选中」样式。 */
        public JButton get(int i) {
            return i >= 0 && i < items.size() ? items.get(i) : null;
        }

        public void step(int delta) {
            if (items.isEmpty()) {
                return;
            }
            int n = items.size();
            index = ((index + delta) % n + n) % n;
            refresh();
        }

        public void focus(int i) {
            if (items.isEmpty()) {
                return;
            }
            index = Math.max(0, Math.min(items.size() - 1, i));
            refresh();
        }

        /**
         * 原地重刷选中样式，不移动位置。
         *
         * add() 只把 index 置 0，并没有套用样式；所以第一次打开菜单时
         * 没有任何按钮是 selected，「继续游戏」会按未选中样式画成灰底。
         */
        public void reselect() {
            if (index >= 0 && index < items.size()) {
                refresh();
            }
        }

        private void refresh() {
            for (int i = 0; i < items.size(); i++) {
                items.get(i).setSelected(i == index);
            }
            JButton c = current();
            if (c != null && c.isShowing()) {
                c.requestFocusInWindow();
            }
        }

        public void activate() {
            JButton c = current();
            if (c != null && c.isEnabled()) {
                c.doClick();
            }
        }
    }

    // ---- 对话框 ----

    public static void error(Component parent, String message) {
        error(parent, "出错了", message);
    }

    public static void error(Component parent, String title, String message) {
        javax.swing.JOptionPane.showMessageDialog(parent, message, title,
                javax.swing.JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component parent, String title, String message) {
        javax.swing.JOptionPane.showMessageDialog(parent, message, title,
                javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String title, String message) {
        int r = javax.swing.JOptionPane.showConfirmDialog(parent, message, title,
                javax.swing.JOptionPane.YES_NO_OPTION,
                javax.swing.JOptionPane.QUESTION_MESSAGE);
        return r == javax.swing.JOptionPane.YES_OPTION;
    }

    /** 让窗口在屏幕中间，并给一个不至于太小的初始尺寸。 */
    public static void place(java.awt.Window w, int prefW, int prefH) {
        w.pack();
        java.awt.Dimension d = w.getSize();
        if (d.width < prefW) {
            d.width = prefW;
        }
        if (d.height < prefH) {
            d.height = prefH;
        }
        w.setSize(d);
        w.setLocationRelativeTo(null);
    }

    public static void onEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    /**
     * 扁平按钮：无系统边框，自绘圆角边框，悬停或选中时用强调色。
     * 选中态同时描粗边，这样键盘导航时当前项一眼可见。
     */
    public static final class FlatButton extends JButton {
        private static final long serialVersionUID = 1L;

        private Skin skin;
        private final boolean primary;
        private final boolean menuStyle;
        private boolean hover;

        FlatButton(Skin skin, String text, int size, boolean primary, boolean menuStyle) {
            super(text);
            this.skin = skin;
            this.primary = primary;
            this.menuStyle = menuStyle;
            setFont(UiKit.font(size, primary || menuStyle ? Font.BOLD : Font.PLAIN));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(CENTER_ALIGNMENT);
            int pad = menuStyle ? 10 : 6;
            setBorder(BorderFactory.createEmptyBorder(pad, 20, pad, 20));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        /** 换皮肤：面板不重建，只把颜色刷一遍。 */
        public void applySkin(Skin s) {
            this.skin = s;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            boolean on = isSelected() || hover;
            if (isFocusOwner()) {
                on = true;
            }
            if (!isEnabled()) {
                on = false;
            }

            Color bg;
            Color fg;
            if (primary) {
                bg = on ? skin.accent : skin.button;
                fg = on ? contrastOn(skin.accent) : skin.buttonText;
            } else if (menuStyle) {
                // 主菜单那一列压在背景图上，保持半透明，图片还看得见。
                bg = on ? withAlpha(skin.accent, 46) : new Color(0, 0, 0, 0);
                fg = on ? skin.accent : skin.text;
            } else {
                // 选中/悬停用不透明的 buttonHover：
                // 原来这里填的是 16% 透明度的强调色，浮在棋盘上等于没画。
                // buttonHover 每套皮肤都比 button 亮一档，buttonText 压在上面够清楚。
                bg = on ? skin.buttonHover : skin.button;
                fg = skin.buttonText;
            }
            if (!isEnabled()) {
                fg = skin.buttonTextDim;
            }

            g2.setColor(bg);
            g2.fillRoundRect(0, 0, w, h, 10, 10);
            if (isSelected() || isFocusOwner()) {
                g2.setColor(skin.accent);
                // 按钮是 focusable=false，isFocusOwner 永远不成立，
                // 靠它决定描边粗细的话选中态就只有 1px，在棋盘上几乎看不见。
                g2.setStroke(new java.awt.BasicStroke(isSelected() || isFocusOwner() ? 2f : 1f));
                g2.drawRoundRect(1, 1, w - 3, h - 3, 10, 10);
            }
            g2.setColor(fg);
            g2.setFont(getFont());
            java.awt.FontMetrics fm = g2.getFontMetrics();
            int tx = (w - fm.stringWidth(getText())) / 2;
            int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(getText(), tx, ty);
            g2.dispose();
        }

        private static Color withAlpha(Color c, int alpha) {
            return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
        }

        /** 主按钮底色是强调色，文字得换成对比色才看得清。 */
        private static Color contrastOn(Color bg) {
            double lum = (0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue()) / 255.0;
            return lum > 0.6 ? new Color(0x111111) : new Color(0xFFFFFF);
        }
    }

    /** 给容器加上下留白。 */
    public static void pad(JComponent c, int top, int left, int bottom, int right) {
        c.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
    }

    public static Dimension size(int w, int h) {
        return new Dimension(w, h);
    }
}
