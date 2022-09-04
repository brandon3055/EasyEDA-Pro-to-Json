package com.brandon3055.edaprotoibom;

import java.text.ParseException;

/**
 * Created by brandon3055 on 03/09/2022
 */
public class Parsers {


    public static void parsePad(String[] values, int lineNumber) {
        //["VIA","ObjectID",?,"NET","?",centerX,centerY,insideDiameter,outsideDiameter,?,null,null,?] //Dimensions are in mil
//        String netId = stripOuterCharacters(values[3]);
//        int layer = Integer.parseInt(values[4]);
//        double centerX = Double.parseDouble(values[5]);
//        double centerY = Double.parseDouble(values[6]);
//        double insideDiameter = Double.parseDouble(values[7]);
//        double outsideDiameter = Double.parseDouble(values[8]);
//
//
//
//        JsonObject lineObj = new JsonObject();
//        JsonArray start = new JsonArray();
//        start.add(startX);
//        start.add(startY);
//        lineObj.add("start", start);
//
//        JsonArray end = new JsonArray();
//        end.add(endX);
//        end.add(endY);
//        lineObj.add("end", end);
//
//        lineObj.addProperty("width", width);
//        lineObj.addProperty("net", netId);
//
//        if (layer == 1) {
//            tracksF.add(lineObj);
//        } else if (layer == 2) {
//            tracksB.add(lineObj);
//        } else {
//            LOGGER.warn("Found line that is not on a valid track layer! Skipping. L:" + lineNumber);
//            return;
//        }
//        updateBB(startX, startY, endX, endY);
    }

}
