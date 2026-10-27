package com.snake;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Top N 榜单。分数降序，同分时先达到的排前面。
 */
public final class HighScores {

    public static final int CAPACITY = 10;
    private static final String SEP = ";";

    private final List<ScoreRecord> records = new ArrayList<ScoreRecord>();

    public List<ScoreRecord> records() {
        return Collections.unmodifiableList(records);
    }

    public boolean isEmpty() {
        return records.isEmpty();
    }

    public int best() {
        return records.isEmpty() ? 0 : records.get(0).score;
    }

    /**
     * 收录一条记录并保持排序。分数为 0 的空局不入榜。
     *
     * 「同一局只记更高分」的判定不在这里，而在 GameScreen：
     * 它拿本局已写入的分数（SaveState.recordedScore）作比较，
     * 因为要防的是续局重复刷榜，而不是分数低就该不记录。
     */
    public boolean add(ScoreRecord r) {
        if (r == null || r.score <= 0) {
            return false;
        }
        records.add(r);
        Collections.sort(records);
        while (records.size() > CAPACITY) {
            records.remove(records.size() - 1);
        }
        return true;
    }

    /** 1-based 名次，不在榜上返回 0。 */
    public int rank(int score) {
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).score <= score) {
                return i + 1;
            }
        }
        return 0;
    }

    public void clear() {
        records.clear();
    }

    public String serialize() {        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < records.size(); i++) {
            if (i > 0) {
                sb.append(SEP);
            }
            sb.append(records.get(i).serialize());
        }
        return sb.toString();
    }

    public static HighScores deserialize(String s) {
        HighScores hs = new HighScores();
        if (s == null || s.isEmpty()) {
            return hs;
        }
        String[] lines = s.split(SEP, -1);
        for (String line : lines) {
            ScoreRecord r = ScoreRecord.deserialize(line);
            if (r != null) {
                hs.records.add(r);
            }
        }
        Collections.sort(hs.records);
        while (hs.records.size() > CAPACITY) {
            hs.records.remove(hs.records.size() - 1);
        }
        return hs;
    }
}
