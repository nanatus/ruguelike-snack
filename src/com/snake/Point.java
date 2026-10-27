package com.snake;

/**
 * 地图上的一个格子坐标。地图无限大，坐标使用 long 以便蛇一直向负方向前进也不溢出。
 */
public final class Point {

    public final long x;
    public final long y;

    public Point(long x, long y) {
        this.x = x;
        this.y = y;
    }

    public Point translate(Direction dir) {
        return new Point(x + dir.dx, y + dir.dy);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Point)) {
            return false;
        }
        Point other = (Point) obj;
        return x == other.x && y == other.y;
    }

    @Override
    public int hashCode() {
        return (int) (x * 31 + y);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
