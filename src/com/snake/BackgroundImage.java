package com.snake;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javax.imageio.ImageIO;

/**
 * 玩家导入的背景图管理。
 *
 * 导入后会被复制到受管目录 {@code ~/.snake/background.png}，而不是记住原始路径——
 * 否则玩家移动或删掉原图、甚至换台机器，游戏就再也开不了。
 */
public final class BackgroundImage {

    private static BufferedImage cached;
    private static String cachedPath;
    /** 缩放结果缓存：源图与目标尺寸、缩放方式都不变就直接复用，避免每帧重采样。 */
    private static BufferedImage scaled;
    private static int scaledSrcW = -1;
    private static int scaledSrcH = -1;
    private static int scaledDstW = -1;
    private static int scaledDstH = -1;
    private static BackgroundSettings.ScaleMode scaledMode;

    private BackgroundImage() {
    }

    public static File managedDir() {
        return new File(System.getProperty("user.home"), ".snake");
    }

    public static File managedFile() {
        return new File(managedDir(), "background.png");
    }

    public static String managedPath() {
        return managedFile().getAbsolutePath();
    }

    public static boolean exists() {
        File f = managedFile();
        return f.isFile() && f.length() > 0;
    }

    /**
     * 把玩家选的文件复制到受管目录。
     *
     * @return 成功返回受管文件，失败返回 null（调用方负责提示）
     */
    public static File importFrom(File src) {
        if (src == null || !src.isFile()) {
            return null;
        }
        BufferedImage probe;
        try {
            probe = ImageIO.read(src);
        } catch (IOException e) {
            return null;
        }
        if (probe == null) {
            return null;
        }
        try {
            File dir = managedDir();
            if (!dir.isDirectory() && !dir.mkdirs()) {
                return null;
            }
            File dst = managedFile();
            // 先删后拷：源和目标是同一路径时，File.copy 会直接抛 FileAlreadyExistsException。
            if (dst.exists() && !dst.delete()) {
                return null;
            }
            Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            return null;
        }
        invalidate();
        return managedFile();
    }

    public static boolean clear() {
        invalidate();
        File f = managedFile();
        return !f.exists() || f.delete();
    }

    public static void invalidate() {
        cached = null;
        cachedPath = null;
        scaled = null;
        scaledSrcW = -1;
        scaledSrcH = -1;
        scaledDstW = -1;
        scaledDstH = -1;
        scaledMode = null;
    }

    /** 读原始图；读不出来就返回 null，让调用方回落到纯色背景。 */
    public static BufferedImage load() {
        if (!exists()) {
            return null;
        }
        String p = managedPath();
        if (cached != null && p.equals(cachedPath)) {
            return cached;
        }
        try {
            BufferedImage img = ImageIO.read(managedFile());
            if (img != null) {
                cached = img;
                cachedPath = p;
            }
            return img;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 取一张已经缩放到 {@code w×h} 的副本，命中缓存时零开销。
     *
     * @param fill 底色，同时用于填掉 PNG 的透明区域（否则透明处会露出黑块）
     */
    public static BufferedImage scaled(int w, int h, BackgroundSettings.ScaleMode mode, Color fill) {
        BufferedImage src = load();
        if (src == null || w <= 0 || h <= 0) {
            return null;
        }
        if (scaled != null && scaledSrcW == src.getWidth() && scaledSrcH == src.getHeight()
                && scaledDstW == w && scaledDstH == h && scaledMode == mode) {
            return scaled;
        }

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        if (fill != null) {
            g.setColor(fill);
            g.fillRect(0, 0, w, h);
        }
        if (mode == BackgroundSettings.ScaleMode.STRETCH) {
            g.drawImage(src, 0, 0, w, h, null);
        } else {
            // COVER：等比缩放到刚好盖满，再居中裁切，保证不变形。
            double k = Math.max((double) w / src.getWidth(), (double) h / src.getHeight());
            int sw = Math.max(1, (int) Math.round(src.getWidth() * k));
            int sh = Math.max(1, (int) Math.round(src.getHeight() * k));
            g.drawImage(src, (w - sw) / 2, (h - sh) / 2, sw, sh, null);
        }
        g.dispose();

        scaled = out;
        scaledSrcW = src.getWidth();
        scaledSrcH = src.getHeight();
        scaledDstW = w;
        scaledDstH = h;
        scaledMode = mode;
        return out;
    }
}
