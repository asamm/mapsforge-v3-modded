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
