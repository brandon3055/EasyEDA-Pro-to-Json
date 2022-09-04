package com.brandon3055.edaprotoibom;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.text.ParseException;

import static com.brandon3055.edaprotoibom.EDAProToIBOM.*;

/**
 * Created by brandon3055 on 03/09/2022
 */
public class PCBProcessor extends DocumentProcessor {

    public PCBProcessor(InputStream fileStream, String fileName) {
        super(fileStream, "PCB", fileName);
    }

    public void postProcess() {
        for (Line line : lines) {
            JsonArray layer = Helpers.getLayerArray(line.layer());
            if (layer != null) layer.add(line.toJson());
            EDAProToIBOM.updateBB(line);
        }

        for (Poly poly : polies) {
            if (poly.isPour) {
                if (poly.layer() == 1) {
                    poly.toJsonZone(zonesF);
                } else if (poly.layer() == 2) {
                    poly.toJsonZone(zonesB);
                }
            } else {
                poly.toJson(Helpers.getLayerArray(poly.layer()));
            }
        }

        nets.forEach(net -> EDAProToIBOM.nets.add(net));

        for (Component component : components.values()) {
            component.postProcess();
            FootprintProcessor footprint = footprintProcessors.get(component.footprintId);
            if (footprint == null) {
                footprint = findFootprint(component);
            }
            EDAProToIBOM.components.add(component.toComponentJson());
            EDAProToIBOM.footprints.add(footprint.jsonForComponent(component));
        }

        for (Pad pad : pads.values()) {
            Component component = new Component("Pad", pad.intLayer, pad.pos, pad.angle);
            pad.pos = new Point(0, 0);
            FootprintProcessor footprint = new PadFootprint(pad);
            EDAProToIBOM.components.add(component.toComponentJson());
            EDAProToIBOM.footprints.add(footprint.jsonForComponent(component));
        }

        int i = 0;
        for (Text text : texts) {
            if (i > 0) continue;
            Helpers.getLayerArray(text.intLayer).add(text.toJson());
            i++;
        }
    }

    private FootprintProcessor findFootprint(Component component) {
        if (deviceFootprintMap.containsKey(component.device)) {
            return footprintProcessors.get(deviceFootprintMap.get(component.device));
        }

        if (projectDevices.has(component.device)) {
            JsonObject device = projectDevices.get(component.device).getAsJsonObject();
            if (device.has("attributes")) {
                JsonObject attributes = device.get("attributes").getAsJsonObject();
                if (attributes.has("Footprint")) {
                    String footprint = attributes.get("Footprint").getAsString();
                    if (footprintProcessors.containsKey(footprint)) {
                        return footprintProcessors.get(footprint);
                    }
                }
            }
        }

        return EDAProToIBOM.fallbackFootprint;
    }

    @Override
    public void parseCOMPONENT(String[] lineValues, String rawLine, int lineNumber) throws ParseException {
        Component component = new Component(lineValues, rawLine, lineNumber);
        components.put(component.uniqueId, component);
    }

    @Override
    public void parseATTR(String[] lineValues, String rawLine, int lineNumber) throws ParseException {
        String ref = Helpers.stripOuterCharacters(lineValues[3]);
        if (components.containsKey(ref)) {
            components.get(ref).addAttribute(lineValues, rawLine, lineNumber);
        }
    }

    @Override
    public void parsePAD_NET(String[] lineValues, String rawLine, int lineNumber) throws ParseException {
        String ref = Helpers.stripOuterCharacters(lineValues[1]);
        if (components.containsKey(ref)) {
            components.get(ref).addAttribute(lineValues, rawLine, lineNumber);
        }
    }

}
