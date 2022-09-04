package com.brandon3055.edaprotoibom;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Created by brandon3055 on 03/09/2022
 */
public final class Pad implements HasDimensions {
    public static final Logger LOGGER = LogManager.getLogger(Pad.class);

    //Internal Fields
    public String padId;
    public String pin;
    public int intLayer;

    //Exported fields
    public List<String> layers = new ArrayList<>();
    public Point pos;
    public Point size;
    public double angle;
    public boolean isPin1 = false;
    public String shape;
    public List<Point> polys = new ArrayList<>();
    //    private double radius;
    public String type = "smd";// "th" or "smd".
    public String drillshape = "circle";// "circle", "oblong".
    public Point drillsize;// "circle", "oblong".
    public String net = "";

    /**
     *
     */
    public Pad(String[] values, String rawLine, int lineNumber) throws ParseException {
        padId = Helpers.stripOuterCharacters(values[1]);
        net = Helpers.stripOuterCharacters(values[3]);
        intLayer = Integer.parseInt(values[4]);
        if ((intLayer < 1 || intLayer > 2) && intLayer != 12) throw new ParseException("Found pad that is not on a valid layer! Skipping. L:" + lineNumber, lineNumber);
        if (intLayer == 1 || intLayer == 12) layers.add("F");
        if (intLayer == 2 || intLayer == 12) layers.add("B");
        pin = Helpers.stripOuterCharacters(values[5]);
        isPin1 = pin.equals("1");

        pos = new Point(Double.parseDouble(values[6]), -Double.parseDouble(values[7]));
        angle = Double.parseDouble(values[8]);

        //Find the first geometry element
        Matcher matcher = EDAProToIBOM.UNWRAP_BRACKETS.matcher(rawLine);
        if (!matcher.find()) {
            throw new ParseException("Found pad with no geometry data! Skipping. L:" + lineNumber, lineNumber);
        }

        String geoString = Helpers.stripOuterCharacters(matcher.group());
        String[] geoValues = geoString.split(",");

        boolean hasHole = !values[9].equals("null");
        if (hasHole) {
            type = "th";
            String holeShape = Helpers.stripOuterCharacters(geoValues[0]);
            if (holeShape.equals("ROUND")) {
                drillsize = new Point(Double.parseDouble(geoValues[1]), Double.parseDouble(geoValues[2]));
            } else {
                LOGGER.warn("Skipping unsupported hole shape: " + holeShape);
            }
            rawLine = matcher.replaceFirst("");
            matcher = EDAProToIBOM.UNWRAP_BRACKETS.matcher(rawLine);
            if (!matcher.find()) {
                throw new ParseException("Found pad with no geometry data! Skipping. L:" + lineNumber, lineNumber);
            }
            geoString = Helpers.stripOuterCharacters(matcher.group());
            geoValues = geoString.split(",");
        }

        String padShape = Helpers.stripOuterCharacters(geoValues[0]);
        if (padShape.equals("RECT")) {
            shape = "rect";
            size = new Point(Double.parseDouble(geoValues[1]), Double.parseDouble(geoValues[2]));
        } else if (padShape.equals("ELLIPSE")) {
            shape = "oval";
            size = new Point(Double.parseDouble(geoValues[1]), Double.parseDouble(geoValues[2]));
        }else if (padShape.equals("OVAL")) {
            shape = "oval";
            size = new Point(Double.parseDouble(geoValues[1]), Double.parseDouble(geoValues[2]));
        } else if (padShape.equals("POLY")) {
            matcher = EDAProToIBOM.UNWRAP_BRACKETS.matcher(geoString);
            if (!matcher.find()) {
                throw new ParseException("Found pad with no geometry data! Skipping. L:" + lineNumber, lineNumber);
            }
            geoValues = Helpers.stripOuterCharacters(matcher.group()).split(",");
            shape = "custom";
//            double startX = Double.parseDouble(geoValues[0]);
//            double startY = -Double.parseDouble(geoValues[1]);
//            polys.add(new Point(startX, startY));
            for (int i = 3; i + 1 < geoValues.length; i += 2) {
                polys.add(new Point(Double.parseDouble(geoValues[i]), -Double.parseDouble(geoValues[i + 1])));
            }
            size = Helpers.centroid(polys);
        } else {
            throw new ParseException("Found unsupported pad shape: "+ padShape+" Skipping. L:" + lineNumber, lineNumber);
        }
    }
    //PCB Pads //Layer 12 is multi-layer
    //   0     1    2   3    4   5   6          7            Hole                       Pad Shape
    //["PAD","e397",0,"AGND",12,"1",3444.8714,-1377.9528,0,["ROUND",35.4331,35.4331],["ELLIPSE",59.0551,59.0551],[],0,0,0,1,0,null,null,null,null,0]
    //["PAD","e398",0,"AGND",12,"1",3602.3517,-1377.9528,0,["ROUND",35.4331,35.4331],["ELLIPSE",59.0551,59.0551],[],0,0,0,1,0,null,null,null,null,0]
    //["PAD","e1732",0,"$1N93875",12,"1",2513.7745,-1755.902,0,["ROUND",33.4646,33.4646],["ELLIPSE",45.2756,45.2756],[],0,0,0,1,0,null,null,null,null,0]
    //["PAD","e1733",0,"$1N91004",12,"1",1283.462,-1775.587,0,["ROUND",33.4646,33.4646],["ELLIPSE",45.2756,45.2756],[],0,0,0,1,0,null,null,null,null,0]
    //["PAD","e1734",0,"$1N91419",12,"1",1653.54,-1624.0125,0,["ROUND",33.4646,33.4646],["ELLIPSE",45.2756,45.2756],[],0,0,0,1,0,null,null,null,null,0]
    //["PAD","e1735",0,"$1N93618",12,"1",2131.8855,-1624.0125,0,["ROUND",33.4646,33.4646],["ELLIPSE",45.2756,45.2756],[],0,0,0,1,0,null,null,null,null,0]

