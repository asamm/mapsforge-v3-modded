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

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Typeface;

import com.asamm.locus.mapsforge.utils.Utils;

import org.mapsforgeV3.android.maps.rendertheme.RenderCallback;
import org.mapsforgeV3.android.maps.rendertheme.RenderTheme;
import org.mapsforgeV3.android.maps.rendertheme.tools.BgRectangle;
import org.mapsforgeV3.android.maps.rendertheme.tools.FontFamily;
import org.mapsforgeV3.android.maps.rendertheme.tools.FontStyle;
import org.mapsforgeV3.android.maps.rendertheme.tools.ScalableParameter;
import org.mapsforgeV3.android.maps.rendertheme.tools.TextKey;
import org.mapsforgeV3.core.model.Tag;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Represents a text along a polyline on the map.
 */
public final class PathText extends RenderInstruction {
	
	/**
	 * @param elementName
	 *            the name of the XML element.
	 * @param attrs
	 *            the attributes of the XML element.
	 * @return a new PathText with the given rendering attributes.
	 */
	public static PathText create(int indexInRules, String elementName, HashMap<String, String> attrs) {
		// BASIC
        String category = null;
		TextKey textKey = null;
		float dx = 0;
		float dy = 3;
		boolean rotateUp = true;
        ScalableParameter dyScale = null;

		// FONT
		FontFamily fontFamily = FontFamily.DEFAULT;
		FontStyle fontStyle = FontStyle.NORMAL;
        float fontSize = 0;
        ScalableParameter fontSizeScale = null;

		// PAINT
		int fill = Color.BLACK;
		int stroke = Color.BLACK;
		float strokeWidth = 0;
		boolean upperCase = false;

        // BASIC
        if (attrs.containsKey(KEY_CAT)) {
            category = attrs.remove(KEY_CAT);
        }
        if (attrs.containsKey(KEY_K)) {
            textKey = TextKey.getInstance(attrs.remove(KEY_K));
        }
        if (attrs.containsKey(KEY_DX)) {
            dx = parseLengthUnits(attrs.remove(KEY_DX));
        }
        if (attrs.containsKey(KEY_DY)) {
            dy = parseLengthUnits(attrs.remove(KEY_DY));
        }
		if (attrs.containsKey(KEY_ROTATE_UP)) {
			rotateUp = Utils.getHandler().parseBoolean(attrs.remove(KEY_ROTATE_UP));
		}
        if (attrs.containsKey(KEY_SCALE_DY_SIZE)) {
            dyScale = ScalableParameter.create(attrs.remove(KEY_SCALE_DY_SIZE));
        }

        // FONT
        if (attrs.containsKey(KEY_FONT_FAMILY)) {
            fontFamily = FontFamily.valueOf(attrs.remove(KEY_FONT_FAMILY).toUpperCase(Locale.ENGLISH));
        }
        if (attrs.containsKey(KEY_FONT_STYLE)) {
            fontStyle = FontStyle.valueOf(attrs.remove(KEY_FONT_STYLE).toUpperCase(Locale.ENGLISH));
        }
        if (attrs.containsKey(KEY_FONT_SIZE)) {
            fontSize = parseLengthUnits(attrs.remove(KEY_FONT_SIZE));
        }

        // PAINT
        if (attrs.containsKey(KEY_FILL)) {
            fill = Color.parseColor(attrs.remove(KEY_FILL));
        }
        if (attrs.containsKey(KEY_STROKE)) {
            stroke = Color.parseColor(attrs.remove(KEY_STROKE));
        }
        if (attrs.containsKey(KEY_STROKE_WIDTH)) {
            strokeWidth = parseLengthUnits(attrs.remove(KEY_STROKE_WIDTH));
        }
        if (attrs.containsKey(KEY_UPPER_CASE)) {
            upperCase = Utils.getHandler().parseBoolean(attrs.remove(KEY_UPPER_CASE));
        }
        if (attrs.containsKey(KEY_SCALE_FONT_SIZE)) {
            fontSizeScale = ScalableParameter.create(attrs.remove(KEY_SCALE_FONT_SIZE));
        }

        // parse background rectangle
        BgRectangle bgRect = BgRectangle.create(attrs);

		// validate data
		validate(elementName, textKey, fontSize, strokeWidth);
		
		// construct instructions
		Typeface typeface = Typeface.create(fontFamily.toTypeface(), fontStyle.toInt());
		return new PathText(indexInRules, category,
                textKey, fontSize, fontSizeScale,
                dx, dy, dyScale, rotateUp, upperCase,
				generatePaintFill(Align.LEFT, typeface, fill),
				generatePaintStroke(Align.LEFT, typeface, stroke, strokeWidth), bgRect);
	}

	private static void validate(String elementName, TextKey textKey, float fontSize, float strokeWidth) {
		if (textKey == null) {
			throw new IllegalArgumentException("missing attribute k for element: " + elementName);
		} else if (fontSize <= 0) {
			throw new IllegalArgumentException("font-size must not be negative or equal zero: " + fontSize);
		} else if (strokeWidth < 0) {
			throw new IllegalArgumentException("stroke-width must not be negative: " + strokeWidth);
		}
	}

    private final TextKey mTextKey;
	private final float mFontSize;
    private final ScalableParameter mFontSizeScale;
	private final float mDx;
	private final float mDy;
    private final ScalableParameter mDyScale;
	private final boolean mRotateUp;
	private final boolean mUpperCase;
	
	// paint for text
	private final Paint paintFill;
	private final Paint paintStroke;
	
	// paint for background
	private final BgRectangle bgRect;

    // computed vertical offset for current
    private float mDyComputed;
	
	private PathText(int indexInRules, String category,
            TextKey textKey, float fontSize, ScalableParameter fontSizeScale,
            float dx, float dy, ScalableParameter dyScale, boolean rotateUp, boolean upperCase,
			Paint paintFill, Paint paintStroke, BgRectangle bgRect) {
		super(indexInRules, category);
		this.mTextKey = textKey;
		this.mFontSize = fontSize;
        this.mFontSizeScale = fontSizeScale;
		this.mDx = dx;
		this.mDy = dy;
        this.mDyScale = dyScale;
		this.mRotateUp = rotateUp;
		this.mUpperCase = upperCase;

		this.paintFill = paintFill;
		this.paintStroke = paintStroke;
		this.bgRect = bgRect;
	}

	@Override
	public void destroy() {
		// do nothing
	}

	@Override
	public void renderNode(RenderCallback renderCallback, Tag[] tags) {
		// do nothing
	}

	@Override
	public void renderWay(RenderCallback renderCallback, Tag[] tags) {
		String caption = mTextKey.getValue(tags, mUpperCase);
		if (caption == null) {
			return;
		}

		// render texts
		renderCallback.renderWayText(caption, this.mDx, this.mDyComputed, this.mRotateUp,
				this.paintFill, this.paintStroke, this.bgRect);
	}
	
	@Override
	public void prepare(RenderTheme theme, float scaleStroke, float scaleText, byte zoomLevel) {
        // compute vertical offset
        if (mDyScale != null) {
            mDyComputed = mDyScale.computeValue(mDy, zoomLevel);
        } else {
            mDyComputed = mDy;
        }

		// compute base size
		float size = this.mFontSize * scaleText;

		// compute scale size
        if (mFontSizeScale != null) {
            size = mFontSizeScale.computeValue(size, zoomLevel);
        }

		// set paint
		this.paintFill.setTextSize(size);
		this.paintStroke.setTextSize(size);
	}
}