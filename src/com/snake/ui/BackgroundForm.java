package com.snake.ui;

import com.snake.BackgroundImage;
import com.snake.BackgroundSettings;
import com.snake.ConfigStore;
import com.snake.Skin;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * 设置 → 外观 → 背景。
 *
 * 导入的图会复制到受管目录再使用，所以玩家之后移动或删掉原图都不影响。
 * 菜单区和游戏区分别开关：想给主菜单换背景、但保持游戏画面干净是常见需求。
 */
public final class BackgroundForm extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int THUMB_W = 240;
    private static final int THUMB_H = 135;

    private final BackgroundSettings bg;
    private Skin skin;

    private final Thumbnail thumb = new Thumbnail();
    private final JLabel status = new JLabel();
    private final UiKit.FlatButton importBtn;
    private final UiKit.FlatButton clearBtn;
    private final JCheckBox useInGame = new JCheckBox("游戏区使用背景图");
    private final JCheckBox useInMenu = new JCheckBox("主菜单使用背景图");
    private final JRadioButton coverBtn = new JRadioButton("等比填充裁切");
    private final JRadioButton stretchBtn = new JRadioButton("拉伸铺满");
    private final JSlider dim = new JSlider(0, 80, 45);
    private final JLabel dimLabel = new JLabel();
    private final JCheckBox showGrid = new JCheckBox("显示网格线");
    private final JLabel scaleTitle = new JLabel("缩放方式");
    private final JLabel dimTitle = new JLabel("暗化程度");
    private final JLabel tip = new JLabel();

    public BackgroundForm(Skin skin) {
        this.skin = skin;
        this.bg = ConfigStore.get().background();
        this.importBtn = UiKit.button(skin, "导入图片…", 14, false);
        this.clearBtn = UiKit.button(skin, "移除", 14, false);
        build();
        readFromSettings();
        applySkin(skin);
    }

    private void build() {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.setOpaque(false);
        thumb.setPreferredSize(new Dimension(THUMB_W, THUMB_H));
        top.add(thumb, BorderLayout.WEST);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        btns.setOpaque(false);
        btns.add(importBtn);
        btns.add(clearBtn);
        right.add(btns);
        right.add(Box.createVerticalStrut(6));
        status.setFont(UiKit.font(13, UiKit.FONT_PLAIN_));
        right.add(status);
        right.add(Box.createVerticalStrut(6));
        right.add(useInMenu);
        right.add(useInGame);
        top.add(right, BorderLayout.CENTER);
        add(top);
        add(Box.createVerticalStrut(8));

        JPanel scaleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        scaleRow.setOpaque(false);
        scaleTitle.setFont(UiKit.font(14, UiKit.FONT_PLAIN_));
        scaleRow.add(scaleTitle);
        scaleRow.add(coverBtn);
        scaleRow.add(stretchBtn);
        add(scaleRow);

        JPanel dimRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        dimRow.setOpaque(false);
        dimTitle.setFont(UiKit.font(14, UiKit.FONT_PLAIN_));
        dimRow.add(dimTitle);
        dim.setPreferredSize(new Dimension(200, 24));
        dimRow.add(dim);
        dimRow.add(dimLabel);
        add(dimRow);

        add(showGrid);
        add(Box.createVerticalStrut(4));
        tip.setText("图片复制到 " + shortPath() + " 之后即可长期使用；"
                + "暗化是为了让蛇和文字在任何图片上都看得清。");
        tip.setFont(UiKit.font(12, UiKit.FONT_PLAIN_));
        add(tip);

        // 缩放方式与暗化程度互斥，必须成组
        ButtonGroup scaleGroup = new ButtonGroup();
        scaleGroup.add(coverBtn);
        scaleGroup.add(stretchBtn);

        importBtn.addActionListener(e -> doImport());
        clearBtn.addActionListener(e -> {
            if (!bg.hasImage()) {
                return;
            }
            if (!UiKit.confirm(this, "移除背景", "确定移除已导入的背景图吗？")) {
                return;
            }
            BackgroundImage.clear();
            bg.path = null;
            bg.enabledInGame = false;
            bg.enabledInMenu = false;
            ConfigStore.get().saveBackground();
            readFromSettings();
        });
        useInGame.addActionListener(e -> {
            bg.enabledInGame = useInGame.isSelected();
            ConfigStore.get().saveBackground();
            refreshEnabled();
        });
        useInMenu.addActionListener(e -> {
            bg.enabledInMenu = useInMenu.isSelected();
            ConfigStore.get().saveBackground();
            refreshEnabled();
        });
        coverBtn.addActionListener(e -> {
            bg.scale = BackgroundSettings.ScaleMode.COVER;
            ConfigStore.get().saveBackground();
            repaint();
        });
        stretchBtn.addActionListener(e -> {
            bg.scale = BackgroundSettings.ScaleMode.STRETCH;
            ConfigStore.get().saveBackground();
            repaint();
        });
        dim.addChangeListener(e -> {
            bg.dimPercent = dim.getValue();
            dimLabel.setText(bg.dimPercent + "%");
            ConfigStore.get().saveBackground();
        });
        showGrid.addActionListener(e -> {
            bg.showGrid = showGrid.isSelected();
            ConfigStore.get().saveBackground();
        });
    }

    private static String shortPath() {
        String p = BackgroundImage.managedDir().getAbsolutePath();
        return p.length() > 46 ? "…" + p.substring(p.length() - 44) : p;
    }

    private void doImport() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("选择背景图片");
        // png 是主推格式，jpg / gif 也一并放行，ImageIO 原生就能读
        fc.setFileFilter(new FileNameExtensionFilter(
                "图片 (*.png, *.jpg, *.gif)", "png", "jpg", "jpeg", "gif"));
        fc.setAcceptAllFileFilterUsed(true);
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File src = fc.getSelectedFile();
        File ok = BackgroundImage.importFrom(src);
        if (ok == null) {
            UiKit.error(this, "导入失败", "无法读取这张图片，请换一个文件试试。");
            return;
        }
        bg.path = ok.getAbsolutePath();
        // 第一次导入就直接用上，省得还要自己去勾开关
        if (!bg.enabledInGame && !bg.enabledInMenu) {
            bg.enabledInMenu = true;
        }
        ConfigStore.get().saveBackground();
        readFromSettings();
    }

    /** 把设置读回控件。导入或移除之后调一次。 */
    public void readFromSettings() {
        useInGame.setSelected(bg.enabledInGame);
        useInMenu.setSelected(bg.enabledInMenu);
        coverBtn.setSelected(bg.scale == BackgroundSettings.ScaleMode.COVER);
        stretchBtn.setSelected(bg.scale == BackgroundSettings.ScaleMode.STRETCH);
        dim.setValue(bg.dimPercent);
        dimLabel.setText(bg.dimPercent + "%");
        showGrid.setSelected(bg.showGrid);
        if (bg.hasImage()) {
            File f = new File(bg.path);
            status.setText(f.getName() + "（" + (f.length() / 1024) + " KB）");
        } else {
            status.setText("还没有导入图片，当前使用默认背景");
        }
        refreshEnabled();
        thumb.repaint();
    }

    private void refreshEnabled() {
        boolean has = bg.hasImage();
        useInGame.setEnabled(has);
        useInMenu.setEnabled(has);
        clearBtn.setEnabled(has);
        importBtn.setEnabled(true);
        coverBtn.setEnabled(has);
        stretchBtn.setEnabled(has);
        dim.setEnabled(has);
    }

    public void applySkin(Skin s) {
        this.skin = s;
        UiKit.styleChecks(s, useInGame, useInMenu, showGrid);
        UiKit.styleRadio(s, coverBtn);
        UiKit.styleRadio(s, stretchBtn);
        importBtn.applySkin(s);
        clearBtn.applySkin(s);
        status.setForeground(s.textDim);
        dimLabel.setForeground(s.accent);
        scaleTitle.setForeground(s.text);
        dimTitle.setForeground(s.text);
        tip.setForeground(s.textDim);
        dim.setOpaque(false);
        dim.setForeground(s.accent);
        thumb.applySkin(s);
        repaint();
    }

    /** 背景缩略图预览。 */
    private final class Thumbnail extends JComponent {
        private static final long serialVersionUID = 1L;

        Thumbnail() {
            setOpaque(true);
        }

        void applySkin(Skin s) {
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(skin.bg);
            g2.fillRect(0, 0, w, h);

            BufferedImage img = bg.hasImage()
                    ? BackgroundImage.scaled(w, h, bg.scale, skin.bg)
                    : null;
            if (img != null) {
                g2.drawImage(img, 0, 0, null);
                if (bg.dimPercent > 0) {
                    g2.setColor(new Color(skin.bg.getRed(), skin.bg.getGreen(),
                            skin.bg.getBlue(), 255 - (int) (bg.dimPercent * 2.55)));
                    g2.fillRect(0, 0, w, h);
                }
            } else {
                g2.setColor(skin.textDim);
                g2.setFont(UiKit.font(13, UiKit.FONT_PLAIN_));
                String t = "默认背景";
                int tw = g2.getFontMetrics().stringWidth(t);
                g2.drawString(t, (w - tw) / 2, h / 2 + 4);
            }
            g2.setColor(skin.panelBorder);
            g2.drawRect(0, 0, w - 1, h - 1);
            g2.dispose();
        }
    }
}
