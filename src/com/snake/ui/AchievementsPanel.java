package com.snake.ui;

import com.snake.Achievement;
import com.snake.Achievements;
import com.snake.ConfigStore;
import com.snake.Navigator;
import com.snake.Skin;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * 成就页。每个成就一条，右侧一条自绘进度条：
 * 达成过的显示解锁时间，没达成的显示当前值 / 目标值。
 */
public final class AchievementsPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final Navigator nav;
    private Skin skin;

    private final JLabel headLabel = new JLabel();
    private final JPanel rows = new JPanel();
    private final JProgress progressBar = new JProgress();

    public AchievementsPanel(Navigator nav) {
        this.nav = nav;
        this.skin = nav.skin();
        build();
        refresh();
        applySkin(skin);
    }

    private void build() {
        setOpaque(true);
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(UiKit.sectionTitle(skin, "成就"), BorderLayout.WEST);
        headLabel.setFont(UiKit.font(12, UiKit.FONT_PLAIN_));
        headLabel.setForeground(skin.textDim);
        top.add(headLabel, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        progressBar.setPreferredSize(new Dimension(10, 10));
        add(progressBar, BorderLayout.NORTH);

        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(rows);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        JLabel hint = new JLabel("Esc 返回主菜单");
        hint.setFont(UiKit.font(12, UiKit.FONT_PLAIN_));
        footer.add(hint, BorderLayout.WEST);
        add(footer, BorderLayout.SOUTH);
    }

    /** 每次进入都要重画：刚解锁的成就只有回到这一页才看得见。 */
    public void refresh() {
        Achievements ach = ConfigStore.get().achievements();
        ConfigStore store = ConfigStore.get();
        headLabel.setText(ach.unlockedCount() + " / " + ach.all().size() + " 已解锁");
        progressBar.setValues(ach.unlockedCount(), ach.all().size(), store);

        rows.removeAll();
        for (Achievement a : ach.all()) {
            rows.add(entry(a, ach));
            rows.add(Box.createVerticalStrut(6));
        }
        rows.add(Box.createVerticalGlue());
        rows.revalidate();
        rows.repaint();
    }

    private JPanel entry(Achievement a, Achievements ach) {
        boolean done = ach.isUnlocked(a.id);
        JPanel p = new JPanel(new BorderLayout(12, 4));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(done ? skin.accent : skin.panelBorder),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel name = new JLabel(a.name + (done ? "  ✓" : ""));
        name.setFont(UiKit.font(15, UiKit.FONT_BOLD_));
        name.setForeground(done ? skin.accent : skin.text);

        JLabel desc = new JLabel(a.desc);
        desc.setFont(UiKit.font(12, UiKit.FONT_PLAIN_));
        desc.setForeground(skin.textDim);

        JPanel mid = new JPanel(new GridLayout(2, 1));
        mid.setOpaque(false);
        mid.add(name);
        mid.add(desc);

        JLabel state = new JLabel(stateText(a, ach, done));
        state.setFont(UiKit.font(12, done ? UiKit.FONT_BOLD_ : UiKit.FONT_PLAIN_));
        state.setForeground(done ? skin.accent : skin.textDim);
        state.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        state.setPreferredSize(new Dimension(150, 20));

        p.add(mid, BorderLayout.CENTER);
        p.add(state, BorderLayout.EAST);
        return p;
    }

    private String stateText(Achievement a, Achievements ach, boolean done) {
        if (done) {
            Long at = ach.unlockedAt(a.id);
            if (at == null) {
                return "已解锁";
            }
            return "已解锁 · " + new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date(at));
        }
        ConfigStore store = ConfigStore.get();
        int current;
        switch (a.type) {
            case SCORE:
                current = store.bestScore();
                break;
            case LENGTH:
                current = store.bestLength();
                break;
            case GAMES:
                current = store.gamesPlayed();
                break;
            case TOTAL_SCORE:
                current = store.totalScore();
                break;
            case SKINS_USED:
                current = store.skinsUsed();
                break;
            case DAILY_STREAK:
                current = store.dailyStreak();
                break;
            case RESUME:
                current = 0;
                break;
            case PERFECT:
            default:
                return "未解锁";
        }
        return a.progressText(Math.min(current, a.threshold), store.config().maxLength);
    }

    public void applySkin(Skin s) {
        this.skin = s;
        setBackground(s.bg);
        headLabel.setForeground(s.textDim);
        progressBar.applySkin(s);
        refresh();
    }

    /** 一条极简的总进度条，代替常见的「已解锁 7/12」文字里缺的那点视觉。 */
    private static final class JProgress extends JComponent {
        private static final long serialVersionUID = 1L;

        private Skin skin;
        private int done;
        private int total;

        void setValues(int done, int total, ConfigStore store) {
            this.done = done;
            this.total = Math.max(1, total);
            repaint();
        }

        void applySkin(Skin s) {
            this.skin = s;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (skin == null) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(skin.panelBg);
            g2.fillRoundRect(0, 0, w, h, h, h);
            int filled = (int) Math.round((double) w * done / total);
            if (filled > 0) {
                g2.setColor(skin.accent);
                g2.fillRoundRect(0, 0, Math.max(h, filled), h, h, h);
            }
            g2.dispose();
        }
    }
}
