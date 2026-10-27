package com.snake;

import java.util.ArrayList;
import java.util.List;

/**
 * 「继续游戏」的存档：完整还原上一次中途退出的那一局。
 *
 * 注意这里存的是**局内配置与皮肤**，不是玩家当前设置——
 * 换了设置再回来续局，应该接着原来那局打，而不是变成新规则。
 */
public final class SaveState {

    public static final int VERSION = 1;

    public GameConfig config;
    public String skinId;
    public Direction dir;
    /** 索引 0 是蛇头。 */
    public ArrayList<Point> body;
    /** 还没被消费的输入，恢复后原样灌回，避免丢按键。 */
    public ArrayList<Direction> pending;
    public ArrayList<Point> foods;
    /** 本存档是否已通过猪头对话解锁商店。 */
    public boolean shopUnlocked;
    /** 本局是否已经生成过猪头，防止续局后重复生成。 */
    public boolean pigDropped;
    /** 当前猪头位置；已被碰到或尚未生成时为 null。 */
    public Point pigPos;
    public int score;
    /** 本局开始的时刻，用于算存活时长。 */
    public long startedAt;
    public long savedAt;
    /** 已经写进榜单的分数；新分数超过它才允许再记一次，防止续局重复刷榜。 */
    public int recordedScore = -1;
    /** 本局是否由存档恢复而来（成就用）。 */
    public boolean resumed;

    public SaveState() {
        config = new GameConfig();
        skinId = Skins.defaultSkin().id;
        dir = Direction.RIGHT;
        body = new ArrayList<Point>();
        pending = new ArrayList<Direction>();
        foods = new ArrayList<Point>();
        startedAt = System.currentTimeMillis();
        savedAt = startedAt;
    }

    public Skin skin() {
        return Skins.byId(skinId);
    }

    public long durationSec() {
        return Math.max(0, (savedAt - startedAt) / 1000);
    }

    public String modeLabel() {
        return config.mode == GameConfig.Mode.CLASSIC
                ? "经典 " + config.cols + "×" + config.rows
                : "无尽无限";
    }

    /** 主菜单「继续游戏」按钮下方那行摘要。 */
    public String summary() {
        return modeLabel()
                + " · 分数 " + score
                + " · " + relativeTime(savedAt);
    }

