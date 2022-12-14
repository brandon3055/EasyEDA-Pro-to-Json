package com.brandon3055.edaprotoibom;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;

/**
 * Created by brandon3055 on 03/09/2022
 */
public final class Poly {
    public static final Logger LOGGER = LogManager.getLogger(Poly.class);

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

    private final int filled = 0;

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
            JsonObject arc = new JsonObject();
            arc.addProperty("type", type);

            double toRad = Math.PI / 180;

            Point A_POS = Helpers.rotate(this.arcStart, centerRef, angle).add(offset);
            Point B_POS = Helpers.rotate(this.arcEnd, centerRef, angle).add(offset);

            if (arcAngle == 180) {
                double c_AB = Helpers.getDistance(A_POS, B_POS); //Distance from point A to point B
                Point C_POS = Helpers.centroid(A_POS, B_POS);
                double AB_deg = A_POS.getAngle(B_POS); // Angle between point a and b

                arc.addProperty("startangle", AB_deg);
                arc.addProperty("endangle", AB_deg + arcAngle);

                arc.add("start", Helpers.valueArray(C_POS));
                arc.addProperty("radius", c_AB/2);

            }else {

                double c_AB = Helpers.getDistance(A_POS, B_POS);    // Distance from point A to point B
                double A_deg = (180 - arcAngle) / 2;                // Angle at point A (I know A & B will be the same)
                double B_deg = A_deg;                               // Angle at point B
                double C_deg = arcAngle;                            // Angle at point C
                double a_CB = c_AB * Math.sin(A_deg * toRad) / Math.sin(C_deg * toRad); //Distance from point C to point B
                double b_CA = c_AB * Math.sin(B_deg * toRad) / Math.sin(C_deg * toRad); //Distance from point C to point A

                double AB_deg = A_POS.getAngle(B_POS);              // Angle between point a and b
                double Cx = A_POS.x() + (Math.cos((AB_deg - A_deg) * toRad) * a_CB);
                double Cy = A_POS.y() + (Math.sin((AB_deg - A_deg) * toRad) * a_CB);
                Point C_POS = new Point(Cx, Cy);

                double startAngle = AB_deg + A_deg;

                if (a_CB <= 0) {
                    a_CB = Math.abs(a_CB);
                    arc.addProperty("startangle", startAngle + arcAngle + 180);
                    arc.addProperty("endangle", startAngle + 180);
                } else {
                    arc.addProperty("startangle", startAngle);
                    arc.addProperty("endangle", startAngle + arcAngle);
                }
                arc.add("start", Helpers.valueArray(C_POS));
                arc.addProperty("radius", a_CB);
            }

            arc.addProperty("width", lineWidth);
            array.add(arc);

        } else if (type.equals("circle")) {
            JsonObject circle = new JsonObject();
            circle.addProperty("type", type);
            Point start = Helpers.rotate(pos, centerRef, angle);
            circle.add("start", Helpers.valueArray(start.add(offset)));
            circle.addProperty("radius", circleRadius);
            circle.addProperty("filled", filled);
            circle.addProperty("width", lineWidth);
            array.add(circle);
        } else {
            if (layer == 1 || layer == 2) {
                LOGGER.error("Line segments are not currently supported on copper layers. Skipping...");
                return;
            }
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
        } else {
            for (Point point : points) {
                poly.add(Helpers.valueArray(point));
            }
        }

        polies.add(poly);
        zone.add("polygons", polies);
        zone.addProperty("net", net);
        array.add(zone);
    }

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
}
