/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforgeV3.android.maps.rendertheme.renderinstruction;

import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Typeface;

import com.asamm.locus.mapsforge.utils.Utils;

import org.mapsforgeV3.android.maps.rendertheme.RenderCallback;
import org.mapsforgeV3.android.maps.rendertheme.RenderTheme;
import org.mapsforgeV3.core.model.Tag;

import java.util.regex.Pattern;

/**
 * A RenderInstruction is a basic graphical primitive to draw a map.
 */
public abstract class RenderInstruction {

    public static final Pattern SPLIT_PATTERN = Pattern.compile(",");

    public static final String KEY_ALIGN_CENTER = "align-center";
    public static final String KEY_BG_RECT_FILL = "bg-rect-fill";
    public static final String KEY_BG_RECT_STROKE = "bg-rect-stroke";
    public static final String KEY_BG_RECT_OVER = "bg-rect-over";
    public static final String KEY_BG_RECT_STROKE_WIDTH = "bg-rect-stroke-width";
    public static final String KEY_BG_RECT_ROUNDED = "bg-rect-rounded";
    public static final String KEY_BORDER_COLOR = "border-color";
    public static final String KEY_BORDER_WIDTH = "border-width";
    public static final String KEY_CAT = "cat";
    public static final String KEY_CURVE = "curve";
    public static final String KEY_DB_OUTLINE_COLOR = "db-outline-color";
    public static final String KEY_DB_OUTLINE_WIDTH = "db-outline-width";
    public static final String KEY_DX = "dx";
    public static final String KEY_DY = "dy";
    public static final String KEY_FONT_FAMILY = "font-family";
    public static final String KEY_FONT_SIZE = "font-size";
    public static final String KEY_FONT_STYLE = "font-style";
    public static final String KEY_FORCE_DRAW = "force-draw";
    public static final String KEY_K = "k";
    public static final String KEY_FILL = "fill";
    public static final String KEY_R = "r";
    public static final String KEY_RENDER_DB_ONLY = "render-db-only";
    public static final String KEY_REPEAT = "repeat";
    public static final String KEY_REPEAT_GAP = "repeat-gap";
    public static final String KEY_ROTATE_UP = "rotate_up";
    public static final String KEY_SCALE = "scale";
    public static final String KEY_SCALE_DY_SIZE = "scale-dy-size";
    public static final String KEY_SCALE_FONT_SIZE = "scale-font-size";
    public static final String KEY_SCALE_ICON_SIZE = "scale-icon-size";
    public static final String KEY_SCALE_RADIUS = "scale-radius";
    public static final String KEY_SRC = "src";
    public static final String KEY_STROKE = "stroke";
    public static final String KEY_STROKE_DASHARRAY = "stroke-dasharray";
    public static final String KEY_STROKE_LINECAP = "stroke-linecap";
    public static final String KEY_STROKE_WIDTH = "stroke-width";
    public static final String KEY_SYMBOL_COLOR = "symbol-color";
    public static final String KEY_SYMBOL_HEIGHT = "symbol-height";
    public static final String KEY_SYMBOL_WIDTH = "symbol-width";
    public static final String KEY_UPPER_CASE = "upper-case";

    public static final String VALUE_CUBIC = "cubic";

    // defined index (ID)
    protected int indexInRules;
    // defined category
    private final String mCategory;

    public RenderInstruction(int indexInRules, String category) {
        this.indexInRules = indexInRules;
        this.mCategory = category;
    }

    // CATEGORY

    /**
     * Get current defined category.
     *
     * @return defined category (may be null)
     */
    public String getCategory() {
        return mCategory;
    }

    /**
     * Destroys this RenderInstruction and cleans up all its internal resources.
     */
    public abstract void destroy();

    /**
     * @param renderCallback a reference to the receiver of all render callbacks.
     * @param tags           the tags of the node.
     */
    public abstract void renderNode(RenderCallback renderCallback, Tag[] tags);

    /**
     * @param renderCallback a reference to the receiver of all render callbacks.
     * @param tags           the tags of the way.
     */
    public abstract void renderWay(RenderCallback renderCallback, Tag[] tags);

    /**
     * Prepare rule before next usage.
     *
     * @param theme       reference to main theme, that may supply theme metadata
     * @param scaleStroke the factor by which stroke widths should be scaled
     * @param scaleText   the factor by which the text size should be scaled
     * @param zoomLevel   new zoom level
     */
    public abstract void prepare(RenderTheme theme, float scaleStroke, float scaleText, byte zoomLevel);

    static Paint generatePaintFill(Align align, Typeface typeface, int fillColor) {
        Paint paintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintFill.setTextAlign(align);
        paintFill.setTypeface(typeface);
        paintFill.setColor(fillColor);
        return paintFill;
    }

    static Paint generatePaintStroke(Align align, Typeface typeface, int strokeColor, float strokeWidth) {
        Paint paintStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintStroke.setStyle(Style.STROKE);
        paintStroke.setTextAlign(align);
        paintStroke.setTypeface(typeface);
        paintStroke.setColor(strokeColor);
        paintStroke.setStrokeWidth(strokeWidth);
        return paintStroke;
    }

    /**
     * Parse units based on "length" attribute.
     *
     * @param text text value
     * @return parsed units
     */
    public static float parseLengthUnits(String text) {
        return parseLengthUnits(text, false, false);
    }

    /**
     * Parse units based on "length" attribute.
     *
     * @param text        text value
     * @param defaultAsDp <code>true</code> if value without units will be consider as DP
     * @return computed value
     */
    public static float parseLengthUnits(String text, boolean defaultAsDp, boolean roundDensity) {
        // check text
        if (text == null || Utils.getHandler() == null) {
            return 0;
        }
        text = text.trim();
        if (text.length() == 0) {
            return 0;
        }

        // parse text
        if (text.endsWith("dp")) {
            float value = Utils.parseFloat(text.substring(0, text.length() - 2));
            return Utils.getHandler().getDpPixels(value, roundDensity);
        } else if (text.endsWith("dip")) {
            float value = Utils.parseFloat(text.substring(0, text.length() - 3));
            return Utils.getHandler().getDpPixels(value, roundDensity);
        } else if (text.endsWith("px")) {
            return Utils.parseFloat(text.substring(0, text.length() - 2));
        } else if (text.endsWith("sp")) {
            return convertSpToPx(Utils.parseFloat(text.substring(0, text.length() - 2)));
        } else {
            float value = Utils.parseFloat(text);
            if (defaultAsDp) {
                return Utils.getHandler().getDpPixels(value, roundDensity);
            }
            return value;
        }
    }

    private static float convertSpToPx(float sp) {
        float scaledDensity = Utils.getHandler().getContext().getResources().
                getDisplayMetrics().scaledDensity;
        return sp / scaledDensity;
    }
}
