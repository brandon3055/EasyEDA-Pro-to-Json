package com.brandon3055.edaprotoibom;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.annotation.Nullable;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;

/**
 * Created by brandon3055 on 03/09/2022
 */
public final class Poly {
    @Nullable
    private final String net;
    private final int layer;
    private final double lineWidth;
    private Point pos;
    private Point size;
    private final String type;
    private Point[] points;

    private Point arcStart;
    private Point arcEnd;
    private double arcAngle;

    private double circleRadius;

    private final int filled = 1;

    public boolean isPour = false;

    /**
     *
     */
    public Poly(String[] values, String rawLine, int lineNumber) throws ParseException {
        String id = Helpers.stripOuterCharacters(values[1]);
        net = Helpers.stripOuterCharacters(values[3]);
        layer = Integer.parseInt(values[4]); //11 = bourd outline, 3 = top silk, 4 = bottom silk
        lineWidth = Double.parseDouble(values[5]);

        Matcher matcher = EDAProToIBOM.UNWRAP_BRACKETS.matcher(rawLine);
        if (!matcher.find()) {
            throw new ParseException("Found poly with no geometry data! Skipping. L:" + lineNumber, lineNumber);
        }

        isPour = values[0].contains("POUR");

        String valueString = Helpers.stripOuterCharacters(matcher.group());
        if (valueString.startsWith("[") && valueString.endsWith("]")) {
            valueString = Helpers.stripOuterCharacters(valueString);
        }
        values = valueString.split(",");
//        if (values[0].contains("FILL")) {
//            matcher = EDAProToIBOM.UNWRAP_BRACKETS.matcher(valueString);
//            if (!matcher.find()) {
//                throw new ParseException("Unable to parse poly! Skipping. L:" + lineNumber, lineNumber);
//            }
//            valueString = Helpers.stripOuterCharacters(Helpers.stripOuterCharacters(matcher.group()));
//            values = valueString.split(",");
//        }


        if (Helpers.stripOuterCharacters(values[0]).equals("R")) {
            pos = new Point(Double.parseDouble(values[1]), -Double.parseDouble(values[2]));
            size = new Point(Double.parseDouble(values[3]), Double.parseDouble(values[4]));
            type = "rect";
        } else if (Helpers.stripOuterCharacters(values[2]).equals("L")) {
            pos = new Point(Double.parseDouble(values[0]), -Double.parseDouble(values[1]));
            List<Point> pointList = new ArrayList<>();
            for (int i = 3; i + 1 < values.length; i += 2) {
                pointList.add(new Point(Double.parseDouble(values[i]), -Double.parseDouble(values[i + 1])));
            }
            type = "polygon";
            points = pointList.toArray(new Point[0]);
        } else if (Helpers.stripOuterCharacters(values[0]).contains("CIRCLE")) {
            pos = new Point(Double.parseDouble(values[1]), -Double.parseDouble(values[2]));
            circleRadius = Double.parseDouble(values[3]);
            type = "circle";
        } else if (Helpers.stripOuterCharacters(values[2]).contains("ARC")) {
            arcStart = new Point(Double.parseDouble(values[0]), -Double.parseDouble(values[1]));
            arcEnd = new Point(Double.parseDouble(values[4]), -Double.parseDouble(values[5]));
            arcAngle = Double.parseDouble(values[3]);
            type = "arc";
        } else {
            throw new ParseException("Unknown poly type: " + Helpers.stripOuterCharacters(values[0]) + " Skipping. L:" + lineNumber, lineNumber);
        }
    }


    public void toJson(JsonArray array) {
        if (array == null) return;
        toJson(array, new Point(0, 0), 0, new Point(0, 0));
    }

