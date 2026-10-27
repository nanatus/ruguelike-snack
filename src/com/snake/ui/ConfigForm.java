package com.snake.ui;

import com.snake.GameConfig;
import com.snake.Skin;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * 一局怎么打的设置：模式、棋盘大小、是否穿墙、速度、食物数量。
 *
 * 设置对话框和开局设置页共用这一个组件，两处不会出现规则不一致。
 * 控件直接读写传入的 {@link GameConfig}，改动立刻反映到配置上。
 */
public final class ConfigForm extends JPanel {

    private static final long serialVersionUID = 1L;

    /** 棋盘预设，按玩家实际会选的尺寸排列。 */
    private static final int[][] PRESETS = {
            {15, 15}, {20, 20}, {25, 25}, {30, 30}, {40, 25}, {25, 40}
    };

    private final GameConfig config;
    private Skin skin;

    private final JRadioButton classicBtn = new JRadioButton("经典有限棋盘");
    private final JRadioButton endlessBtn = new JRadioButton("无尽无限地图");

    private final JComboBox<String> sizeBox = new JComboBox<String>();
    private final JCheckBox customSize = new JCheckBox("自定义");
    private final JSlider cols = new JSlider(GameConfig.MIN_COLS, GameConfig.MAX_COLS, 25);
    private final JSlider rows = new JSlider(GameConfig.MIN_ROWS, GameConfig.MAX_ROWS, 25);
    private final JLabel sizeLabel = new JLabel();
    private final JCheckBox wrapBox = new JCheckBox("撞到边界从对面穿出（不勾选则撞墙结束）");

    /** 左边慢右边快，滑块值就是 TICK_LEVELS 的下标。 */
    private final JSlider speed = new JSlider(
            JSlider.HORIZONTAL, 0, GameConfig.TICK_LEVELS.length - 1, 3);
    private final JLabel speedLabel = new JLabel();
    private final JSlider foods = new JSlider(GameConfig.MIN_FOODS, GameConfig.MAX_FOODS, 1);
    private final FoodDots foodDots = new FoodDots();
    private final JLabel foodLabel = new JLabel();
    private JPanel foodRow;
    private final JLabel maxLengthLabel = new JLabel();
    private final JSlider maxLength = new JSlider(GameConfig.MIN_LENGTH,
            GameConfig.MAX_LENGTH_CAP, 30);

    private boolean syncing;
    private final java.util.List<ChangeListener> changeListeners =
            new java.util.ArrayList<ChangeListener>();

    /**
     * 表单里任何一项被改动都会通知这里。
     * 配置是被就地修改的，外部页面要靠这个回调刷新自己的摘要文字。
     */
    public void addChangeListener(ChangeListener l) {
        changeListeners.add(l);
    }

    private void fireChanged() {
        if (syncing) {
            return;
        }
        ChangeEvent e = new ChangeEvent(this);
        for (ChangeListener l : changeListeners) {
            l.stateChanged(e);
        }
    }

    public ConfigForm(GameConfig config, Skin skin) {
        this.config = config;
        this.skin = skin;
        build();
        readFromConfig();
        applySkin(skin);
    }

