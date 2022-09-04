package com.brandon3055.edaprotoibom;

/**
 * Created by brandon3055 on 03/09/2022
 */
public class Rect {
    public double xMin;
    public double yMin;
    public double xMax;
    public double yMax;

    public Rect(Point min, Point max) {
        this(min.x(), min.y(), max.x(), max.y());
    }

    public Rect(double xMin, double yMin, double xMax, double yMax) {
        this.xMin = xMin;
        this.yMin = yMin;
        this.xMax = xMax;
        this.yMax = yMax;
    }

    public Point min() {
        return new Point(xMin, yMin);
    }

    public Point max() {
        return new Point(xMax, yMax);
    }

    public Rect enclose(Rect other) {
        return new Rect(Point.minValues(min(), other.min()), Point.maxValues(max(), other.max()));
    }
}
