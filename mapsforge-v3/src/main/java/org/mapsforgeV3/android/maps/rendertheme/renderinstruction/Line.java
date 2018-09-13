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
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Shader;

import org.mapsforgeV3.android.maps.rendertheme.RenderCallback;
import org.mapsforgeV3.android.maps.rendertheme.RenderTheme;
import org.mapsforgeV3.android.maps.rendertheme.tools.BitmapUtils;
import org.mapsforgeV3.android.maps.rendertheme.tools.CurveStyle;
import org.mapsforgeV3.android.maps.rendertheme.tools.ImageSymbol;
import org.mapsforgeV3.android.maps.rendertheme.tools.ScalableParameter;
import org.mapsforgeV3.core.model.Tag;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Represents a polyline on the map.
 */
public final class Line extends RenderInstruction {
	
	private static final Pattern SPLIT_PATTERN = Pattern.compile(",");

	/**
	 * @param elementName
	 *            the name of the XML element.
	 * @param level
	 *            the drawing level of this instruction.
	 * @param relativePathPrefix
	 *            the prefix for relative resource paths.
	 * @return a new Line with the given rendering attributes.
	 * @throws IOException
	 *             if an I/O error occurs while reading a resource.
	 */
	public static Line create(int indexInRules, String elementName, HashMap<String, String> attrs,
            int level, String relativePathPrefix) throws IOException {
        String category = null;
		int stroke = Color.BLACK;
		float strokeWidth;
		float[] strokeDasharray = null;
		Cap strokeLinecap = Cap.ROUND;
		int borderColor = 0;
		float borderWidth = 0.0f;
		float dy = 0.0f;
        ScalableParameter dyScale = null;
		CurveStyle curveStyle = CurveStyle.NO_CURVE;

        // get data from attributes
        if (attrs.containsKey(KEY_CAT)) {
            category = attrs.remove(KEY_CAT);
        }
        if (attrs.containsKey(KEY_STROKE)) {
            stroke = Color.parseColor(attrs.remove(KEY_STROKE));
        }
        strokeWidth = parseLengthUnits(attrs.remove(KEY_STROKE_WIDTH));
        if (attrs.containsKey(KEY_STROKE_DASHARRAY)) {
            strokeDasharray = parseFloatArray(attrs.remove(KEY_STROKE_DASHARRAY));
        }
        if (attrs.containsKey(KEY_STROKE_LINECAP)) {
            strokeLinecap = Cap.valueOf(attrs.remove(KEY_STROKE_LINECAP).toUpperCase(Locale.ENGLISH));
        }
        if (attrs.containsKey(KEY_BORDER_COLOR)) {
            borderColor = Color.parseColor(attrs.remove(KEY_BORDER_COLOR));
        }
        if (attrs.containsKey(KEY_BORDER_WIDTH)) {
            borderWidth = parseLengthUnits(attrs.remove(KEY_BORDER_WIDTH));
        }
        if (attrs.containsKey(KEY_DY)) {
            dy = parseLengthUnits(attrs.remove(KEY_DY));
        }
        if (attrs.containsKey(KEY_SCALE_DY_SIZE)) {
            dyScale = ScalableParameter.create(attrs.remove(KEY_SCALE_DY_SIZE));
        }
        if (attrs.containsKey(KEY_CURVE)) {
            if (attrs.remove(KEY_CURVE).equals("cubic")) {
                curveStyle = CurveStyle.CUBIC;
            }
        }

        // read image
        ImageSymbol is = ImageSymbol.create(attrs, relativePathPrefix, false);

        // validate object
		validate(strokeWidth);

        // create instruction
		return new Line(indexInRules, category,
                stroke, strokeWidth, strokeDasharray,
				strokeLinecap, level, borderColor, borderWidth, dy, dyScale, curveStyle, is);
	}

	private static void validate(float strokeWidth) {
		if (strokeWidth < 0) {
			throw new IllegalArgumentException("stroke-width must not be negative: " + strokeWidth);
		}
	}

	static float[] parseFloatArray(String dashString) {
		String[] dashEntries = SPLIT_PATTERN.split(dashString);
		float[] dashIntervals = new float[dashEntries.length];
		for (int i = 0; i < dashEntries.length; ++i) {
			dashIntervals[i] = Float.parseFloat(dashEntries[i]);
		}
		return dashIntervals;
	}