    private void build() {
        setOpaque(false);
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));

        // 两个模式按钮必须成组，否则可以同时选中两个
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(classicBtn);
        modeGroup.add(endlessBtn);

        add(UiKit.sectionTitle(skin, "地图"));
        add(strut());
        add(row(classicBtn, gap(18), endlessBtn));
        add(strut());

        for (int[] p : PRESETS) {
            sizeBox.addItem(p[0] + " × " + p[1]);
        }
        sizeBox.addItem("自定义");
        sizeBox.setMaximumSize(new Dimension(160, 28));
        add(row(new JLabel("棋盘大小 "), gap(8), sizeBox, gap(10),
                customSize, gap(14), sizeLabel));

        add(row(new JLabel("宽 "), slider(cols), gap(12), new JLabel("高 "), slider(rows)));
        add(wrapBox);
        add(strut());

        add(UiKit.sectionTitle(skin, "玩法"));
        add(strut());
        add(row(new JLabel("速度 "), slider(speed), gap(12), speedLabel));
        foodRow = row(new JLabel("同屏食物 "), slider(foods), gap(12), foodDots, gap(10), foodLabel);
        add(foodRow);
        add(row(new JLabel("长度上限 "), slider(maxLength), gap(12), maxLengthLabel));

        wrapBox.addActionListener(e -> {
            if (!syncing) {
                config.wrapWalls = wrapBox.isSelected();
                fireChanged();
            }
        });
        customSize.addActionListener(e -> {
            if (syncing) {
                return;
            }
            boolean on = customSize.isSelected();
            if (on) {
                int idx = sizeBox.getSelectedIndex();
                if (idx >= 0 && idx < PRESETS.length) {
                    cols.setValue(PRESETS[idx][0]);
                    rows.setValue(PRESETS[idx][1]);
                }
            }
            syncSizeEnabled();
            writeSize();
        });
        sizeBox.addActionListener(e -> onPresetPicked());
        ChangeListener writeCols = e -> writeSize();
        cols.addChangeListener(writeCols);
        rows.addChangeListener(writeCols);
        speed.addChangeListener(e -> {
            if (syncing) {
                return;
            }
            int ms = GameConfig.TICK_LEVELS[speed.getValue()];
            config.tickMs = ms;
            speedLabel.setText(GameConfig.speedName(ms) + "（" + ms + " 毫秒/步）");
            fireChanged();
        });
        foods.addChangeListener(e -> {
            if (syncing) {
                return;
            }
            config.foodCount = foods.getValue();
            foodDots.setCount(config.foodCount);
            foodLabel.setText(config.foodCount + " 个");
            fireChanged();
        });
        maxLength.addChangeListener(e -> {
            if (syncing) {
                return;
            }
            config.maxLength = maxLength.getValue();
            updateMaxLengthLabel();
            fireChanged();
        });
        classicBtn.addActionListener(e -> {
            if (!syncing) {
                config.mode = GameConfig.Mode.CLASSIC;
                syncEnabled();
                fireChanged();
            }
        });
        endlessBtn.addActionListener(e -> {
            if (!syncing) {
                config.mode = GameConfig.Mode.ENDLESS;
                syncEnabled();
                fireChanged();
            }
        });
    }

    private void onPresetPicked() {
        if (syncing) {
            return;
        }
        int idx = sizeBox.getSelectedIndex();
        if (idx >= 0 && idx < PRESETS.length) {
            customSize.setSelected(false);
            cols.setValue(PRESETS[idx][0]);
            rows.setValue(PRESETS[idx][1]);
        } else {
            customSize.setSelected(true);
        }
        syncSizeEnabled();
        writeSize();
    }

    private void writeSize() {
        if (syncing) {
            return;
        }
        config.cols = cols.getValue();
        config.rows = rows.getValue();
        sizeLabel.setText("→ " + config.cols + " × " + config.rows + " 格");
        fireChanged();
    }

    /** 把配置读进控件。设置对话框每次打开都要走一遍。 */
    public void readFromConfig() {
        syncing = true;
        try {
            config.sanitize();
            classicBtn.setSelected(config.mode == GameConfig.Mode.CLASSIC);
            endlessBtn.setSelected(config.mode == GameConfig.Mode.ENDLESS);
            cols.setValue(config.cols);
            rows.setValue(config.rows);
            customSize.setSelected(!matchesPreset(config.cols, config.rows));
            selectPresetItem(config.cols, config.rows);
            speed.setValue(GameConfig.speedIndexOfOrDefault(config.tickMs));
            foods.setValue(config.foodCount);
            maxLength.setValue(config.maxLength);
            wrapBox.setSelected(config.wrapWalls);
        } finally {
            syncing = false;
        }
        sizeLabel.setText("→ " + config.cols + " × " + config.rows + " 格");
        speedLabel.setText(GameConfig.speedName(config.tickMs) + "（" + config.tickMs + " 毫秒/步）");
        foodDots.setCount(config.foodCount);
        foodLabel.setText(config.foodCount + " 个");
        syncEnabled();
    }
    private void selectPresetItem(int c, int r) {
        for (int i = 0; i < PRESETS.length; i++) {
            if (PRESETS[i][0] == c && PRESETS[i][1] == r) {
                sizeBox.setSelectedIndex(i);
                return;
            }
        }
        sizeBox.setSelectedIndex(PRESETS.length);
    }

    private static boolean matchesPreset(int c, int r) {
        for (int[] p : PRESETS) {
            if (p[0] == c && p[1] == r) {
                return true;
            }
        }
        return false;
    }

    /** 经典模式下才有棋盘和墙，无尽模式下这些控件要灰掉。 */
    private void syncEnabled() {
        boolean classic = config.mode == GameConfig.Mode.CLASSIC;
        sizeBox.setEnabled(classic && !customSize.isSelected());
        customSize.setEnabled(classic);
        cols.setEnabled(classic && customSize.isSelected());
        rows.setEnabled(classic && customSize.isSelected());
        sizeLabel.setEnabled(classic);
        wrapBox.setEnabled(classic);
        wrapBox.setText(classic
                ? "撞到边界从对面穿出（不勾选则撞墙结束）"
                : "无尽模式没有边界");
        foods.setVisible(classic);
        foodDots.setVisible(classic);
        foodLabel.setText(classic ? config.foodCount + " 个" : "自动（按视野）");
        if (foodRow != null) {
            foodRow.revalidate();
        }
        updateMaxLengthLabel();
    }

    private void updateMaxLengthLabel() {
        if (config.mode == GameConfig.Mode.ENDLESS) {
            maxLengthLabel.setText("吃满 " + config.maxLength + " 节后不再变长，食物仍会生成");
        } else {
            maxLengthLabel.setText("吃满 " + config.maxLength + " 节就不再生成食物");
        }
    }

    private void syncSizeEnabled() {
        boolean classic = config.mode == GameConfig.Mode.CLASSIC;
        sizeBox.setEnabled(classic && !customSize.isSelected());
        customSize.setEnabled(classic);
        cols.setEnabled(classic && customSize.isSelected());
        rows.setEnabled(classic && customSize.isSelected());
    }

    public void applySkin(Skin s) {
        this.skin = s;
        // 逐个登记需要跟着换肤的控件，避免每加一个控件就想起更新这里
        UiKit.styleRadio(s, classicBtn);
        UiKit.styleRadio(s, endlessBtn);
        UiKit.styleChecks(s, wrapBox, customSize);
        sizeLabel.setForeground(s.text);
        speedLabel.setForeground(s.accent);
        foodLabel.setForeground(s.accent);
        maxLengthLabel.setForeground(s.textDim);
        for (JSlider sl : new JSlider[]{speed, foods, maxLength, cols, rows}) {
            sl.setOpaque(false);
            sl.setForeground(s.accent);
            sl.setBackground(s.panelBg);
        }
        foodDots.applySkin(s);
        for (java.awt.Component c : getComponents()) {
            styleChildren(c, s);
        }
        repaint();
    }

    /** 容器里的 JLabel 也要跟着换色，否则深色皮肤下会看不清。 */
    private void styleChildren(java.awt.Component c, Skin s) {
        if (!(c instanceof JPanel)) {
            return;
        }
        JPanel p = (JPanel) c;
        p.setOpaque(false);
        for (java.awt.Component ch : p.getComponents()) {
            if (ch instanceof JLabel) {
                ((JLabel) ch).setForeground(s.text);
            } else if (ch instanceof JSlider) {
                ((JSlider) ch).setForeground(s.accent);
            }
        }
    }

    private static java.awt.Component strut() {
        return javax.swing.Box.createVerticalStrut(8);
    }

    private static java.awt.Component gap(int w) {
        return javax.swing.Box.createHorizontalStrut(w);
    }

    /** 一行横排控件，背景透明以便皮肤统一上色。 */
    private static JPanel row(java.awt.Component... items) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new javax.swing.BoxLayout(p, javax.swing.BoxLayout.X_AXIS));
        for (java.awt.Component c : items) {
            p.add(c);
        }
        return p;
    }

    private static JSlider slider(JSlider s) {
        s.setMaximumSize(new Dimension(240, 24));
        return s;
    }

    /** 食物数量的圆点预览。 */
    private static final class FoodDots extends JComponent {
        private static final long serialVersionUID = 1L;
        private int count = 1;
        private Color food = new Color(0xEF4444);

        FoodDots() {
            setPreferredSize(new Dimension(96, 18));
            setMaximumSize(new Dimension(96, 18));
        }

        void setCount(int c) {
            this.count = c;
            repaint();
        }

        void applySkin(Skin s) {
            this.food = s.food;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            int x = 0;
            for (int i = 0; i < GameConfig.MAX_FOODS; i++) {
                g2.setColor(i < count ? food : new Color(food.getRed(), food.getGreen(),
                        food.getBlue(), 60));
                g2.fillOval(x, 4, 10, 10);
                x += 17;
            }
            g2.dispose();
        }
    }
}
