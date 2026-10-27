package com.snake;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * 全部持久化数据的唯一入口，落在 {@link Preferences}（Windows 下是注册表，不涉及文件路径）。
 *
 * 约定：
 * - 读不到 / 解析失败一律回落到默认值，绝不让游戏因为存档损坏而打不开。
 * - 写入全部 try/catch 并吞掉异常，退出流程不能因为写盘失败卡住。
 */
public final class ConfigStore {

    private static final String NODE = "com/snake/game";
    /** Preferences 单个 value 的字符上限。 */
    private static final int MAX_VALUE = 8000;

    private static final Preferences P = Preferences.userRoot().node(NODE);

    private GameConfig config = new GameConfig();
    private final HighScores highScores = new HighScores();
    private final Achievements achievements = new Achievements();
    private final BackgroundSettings background = new BackgroundSettings();

    private String skinId = Skins.defaultSkin().id;
    private boolean soundOn = true;
    private boolean useExternalAudio = false;

    private int gamesPlayed;
    private int totalScore;
    private int bestScore;
    private int bestLength;
    private long lastDailyEpochDay = -1;
    private int dailyStreak;
    private int resumes;

    private ConfigStore() {
        load();
    }

    private static ConfigStore instance;

    public static synchronized ConfigStore get() {
        if (instance == null) {
            instance = new ConfigStore();
        }
        return instance;
    }

    private void load() {
        config.mode = readEnum(GameConfig.Mode.class, "config.mode", config.mode);
        config.cols = readInt("config.cols", config.cols);
        config.rows = readInt("config.rows", config.rows);
        config.wrapWalls = readBoolean("config.wrapWalls", config.wrapWalls);
        config.tickMs = readInt("config.tickMs", config.tickMs);
        config.foodCount = readInt("config.foodCount", config.foodCount);
        config.maxLength = readInt("config.maxLength", config.maxLength);
        config.sanitize();

        skinId = Skins.byId(P.get("skin", skinId)).id;
        soundOn = readBoolean("sound.on", soundOn);
        useExternalAudio = readBoolean("sound.external", useExternalAudio);

        background.enabledInGame = readBoolean("bg.game", background.enabledInGame);
        background.enabledInMenu = readBoolean("bg.menu", background.enabledInMenu);
        background.path = P.get("bg.path", null);
        background.scale = readEnum(BackgroundSettings.ScaleMode.class, "bg.scale", background.scale);
        background.dimPercent = readInt("bg.dim", background.dimPercent);
        background.showGrid = readBoolean("bg.grid", background.showGrid);
        background.sanitize();

        HighScores loaded = HighScores.deserialize(P.get("scores", null));
        for (ScoreRecord r : loaded.records()) {
            highScores.add(r);
        }
        achievements.deserialize(P.get("achievements", null));

        gamesPlayed = readInt("stats.games", 0);
        totalScore = readInt("stats.total", 0);
        bestScore = readInt("stats.best", 0);
        bestLength = readInt("stats.bestLength", 0);
        skinMaskUsed = readInt("stats.skinMask", 1);
        lastDailyEpochDay = readLong("stats.lastDaily", -1);
        dailyStreak = readInt("stats.streak", 0);
        resumes = readInt("stats.resumes", 0);
        // 默认皮肤在任何情况下都算用过。
        noteSkinUsed(Skins.defaultSkin().id);
    }

    public GameConfig config() {
        return config;
    }

    public void saveConfig() {
        config.sanitize();
        putInt("config.cols", config.cols);
        putInt("config.rows", config.rows);
        putBoolean("config.wrapWalls", config.wrapWalls);
        putInt("config.tickMs", config.tickMs);
        putInt("config.foodCount", config.foodCount);
        putInt("config.maxLength", config.maxLength);
        put("config.mode", config.mode.name());
        flush();
    }

    public Skin skin() {
        return Skins.byId(skinId);
    }

    public void setSkin(String id) {
        Skin s = Skins.byId(id);
        if (!s.id.equals(skinId)) {
            skinId = s.id;
            noteSkinUsed(s.id);
            put("skin", skinId);
            flush();
        }
    }

    public HighScores highScores() {
        return highScores;
    }

    public Achievements achievements() {
        return achievements;
    }

    public BackgroundSettings background() {
        return background;
    }

    public void saveBackground() {
        background.sanitize();
        putBoolean("bg.game", background.enabledInGame);
        putBoolean("bg.menu", background.enabledInMenu);
        put("bg.path", background.path);
        put("bg.scale", background.scale.name());
        putInt("bg.dim", background.dimPercent);
        putBoolean("bg.grid", background.showGrid);
        flush();
    }

    public boolean soundOn() {
        return soundOn;
    }

    public void setSoundOn(boolean on) {
        soundOn = on;
        putBoolean("sound.on", on);
        flush();
    }

    public boolean useExternalAudio() {
        return useExternalAudio;
    }

    public void setUseExternalAudio(boolean v) {
        useExternalAudio = v;
        putBoolean("sound.external", v);
        flush();
    }

    // ---- 统计 ----

    public int gamesPlayed() {
        return gamesPlayed;
    }

    public int totalScore() {
        return totalScore;
    }

    public int bestScore() {
        return Math.max(bestScore, highScores.best());
    }

    public int bestLength() {
        return bestLength;
    }

    public int dailyStreak() {
        return dailyStreak;
    }

    public int resumes() {
        return resumes;
    }

