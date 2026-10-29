package com.snake.ui;

import com.snake.GameConfig;
import com.snake.Skin;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * 玩法说明。只读页面，纯文字。
 */
public final class HelpPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final String[][] KEYS = {
            {"方向键 / WASD", "控制蛇移动"},
            {"Esc", "打开或关闭暂停菜单"},
            {"空格", "游戏中暂停 / 继续；结束后重开"},
            {"Enter / R", "游戏结束后重开"},
            {"B", "解锁后打开商店（再按 B 或 Esc 关闭）"},
    };

    private static final String[][] TIPS = {
            {"经典有限棋盘", "固定大小的棋盘。可以选择撞墙结束，或者从对面穿出。"},
            {"无尽无限地图", "没有边界，镜头跟着蛇头走。食物按可见区域自动生成，走出视野的旧食物会被清掉。"},
            {"同屏食物", "经典模式可以设置同时出现的数量；无尽模式不再限制，按可见面积自动补充。"},
            {"长度上限", "吃满上限后蛇身不再变长。经典模式不再生成新食物；无尽模式仍会生成，吃食物只加分。"},
            {"大病毒", "无尽模式中出现 2×2～10×10 格带刺病毒。蛇头碰到会削减并反向击退 3～5 格，病毒保留。"},
            {"小病毒", "一格小病毒每两回合追踪一次，碰到蛇头削减 1 格后消失。命中后有 4 回合病毒伤害免疫，长度降为 0 游戏结束。"},
            {"猪头与商店", "蛇第一次吃满长度上限会出现猪头。碰到后听完 7 句对话，当前存档解锁商店，按 B 打开。"},
            {"商店货币", "使用蛇身长度交换商品；当前版本货架为空，后续会加入可购买内容。"},
            {"每日挑战", "每天的地图参数是固定的，所有人同一天拿到的是同一局。"},
            {"继续游戏", "局中退出后进度会保存，下次从主菜单「继续游戏」原样恢复。"},
            {"背景图片", "可以在设置里导入 PNG，作为菜单和游戏区的背景。"},
    };

    private final JPanel content = new JPanel();
    private Skin skin;

    public HelpPanel(Skin skin) {
        this.skin = skin;
        setOpaque(false);
        setLayout(new BorderLayout());
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        content.add(UiKit.sectionTitle(skin, "按键"));
        content.add(Box.createVerticalStrut(6));
        for (String[] k : KEYS) {
            content.add(row(k[0], k[1]));
        }
        content.add(Box.createVerticalStrut(16));

        content.add(UiKit.sectionTitle(skin, "玩法"));
        content.add(Box.createVerticalStrut(6));
        for (String[] t : TIPS) {
            content.add(row(t[0], t[1]));
        }
        content.add(Box.createVerticalStrut(16));

        content.add(UiKit.sectionTitle(skin, "速度档位"));
        content.add(Box.createVerticalStrut(6));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < GameConfig.TICK_NAMES.length; i++) {
            if (i > 0) {
                sb.append("　");
            }
            sb.append(GameConfig.TICK_NAMES[i]).append(' ')
              .append(GameConfig.TICK_LEVELS[i]).append("ms");
        }
        JLabel speeds = new JLabel(sb.toString());
        speeds.setFont(UiKit.font(13, UiKit.FONT_PLAIN_));
        speeds.setAlignmentX(LEFT_ALIGNMENT);
        content.add(speeds);
        content.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    private JComponent row(String key, String desc) {
        JPanel p = new JPanel(new BorderLayout(16, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel k = new JLabel(key);
        k.setFont(UiKit.font(13, UiKit.FONT_BOLD_));
        k.setPreferredSize(new Dimension(150, 20));
        JLabel d = new JLabel(desc);
        d.setFont(UiKit.font(13, UiKit.FONT_PLAIN_));
        p.add(k, BorderLayout.WEST);
        p.add(d, BorderLayout.CENTER);
        return p;
    }

    public void applySkin(Skin s) {
        this.skin = s;
        for (Component c : content.getComponents()) {
            if (c instanceof JPanel) {
                JPanel p = (JPanel) c;
                p.setOpaque(false);
                for (Component ch : p.getComponents()) {
                    if (ch instanceof JLabel) {
                        ((JLabel) ch).setForeground(s.textDim);
                    }
                }
            } else if (c instanceof JLabel) {
                JLabel l = (JLabel) c;
                l.setForeground(l.getText().startsWith("按键")
                        || l.getText().startsWith("玩法")
                        || l.getText().startsWith("速度") ? s.text : s.textDim);
            }
        }
        repaint();
    }
}
