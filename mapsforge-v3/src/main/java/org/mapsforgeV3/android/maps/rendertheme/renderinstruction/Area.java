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
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Shader;

import org.mapsforgeV3.android.maps.rendertheme.RenderCallback;
import org.mapsforgeV3.android.maps.rendertheme.RenderTheme;
import org.mapsforgeV3.android.maps.rendertheme.tools.BitmapUtils;
import org.mapsforgeV3.android.maps.rendertheme.tools.ImageSymbol;
import org.mapsforgeV3.core.model.Tag;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Represents a closed polygon on the map.
 */
public final class Area extends RenderInstruction {
	/**
	 * @param level
	 *            the drawing level of this instruction.
	 * @param relativePathPrefix
	 *            the prefix for relative resource paths.
	 * @return a new Area with the given rendering attributes.
	 * @throws IOException
	 *             if an I/O error occurs while reading a resource.
	 */
	public static Area create(int indexInRules, HashMap<String, String> attrs,
            int level, String relativePathPrefix) throws IOException {
        String category = null;
		int fill = Color.BLACK;
		int stroke = Color.TRANSPARENT;
		float strokeWidth;

        // get data from attributes
        if (attrs.containsKey(KEY_CAT)) {
            category = attrs.remove(KEY_CAT);
        }
        if (attrs.containsKey(KEY_FILL)) {
            fill = Color.parseColor(attrs.remove(KEY_FILL));
        }
        if (attrs.containsKey(KEY_STROKE)) {
            stroke = Color.parseColor(attrs.remove(KEY_STROKE));
        }
        strokeWidth = parseLengthUnits(attrs.remove(KEY_STROKE_WIDTH));

        // read image
        ImageSymbol is = ImageSymbol.create(attrs, relativePathPrefix, true);

        // validate object
		validate(strokeWidth);

        // create instruction
		return new Area(indexInRules, category,
                fill, stroke, strokeWidth, is, level);
	}

	private static void validate(float strokeWidth) {
		if (strokeWidth < 0) {
			throw new IllegalArgumentException("stroke-width must not be negative: " + strokeWidth);
		}
	}

	private final Paint fill;
	private final int level;
	private final Paint outline;
	private final float strokeWidth;
    // symbol for a object
    private final ImageSymbol mImageSymbol;
    // flag if shader was set in this cycle
    private boolean mShaderSet;

	private Area(int indexInRules, String category,
                 int fill, int stroke, float strokeWidth,
                 ImageSymbol is, int level) throws IOException {
		super(indexInRules, category);

        // set data
        this.mImageSymbol = is;
		if (fill == Color.TRANSPARENT) {
			this.fill = null;
		} else {
			this.fill = new Paint(Paint.ANTI_ALIAS_FLAG);
			this.fill.setStyle(Style.FILL);
			this.fill.setColor(fill);
			this.fill.setStrokeCap(Cap.ROUND);
		}

		if (stroke == Color.TRANSPARENT) {
			this.outline = null;
		} else {
			this.outline = new Paint(Paint.ANTI_ALIAS_FLAG);
			this.outline.setStyle(Style.STROKE);
			this.outline.setColor(stroke);
			this.outline.setStrokeCap(Cap.ROUND);
		}

		this.strokeWidth = strokeWidth;
		this.level = level;
        this.mShaderSet = false;
	}

	@Override
	public void destroy() {
        if (mImageSymbol != null) {
            mImageSymbol.destroy();
        }
	}

	@Override
	public void renderNode(RenderCallback renderCallback, List<Tag> tags) {
		// do nothing
	}

	@Override
	public void renderWay(RenderCallback renderCallback, List<Tag> tags) {
        // add outline
		if (this.outline != null) {
			renderCallback.renderArea(this.outline, this.level);
		}

        // add fill color
		if (this.fill != null) {
            setShader();
			renderCallback.renderArea(this.fill, this.level);
		}
	}

    @Override
    public void prepare(RenderTheme theme, float scaleStroke, float scaleText, byte zoomLevel) {
		// update outline
        if (this.outline != null) {
            this.outline.setStrokeWidth(this.strokeWidth * scaleStroke);
        }

        // scale icon
        mShaderSet = false;
        if (mImageSymbol != null) {
            mImageSymbol.scaleSize(zoomLevel);
        }
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
        if (fill != null && mImageSymbol != null) {
            Shader shader = BitmapUtils.createBitmapShader(
                    mImageSymbol.getBitmap());
            this.fill.setShader(shader);
        }
        mShaderSet = true;
    }
}
