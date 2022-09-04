package com.brandon3055.edaprotoibom;

import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.ParseException;
import java.util.List;

/**
 * Created by brandon3055 on 03/09/2022
 */
public final class Text {
    public static final Logger LOGGER = LogManager.getLogger(Text.class);

    //Internal Fields
    public int intLayer;
    public String id;

    //Exported fields
    public Point pos;
    public String textString;
    public List<Point> polys;
    public double width;
    public double height;
    public int justify = -1;
    public double thickness;
    public double angle;
    public int ref = 0;
    public int val = 0;

    /**
     *
     */
    public Text(String[] values, String rawLine, int lineNumber) throws ParseException {
        values = rawLine.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        id = Helpers.stripOuterCharacters(values[1]);
        int i1 = Integer.parseInt(values[2]);
        intLayer = Integer.parseInt(values[3]);
        pos = new Point(Double.parseDouble(values[4]), -Double.parseDouble(values[5]));
        textString = Helpers.stripOuterCharacters(values[6]);
        height = Double.parseDouble(values[8]);
        width = height; //TODO figure out how to approximate width
        thickness = Double.parseDouble(values[9]);
        angle = Double.parseDouble(values[13]);
    }

    //I have no clue why this does not work...
    public JsonObject toJson() {
        JsonObject text = new JsonObject();
        text.add("pos", Helpers.valueArray(pos));
        text.addProperty("type", "text");
        text.addProperty("text", textString);
        text.addProperty("height", height);
        text.addProperty("width", width);
        text.addProperty("justify", justify);
        text.addProperty("thickness", thickness);
        text.addProperty("angle", angle);
        return text;
    }
}
