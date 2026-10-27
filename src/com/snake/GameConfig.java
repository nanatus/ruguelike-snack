package com.snake;

/**
 * 一局游戏的配置快照。所有设置面板直接绑定这些字段（可变），
 * 开局时调用 {@link #deepCopy()} 冻结一份，避免局中改设置影响正在跑的对局。
 */
public final class GameConfig {

    /** 地图类型：经典有限棋盘 / 无尽无限地图。 */
    public enum Mode {
        CLASSIC("经典有限"),
        ENDLESS("无尽无限");

        private final String label;

        Mode(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /** 这一局的来源。 */
    public enum Challenge {
        NONE("普通对局"),
        DAILY("每日挑战"),
        RANDOM("随机一局");

        private final String label;

        Challenge(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /** 速度档位，界面用固定档位而不是连续数值，方便命名与显示。 */
    public static final int[] TICK_LEVELS = {200, 170, 140, 120, 100, 70};
    public static final String[] TICK_NAMES = {"悠闲", "舒缓", "普通", "灵敏", "疾速", "狂飙"};

    public static final int MIN_COLS = 11;
    public static final int MAX_COLS = 45;
    public static final int MIN_ROWS = 11;
    public static final int MAX_ROWS = 32;
    public static final int MIN_FOODS = 1;
    public static final int MAX_FOODS = 5;
    public static final int MIN_LENGTH = 5;
    public static final int MAX_LENGTH_CAP = 99;

    public Mode mode = Mode.CLASSIC;
    /** 经典模式的棋盘格数；无尽模式忽略。 */
    public int cols = 25;
    public int rows = 25;
    /** 仅经典模式：撞到边界时从对面穿出。 */
    public boolean wrapWalls = false;
    public int tickMs = 120;
    public int foodCount = 1;
    public int maxLength = 30;
    public Challenge challenge = Challenge.NONE;
    /** 每日/随机挑战的种子，0 表示无固定种子。 */
    public long seed = 0L;

    public GameConfig deepCopy() {
        GameConfig c = new GameConfig();
        c.mode = mode;
        c.cols = cols;
        c.rows = rows;
        c.wrapWalls = wrapWalls;
        c.tickMs = tickMs;
        c.foodCount = foodCount;
        c.maxLength = maxLength;
        c.challenge = challenge;
        c.seed = seed;
        return c;
    }

    /** 把越界或非法的数值夹回可用区间，设置界面与存档解析共用。 */
    public GameConfig sanitize() {
        cols = clamp(cols, MIN_COLS, MAX_COLS);
        rows = clamp(rows, MIN_ROWS, MAX_ROWS);
        foodCount = clamp(foodCount, MIN_FOODS, MAX_FOODS);
        maxLength = clamp(maxLength, MIN_LENGTH, MAX_LENGTH_CAP);
        if (speedIndexOf(tickMs) < 0) {
            tickMs = 120;
        }
        if (mode == null) {
            mode = Mode.CLASSIC;
        }
        if (challenge == null) {
            challenge = Challenge.NONE;
        }
        return this;
    }

    /** 找到 tickMs 所属档位下标，不属于任何档位时返回 -1。 */
    public static int speedIndexOf(int tickMs) {
        for (int i = 0; i < TICK_LEVELS.length; i++) {
            if (TICK_LEVELS[i] == tickMs) {
                return i;
            }
        }
        return -1;
    }

    public static int speedIndexOfOrDefault(int tickMs) {
        int i = speedIndexOf(tickMs);
        return i < 0 ? 3 : i;
    }

    public static int speedLabel(int tickMs) {
        return speedIndexOfOrDefault(tickMs);
    }

    public static String speedName(int tickMs) {
        return TICK_NAMES[speedLabel(tickMs)];
    }

    /** 一行摘要，用于挑战简介、存档按钮、暂停菜单与榜单记录。 */
    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append(mode.label());
        if (mode == Mode.CLASSIC) {
            sb.append(' ').append(cols).append('×').append(rows);
            sb.append(wrapWalls ? " · 穿墙" : " · 撞墙死");
        } else {
            sb.append(" · 无限");
        }
        if (mode == Mode.ENDLESS) {
            sb.append(" · 自动食物");
        } else {
            sb.append(" · ").append(foodCount).append(" 食物");
        }
        sb.append(" · ").append(speedName(tickMs));
        return sb.toString();
    }

    public static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