    public void toJson(JsonArray array, Point offset, double angle, Point centerRef) {
        if (type.equals("rect")) {
            JsonObject object = new JsonObject();
            object.addProperty("type", type);
            Point start = Helpers.rotate(pos, centerRef, angle);
            Point end = Helpers.rotate(pos.add(size), centerRef, angle);
            object.add("start", Helpers.valueArray(start.add(offset)));
            object.add("end", Helpers.valueArray(end.add(offset)));
            object.addProperty("width", lineWidth);
            array.add(object);
        } else if (type.equals("arc")) {
            JsonObject object = new JsonObject();
            object.addProperty("type", type);
//            Point arcStart = Helpers.rotate(this.arcStart, centerRef, angle);
//            Point arcEnd = Helpers.rotate(this.arcEnd, centerRef, angle);

            double triAngles = (180 - Math.abs(arcAngle)) / 2;
            double L = Helpers.getDistance(arcStart, arcEnd) / 2;
            double d = L / (2 * Math.cos(triAngles));
            double inclination = -18.434948823D + triAngles;
            double centerX = 2 + (d * Math.cos(inclination));
            double centerY = 3 + (d * Math.sin(inclination));
            Point center = new Point(centerX, centerY);

//            arcAngle

//            Point arcCenter = Helpers.centroid(arcStart, arcEnd);
            double radius = Helpers.getDistance(center.add(offset), arcStart.add(offset)) / 2;


//            Point start = Helpers.rotate(center, centerRef, angle);


//            float startangle = (float) Math.toDegrees(Math.atan2(start.y() - arcCenter.y(), start.x() - arcCenter.x()));

            center = Helpers.rotate(center, centerRef, angle);

            object.add("start", Helpers.valueArray(center.add(offset)));
            object.addProperty("radius", 0);
//            object.addProperty("startangle", startangle);
            object.addProperty("startangle", 0);
//            object.addProperty("endangle", startangle + arcAngle);
            object.addProperty("endangle", 45);
            object.addProperty("width", lineWidth);


//            array.add(object);
        } else if (type.equals("circle")) {
            JsonObject object = new JsonObject();
            object.addProperty("type", type);
            Point start = Helpers.rotate(pos, centerRef, angle);
            object.add("start", Helpers.valueArray(start.add(offset)));
            object.addProperty("radius", circleRadius);
            object.addProperty("filled", filled);
            object.addProperty("width", lineWidth);
            array.add(object);
        } else {
            Point last = pos;
            //Poly to segments because IBOM does not seem to support open-ended polies.
            for (Point point : points) {
                JsonObject object = new JsonObject();
                object.addProperty("type", "segment");
                object.addProperty("width", lineWidth);
                Point start = Helpers.rotate(last, centerRef, angle);
                Point end = Helpers.rotate(point, centerRef, angle);
                object.add("start", Helpers.valueArray(start.add(offset)));
                object.add("end", Helpers.valueArray(end.add(offset)));
                array.add(object);
                last = point;
            }
        }
    }

//    public JsonObject toJson() {
//        return toJson(new Point(0, 0));
//    }
//
//    public JsonObject toJson(Point offset) {
//        JsonObject object = new JsonObject();
//        object.addProperty("type", type);
//
//        if (type.equals("rect")) {
//            object.add("start", Helpers.valueArray(pos.add(offset)));
//            object.add("end", Helpers.valueArray(pos.add(points[0]).add(offset)));
//            object.addProperty("width", lineWidth);
//        } else {
//            object.addProperty("width", lineWidth);
//            object.addProperty("filled", 0);
//            object.addProperty("angle", 0);
////            object.add("pos", Utils.valueArray(points[0].x, points[0].y));
//            object.add("pos", Helpers.valueArray(0, 0));
//            JsonArray polyPoints = new JsonArray();
//            for (Point point : points) {
//                polyPoints.add(Helpers.valueArray(point.add(offset)));
//            }
//            JsonArray polyArray = new JsonArray();
//            polyArray.add(polyPoints);
//            object.add("polygons", polyArray);
//        }
//
//        return object;
//    }

    public void toJsonZone(JsonArray array) {
        JsonObject zone = new JsonObject();
        JsonArray polies = new JsonArray();
        JsonArray poly = new JsonArray();

        if (points == null) {
            if (type.equals("rect")) {
                poly.add(Helpers.valueArray(pos));
                poly.add(Helpers.valueArray(pos.add(size.x(), 0)));
                poly.add(Helpers.valueArray(pos.add(size)));
                poly.add(Helpers.valueArray(pos.add(0, size.y())));
            } else {
                return;
            }
        }else {
            for (Point point : points) {
                poly.add(Helpers.valueArray(point));
            }
        }

        polies.add(poly);
        zone.add("polygons", polies);
        zone.addProperty("net", net);
        array.add(zone);
    }

    @Nullable
    public String net() {return net;}

    public int layer() {return layer;}

    public double lineWidth() {return lineWidth;}

    public Point pos() {return pos;}

    public String type() {return type;}

    public Point[] points() {return points;}

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Poly) obj;
        return Objects.equals(this.net, that.net) &&
                this.layer == that.layer &&
                Double.doubleToLongBits(this.lineWidth) == Double.doubleToLongBits(that.lineWidth) &&
                Objects.equals(this.pos, that.pos) &&
                Objects.equals(this.type, that.type) &&
                Objects.equals(this.points, that.points);
    }

    @Override
    public int hashCode() {
        return Objects.hash(net, layer, lineWidth, pos, type, points);
    }

    @Override
    public String toString() {
        return "Poly[" +
                "net=" + net + ", " +
                "layer=" + layer + ", " +
                "lineWidth=" + lineWidth + ", " +
                "pos=" + pos + ", " +
                "type=" + type + ", " +
                "points=" + points + ']';
    }


//    public void rectAsSegments(JsonArray array) {
//        Point p = points[0];
//        JsonObject segTop = segStart();
//        segTop.add("start", Helpers.valueArray(x, y));
//        segTop.add("end", Helpers.valueArray(x + p.x(), y));
//        array.add(segTop);
//
//        JsonObject segBottom = segStart();
//        segBottom.add("start", Helpers.valueArray(x, y + p.y()));
//        segBottom.add("end", Helpers.valueArray(x + p.x(), y + p.y()));
//        array.add(segBottom);
//
//        JsonObject segLeft = segStart();
//        segLeft.add("start", Helpers.valueArray(x, y));
//        segLeft.add("end", Helpers.valueArray(x, y + p.y()));
//        array.add(segLeft);
//
//        JsonObject segRight = segStart();
//        segRight.add("start", Helpers.valueArray(x + p.x(), y));
//        segRight.add("end", Helpers.valueArray(x + p.x(), y + p.y()));
//        array.add(segRight);
//    }

//    private JsonObject segStart() {
//        JsonObject object = new JsonObject();
//        object.addProperty("type", "segment");
//        object.addProperty("width", lineWidth);
//        return object;
//    }


}
