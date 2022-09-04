package com.brandon3055.edaprotoibom;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

/**
 * Created by brandon3055 on 03/09/2022
 */
public class Helpers {

    public static String stripOuterCharacters(String input) {
        if (input.length() < 2) return input;
        return input.substring(1, input.length() - 1);
    }

    public static JsonArray valueArray(double... values) {
        JsonArray array = new JsonArray();
        for (double value : values) {
            array.add(value);
        }
        return array;
    }

    public static JsonArray valueArray(Point pos) {
        return valueArray(pos.x(), pos.y());
    }

    public static JsonArray valueArray(String... values) {
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }

    public static JsonArray getLayerArray(int layer) {
        return switch (layer) {
            case 1 -> EDAProToIBOM.tracksF;
            case 2 -> EDAProToIBOM.tracksB;
            case 3 -> EDAProToIBOM.silkscreenF;
            case 4 -> EDAProToIBOM.silkscreenB;
            case 11, 12, 47 -> EDAProToIBOM.edges;
            default -> null;
        };
    }

    public static JsonArray getSilkLayerArray(int layer) {
        switch (layer) {
            case 1:
                return EDAProToIBOM.silkscreenF;
            case 2:
                return EDAProToIBOM.silkscreenB;
            case 3:
                return EDAProToIBOM.silkscreenF;
            case 4:
                return EDAProToIBOM.silkscreenB;
//            case 11:
//            case 47:
//                return EDAProToIBOM.edges;
        }
        return new JsonArray();
    }

    public static Point rotate(Point point, Point around, double angle) {
        double s = Math.sin(angle * (Math.PI / 180));
        double c = Math.cos(angle * (Math.PI / 180));

        point = point.subtract(around);

        double x = point.x() * c - point.y() * s;
        double y = point.x() * s + point.y() * c;

        return new Point(x + around.x(), y + around.y());
    }

    public static Point centroid(Point start, Point... points) {
        double centroidX = 0, centroidY = 0;
        centroidX += start.x();
        centroidY += start.y();
        for (Point point : points) {
            centroidX += point.x();
            centroidY += point.y();
        }
        return new Point(centroidX / (points.length + 1), centroidY / (points.length + 1));
    }

    public static Point centroid(List<Point> points) {
        double centroidX = 0, centroidY = 0;
        for (Point point : points) {
            centroidX += point.x();
            centroidY += point.y();
        }
        return new Point(centroidX / (points.size()), centroidY / (points.size()));
    }

    public static double getDistance(Point p1, Point p2) {
        double dx = p1.x() - p2.x();
        double dy = p1.y() - p2.y();
        return Math.sqrt((dx * dx + dy * dy));
    }

    public static String getString(JsonObject object, String name, String fallback) {
        if (object.has(name)) {
            return object.get(name).getAsString();
        }
        return fallback;
    }
}
