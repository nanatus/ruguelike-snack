package com.snake;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.EnumMap;
import java.util.Map;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

/**
 * 从 classpath 加载外部音频文件。这是「用自己音频」的入口：
 * 把文件放进 {@code /sounds/}（例如 {@code res/sounds/eat.wav}）并加进 classpath，
 * 到设置里切到「外部音频文件」就能生效，业务代码一行都不用改。
 *
 * JDK 自带解码器支持 WAV / AU / AIFF，**不支持 MP3**。
 * 要 MP3 就往 {@code lib/} 放一个 SPI 解码器（如 JLayer），
 * {@code AudioSystem} 会自动发现它，这里的代码同样不用改。
 *
 * 找不到或读不了就返回 null，由 {@link Sound} 回落到合成音。
 */
final class ResourceSoundPlayer implements SoundPlayer {

    private final Map<Sfx, Clip> clips = new EnumMap<Sfx, Clip>(Sfx.class);
    private volatile boolean closed;

    /**
     * 扫一遍资源目录，只要任意一个音效真的能解码成音频流就返回 true。
     * 故意要真的解码：JDK 读不了 MP3，光看资源存不存在会给出假阳性，
     * 结果是玩家选了「外部音频」却一声不吭。
     */
    static boolean probe() {
        for (Sfx s : Sfx.values()) {
            if (load(s) != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean play(Sfx sfx, float volume) {
        if (closed || volume <= 0.01f) {
            return false;
        }
        Clip clip = cached(sfx);
        if (clip == null) {
            clip = open(sfx);
            if (clip == null) {
                return false;
            }
            synchronized (clips) {
                Clip prev = clips.put(sfx, clip);
                if (prev != null && prev != clip) {
                    closeQuietly(prev);
                }
            }
        }
        try {
            applyGain(clip, volume);
            clip.setFramePosition(0);
            clip.start();
            return true;
        } catch (IllegalStateException e) {
            // 线被别人关掉了（系统休眠、切音频设备），丢掉缓存下次重开。
            discard(sfx, clip);
            return false;
        }
    }

    private Clip cached(Sfx sfx) {
        synchronized (clips) {
            return clips.get(sfx);
        }
    }

    private void discard(Sfx sfx, Clip clip) {
        synchronized (clips) {
            if (clips.get(sfx) == clip) {
                clips.remove(sfx);
            }
        }
        closeQuietly(clip);
    }

    private static void applyGain(Clip clip, float volume) {
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            return;
        }
        FloatControl fc = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        // 分贝是线性刻度，按振幅折算
        float amp = Math.max(0.0001f, volume);
        float db = (float) (20 * Math.log10(amp));
        if (db < fc.getMinimum()) {
            db = fc.getMinimum();
        }
        if (db > fc.getMaximum()) {
            db = fc.getMaximum();
        }
        fc.setValue(db);
    }

    private static URL url(Sfx sfx, String file) {
        return ResourceSoundPlayer.class.getResource(sfx.resourcePath(file));
    }

    private static InputStream openStream(Sfx sfx, String file) throws Exception {
        URL u = url(sfx, file);
        return u == null ? null : new BufferedInputStream(u.openStream());
    }

    /**
     * 只做解码，不占用音频线。
     * 同一个 classpath 目录里 {@code eat.wav} 和 {@code eat.mp3} 可能只有一个能读，
     * 所以要挨个试到成功为止，不能读不动就整个放弃。
     */
    private static AudioFormat load(Sfx sfx) {
        for (String f : sfx.candidates()) {
            InputStream in = null;
            AudioInputStream ais = null;
            try {
                in = openStream(sfx, f);
                if (in == null) {
                    continue;
                }
                ais = AudioSystem.getAudioInputStream(in);
                AudioFormat fmt = ais.getFormat();
                // 强制读一点数据，header 合法但内容是垃圾的情况才算暴露。
                ais.getFrameLength();
                return fmt;
            } catch (Exception e) {
                // 文件存在但解码不了（典型是没放 MP3 解码器），试下一个
            } finally {
                closeQuietly(ais);
                closeQuietly(in);
            }
        }
        return null;
    }

    private static Clip open(Sfx sfx) {
        for (String f : sfx.candidates()) {
            InputStream in = null;
            AudioInputStream ais = null;
            try {
                in = openStream(sfx, f);
                if (in == null) {
                    continue;
                }
                ais = AudioSystem.getAudioInputStream(in);
                Clip clip = AudioSystem.getClip();
                try {
                    clip.open(ais);
                } catch (Exception e) {
                    closeQuietly(clip);
                    throw e;
                }
                // ais 交给 clip 了，这里不能关。
                return clip;
            } catch (Exception e) {
                // 读不了就试下一个候选扩展名
                closeQuietly(ais);
                closeQuietly(in);
            }
        }
        return null;
    }

    private static void closeQuietly(Clip c) {
        try {
            c.close();
        } catch (RuntimeException ignored) {
            // 已经关了
        }
    }

    private static void closeQuietly(AudioInputStream a) {
        if (a != null) {
            try {
                a.close();
            } catch (Exception ignored) {
                // 忽略
            }
        }
    }

    private static void closeQuietly(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (Exception ignored) {
                // 忽略
            }
        }
    }

    @Override
    public void close() {
        closed = true;
        synchronized (clips) {
            for (Clip c : clips.values()) {
                closeQuietly(c);
            }
            clips.clear();
        }
    }

}