    public String tooltip() {
        StringBuilder sb = new StringBuilder();
        sb.append(modeLabel()).append('\n');
        sb.append(config.summary()).append('\n');
        sb.append("长度 ").append(body.size()).append(" / ").append(config.maxLength).append('\n');
        sb.append("皮肤 ").append(skin().name).append('\n');
        sb.append("本局时长 ").append(durationSec() / 60).append(" 分 ")
          .append(durationSec() % 60).append(" 秒\n");
        sb.append("保存于 ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm",
                java.util.Locale.CHINA).format(new java.util.Date(savedAt)));
        if (config.challenge == GameConfig.Challenge.DAILY) {
            sb.append('\n').append("来自每日挑战");
        }
        return sb.toString();
    }

    static String relativeTime(long t) {
        long s = Math.max(0, (System.currentTimeMillis() - t) / 1000);
        if (s < 60) {
            return "刚刚保存";
        }
        if (s < 3600) {
            return (s / 60) + " 分钟前";
        }
        if (s < 86400) {
            return (s / 3600) + " 小时前";
        }
        return (s / 86400) + " 天前";
    }

    // ---- 序列化 ----

    public String serialize() {
        StringBuilder sb = new StringBuilder(256);
        sb.append("v=").append(VERSION);
        sb.append("&skin=").append(ScoreRecord.encode(skinId));
        sb.append("&dir=").append(dir.name());
        sb.append("&score=").append(score);
        sb.append("&maxLen=").append(config.maxLength);
        sb.append("&started=").append(startedAt);
        sb.append("&saved=").append(savedAt);
        sb.append("&recorded=").append(recordedScore);
        sb.append("&resumed=").append(resumed ? 1 : 0);
        sb.append("&mode=").append(config.mode.name());
        sb.append("&cols=").append(config.cols);
        sb.append("&rows=").append(config.rows);
        sb.append("&wrap=").append(config.wrapWalls ? 1 : 0);
        sb.append("&tick=").append(config.tickMs);
        sb.append("&foods=").append(config.foodCount);
        sb.append("&chal=").append(config.challenge.name());
        sb.append("&seed=").append(config.seed);
        sb.append("&body=").append(points(body));
        sb.append("&f=").append(points(foods));
        sb.append("&pend=").append(dirs(pending));
        sb.append("&shop=").append(shopUnlocked ? 1 : 0);
        sb.append("&pigSpawned=").append(pigDropped ? 1 : 0);
        if (pigPos != null) {
            sb.append("&pig=").append(pigPos.x).append(',').append(pigPos.y);
        }
        return sb.toString();
    }

    public static SaveState deserialize(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        java.util.Map<String, String> m = new java.util.HashMap<String, String>();
        for (String part : s.split("&")) {
            int i = part.indexOf('=');
            if (i > 0) {
                m.put(part.substring(0, i), part.substring(i + 1));
            }
        }
        if (!"1".equals(m.get("v"))) {
            return null;
        }
        try {
            SaveState st = new SaveState();
            st.skinId = Skins.byId(ScoreRecord.decode(m.get("skin"))).id;
            st.dir = Direction.valueOf(m.get("dir"));
            st.score = Integer.parseInt(m.get("score"));
            st.startedAt = Long.parseLong(m.get("started"));
            st.savedAt = Long.parseLong(m.get("saved"));
            st.recordedScore = Integer.parseInt(m.get("recorded"));
            st.resumed = "1".equals(m.get("resumed"));

            GameConfig c = st.config;
            c.maxLength = Integer.parseInt(m.get("maxLen"));
            c.mode = GameConfig.Mode.valueOf(m.get("mode"));
            c.cols = Integer.parseInt(m.get("cols"));
            c.rows = Integer.parseInt(m.get("rows"));
            c.wrapWalls = "1".equals(m.get("wrap"));
            c.tickMs = Integer.parseInt(m.get("tick"));
            c.foodCount = Integer.parseInt(m.get("foods"));
            c.challenge = GameConfig.Challenge.valueOf(m.get("chal"));
            c.seed = Long.parseLong(m.get("seed"));
            c.sanitize();

            st.body = parsePoints(m.get("body"));
            st.foods = parsePoints(m.get("f"));
            st.pending = parseDirs(m.get("pend"));
            st.shopUnlocked = "1".equals(m.get("shop"));
            st.pigDropped = "1".equals(m.get("pigSpawned"));
            st.pigPos = parsePoint(m.get("pig"));

            // 存档写坏时不能返回一个半残状态，那会让恢复后的游戏直接崩。
            if (st.body.isEmpty()) {
                return null;
            }
            return st;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String points(List<Point> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append('|');
            }
            sb.append(list.get(i).x).append(',').append(list.get(i).y);
        }
        return sb.toString();
    }

    private static ArrayList<Point> parsePoints(String s) {
        ArrayList<Point> out = new ArrayList<Point>();
        if (s == null || s.isEmpty()) {
            return out;
        }
        for (String item : s.split("\\|")) {
            int c = item.indexOf(',');
            if (c <= 0) {
                continue;
            }
            out.add(new Point(Long.parseLong(item.substring(0, c)),
                    Long.parseLong(item.substring(c + 1))));
        }
        return out;
    }

    private static Point parsePoint(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        int c = s.indexOf(',');
        if (c <= 0) {
            return null;
        }
        return new Point(Long.parseLong(s.substring(0, c)),
                Long.parseLong(s.substring(c + 1)));
    }

    private static String dirs(List<Direction> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append('|');
            }
            sb.append(list.get(i).name());
        }
        return sb.toString();
    }

    private static ArrayList<Direction> parseDirs(String s) {
        ArrayList<Direction> out = new ArrayList<Direction>();
        if (s == null || s.isEmpty()) {
            return out;
        }
        for (String item : s.split("\\|")) {
            try {
                out.add(Direction.valueOf(item));
            } catch (RuntimeException ignored) {
                // 单个方向损坏就跳过
            }
        }
        return out;
    }
}
