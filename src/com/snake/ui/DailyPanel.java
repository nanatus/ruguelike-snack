package com.snake.ui;

import com.snake.ChallengeFactory;
import com.snake.ConfigStore;
import com.snake.GameConfig;
import com.snake.HighScores;
import com.snake.Navigator;
import com.snake.ScoreRecord;
import com.snake.Skin;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 每日挑战页。上下两张卡片：
 * 上面是「今日挑战」（同一天参数固定），下面是「随机一局」（每次重抽）。
 *
 * 随机挑战原本是主菜单上的独立入口，按需求合并到这里，
 * 都在「开始前看一眼参数」的位置，玩家不用来回切页。
 */
public final class DailyPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final Navigator nav;
    private Skin skin;

    private final JLabel dateLabel = new JLabel();
    private final JLabel streakLabel = new JLabel();
    private final JLabel dailyParam = new JLabel();
    private final JLabel dailyBest = new JLabel();
    private final UiKit.FlatButton dailyStart;
    private final UiKit.FlatButton dailyDetail;

    private final JLabel randomParam = new JLabel();
    private final UiKit.FlatButton randomReroll;
    private final UiKit.FlatButton randomStart;

    private GameConfig today;
    private GameConfig random;

    public DailyPanel(Navigator nav) {
        this.nav = nav;
        this.skin = nav.skin();
        this.dailyStart = UiKit.button(skin, "开始今日挑战", 15, true);
        this.dailyDetail = UiKit.button(skin, "调整参数", 15, false);
        this.randomReroll = UiKit.button(skin, "换一局", 15, false);
        this.randomStart = UiKit.button(skin, "开始", 15, true);

        today = ChallengeFactory.daily();
        random = ChallengeFactory.randomNear(nav.config());
        build();
        refresh();
        applySkin(skin);
    }

    private void build() {
        setOpaque(true);
        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        col.add(UiKit.sectionTitle(skin, "今日挑战"));
        col.add(Box.createVerticalStrut(8));
        JPanel dailyCard = card();
        JPanel dailyBody = new JPanel();
        dailyBody.setOpaque(false);
        dailyBody.setLayout(new BoxLayout(dailyBody, BoxLayout.Y_AXIS));
        dailyBody.add(dateLabel);
        dailyBody.add(streakLabel);
        dailyBody.add(dailyParam);
        dailyBody.add(dailyBest);
        dailyCard.add(dailyBody, BorderLayout.CENTER);
        dailyCard.add(row(dailyStart, dailyDetail), BorderLayout.EAST);
        col.add(dailyCard);
        col.add(Box.createVerticalStrut(18));

        col.add(UiKit.sectionTitle(skin, "随机一局"));
        col.add(Box.createVerticalStrut(8));
        JPanel randCard = card();
        randCard.add(randomParam, BorderLayout.CENTER);
        randCard.add(row(randomReroll, randomStart), BorderLayout.EAST);
        col.add(randCard);
        col.add(Box.createVerticalGlue());

        add(col, BorderLayout.CENTER);
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        footer.setOpaque(false);
        JLabel back = new JLabel("Esc 返回主菜单");
        back.setFont(UiKit.font(12, UiKit.FONT_PLAIN_));
        footer.add(back);
        add(footer, BorderLayout.SOUTH);

        dailyStart.addActionListener(e -> nav.startNewGame(today.deepCopy()));
        dailyDetail.addActionListener(e -> {
            // 把今日参数灌进设置，玩家可以在此基础上微调再打
            GameConfig c = nav.config();
            c.mode = today.mode;
            c.cols = today.cols;
            c.rows = today.rows;
            c.wrapWalls = today.wrapWalls;
            c.tickMs = today.tickMs;
            c.foodCount = today.foodCount;
            c.maxLength = today.maxLength;
            nav.show(Navigator.CARD_SETUP);
        });
        randomReroll.addActionListener(e -> {
            random = ChallengeFactory.randomNear(nav.config());
            refresh();
        });
        randomStart.addActionListener(e -> nav.startNewGame(random.deepCopy()));
    }

    private JPanel card() {
        JPanel p = new JPanel(new BorderLayout(14, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(skin.panelBorder),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        return p;
    }

    private JPanel row(JComponent... items) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        for (JComponent c : items) {
            p.add(c);
        }
        return p;
    }

    /** 每次进入都要重算：日期会翻篇，成绩和连续天数也会变。 */
    public void refresh() {
        LocalDate d = LocalDate.now();
        ConfigStore store = ConfigStore.get();
        dateLabel.setText(d.format(DateTimeFormatter.ofPattern("yyyy 年 M 月 d 日")));
        streakLabel.setText("连续挑战 " + store.dailyStreak() + " 天"
                + (store.dailyStreakAlive(ChallengeFactory.todayEpochDay()) ? "" : "（今天还没玩）"));
        dailyParam.setText(today.summary() + " · 皮肤 " + store.skin().name);
        dailyBest.setText("今日最高 " + todayBest());

        boolean alive = store.dailyStreakAlive(ChallengeFactory.todayEpochDay());
        dailyStart.setText(alive ? "开始今日挑战" : "今天已结束，明天再来");
        dailyStart.setEnabled(alive);
        dailyDetail.setEnabled(alive);

        randomParam.setText(random.summary() + " · 皮肤 " + store.skin().name);
    }

    /** 今天这个种子下已经拿到过的最高分。 */
    private int todayBest() {
        ConfigStore store = ConfigStore.get();
        long start = java.time.LocalDate.now()
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        long end = start + 24L * 3600 * 1000;
        int best = 0;
        for (ScoreRecord r : store.highScores().records()) {
            if (r.challenge == GameConfig.Challenge.DAILY
                    && r.seedDay == ChallengeFactory.todayEpochDay()
                    && r.when >= start && r.when < end) {
                best = Math.max(best, r.score);
            }
        }
        return best;
    }

    public void applySkin(Skin s) {
        this.skin = s;
        setBackground(s.bg);
        dateLabel.setForeground(s.text);
        streakLabel.setForeground(s.textDim);
        dailyParam.setForeground(s.accent);
        dailyBest.setForeground(s.textDim);
        randomParam.setForeground(s.accent);
        for (UiKit.FlatButton b : new UiKit.FlatButton[]{
                dailyStart, dailyDetail, randomReroll, randomStart}) {
            b.applySkin(s);
        }
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
