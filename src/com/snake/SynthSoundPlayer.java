package com.snake;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.Random;

/**
 * 用 {@code javax.sound.sampled} 现场合成音效，不依赖任何外部音频文件。
 *
 * 波形都是算出来的：一个基频加几个衰减的泛音，再套一层包络。
 * 每个音效几 KB 数据，生成和播放都在后台线程完成。
 */
final class SynthSoundPlayer implements SoundPlayer {

    private static final float RATE = 22050f;

    /** 关闭之后不再开新的音频行，避免退出时还在播。 */
    private volatile boolean closed;

    @Override
    public boolean play(final Sfx sfx, final float volume) {
        if (closed || volume <= 0.01f) {
            return false;
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                render(sfx, volume);
            }
        }, "snake-sfx");
        t.setDaemon(true);
        t.start();
        return true;
    }

    @Override
    public void close() {
        closed = true;
    }

    private void render(Sfx sfx, float volume) {
        if (closed) {
            return;
        }
        try {
            float[] samples = samplesFor(sfx);
            if (samples.length > 0) {
                playSamples(samples, volume);
            }
        } catch (LineUnavailableException | IllegalArgumentException e) {
            // 没声卡、音频设备被占用等情况，静默跳过，不能影响游戏
        } catch (RuntimeException e) {
            // 同上
        }
    }

    private float[] samplesFor(Sfx sfx) {
        switch (sfx) {
            case EAT:
                return tone(760, 980, 0.09f, 0.35f, 0.12f);
            case DEATH:
                return tone(420, 90, 0.55f, 0.4f, 0.02f);
            case MENU:
                return tone(660, 700, 0.045f, 0.2f, 0.25f);
            case ACHIEVEMENT:
                return arpeggio(new float[]{660, 880, 1175}, 0.075f, 0.32f);
            case START:
                return arpeggio(new float[]{523, 659, 784, 1047}, 0.065f, 0.3f);
            case GAME_OVER:
                return arpeggio(new float[]{523, 440, 349, 262}, 0.13f, 0.34f);
            case WARN:
                return tone(330, 330, 0.12f, 0.25f, 0.03f);
            default:
                return new float[0];
        }
    }

    /** 从 startHz 滑到 endHz 的音，带包络和一点泛音。 */
    private float[] tone(double startHz, double endHz, float seconds,
                         float amp, float vibrato) {
        int n = (int) (RATE * seconds);
        float[] out = new float[n];
        double phase = 0;
        Random rnd = new Random(0x5EED);
        for (int i = 0; i < n; i++) {
            double t = (double) i / n;
            double hz = startHz + (endHz - startHz) * t;
            if (vibrato > 0) {
                hz += Math.sin(i / RATE * vibrato * 2 * Math.PI) * 6;
            }
            phase += 2 * Math.PI * hz / RATE;
            // 基音 + 三次泛音，声音才不至于像蜂鸣器
            double v = Math.sin(phase) * 0.75
                    + Math.sin(phase * 3) * 0.18
                    + Math.sin(phase * 5) * 0.07;
            v *= envelope(t, seconds);
            out[i] = (float) (v * amp);
        }
        // 结尾极短的去噪，避免爆音
        for (int i = 0; i < 32 && i < n; i++) {
            out[n - 1 - i] *= i / 32f;
        }
        rnd.nextInt();
        return out;
    }

    private float[] arpeggio(float[] notes, float noteSeconds, float amp) {
        int per = (int) (RATE * noteSeconds);
        float[] out = new float[per * notes.length];
        for (int k = 0; k < notes.length; k++) {
            float[] seg = tone(notes[k], notes[k], noteSeconds, amp, 0);
            System.arraycopy(seg, 0, out, k * per, Math.min(per, seg.length));
        }
        return out;
    }

    /** 快起慢落，听起来像个「叮」而不是方波。 */
    private static float envelope(double t, float seconds) {
        double attack = 0.012;
        double release = Math.max(0.04, seconds * 0.6);
        if (t < attack) {
            return (float) (t / attack);
        }
        double r = (t - attack) / (seconds - attack);
        double shape = Math.max(0, Math.pow(1 - r, 1.7));
        double tail = Math.min(1, (seconds - t) / release);
        return (float) (shape * tail);
    }

    private void playSamples(float[] samples, float volume) throws LineUnavailableException {
        byte[] pcm = toPcm(samples, volume);
        AudioFormat format = new AudioFormat(RATE, 16, 1, true, false);
        // 缓冲开小一点，音效才跟得上操作
        SourceDataLine line = AudioSystem.getSourceDataLine(format);
        line.open(format, Math.max(1024, pcm.length));
        line.start();
        line.write(pcm, 0, pcm.length);
        line.drain();
        line.stop();
        line.close();
    }

    private static byte[] toPcm(float[] samples, float volume) {
        byte[] out = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            float v = Math.max(-1f, Math.min(1f, samples[i] * volume));
            int s = (int) (v * 32767);
            out[i * 2] = (byte) (s & 0xFF);
            out[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
        }
        return out;
    }
}
