package com.snake.ui;

import com.snake.ConfigStore;
import com.snake.Skin;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

/**
 * 设置 → 音频。默认用 JDK 现场合成的声音，不需要任何外部资源；
 * 也留了外部文件入口，把音频放进 classpath 的 {@code /sounds/} 即可切换。
 */
public final class AudioForm extends JPanel {

    private static final long serialVersionUID = 1L;

    private final ConfigStore store = ConfigStore.get();
    private Skin skin;

    private final JCheckBox soundOn = new JCheckBox("开启音效");
    private final JRadioButton synth = new JRadioButton("内置合成音");
    private final JRadioButton files = new JRadioButton("外部音频文件");
    private final JLabel status = new JLabel();
    private final UiKit.FlatButton test;

    public AudioForm(Skin skin) {
        this.skin = skin;
        this.test = UiKit.button(skin, "试听", 13, false);
        build();
        readFromStore();
        applySkin(skin);
    }

    private void build() {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(soundOn);
        add(Box.createVerticalStrut(8));
        add(UiKit.sectionTitle(skin, "音源"));
        add(Box.createVerticalStrut(4));

        // 合成音和外部文件是二选一，必须成组，否则可能两个都选中
        ButtonGroup src = new ButtonGroup();
        src.add(synth);
        src.add(files);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        row.add(synth);
        row.add(files);
        row.add(test);
        add(row);

        add(Box.createVerticalStrut(6));
        status.setFont(UiKit.font(13, UiKit.FONT_PLAIN_));
        add(status);
        add(Box.createVerticalStrut(10));

        JPanel tip = new JPanel();
        tip.setOpaque(false);
        tip.setLayout(new BoxLayout(tip, BoxLayout.Y_AXIS));
        tip.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        for (String line : new String[]{
                "内置合成音由程序实时生成，零资源、零依赖。",
                "要用自己的音频：把文件放进 classpath 的 sounds/ 目录，",
                "支持 WAV / AU（JDK 自带）；MP3 需要额外放入一个解码器，",
                "程序会自动发现它，业务代码无需改动。"}) {
            JLabel l = new JLabel(line);
            l.setFont(UiKit.font(12, UiKit.FONT_PLAIN_));
            l.setForeground(skin.textDim);
            l.setAlignmentX(LEFT_ALIGNMENT);
            tip.add(l);
        }
        add(tip);

        soundOn.addActionListener(e -> store.setSoundOn(soundOn.isSelected()));
        synth.addActionListener(e -> {
            store.setUseExternalAudio(false);
            com.snake.Sound.resetPlayer();
            readFromStore();
        });
        files.addActionListener(e -> {
            store.setUseExternalAudio(true);
            com.snake.Sound.resetPlayer();
            readFromStore();
        });
        test.addActionListener(e -> {
            com.snake.Sound.play(com.snake.Sfx.EAT);
            com.snake.Sound.play(com.snake.Sfx.ACHIEVEMENT);
        });
    }

    private void readFromStore() {
        soundOn.setSelected(store.soundOn());
        synth.setSelected(!store.useExternalAudio());
        files.setSelected(store.useExternalAudio());
        boolean usable = com.snake.Sound.externalAvailable();
        files.setEnabled(usable);
        if (usable) {
            status.setText("已检测到外部音频资源，将优先使用，缺哪个就回落到合成音。");
        } else {
            status.setText("没有找到 /sounds/ 下的音频文件，正在使用内置合成音。");
        }
    }

    public void applySkin(Skin s) {
        this.skin = s;
        UiKit.styleChecks(s, soundOn);
        UiKit.styleRadio(s, synth);
        UiKit.styleRadio(s, files);
        test.applySkin(s);
        status.setForeground(s.textDim);
        repaint();
    }
}