    /** 记录一局的统计，用于成就判定。 */
    public void recordRun(int score, int length) {
        gamesPlayed++;
        totalScore += score;
        if (score > bestScore) {
            bestScore = score;
        }
        if (length > bestLength) {
            bestLength = length;
        }
        writeStats();
    }

    public void recordResume() {
        resumes++;
        writeStats();
    }

    /**
     * 「用满全部皮肤」用位掩码记录：第 i 位代表预设清单里第 i 套皮肤被选中过。
     * 这样以后往 {@link Skins#all()} 里加皮肤，进度会自动变长而不用改这里。
     */
    private int skinMaskUsed = 1;

    public int skinsUsed() {
        return Integer.bitCount(skinMaskUsed);
    }

    public void noteSkinUsed(String id) {
        for (int i = 0; i < Skins.all().size(); i++) {
            if (Skins.all().get(i).id.equals(id)) {
                int bit = 1 << i;
                if ((skinMaskUsed & bit) == 0) {
                    skinMaskUsed |= bit;
                    writeStats();
                }
                return;
            }
        }
    }

    private void writeStats() {
        putInt("stats.games", gamesPlayed);
        putInt("stats.total", totalScore);
        putInt("stats.best", bestScore);
        putInt("stats.bestLength", bestLength);
        putInt("stats.skinMask", skinMaskUsed);
        putInt("stats.resumes", resumes);
        flush();
    }

    /** 每日挑战连勤：同一天重复玩不重复加天数，隔天才 +1，断档归零。 */
    public void touchDaily(long epochDay) {
        if (lastDailyEpochDay == epochDay) {
            return;
        }
        dailyStreak = (lastDailyEpochDay == epochDay - 1) ? dailyStreak + 1 : 1;
        lastDailyEpochDay = epochDay;
        putLong("stats.lastDaily", lastDailyEpochDay);
        putInt("stats.streak", dailyStreak);
        flush();
    }

    public boolean dailyStreakAlive(long epochDay) {
        return lastDailyEpochDay == epochDay || lastDailyEpochDay == epochDay - 1;
    }

    public void saveScores() {
        put("scores", highScores.serialize());
        flush();
    }

    public void clearHighScores() {
        highScores.clear();
    }

    public void saveAchievements() {
        put("achievements", achievements.serialize());
        flush();
    }

    // ---- 存档（继续游戏） ----

    public boolean hasSave() {
        String s = P.get("save", null);
        return s != null && !s.isEmpty();
    }

    public SaveState loadSave() {
        return SaveState.deserialize(P.get("save", null));
    }

    public void writeSave(SaveState s) {
        if (s == null) {
            clearSave();
            return;
        }
        String line = s.serialize();
        // 存档失败绝不能影响退出流程，因此超长时直接放弃存档而不是抛异常。
        if (line.length() > MAX_VALUE) {
            clearSave();
            return;
        }
        put("save", line);
        flush();
    }

    public void clearSave() {
        P.remove("save");
        flush();
    }

    /**
     * 恢复出厂设置。注意必须先把内存字段也复位再 load()，
     * 因为 load() 里有些键的默认值取自「当前值」（例如皮肤），不清会沿用旧值。
     */
    public void resetAll() {
        try {
            P.clear();
        } catch (BackingStoreException ignored) {
            // 忽略
        }
        // sanitize() 只是夹紧越界值，那不是「恢复默认」。
        // 这里要的是整个换回 new GameConfig()，否则玩家改了 40 列之后
        // 点恢复默认还是 40 列。
        config = new GameConfig();
        highScores.clear();
        achievements.deserialize(null);
        background.reset();
        skinId = Skins.defaultSkin().id;
        soundOn = true;
        useExternalAudio = false;
        gamesPlayed = 0;
        totalScore = 0;
        bestScore = 0;
        bestLength = 0;
        lastDailyEpochDay = -1;
        dailyStreak = 0;
        resumes = 0;
        skinMaskUsed = 1;
        load();
        // 音源设置刚被改掉，播放器必须重建，否则退出设置后还在用旧音源。
        Sound.resetPlayer();
    }

    // ---- 底层读写 ----

    private static void put(String key, String value) {
        if (value == null) {
            P.remove(key);
        } else if (value.length() <= MAX_VALUE) {
            P.put(key, value);
        }
    }

    private static void putInt(String key, int v) {
        P.putInt(key, v);
    }

    private static void putLong(String key, long v) {
        P.putLong(key, v);
    }

    private static void putBoolean(String key, boolean v) {
        P.putBoolean(key, v);
    }

    private static int readInt(String key, int def) {
        try {
            return P.getInt(key, def);
        } catch (RuntimeException e) {
            return def;
        }
    }

    private static long readLong(String key, long def) {
        try {
            return P.getLong(key, def);
        } catch (RuntimeException e) {
            return def;
        }
    }

    private static boolean readBoolean(String key, boolean def) {
        try {
            return P.getBoolean(key, def);
        } catch (RuntimeException e) {
            return def;
        }
    }

    private static <E extends Enum<E>> E readEnum(Class<E> type, String key, E def) {
        try {
            String v = P.get(key, null);
            if (v == null) {
                return def;
            }
            return Enum.valueOf(type, v);
        } catch (RuntimeException e) {
            return def;
        }
    }

    private static void flush() {
        try {
            P.flush();
        } catch (BackingStoreException ignored) {
            // 偏好写入失败不影响本次游戏
        }
    }
}
