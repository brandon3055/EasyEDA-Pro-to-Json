package com.brandon3055.edaprotoibom;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by brandon3055 on 03/09/2022
 */
public class EDAProToIBOM {
    public static final Logger LOGGER = LogManager.getLogger("EDAProToIBOM");
    public static final Pattern UNWRAP_BRACKETS = Pattern.compile("\\[(?:[^\\]\\[]+|\\[(?:[^\\]\\[]+|\\[[^\\]\\[]*\\])*\\])*\\]");

    public static final int SPEC_VERSION = 1;
    public static double bbMinX = 9999999999999999D;
    public static double bbMinY = 9999999999999999D;
    public static double bbMaxX = -9999999999999999D;
    public static double bbMaxY = -9999999999999999D;

    //pcbdata Obj
    public static JsonObject edges_bbox = new JsonObject();
    public static JsonArray edges = new JsonArray();

    public static JsonObject drawings = new JsonObject();
    public static JsonArray silkscreenF = new JsonArray();
    public static JsonArray silkscreenB = new JsonArray();

    public static JsonArray footprints = new JsonArray();

    public static JsonObject tracks = new JsonObject();
    public static JsonArray tracksF = new JsonArray();
    public static JsonArray tracksB = new JsonArray();

    public static JsonObject zones = new JsonObject();
    public static JsonArray zonesF = new JsonArray();
    public static JsonArray zonesB = new JsonArray();

    public static JsonArray nets = new JsonArray();
    public static JsonObject metadata = new JsonObject();
    //
    public static JsonArray components = new JsonArray();

    public static Map<String, FootprintProcessor> footprintProcessors = new HashMap<>();
    public static Map<String, String> deviceFootprintMap = new HashMap<>();
    public static FootprintProcessor fallbackFootprint = new FallbackFootprint();

    public static JsonObject projectDevices;

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage:");
            System.err.println("java -jar EDAProToIBOM.jar Project/Document-Export.zip");
            System.err.println("or");
            System.err.println("java -jar EDAProToIBOM.jar Project/Document-Export.zip output_file.json");
            System.exit(0);
        }

        File input = new File(args[0]);
        if (!input.exists() || !input.isFile()) {
            throw new FileNotFoundException("Input file does not exist");
        }

        if (!input.getName().endsWith(".zip")) {
            throw new IllegalStateException("Input should be a zip file");
        }

        File output = new File(input.getParentFile(), input.getName().replace(".zip", "") + ".json");
        if (args.length > 1) {
            output = new File(args[1]);
        }

        if (output.exists()) {
            LOGGER.error("Output file already exists! " + output.getAbsolutePath());
            LOGGER.error("This application will not overwrite an existing file. Please remove the existing output file or choose a new file name.");
            LOGGER.info("Output File: " + output.getAbsolutePath());
            System.exit(0);
        }

        PCBProcessor pcbProcessor = null;

        //TODO switch to nio zip system
        try (ZipFile zipInput = new ZipFile(input)) {
            Enumeration<? extends ZipEntry> entries = zipInput.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith("FOOTPRINT/") && name.endsWith(".efoo")) {
                    name = name.substring(10, name.length() - 5);
                    footprintProcessors.put(name, new FootprintProcessor(zipInput.getInputStream(entry), entry.getName()));
                } else if (name.startsWith("PCB/") && name.endsWith(".epcb")) {
                    if (pcbProcessor != null) {
                        throw new RuntimeException("Detected more than one PCB file ins zip! This is not supported.");
                    }
                    pcbProcessor = new PCBProcessor(zipInput.getInputStream(entry), entry.getName());
                }else if (name.equals("project.json")) {
                    readProjectFile(zipInput.getInputStream(entry));
                }
            }
        }

        if (pcbProcessor == null) {
            throw new RuntimeException("No PCB file detected in zip! Expected: PBB/***...***.epcb");
        }

        pcbProcessor.postProcess();

        JsonObject assembledJson = assembleJson(input.getName());
        try (JsonWriter writer = new JsonWriter(new FileWriter(output))) {
            writer.setIndent("    ");
            new Gson().toJson(assembledJson, writer);
        }

        LOGGER.info("");
        LOGGER.info("## Conversion Complete ##");
        LOGGER.info(output.getAbsolutePath());
        LOGGER.info("#########################");
    }


    public static void readProjectFile(InputStream fileStream) {
        JsonObject project;
        try (JsonReader reader = new JsonReader(new InputStreamReader(fileStream))){
            project = new JsonParser().parse(reader).getAsJsonObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        projectDevices = project.getAsJsonObject("devices");
    }

    public static void updateBB(HasDimensions object) {
        if (object.getMinX() < bbMinX) bbMinX = object.getMinX();
        if (object.getMinY() < bbMinY) bbMinY = object.getMinY();
        if (object.getMaxX() > bbMaxX) bbMaxX = object.getMaxX();
        if (object.getMaxY() > bbMaxY) bbMaxY = object.getMaxY();
    }

    public static void updateCanvas() {
        edges_bbox.addProperty("minx", bbMinX);
        edges_bbox.addProperty("miny", bbMinY);
        edges_bbox.addProperty("maxx", bbMaxX);
        edges_bbox.addProperty("maxy", bbMaxY);
    }

    public static JsonObject assembleJson(String name) {
        updateCanvas();

        metadata.addProperty("title", "Converted: " + name);
        metadata.addProperty("revision", "");
        metadata.addProperty("company", "");
        metadata.addProperty("date", Instant.now().toString());


        JsonObject object = new JsonObject();
        object.addProperty("spec_version", SPEC_VERSION);
        JsonObject pcbdata = new JsonObject();
        pcbdata.add("edges_bbox", edges_bbox);
        pcbdata.add("edges", edges);

        JsonObject silkscreen = new JsonObject();
        silkscreen.add("F", silkscreenF);
        silkscreen.add("B", silkscreenB);
        JsonObject fabrication = new JsonObject();
        fabrication.add("F", new JsonArray());
        fabrication.add("B", new JsonArray());
        drawings.add("silkscreen", silkscreen);
        pcbdata.add("drawings", drawings);

        pcbdata.add("footprints", footprints);

        tracks.add("F", tracksF);
        tracks.add("B", tracksB);
        pcbdata.add("tracks", tracks);

        zones.add("F", zonesF);
        zones.add("B", zonesB);
        pcbdata.add("zones", zones);

        pcbdata.add("nets", nets);
        pcbdata.add("metadata", metadata);
        object.add("pcbdata", pcbdata);
        object.add("components", components);
        return object;
    }


}
