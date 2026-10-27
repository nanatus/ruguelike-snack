package com.snake;

/**
 * 音效种类。外部资源入口预留在这里：
 * 每个枚举自带默认文件名，播放器会去 classpath 的 {@code /sounds/} 下找。
 *
 * 想换成自己的音频，把 eat.wav 之类放进 {@code res/sounds/} 并加进 classpath，
 * 再到设置里切到「外部音频文件」即可，不用改任何代码。
 */
public enum Sfx {

    EAT("eat", 0.9f),
    DEATH("death", 0.9f),
    GAME_OVER("gameover", 0.9f),
    MENU("menu", 0.6f),
    START("start", 0.7f),
    ACHIEVEMENT("achievement", 0.8f),
    WARN("warn", 0.5f);

    private final String baseName;
    private final float volume;

    Sfx(String baseName, float volume) {
        this.baseName = baseName;
        this.volume = volume;
    }

    public String baseName() {
        return baseName;
    }

    public float volume() {
        return volume;
    }

    /**
     * 依次尝试这些扩展名。
     * WAV / AU / AIFF 是 JDK 自带解码器一定能读的，OGG 留给外部 SPI。
     */
    public String[] candidates() {
        return new String[]{
            baseName + ".wav",
            baseName + ".au",
            baseName + ".aiff",
            baseName + ".ogg"};
    }

    public String resourcePath(String fileName) {
        return "/sounds/" + fileName;
    }
}
