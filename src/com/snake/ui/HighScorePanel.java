package com.snake.ui;

import com.snake.ConfigStore;
import com.snake.HighScores;
import com.snake.Navigator;
import com.snake.ScoreRecord;
import com.snake.Skin;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

/**
 * 排行榜。本地 Top 10，每条都带上当时的配置快照，
 * 这样以后改了设置也能看懂旧记录是什么难度拿到的。
 */
public final class HighScorePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final Navigator nav;
    private Skin skin;

    private final JLabel headLabel = new JLabel();
    private final JPanel rows = new JPanel();
    private final UiKit.FlatButton clearBtn;

    public HighScorePanel(Navigator nav) {
        this.nav = nav;
        this.skin = nav.skin();
        this.clearBtn = UiKit.button(skin, "清空榜单", 14, false);
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
        top.add(UiKit.sectionTitle(skin, "排行榜"), BorderLayout.WEST);
        JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topRight.setOpaque(false);
        topRight.add(headLabel);
        topRight.add(clearBtn);
        top.add(topRight, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

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

        clearBtn.addActionListener(e -> {
            if (UiKit.confirm(this, "清空榜单", "确定要清空全部本地记录吗？此操作不可撤销。")) {
                ConfigStore store = ConfigStore.get();
                store.clearHighScores();
                store.saveScores();
                refresh();
            }
        });
    }

    /** 每次进入都要重画：刚打完一局回来时榜单必然变了。 */
    public void refresh() {
        HighScores hs = ConfigStore.get().highScores();
        List<ScoreRecord> recs = hs.records();
        headLabel.setText(recs.size() + " / " + HighScores.CAPACITY + " 条");

        rows.removeAll();
        if (recs.isEmpty()) {
            rows.add(UiKit.dim(skin, "还没有记录，先去玩一局吧。", 14));
        } else {
            for (int i = 0; i < recs.size(); i++) {
                rows.add(entry(i, recs.get(i)));
                rows.add(Box.createVerticalStrut(6));
            }
        }
        rows.add(Box.createVerticalGlue());
        rows.revalidate();
        rows.repaint();
    }

    private JPanel entry(int i, ScoreRecord r) {
        String tag = r.tag();
        JPanel p = new JPanel(new BorderLayout(12, 2));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(skin.panelBorder),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel rank = new JLabel(String.valueOf(i + 1), SwingConstants.CENTER);
        rank.setFont(UiKit.font(16, UiKit.FONT_BOLD_));
        rank.setForeground(i == 0 ? skin.accent : skin.textDim);
        rank.setPreferredSize(new Dimension(26, 26));

        JLabel score = new JLabel(String.valueOf(r.score));
        score.setFont(UiKit.font(17, UiKit.FONT_BOLD_));
        score.setForeground(skin.text);
        score.setPreferredSize(new Dimension(72, 24));

        JLabel meta = new JLabel(r.configSummary() + " · " + r.length + " 节 · " + r.durationText());
        meta.setFont(UiKit.font(12, UiKit.FONT_PLAIN_));
        meta.setForeground(skin.textDim);
        JLabel skinAndTime = new JLabel(r.skinName + " · " + r.dateText());
        skinAndTime.setFont(UiKit.font(12, UiKit.FONT_PLAIN_));
        skinAndTime.setForeground(skin.textDim);

        JPanel mid = new JPanel(new GridLayout(2, 1));
        mid.setOpaque(false);
        JPanel line1 = new JPanel(new BorderLayout(8, 0));
        line1.setOpaque(false);
        line1.add(score, BorderLayout.WEST);
        line1.add(meta, BorderLayout.CENTER);
        mid.add(line1);
        mid.add(skinAndTime);

        JLabel badge = new JLabel(tag);
        badge.setFont(UiKit.font(12, UiKit.FONT_BOLD_));
        badge.setForeground(skin.accent);

        p.add(rank, BorderLayout.WEST);
        p.add(mid, BorderLayout.CENTER);
        if (!tag.isEmpty()) {
            JPanel east = new JPanel(new BorderLayout());
            east.setOpaque(false);
            east.add(badge, BorderLayout.NORTH);
            p.add(east, BorderLayout.EAST);
        }
        return p;
    }

    public void applySkin(Skin s) {
        this.skin = s;
        setBackground(s.bg);
        headLabel.setForeground(s.textDim);
        clearBtn.applySkin(s);
        refresh();
    }
}
