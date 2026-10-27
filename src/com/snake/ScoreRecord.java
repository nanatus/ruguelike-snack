package com.snake;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 榜单里的一条记录。保存当时的对局配置快照，
 * 这样以后改了设置，旧记录仍然能说明它是在什么难度下拿到的。
 */
public final class ScoreRecord implements Comparable<ScoreRecord> {

    private static final String SEP = ",";

    public final int score;
    public final int length;
    public final GameConfig.Mode mode;
    public final int cols;
    public final int rows;
    public final boolean wrapWalls;
    public final int foodCount;
    public final int tickMs;
    public final String skinName;
    public final GameConfig.Challenge challenge;
    /** 每日挑战的 epochDay，其它挑战为 0。 */
    public final long seedDay;
    /** 本局存活秒数。 */
    public final long durationSec;
    /** 记录写入时刻。 */
    public final long when;
    /** 局中主动退出而非正常死亡。 */
    public final boolean abandoned;

    public ScoreRecord(int score, int length, GameConfig config, String skinName,
                       long durationSec, long when, boolean abandoned) {
        this(score, length, config, skinName, durationSec, when, abandoned, false);
    }

    private ScoreRecord(int score, int length, GameConfig config, String skinName,
                        long durationSec, long when, boolean abandoned, boolean keepFoodCount) {
        this.score = score;
        this.length = length;
        this.mode = config.mode;
        this.cols = config.cols;
        this.rows = config.rows;
        this.wrapWalls = config.wrapWalls;
        this.foodCount = keepFoodCount ? config.foodCount
                : (config.mode == GameConfig.Mode.ENDLESS ? 0 : config.foodCount);
        this.tickMs = config.tickMs;
        this.skinName = skinName == null ? "未知" : skinName;
        this.challenge = config.challenge;
        this.seedDay = config.challenge == GameConfig.Challenge.DAILY ? config.seed : 0L;
        this.durationSec = durationSec;
        this.when = when;
        this.abandoned = abandoned;
    }

    public String configSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(mode.label());
        if (mode == GameConfig.Mode.CLASSIC) {
            sb.append(' ').append(cols).append('×').append(rows);
            sb.append(wrapWalls ? " 穿墙" : " 撞墙死");
        } else {
            sb.append(" 无限");
        }
        if (foodCount == 0) {
            sb.append(" · 自动食物");
        } else {
            sb.append(" · ").append(foodCount).append("食物");
        }
        sb.append(" · ").append(GameConfig.speedName(tickMs));
        if (challenge != GameConfig.Challenge.NONE) {
            sb.append(" · ").append(challenge.label());
        }
        return sb.toString();
    }

    public String tag() {
        if (abandoned) {
            return "中途退出";
        }
        if (challenge == GameConfig.Challenge.DAILY) {
            return "每日";
        }
        if (challenge == GameConfig.Challenge.RANDOM) {
            return "随机";
        }
        return "";
    }

    public String dateText() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(new Date(when));
    }

    public String durationText() {
        long s = Math.max(0, durationSec);
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        if (h > 0) {
            return String.format(Locale.CHINA, "%d:%02d:%02d", h, m, sec);
        }
        return String.format(Locale.CHINA, "%02d:%02d", m, sec);
    }

    /** 分数降序；同分时先写入的排前面。 */
    @Override
    public int compareTo(ScoreRecord o) {
        if (score != o.score) {
            return o.score - score;
        }
        return Long.compare(o.when, when);
    }

    /** 持久化用的一行：字段之间逗号分隔，记录之间由 HighScores 负责拼接。 */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(score).append(SEP)
          .append(length).append(SEP)
          .append(mode.name()).append(SEP)
          .append(cols).append(SEP)
          .append(rows).append(SEP)
          .append(wrapWalls ? 1 : 0).append(SEP)
          .append(foodCount).append(SEP)
          .append(tickMs).append(SEP)
          .append(encode(skinName)).append(SEP)
          .append(challenge.name()).append(SEP)
          .append(durationSec).append(SEP)
          .append(when).append(SEP)
          .append(abandoned ? 1 : 0).append(SEP)
          .append(seedDay);
        return sb.toString();
    }

    public static ScoreRecord deserialize(String line) {
        if (line == null || line.isEmpty()) {
            return null;
        }
        String[] f = line.split(SEP, -1);
        if (f.length < 13) {
            return null;
        }
        try {
            GameConfig c = new GameConfig();
            c.mode = GameConfig.Mode.valueOf(f[2]);
            c.cols = Integer.parseInt(f[3]);
            c.rows = Integer.parseInt(f[4]);
            c.wrapWalls = "1".equals(f[5]);
            c.foodCount = Integer.parseInt(f[6]);
            c.tickMs = Integer.parseInt(f[7]);
            c.challenge = GameConfig.Challenge.valueOf(f[9]);
            // 旧记录没有 seedDay（第 14 段），按 0 处理
            long day = f.length > 13 ? Long.parseLong(f[13]) : 0L;
            c.seed = day;
            return new ScoreRecord(
                    Integer.parseInt(f[0]),
                    Integer.parseInt(f[1]),
                    c,
                    decode(f[8]),
                    Long.parseLong(f[10]),
                    Long.parseLong(f[11]),
                    "1".equals(f[12]),
                    true);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** 皮肤名可能含逗号，写入时用 # 编码。 */
    static String encode(String s) {
        return s.replace(",", "#").replace(";", "#");
    }

    static String decode(String s) {
        return s.replace('#', ',');
    }
}
