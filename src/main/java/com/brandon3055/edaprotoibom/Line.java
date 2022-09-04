package com.brandon3055.edaprotoibom;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.text.ParseException;

/**
 * Created by brandon3055 on 03/09/2022
 */
public record Line(String net, int layer, double startX, double startY, double endX, double endY, double width) implements HasDimensions {

    public static Line parse(String[] values, int lineNumber) throws ParseException {
        //["LINE","ObjectID",?,"NET",layer,startX,startY,endX,endY,width,?] //Dimensions are in mil
        String netId = Helpers.stripOuterCharacters(values[3]);
        int layer = Integer.parseInt(values[4]);
        double startX = Double.parseDouble(values[5]);
        double startY = -Double.parseDouble(values[6]);
        double endX = Double.parseDouble(values[7]);
        double endY = -Double.parseDouble(values[8]);
        double width = Double.parseDouble(values[9]);

        if (layer < 1 || layer > 2) {
            throw new ParseException("Found line that is not on a valid track layer! Skipping. L:" + lineNumber, lineNumber);
        }

        return new Line(netId, layer, startX, startY, endX, endY, width);
    }


    public JsonObject toJson() {
        JsonObject lineObj = new JsonObject();
        JsonArray start = new JsonArray();
        start.add(startX);
        start.add(startY);
        lineObj.add("start", start);

        JsonArray end = new JsonArray();
        end.add(endX);
        end.add(endY);
        lineObj.add("end", end);

        lineObj.addProperty("width", width);
        lineObj.addProperty("net", net);

        return lineObj;
    }

    @Override
    public double getMinX() {
        return startX;
    }

    @Override
    public double getMinY() {
        return startY;
    }

    @Override
    public double getMaxX() {
        return endX;
    }

    @Override
    public double getMaxY() {
        return endY;
    }
}
