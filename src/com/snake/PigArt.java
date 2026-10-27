package com.snake;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/** 猪头绘制：棋盘上的特殊食物和剧情对话框共用同一套图形。 */
public final class PigArt {

    private static final Color FACE = new Color(0xF7B7C9);
    private static final Color EAR = new Color(0xE78FA8);
    private static final Color SNOUT = new Color(0xE27292);
    private static final Color OUTLINE = new Color(0x7B3B4D);
    private static final Color EYE = new Color(0x2F1B22);
    private static final Color HIGHLIGHT = new Color(255, 255, 255, 90);

    private PigArt() {
    }

    public static void paint(Graphics2D g, int x, int y, int size) {
        if (size <= 0) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        float s = size;
        float stroke = Math.max(1f, s * 0.035f);
        g2.setStroke(new BasicStroke(stroke));

        Path2D.Float leftEar = new Path2D.Float();
        leftEar.moveTo(x + s * 0.14f, y + s * 0.32f);
        leftEar.lineTo(x + s * 0.27f, y + s * 0.03f);
        leftEar.lineTo(x + s * 0.45f, y + s * 0.23f);
        leftEar.closePath();

        Path2D.Float rightEar = new Path2D.Float();
        rightEar.moveTo(x + s * 0.86f, y + s * 0.32f);
        rightEar.lineTo(x + s * 0.73f, y + s * 0.03f);
        rightEar.lineTo(x + s * 0.55f, y + s * 0.23f);
        rightEar.closePath();

        g2.setColor(EAR);
        g2.fill(leftEar);
        g2.fill(rightEar);
        g2.setColor(OUTLINE);
        g2.draw(leftEar);
        g2.draw(rightEar);

        Ellipse2D.Float face = new Ellipse2D.Float(
                x + s * 0.11f, y + s * 0.14f, s * 0.78f, s * 0.74f);
        g2.setColor(FACE);
        g2.fill(face);
        g2.setColor(OUTLINE);
        g2.draw(face);

        g2.setColor(HIGHLIGHT);
        g2.fill(new Ellipse2D.Float(
                x + s * 0.20f, y + s * 0.20f, s * 0.26f, s * 0.18f));

        Ellipse2D.Float snout = new Ellipse2D.Float(
                x + s * 0.30f, y + s * 0.48f, s * 0.40f, s * 0.27f);
        g2.setColor(SNOUT);
        g2.fill(snout);
        g2.setColor(OUTLINE);
        g2.draw(snout);

        g2.setColor(OUTLINE);
        g2.fill(new Ellipse2D.Float(
                x + s * 0.38f, y + s * 0.57f, s * 0.07f, s * 0.10f));
        g2.fill(new Ellipse2D.Float(
                x + s * 0.55f, y + s * 0.57f, s * 0.07f, s * 0.10f));

        g2.setColor(EYE);
        g2.fill(new Ellipse2D.Float(
                x + s * 0.29f, y + s * 0.36f, s * 0.08f, s * 0.10f));
        g2.fill(new Ellipse2D.Float(
                x + s * 0.63f, y + s * 0.36f, s * 0.08f, s * 0.10f));

        g2.dispose();
    }
}
