package com.snake;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/** 大小病毒共用的 Java2D 带刺球体绘制。 */
public final class VirusArt {

    private static final Color LARGE_CORE = new Color(0x8E44AD);
    private static final Color LARGE_INNER = new Color(0xA95CC2);
    private static final Color LARGE_SPIKE = new Color(0xC175D2);
    private static final Color LARGE_EDGE = new Color(0x43204F);
    private static final Color SMALL_CORE = new Color(0x6D9F45);
    private static final Color SMALL_INNER = new Color(0x8FBD58);
    private static final Color SMALL_SPIKE = new Color(0xA8CD62);
    private static final Color SMALL_EDGE = new Color(0x304922);

    private VirusArt() {
    }

    public static void paint(Graphics2D g, int x, int y, int pixelSize, boolean small) {
        if (pixelSize <= 0) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        float s = pixelSize;
        float cx = x + s * 0.5f;
        float cy = y + s * 0.5f;
        Color spike = small ? SMALL_SPIKE : LARGE_SPIKE;
        Color edge = small ? SMALL_EDGE : LARGE_EDGE;
        int count = small ? 8 : 14;
        g2.setStroke(new BasicStroke(Math.max(1f, s * 0.025f),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < count; i++) {
            double a = Math.PI * 2 * i / count;
            double ux = Math.cos(a);
            double uy = Math.sin(a);
            double px = -uy;
            double py = ux;
            float baseX = (float) (cx + ux * s * 0.27f);
            float baseY = (float) (cy + uy * s * 0.27f);
            float half = s * (small ? 0.055f : 0.04f);
            Path2D.Float triangle = new Path2D.Float();
            triangle.moveTo(baseX + px * half, baseY + py * half);
            triangle.lineTo(cx + ux * s * 0.47f, cy + uy * s * 0.47f);
            triangle.lineTo(baseX - px * half, baseY - py * half);
            triangle.closePath();
            g2.setColor(spike);
            g2.fill(triangle);
            g2.setColor(edge);
            g2.draw(triangle);
        }

        float coreX = x + s * 0.17f;
        float coreY = y + s * 0.17f;
        float coreS = s * 0.66f;
        g2.setColor(small ? SMALL_CORE : LARGE_CORE);
        g2.fill(new Ellipse2D.Float(coreX, coreY, coreS, coreS));
        g2.setColor(edge);
        g2.setStroke(new BasicStroke(Math.max(1f, s * 0.035f)));
        g2.draw(new Ellipse2D.Float(coreX, coreY, coreS, coreS));

        g2.setColor(small ? SMALL_INNER : LARGE_INNER);
        g2.fill(new Ellipse2D.Float(x + s * 0.27f, y + s * 0.25f, s * 0.22f, s * 0.18f));
        g2.setColor(edge);
        float dot = Math.max(1.5f, s * 0.065f);
        g2.fill(new Ellipse2D.Float(x + s * 0.39f, y + s * 0.48f, dot, dot));
        g2.fill(new Ellipse2D.Float(x + s * 0.58f, y + s * 0.40f, dot, dot));
        g2.fill(new Ellipse2D.Float(x + s * 0.57f, y + s * 0.61f, dot, dot));
        g2.dispose();
    }
}
