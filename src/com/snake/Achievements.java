package com.snake;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 成就定义表 + 解锁判定。
 *
 * 判定入口 {@link #check(Progress)} 在每局结束时调用一次，
 * 返回本次新解锁的成就列表交给 Toast 展示。
 */
public final class Achievements {

    /** 一局结束后用于判定的累计数据。 */
    public static final class Progress {
        public int lastScore;
        public int lastLength;
        public int maxLengthThisRun;
        public boolean lastClassic;
        public boolean lastFilled;
        public int gamesPlayed;
        public int totalScore;
        public int skinsUsed;
        public int dailyStreak;
        /** 本局是否由存档恢复而来。 */
        public boolean resumed;
    }

    public static final List<Achievement> ALL = Collections.unmodifiableList(build());

    private final Map<String, Long> unlocked = new LinkedHashMap<String, Long>();
    private final List<Achievement> newlyUnlocked = new ArrayList<Achievement>();

    public List<Achievement> all() {
        return ALL;
    }

    public boolean isUnlocked(String id) {
        return unlocked.containsKey(id);
    }

    public Long unlockedAt(String id) {
        return unlocked.get(id);
    }

    public int unlockedCount() {
        return unlocked.size();
    }

    public boolean isAllUnlocked() {
        return unlocked.size() >= ALL.size();
    }

    /** 本次调用中新解锁的成就，取走后清空。 */
    public List<Achievement> drainNewly() {
        List<Achievement> copy = new ArrayList<Achievement>(newlyUnlocked);
        newlyUnlocked.clear();
        return copy;
    }

    public List<Achievement> check(Progress p) {
        newlyUnlocked.clear();
        long now = System.currentTimeMillis();
        for (Achievement a : ALL) {
            if (unlocked.containsKey(a.id)) {
                continue;
            }
            if (isSatisfied(a, p)) {
                unlocked.put(a.id, now);
                newlyUnlocked.add(a);
            }
        }
        return new ArrayList<Achievement>(newlyUnlocked);
    }

    private boolean isSatisfied(Achievement a, Progress p) {
        switch (a.type) {
            case SCORE:
                return p.lastScore >= a.threshold;
            case LENGTH:
                return p.lastLength >= a.threshold;
            case GAMES:
                return p.gamesPlayed >= a.threshold;
            case TOTAL_SCORE:
                return p.totalScore >= a.threshold;
            case SKINS_USED:
                return p.skinsUsed >= a.threshold;
            case DAILY_STREAK:
                return p.dailyStreak >= a.threshold;
            case PERFECT:
                if (a.classicOnly && !p.lastClassic) {
                    return false;
                }
                return p.lastFilled;
            case RESUME:
                return p.resumed;
            default:
                return false;
        }
    }

    public String serialize() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Long> e : unlocked.entrySet()) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(e.getKey()).append(',').append(e.getValue());
        }
        return sb.toString();
    }

    public void deserialize(String s) {
        unlocked.clear();
        if (s == null || s.isEmpty()) {
            return;
        }
        for (String line : s.split(";", -1)) {
            int c = line.indexOf(',');
            if (c <= 0) {
                continue;
            }
            try {
                unlocked.put(line.substring(0, c), Long.parseLong(line.substring(c + 1)));
            } catch (RuntimeException ignored) {
                // 单条损坏就跳过，不影响其它成就
            }
        }
    }

    private static List<Achievement> build() {
        List<Achievement> l = new ArrayList<Achievement>();
        l.add(new Achievement("first_bite", "初次进食", "单局拿到 1 分", Achievement.Type.SCORE, 1, false));
        l.add(new Achievement("snack", "小有所成", "单局拿到 25 分", Achievement.Type.SCORE, 25, false));
        l.add(new Achievement("glutton", "贪吃专家", "单局拿到 100 分", Achievement.Type.SCORE, 100, false));
        l.add(new Achievement("endless_legend", "无尽之蛇", "单局拿到 300 分", Achievement.Type.SCORE, 300, false));
        l.add(new Achievement("slender", "修长", "单局长度达到 20 节", Achievement.Type.LENGTH, 20, false));
        l.add(new Achievement("full_body", "圆满", "单局长度填满上限", Achievement.Type.PERFECT, 0, true));
        l.add(new Achievement("rookie", "新手上路", "累计游玩 10 局", Achievement.Type.GAMES, 10, false));
        l.add(new Achievement("veteran", "老玩家", "累计游玩 100 局", Achievement.Type.GAMES, 100, false));
        l.add(new Achievement("savings", "积少成多", "累计得分达到 1000", Achievement.Type.TOTAL_SCORE, 1000, false));
        l.add(new Achievement("collector", "集邮者", "用满全部皮肤", Achievement.Type.SKINS_USED, 6, false));
        l.add(new Achievement("streak3", "三日连勤", "每日挑战连续 3 天", Achievement.Type.DAILY_STREAK, 3, false));
        l.add(new Achievement("comeback", "续写", "从存档恢复过一局", Achievement.Type.RESUME, 1, false));
        return Collections.unmodifiableList(l);
    }
}
