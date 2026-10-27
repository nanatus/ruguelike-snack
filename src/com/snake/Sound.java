package com.snake;

/**
 * 音效门面。游戏逻辑只调这里，不关心底下是合成音还是外部文件。
 *
 * 默认走内置合成音（零依赖）。玩家在设置里切到「外部音频文件」后，
 * 优先用 {@code /sounds/} 下的文件；某个音效的文件缺失或解码不了，
 * 就单独回落回合成音，不会整体静音。
 */
public final class Sound {

    private static SoundPlayer player;
    /** 永远存在的兜底音源，专职接住外部音频处理不了的音效。 */
    private static SoundPlayer synth;
    private static Boolean externalOk;

    private Sound() {
    }

    public static void play(Sfx sfx) {
        if (sfx == null || !ConfigStore.get().soundOn()) {
            return;
        }
        try {
            SoundPlayer p = ensurePlayer();
            if (p.play(sfx, sfx.volume())) {
                return;
            }
            // 当前音源处理不了这个音效（缺文件 / 解码不了）：单独回落到合成音，
            // 而不是让整个游戏变成哑巴。
            fallback().play(sfx, sfx.volume());
        } catch (RuntimeException e) {
            // 音频出问题绝不能拖垮游戏
        }
    }

    private static SoundPlayer ensurePlayer() {
        if (player == null) {
            player = ConfigStore.get().useExternalAudio() && externalAvailable()
                    ? new ResourceSoundPlayer()
                    : fallback();
        }
        return player;
    }

    private static synchronized SoundPlayer fallback() {
        if (synth == null) {
            synth = new SynthSoundPlayer();
        }
        return synth;
    }

    /** 外部音频是否可用。结果缓存，避免每次播声音都去扫 classpath。 */
    public static boolean externalAvailable() {
        if (externalOk == null) {
            try {
                externalOk = ResourceSoundPlayer.probe();
            } catch (RuntimeException | NoClassDefFoundError e) {
                externalOk = Boolean.FALSE;
            }
        }
        return externalOk;
    }

    /** 设置里改了音源之后调用，下次播放会用新的播放器。 */
    public static void resetPlayer() {
        closePlayer();
        externalOk = null;
    }

    static void closePlayer() {
        closeQuietly(player);
        closeQuietly(synth);
        player = null;
        synth = null;
    }

    private static void closeQuietly(SoundPlayer p) {
        if (p != null) {
            try {
                p.close();
            } catch (RuntimeException ignored) {
                // 忽略
            }
        }
    }
}
