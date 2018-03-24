/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
 * Copyright 2014 Ludwig M Brinckmann
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
package org.mapsforgeV3.core.mapelements;

import android.graphics.Paint;

import org.mapsforgeV3.core.graphics.Display;
import org.mapsforgeV3.core.graphics.DrawTools;
import org.mapsforgeV3.core.graphics.Position;
import org.mapsforgeV3.core.model.Point;

public abstract class PointTextContainer extends MapElementContainer {

	public final boolean isVisible;
	public final int maxTextWidth;
	public final Paint paintBack;
	public final Paint paintFront;
	public final Position position;
	public final SymbolContainer symbolContainer;
	public final String text;
	public final int textHeight;
	public final int textWidth;

	/**
	 * Create a new point container, that holds the x-y coordinates of a point, a text variable, two paint objects, and
	 * a reference on a symbolContainer, if the text is connected with a POI.
	 */
	protected PointTextContainer(Point point, Display display, int priority, String text, Paint paintFront, Paint paintBack,
	                             SymbolContainer symbolContainer, Position position, int maxTextWidth) {
		super(point, display, priority);

		this.maxTextWidth = maxTextWidth;
		this.text = text;
		this.symbolContainer = symbolContainer;
		this.paintFront = paintFront;
		this.paintBack = paintBack;
		this.position = position;
		if (paintBack != null) {
			this.textWidth = (int) paintBack.measureText(text);
			this.textHeight = DrawTools.getTextHeight(paintBack, text);
		} else {
			this.textWidth = (int) paintFront.measureText(text);
			this.textHeight = DrawTools.getTextHeight(paintFront, text);
		}
		this.isVisible = !DrawTools.isTransparent(this.paintFront) ||
                (this.paintBack != null && !DrawTools.isTransparent(this.paintBack));
	}

	@Override
	public boolean clashesWith(MapElementContainer other) {
		if (super.clashesWith(other)) {
			return true;
		}
		if (!(other instanceof PointTextContainer)) {
			return false;
		}
		PointTextContainer ptc = (PointTextContainer) other;
        return this.text.equals(ptc.text) && this.xy.distance(ptc.xy) < 200;
    }

	@Override
	public boolean equals(Object obj) {
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof PointTextContainer)) {
			return false;
		}
		PointTextContainer other = (PointTextContainer) obj;
        return this.text.equals(other.text);
    }

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + text.hashCode();
		return result;
	}



	@Override
	public String toString() {
        return super.toString() + ", text=" + this.text;
	}
}
