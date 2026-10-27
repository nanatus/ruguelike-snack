package com.snake;

import java.time.LocalDate;
import java.util.Random;

/**
 * 随机一局 / 每日挑战的配置生成。
 *
 * 每日挑战用日期的 epochDay 当种子，所以同一天任何人、任何时候生成的参数都完全一致；
 * 随机一局则用真随机种子。
 */
public final class ChallengeFactory {

    private ChallengeFactory() {
    }

    /** 棋盘尺寸候选，避免出现特别憋屈的比例。 */
    private static final int[] EVEN_SIZES = {15, 20, 25, 30, 35, 40};

    public static long todayEpochDay() {
        return LocalDate.now().toEpochDay();
    }

    public static GameConfig daily(long epochDay) {
        GameConfig c = fromRandom(new Random(epochDay * 0x9E3779B97F4A7C15L));
        c.seed = epochDay;
        c.challenge = GameConfig.Challenge.DAILY;
        return c;
    }

    public static GameConfig daily() {
        return daily(todayEpochDay());
    }

    public static GameConfig random() {
        GameConfig c = fromRandom(new Random());
        c.seed = System.nanoTime();
        c.challenge = GameConfig.Challenge.RANDOM;
        return c;
    }

    /** 基于玩家当前设置做「相邻难度的微调」，比完全随机更贴近玩家口味。 */
    public static GameConfig randomNear(GameConfig base) {
        GameConfig c = base.deepCopy();
        Random r = new Random();
        c.mode = r.nextInt(4) == 0 ? GameConfig.Mode.CLASSIC : base.mode;
        c.cols = pick(r, EVEN_SIZES);
        c.rows = pick(r, EVEN_SIZES);
        c.wrapWalls = r.nextBoolean();
        int speed = GameConfig.speedIndexOfOrDefault(base.tickMs);
        c.tickMs = GameConfig.TICK_LEVELS[GameConfig.clamp(speed + r.nextInt(3) - 1, 0, 5)];
        c.foodCount = 1 + r.nextInt(Math.min(5, base.foodCount + 2));
        c.maxLength = GameConfig.clamp(base.maxLength, GameConfig.MIN_LENGTH, GameConfig.MAX_LENGTH_CAP);
        c.seed = System.nanoTime();
        c.challenge = GameConfig.Challenge.RANDOM;
        return c.sanitize();
    }

    private static GameConfig fromRandom(Random r) {
        GameConfig c = new GameConfig();
        c.mode = r.nextInt(5) == 0 ? GameConfig.Mode.ENDLESS : GameConfig.Mode.CLASSIC;
        c.cols = pick(r, EVEN_SIZES);
        c.rows = pick(r, EVEN_SIZES);
        c.wrapWalls = r.nextInt(3) == 0;
        c.tickMs = pick(r, GameConfig.TICK_LEVELS);
        c.foodCount = 1 + r.nextInt(5);
        c.maxLength = 20 + r.nextInt(21);
        return c.sanitize();
    }

    private static int pick(Random r, int[] values) {
        return values[r.nextInt(values.length)];
    }
}
