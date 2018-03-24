package org.mapsforgeV3.android.maps.rendertheme.tools;

import com.asamm.locus.mapsforge.utils.Utils;

import java.util.regex.Pattern;

/**
 * Created by menion on 28/11/14.
 * Asamm Software, s. r. o.
 */
public class ScalableParameter {

    private static final Pattern SPLIT_PATTERN = Pattern.compile(",");

    public static ScalableParameter create(String value) {
        String[] scaleEntries = SPLIT_PATTERN.split(value);

        // construct parameter
        if (scaleEntries.length == 2) {
            byte baseZoom = (byte) Utils.getHandler().parseInt(scaleEntries[0]);
            float increment = Utils.getHandler().parseFloat(scaleEntries[1]);
            return new ScalableParameter(baseZoom, increment);
        }
        return null;
    }

    // base zoom level
    private final byte mBaseZoom;
    // defined scale value
    private final float mIncrement;

    // last used (default) value
    private float mDefaultValue;

    public ScalableParameter(byte baseLevel, float increment) {
        mBaseZoom = baseLevel;
        mIncrement = increment;
    }

    public float computeValue(float size, byte zoomLevel) {
        setDefaultValue(size);
        return computeValue(zoomLevel);
    }

    private void setDefaultValue(float value) {
        this.mDefaultValue = value;
    }

    private float computeValue(byte zoomLevel) {
        float size = mDefaultValue;
        if (mBaseZoom > 0 && zoomLevel > mBaseZoom) {
            for (int i = 0; i < zoomLevel - mBaseZoom; i++) {
                size *= mIncrement;
            }
        }
        return size;
    }
}
