package com.snake;

/** 无限模式中的静态大病毒：左上角坐标加正方形边长（格）。 */
public final class LargeVirus {

    public static final int MIN_SIZE = 2;
    public static final int MAX_SIZE = 10;

    public final Point topLeft;
    public final int size;

    public LargeVirus(Point topLeft, int size) {
        if (size < MIN_SIZE || size > MAX_SIZE) {
            throw new IllegalArgumentException("virus size out of range: " + size);
        }
        this.topLeft = topLeft;
        this.size = size;
    }

    public boolean contains(Point p) {
        return p.x >= topLeft.x && p.x < topLeft.x + size
                && p.y >= topLeft.y && p.y < topLeft.y + size;
    }

    public boolean overlaps(LargeVirus other) {
        return topLeft.x < other.topLeft.x + other.size
                && topLeft.x + size > other.topLeft.x
                && topLeft.y < other.topLeft.y + other.size
                && topLeft.y + size > other.topLeft.y;
    }

    @Override
    public String toString() {
        return "LargeVirus[" + topLeft + ", " + size + "x" + size + "]";
    }
}