	private final int level;
	private final Paint mPaint;
	private final Paint mPaintBorder;
	
	private final float strokeWidth;
	private final float[] strokeDasharray;
	private final float borderWidth;

	// vertical offset of line from source
    private final float mDy;
    private final ScalableParameter mDyScale;

	// style of curvature
	private final CurveStyle mCurveStyle;

    // symbol for a object
    private final ImageSymbol mImageSymbol;

    // computed vertical offset for current
    private float mDyComputed;
    // flag if shader was set in this cycle
    private boolean mShaderSet;

	private Line(int indexInRules, String category,
                 int stroke, float strokeWidth, float[] strokeDasharray,
                 Cap strokeLinecap, int level, int borderColor, float borderWidth,
                 float dy, ScalableParameter dyScale, CurveStyle curveStyle, ImageSymbol is) throws IOException {
		super(indexInRules, category);

        this.mImageSymbol = is;
		this.mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		this.mPaint.setStyle(Style.STROKE);
		this.mPaint.setColor(stroke);
		this.mPaint.setStrokeCap(strokeLinecap);

		this.strokeWidth = strokeWidth;
		this.strokeDasharray = strokeDasharray;
		this.level = level;
		this.borderWidth = borderWidth;
		
		if (borderWidth > 0) {
			mPaintBorder = new Paint(mPaint);
			mPaintBorder.setColor(borderColor);
		} else {
			mPaintBorder = null;
		}

        this.mDy = dy;
        this.mDyScale = dyScale;
		this.mCurveStyle = curveStyle;
        this.mShaderSet = false;
		
		// set path effect
		setStrokeDashArray(strokeDasharray);
	}

	@Override
	public void destroy() {
        if (mImageSymbol != null) {
            mImageSymbol.destroy();
        }
	}

	@Override
	public void renderNode(RenderCallback renderCallback, Tag[] tags) {
		// do nothing
	}

	@Override
	public void renderWay(RenderCallback renderCallback, Tag[] tags) {
        setShader();
		renderCallback.renderWay(this.mPaint, this.mPaintBorder,
				this.mDyComputed, this.mCurveStyle, this.level);
	}

	/**
	 * Set dash style to current track paint object.
	 * @param strokeDasharray array for dash style
	 */
	private void setStrokeDashArray(float[] strokeDasharray) {
		DashPathEffect dpf = null;
		if (strokeDasharray != null) {
			dpf = new DashPathEffect(strokeDasharray, 0);
		}
		
		// set effect
		this.mPaint.setPathEffect(dpf);
		if (this.mPaintBorder != null) {
			this.mPaintBorder.setPathEffect(dpf);
		}
	}

	@Override
	public void prepare(RenderTheme theme, float scaleStroke, float scaleText, byte zoomLevel) {
		// scale stroke dash for Locus
        if (theme.isLocusExtended() && strokeDasharray != null &&
                strokeDasharray.length > 0) {
            float[] newStroke = new float[strokeDasharray.length];
            for (int i = 0; i < strokeDasharray.length; i++) {
                newStroke[i] = (float) (strokeDasharray[i] * Math.pow(scaleStroke, 1/1.5));
            }

            // set path effect
            setStrokeDashArray(newStroke);
        }

        // compute vertical offset
        if (mDyScale != null) {
            mDyComputed = mDyScale.computeValue(mDy, zoomLevel);
        } else if (theme.isScaleLineDyByZoom()) {
            mDyComputed = mDy * scaleStroke;
        } else {
            mDyComputed = mDy;
		}

        // set stroke for width
        if (this.mPaintBorder != null) {
            this.mPaintBorder.setStrokeWidth(this.borderWidth * scaleStroke);
        }
        this.mPaint.setStrokeWidth(this.strokeWidth * scaleStroke);

        // scale icon
        if (mImageSymbol != null) {
            mImageSymbol.scaleSize(zoomLevel);
        }
        mShaderSet = false;
	}

    /**
     * Set shader for a fill paint object.
     */
    private void setShader() {
        // check state
        if (mShaderSet) {
            return;
        }

        // generate and set shader
        if (mImageSymbol != null) {
            Shader shader = BitmapUtils.createBitmapShader(
                    mImageSymbol.getBitmap());
            this.mPaint.setShader(shader);
        }
        mShaderSet = true;
    }
}
