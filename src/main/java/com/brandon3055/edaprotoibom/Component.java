package com.brandon3055.edaprotoibom;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

import static com.brandon3055.edaprotoibom.EDAProToIBOM.projectDevices;

/**
 * Created by brandon3055 on 03/09/2022
 */
public class Component {
    //Internal
    public String uniqueId;
    public String footprintId = "";
    public String device = "";
    public int layerInt;

    //Component
    public String ref = "";
    public String footprint = "";
    public String layer;
    public String val = "";

    public Point center;

    public JsonObject extra_fields = new JsonObject();

    public double angle;

    public Map<String, String> pinToNetMap = new HashMap<>();


    public Component(String[] values, String rawLine, int lineNumber) {
        uniqueId = Helpers.stripOuterCharacters(values[1]);
        ref = uniqueId;
        layerInt = Integer.parseInt(values[3]);
        this.layer = layerInt == 1 ? "F" : "B";
        center = new Point(Double.parseDouble(values[4]), -Double.parseDouble(values[5]));
        angle = -Double.parseDouble(values[6]);
    }

    public Component(String ref, int layer, Point center, double angle) {
        this.ref = ref;
        this.layerInt = layer;
        this.layer = layerInt == 1 ? "F" : "B";
        this.center = center;
        this.angle = angle;
    }

    public void addAttribute(String[] values, String rawLine, int lineNumber) {
        if (values[0].equals("\"ATTR\"")) {
            String type = Helpers.stripOuterCharacters(values[7]);
            if (type.equals("Footprint")) {
                footprintId = Helpers.stripOuterCharacters(values[8]);
                if (!device.isEmpty() && !EDAProToIBOM.deviceFootprintMap.containsKey(device)) {
                    EDAProToIBOM.deviceFootprintMap.put(device, footprintId);
                }
            } else if (type.equals("Designator")) {
                ref = Helpers.stripOuterCharacters(values[8]);
            } else if (type.equals("Device")) {
                device = Helpers.stripOuterCharacters(values[8]);
                if (!footprintId.isEmpty() && !EDAProToIBOM.deviceFootprintMap.containsKey(device)) {
                    EDAProToIBOM.deviceFootprintMap.put(device, footprintId);
                }
                val = device;
            } else if (type.equals("Name")) {
                extra_fields.addProperty("NAME", Helpers.stripOuterCharacters(values[8]));
            }
        }else if (values[0].equals("\"PAD_NET\"")) {
            pinToNetMap.put(Helpers.stripOuterCharacters(values[2]), Helpers.stripOuterCharacters(values[3]));
        }
    }

    public void postProcess() {
        if (projectDevices.has(device)) {
            JsonObject dev = projectDevices.getAsJsonObject(device);
            if (!extra_fields.has("NAME")) {
                extra_fields.addProperty("NAME", Helpers.getString(dev, "title", ""));
            }

            if (dev.has("attributes")) {
                JsonObject attributes = dev.getAsJsonObject("attributes");
                footprint = Helpers.getString(attributes, "Supplier Footprint", footprint);

                String mfp = Helpers.getString(attributes, "Manufacturer Part", "");
                String lcsc = Helpers.getString(attributes, "LCSC Part Name", "");
                if (attributes.has("Value")) {
                    val = Helpers.getString(attributes, "Value", val);
                } else {
                    if (mfp.contains("Ω")) {
                        val = mfp;
                    } else if (lcsc.contains("Ω")) {
                        val = lcsc;
                    } else {
                        val = !mfp.isEmpty() ? mfp : !lcsc.isEmpty() ? lcsc : val;
                    }
                }

                if (mfp.contains("Ω") && mfp.contains("%")) {
                    val = mfp;
                } else if (lcsc.contains("Ω") && lcsc.contains("%")) {
                    val = lcsc;
                }

                for (Map.Entry<String, JsonElement> entry : attributes.entrySet()) {
                    String value = entry.getValue().toString();
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = Helpers.stripOuterCharacters(value);
                    }
                    extra_fields.addProperty(entry.getKey(), value);
                }
            }
        }
    }

    public JsonObject toComponentJson() {
        JsonObject object = new JsonObject();
        object.addProperty("attr", "");
        object.addProperty("footprint", footprint);
        object.addProperty("layer", layer);
        object.addProperty("ref", ref);
        object.addProperty("val", val);
        object.add("extra_fields", extra_fields);
        return object;
    }
}
