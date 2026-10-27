package com.snake;

/**
 * 一条成就定义。判定方式是「当前值达到阈值」，进度条直接复用这个值。
 */
public final class Achievement {

    public enum Type {
        /** 单局分数 */
        SCORE,
        /** 单局长度 */
        LENGTH,
        /** 累计局数 */
        GAMES,
        /** 累计得分 */
        TOTAL_SCORE,
        /** 用过的皮肤数量 */
        SKINS_USED,
        /** 每日挑战连续天数 */
        DAILY_STREAK,
        /** 吃满一局（长度达到该局上限） */
        PERFECT,
        /** 从存档恢复过对局 */
        RESUME
    }

    public final String id;
    public final String name;
    public final String desc;
    public final Type type;
    public final int threshold;
    /** PERFECT 类成就是否只看经典模式。 */
    public final boolean classicOnly;

    public Achievement(String id, String name, String desc, Type type, int threshold,
                       boolean classicOnly) {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.type = type;
        this.threshold = threshold;
        this.classicOnly = classicOnly;
    }

    public String progressText(int current, int maxLength) {
        switch (type) {
            case SCORE:
                return current + " / " + threshold + " 分";
            case LENGTH:
                return current + " / " + threshold + " 节";
            case GAMES:
                return current + " / " + threshold + " 局";
            case TOTAL_SCORE:
                return current + " / " + threshold + " 分";
            case SKINS_USED:
                return current + " / " + threshold + " 套";
            case DAILY_STREAK:
                return current + " / " + threshold + " 天";
            case PERFECT:
                return "填满一局（" + maxLength + " 节）";
            case RESUME:
                return current > 0 ? "已完成" : "未完成";
            default:
                return current + " / " + threshold;
        }
    }
}
