package com.brandon3055.edaprotoibom;

import java.util.Objects;

public final class Point {
    private double x;
    private double y;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double x() {return x;}

    public double y() {return y;}

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Point) obj;
        return Double.doubleToLongBits(this.x) == Double.doubleToLongBits(that.x) &&
                Double.doubleToLongBits(this.y) == Double.doubleToLongBits(that.y);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "Point[" +
                "x=" + x + ", " +
                "y=" + y + ']';
    }

    public Point add(Point point) {
        return add(point.x, point.y);
    }

    public Point add(double x, double y) {
        return new Point(this.x + x, this.y + y);
    }

    public Point subtract(Point point) {
        return new Point(x - point.x(), y - point.y());
    }

    public Point mult(double multiplier) {
        return new Point(x * multiplier, y * multiplier);
    }

    public static Point minValues(Point p1, Point p2) {
        return new Point(Math.min(p1.x(), p2.x()), Math.min(p1.y(), p2.y()));
    }

    public static Point maxValues(Point p1, Point p2) {
        return new Point(Math.max(p1.x(), p2.x()), Math.max(p1.y(), p2.y()));
    }

    public double getAngle(Point target) {
        double angle = (float) Math.toDegrees(Math.atan2(target.y - y, target.x - x));

        if(angle < 0){
            angle += 360;
        }

        return angle;
    }
}