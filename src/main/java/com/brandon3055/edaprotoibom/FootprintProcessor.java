package com.brandon3055.edaprotoibom;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.text.ParseException;

/**
 * Created by brandon3055 on 03/09/2022
 */
public class FootprintProcessor extends DocumentProcessor {
    protected Point startPos = new Point(9999999, 9999999);
    protected Point endPos = new Point(-9999999, -9999999);

    public FootprintProcessor(InputStream fileStream, String fileName) {
        super(fileStream, "FOOTPRINT", fileName);
        postProcess();
    }

    public void postProcess() {
        if (pads.isEmpty()) {
            startPos = new Point(0, 0);
            endPos = new Point(0, 0);
        } else {
            for (Pad pad : pads.values()) {
                updateBB(pad);
            }
        }
    }

    public JsonObject jsonForComponent(Component component) {
        Point size = endPos.subtract(startPos);
        JsonObject object = new JsonObject();
        object.addProperty("ref", component.ref);
        object.add("center", Helpers.valueArray(component.center));

        Rect bbRect = null;
        JsonArray pads = new JsonArray();
        for (Pad pad : this.pads.values()) {
            pad.net = component.pinToNetMap.getOrDefault(pad.pin, pad.net);
            Rect padRect = pad.toJsonNoPoly(pads, component.center, component.angle);
            bbRect = bbRect == null ? padRect : bbRect.enclose(padRect);
        }

        if (bbRect == null) {
            bbRect = new Rect(startPos.add(component.center), endPos.add(component.center));
        }

        object.add("pads", pads);
        JsonArray drawings = new JsonArray();
        object.add("drawings", drawings);
        object.addProperty("layer", component.layer);

        JsonObject bbox = new JsonObject();
        bbox.add("pos", Helpers.valueArray(bbRect.min()));
        bbox.add("size", Helpers.valueArray(bbRect.max().subtract(bbRect.min())));
        bbox.add("relpos", Helpers.valueArray(0, 0));
        bbox.addProperty("angle", 0);
        object.add("bbox", bbox);

        polies.forEach(poly -> poly.toJson(Helpers.getSilkLayerArray(poly.layer()), component.center, component.angle, startPos.add(size.mult(0.5))));

        return object;
    }


    public void updateBB(HasDimensions object) {
        if (object.getMinX() < startPos.x()) startPos.setX(object.getMinX());
        if (object.getMinY() < startPos.y()) startPos.setY(object.getMinY());
        if (object.getMaxX() > endPos.x()) endPos.setX(object.getMaxX());
        if (object.getMaxY() > endPos.y()) endPos.setY(object.getMaxY());
    }

    @Override
    public void parseCOMPONENT(String[] lineValues, String rawLine, int lineNumber) throws ParseException {

    }

    @Override
    public void parseATTR(String[] lineValues, String rawLine, int lineNumber) throws ParseException {

    }

    @Override
    public void parsePAD_NET(String[] lineValues, String rawLine, int lineNumber) throws ParseException {

    }
}