    //Footprint Pads
    //["PAD","objId",?,"?",layer,"pin",xPosRelComp,yPosRelComp,0,null,["RECT",width,height],[],-0.003,0.002,90,1,0,1.9689999999999999,1.9689999999999999,-3937,-3937,0]

    //   0     1  2  3 4  5     6     7    no-Hole     Pad Shape
    //["PAD","e9",0,"",1,"1",-49.21,-75.1,0,null,["RECT",11.024,26.181],[],-0.003,0.002,90,1,0,1.9689999999999999,1.9689999999999999,-3937,-3937,0]
    //["PAD","e10",0,"",1,"2",-29.53,-75.1,0,null,["RECT",11.024,26.181],[],0.002,0.002,90,1,0,1.9689999999999999,1.9689999999999999,-3937,-3937,0]
    //["PAD","e11",0,"",1,"3",-9.84,-75.1,0,null,["RECT",11.024,26.181],[],-0.003,0.002,90,1,0,1.9689999999999999,1.9689999999999999,-3937,-3937,0]
    //["PAD","e12",0,"",1,"4",9.84,-75.1,0,null,["RECT",11.024,26.181],[],0.003,0.002,90,1,0,1.9689999999999999,1.9689999999999999,-3937,-3937,0]
    //["PAD","e13",0,"",1,"5",29.53,-75.1,0,null,["RECT",11.024,26.181],[],-0.002,0.002,90,1,0,1.9689999999999999,1.9689999999999999,-3937,-3937,0]
    //["PAD","e14",0,"",1,"6",49.21,-75.1,0,null,["RECT",11.024,26.181],[],0.003,0.002,90,1,0,1.9689999999999999,1.9689999999999999,-3937,-3937,0]

    public Rect toJsonNoPoly(JsonArray array, Point offset, double angle) {
        JsonObject pad = new JsonObject();
        pad.add("layers", Helpers.valueArray(layers.toArray(new String[0])));

        Point dif = pos;
        Point pos = Helpers.rotate(this.pos, new Point(0, 0), angle);
        dif = pos.subtract(dif);

        pad.add("pos", Helpers.valueArray(pos.add(offset)));
        pad.add("size", Helpers.valueArray(size));
        pad.addProperty("angle", angle);
        pad.addProperty("shape", shape);
        pad.addProperty("net", net);
        pad.addProperty("type", type);
        if (type.equals("th")) {
            pad.addProperty("drillshape", drillshape);
            pad.add("drillsize", Helpers.valueArray(drillsize));
        }

        if (!polys.isEmpty()) {
            pad.addProperty("angle", -angle);
            JsonArray polyArray = new JsonArray();
            JsonArray poly = new JsonArray();
            for (Point point : polys) {
//                Point p = Helpers.rotate(point, new Point(0, 0), angle);
                poly.add(Helpers.valueArray(point));
            }
            polyArray.add(poly);
            pad.add("polygons", polyArray);
        }

        if (isPin1) {
            pad.addProperty("pin1", 1);
        }

        array.add(pad);
        return new Rect(pos.subtract(size.mult(0.5)).add(offset), pos.add(size.mult(0.5)).add(offset));
    }

    @Override
    public double getMinX() {
        return pos.x() - (size.x() / 2);
    }

    @Override
    public double getMinY() {
        return pos.y() - (size.y() / 2);
    }

    @Override
    public double getMaxX() {
        return getMinX() + size.x();
    }

    @Override
    public double getMaxY() {
        return getMinY() + size.y();
    }
}
