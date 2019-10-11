package com.asamm.locus.mapsforge.utils;

import java.util.List;

public class Utils {

    private static MapsForgeHandler handler;

    public static MapsForgeHandler getHandler() {
        return handler;
    }

    public static void registerHandler(MapsForgeHandler handler) {
        Utils.handler = handler;
    }

    // STRING CONVERSION

    public static int hashTagParameter(String keyValue) {
        return keyValue.hashCode();
    }

    //*************************************************
    // PARSE TOOLS
    //*************************************************

    // BOOLEAN

    /**
     * Parse boolean base object.
     *
     * @param data object to parse
     * @return parsed boolean value
     */
    public static boolean parseBoolean(Object data) {
        return parseBoolean(String.valueOf(data));
    }

    /**
     * Parse boolean base object.
     *
     * @param data object to parse
     * @return parsed boolean value
     */
    public static boolean parseBoolean(String data) {
        return parseBoolean(data, false);
    }

    /**
     * Parse received data into boolean value or return default value in case of any problem.
     */
    public static boolean parseBoolean(Object data, boolean def) {
        return parseBoolean(String.valueOf(data), def);
    }

    /**
     * Parse received data into boolean value or return default value in case of any problem.
     */
    public static boolean parseBoolean(String data, boolean def) {
        try {
            data = data.toLowerCase().trim();
            return data.equals("true") || data.equals("1");
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * Parse integer base object.
     *
     * @param data object to parse
     * @return parsed integer value
     */
    public static int parseInt(String data) {
        return parseInt(data, 0);
    }

    /**
     * Parse integer base object.
     *
     * @param data object to parse
     * @param def  default value in case of any problem
     * @return parsed integer value
     */
    public static int parseInt(String data, int def) {
        try {
            //return Integer.parseInt(data.trim());
            // 'data' parameter may be an decimal number, so to "correctly" handle it,
            // parse content as decimal and convert it to Integer
            return Integer.parseInt(data.trim());
        } catch (Exception e) {
            return Math.round((float) parseDouble(data, def));
        }
    }

    /**
     * Parse float base object.
     *
     * @param data object to parse
     * @return parsed float value
     */
    public static float parseFloat(String data) {
        return parseFloat(data, 0.0f);
    }

    public static float parseFloat(String data, float def) {
        try {
            data = data.trim().replace(",", ".");
            return Float.parseFloat(data);
        } catch (Exception e) {
            return def;
        }
    }

    public static double parseDouble(String data, double def) {
        try {
            data = data.trim().replace(",", ".");
            return Double.parseDouble(data);
        } catch (Exception e) {
            return def;
        }
    }

    // VARIOUS TOOLS

    public static int[] convertListString(List<String> list) {
        int[] values = new int[list.size()];
        for (int i = 0, m = list.size(); i < m; i++) {
            values[i] = hashTagParameter(list.get(i));
        }
        return values;
    }


    public static boolean contains(int[] data, int item) {
        for (int aData : data) {
            if (aData == item) {
                return true;
            }
        }
        return false;
    }
}
