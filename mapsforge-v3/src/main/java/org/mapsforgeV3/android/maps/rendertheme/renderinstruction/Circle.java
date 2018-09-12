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
import android.graphics.Paint.Style;

import org.mapsforgeV3.android.maps.rendertheme.RenderCallback;
import org.mapsforgeV3.android.maps.rendertheme.RenderTheme;
import org.mapsforgeV3.core.model.Tag;

import java.util.HashMap;
import java.util.List;

/**
 * Represents a round area on the map.
 */
public final class Circle extends RenderInstruction {
	/**
	 * @param level
	 *            the drawing level of this instruction.
	 * @return a new Circle with the given rendering attributes.
	 */
	public static Circle create(int indexInRules, HashMap<String, String> attrs, int level) {
        // BASIC
        String category = null;
		float radius = 0.0f;
		boolean scaleRadius = false;
		int fill = Color.TRANSPARENT;
		int stroke = Color.TRANSPARENT;
		float strokeWidth = 0;

        // BASIC
        if (attrs.containsKey(KEY_CAT)) {
            category = attrs.remove(KEY_CAT);
        }
        if (attrs.containsKey(KEY_R)) {
            radius = parseLengthUnits(attrs.remove(KEY_R));
        }
        if (attrs.containsKey(KEY_SCALE_RADIUS)) {
            scaleRadius = Boolean.parseBoolean(attrs.remove(KEY_SCALE_RADIUS));
        }
        if (attrs.containsKey(KEY_FILL)) {
            fill = Color.parseColor(attrs.remove(KEY_FILL));
        }
        if (attrs.containsKey(KEY_STROKE)) {
            stroke = Color.parseColor(attrs.remove(KEY_STROKE));
        }
        if (attrs.containsKey(KEY_STROKE_WIDTH)) {
            strokeWidth = parseLengthUnits(attrs.remove(KEY_STROKE_WIDTH));
        }

        // validate
		validate(radius, strokeWidth);

        // return result
		return new Circle(indexInRules, category,
                radius, scaleRadius, fill, stroke, strokeWidth, level);
	}

	private static void validate(float radius, float strokeWidth) {
		if (radius < 0.0f) {
			throw new IllegalArgumentException("radius missing or invalid, must not be negative: " + radius);
		} else if (strokeWidth < 0.0f) {
			throw new IllegalArgumentException("stroke-width must not be negative: " + strokeWidth);
		}
	}

	private final Paint fill;
	private final int level;
	private final Paint outline;
	private final float radius;
	private float renderRadius;
	private final boolean scaleRadius;
	private final float strokeWidth;

	private Circle(int indexInRules, String category,
            float radius, boolean scaleRadius, int fill, int stroke, float strokeWidth, int level) {
		super(indexInRules, category);

		this.radius = radius;
		this.scaleRadius = scaleRadius;

		if (fill == Color.TRANSPARENT) {
			this.fill = null;
		} else {
			this.fill = new Paint(Paint.ANTI_ALIAS_FLAG);
			this.fill.setStyle(Style.FILL);
			this.fill.setColor(fill);
		}

		if (stroke == Color.TRANSPARENT) {
			this.outline = null;
		} else {
			this.outline = new Paint(Paint.ANTI_ALIAS_FLAG);
			this.outline.setStyle(Style.STROKE);
			this.outline.setColor(stroke);
		}

		this.strokeWidth = strokeWidth;
		this.level = level;

		if (!this.scaleRadius) {
			this.renderRadius = this.radius;
			if (this.outline != null) {
				this.outline.setStrokeWidth(this.strokeWidth);
			}
		}
	}

	@Override
	public void destroy() {
		// do nothing
	}

	@Override
	public void renderNode(RenderCallback renderCallback, Tag[] tags) {
		if (this.outline != null) {
			renderCallback.renderPointOfInterestCircle(this.renderRadius, this.outline, this.level);
		}
		if (this.fill != null) {
			renderCallback.renderPointOfInterestCircle(this.renderRadius, this.fill, this.level);
		}
	}

	@Override
	public void renderWay(RenderCallback renderCallback, Tag[] tags) {
		// do nothing
	}

	@Override
	public void prepare(RenderTheme theme, float scaleStroke, float scaleText, byte zoomLevel) {
		if (this.scaleRadius) {
			this.renderRadius = this.radius * scaleStroke;
			if (this.outline != null) {
				this.outline.setStrokeWidth(this.strokeWidth * scaleStroke);
			}
		}
	}
}
