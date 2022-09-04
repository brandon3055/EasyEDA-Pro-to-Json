package com.brandon3055.edaprotoibom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by brandon3055 on 03/09/2022
 */
public abstract class DocumentProcessor {
    public final Logger LOGGER = LogManager.getLogger(this.getClass());
    private final String docType;
    private final String fileName;

    protected List<Line> lines = new ArrayList<>();
    protected Map<String, Pad> pads = new HashMap<>();
    protected List<Poly> polies = new ArrayList<>();
    protected List<Text> texts = new ArrayList<>();
    protected List<String> nets = new ArrayList<>();

    public Map<String, Component> components = new HashMap<>();

    public DocumentProcessor(InputStream fileStream, String docType, String fileName) {
        this.docType = docType;
        this.fileName = fileName;
        processFile(fileStream);
    }

    protected void processFile(InputStream fileStream) {
        int lineN = 1;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(fileStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (lineN == 1) {
                    if (!line.startsWith("[\"DOCTYPE\",")) {
                        LOGGER.warn("First line does not match expected! Is this a valid .epcb file???");
                    }
                }
                try {
                    processLine(line, lineN);
                }catch (StringIndexOutOfBoundsException | ParseException | NumberFormatException e) {
                    System.err.println("Error processing File: " + fileName);
                    e.printStackTrace();
                }
                lineN++;
            }
        }catch (Throwable e) {
            LOGGER.error("Error processing File: " + fileName);
            LOGGER.error("An error occurred parsing line: " + lineN);
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void processLine(String line, int lineNumber) throws ParseException {
        if (!line.startsWith("[") || !line.endsWith("]")) {
            throw new RuntimeException("Detected invalid line!, No enclosing brackets.");
        }
        String unwrapped = Helpers.stripOuterCharacters(line);
        String[] lineValues = unwrapped.split(",");
        String lineType = Helpers.stripOuterCharacters(lineValues[0]);

        switch (lineType) {
            case "DOCTYPE" -> handleDOCTYPE(lineValues, unwrapped, lineNumber);
            case "LINE" -> parseLINE(lineValues, unwrapped, lineNumber, lines);
            case "NET" -> parseNET(lineValues, unwrapped, lineNumber);
            case "PAD" -> parsePAD(lineValues, unwrapped, lineNumber, pads);
            case "FILL" -> parseFILL(lineValues, unwrapped, lineNumber);
            case "POLY" -> parsePOLY(lineValues, unwrapped, lineNumber);
//            case "REGION" -> parseREGION(lineValues, unwrapped, lineNumber);
            case "POUR" -> parsePOUR(lineValues, unwrapped, lineNumber);
//            case "STRING" -> parseSTRING(lineValues, unwrapped, lineNumber);
            case "COMPONENT" -> parseCOMPONENT(lineValues, unwrapped, lineNumber);
            case "ATTR" -> parseATTR(lineValues, unwrapped, lineNumber);
            case "PAD_NET" -> parsePAD_NET(lineValues, unwrapped, lineNumber);

            case "LAYER",
                    "LAYER_PHYS",
                    "ACTIVE_LAYER",
                    "RULE",
                    "RULE_SELECTOR",
                    "PRIMITIVE",
                    "PANELIZE",
                    "PANELIZE_STAMP",
                    "PANELIZE_SIDE",
                    "CANVAS",
                    "VIA",
                    "CONNECT",
                    "SILK_OPTS" -> {/*Ignored*/}

            default -> handleUnknown(lineType, lineNumber);
        }
    }

    public void handleDOCTYPE(String[] lineValues, String rawLine, int lineNumber) throws ParseException {
        String type = Helpers.stripOuterCharacters(lineValues[1]);
        if (!type.equals(docType)) {
            throw new RuntimeException("Attempted to parse invalid doctype! Expected: " + docType + ". Found: "  + type);
        }
    }

    public void parseLINE(String[] lineValues, String rawLine, int lineNumber, List<Line> lines) throws ParseException {
        lines.add(Line.parse(lineValues, lineNumber));
    }

    public final void parseNET(String[] lineValues, String rawLine, int lineNumber) throws ParseException {
        nets.add(Helpers.stripOuterCharacters(lineValues[1]));
    }

    public void parsePAD(String[] lineValues, String rawLine, int lineNumber, Map<String, Pad> pads) throws ParseException {
        Pad pad = new Pad(lineValues, rawLine, lineNumber);
        pads.put(pad.padId, pad);
    }

    public final void parseFILL(String[] lineValues, String rawLine, int lineNumber) throws ParseException {
        polies.add(new Poly(lineValues, rawLine, lineNumber));
    }

    public void parsePOLY(String[] lineValues, String rawLine, int lineNumber) throws ParseException{
        polies.add(new Poly(lineValues, rawLine, lineNumber));
    }

    public void parseREGION(String[] lineValues, String rawLine, int lineNumber) throws ParseException {
        //TODO
    }

    public void parsePOUR(String[] lineValues, String rawLine, int lineNumber) throws ParseException {
        polies.add(new Poly(lineValues, rawLine, lineNumber));
    }

    public void parseSTRING(String[] lineValues, String rawLine, int lineNumber) throws ParseException {
        texts.add(new Text(lineValues, rawLine, lineNumber));
    }

    public abstract void parseCOMPONENT(String[] lineValues, String rawLine, int lineNumber) throws ParseException;
    public abstract void parseATTR(String[] lineValues, String rawLine, int lineNumber) throws ParseException;
    public abstract void parsePAD_NET(String[] lineValues, String rawLine, int lineNumber) throws ParseException;

    private final List<String> unknownTypes = new ArrayList<>();
    private void handleUnknown(String type, int lineNumber) {
        if (!unknownTypes.contains(type)) {
            LOGGER.info("Detected Unhandled Type!, " + type + ", Line: " + lineNumber);
            unknownTypes.add(type);
        }
    }
}
