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
package org.mapsforgeV3.map.layer.renderer;

import org.mapsforgeV3.android.maps.rendertheme.tools.BgRectangle;

import android.graphics.Paint;
import android.graphics.Rect;

public class PaintContainerPointText implements Comparable<PaintContainerPointText> {

    // text to draw
    final String text;
    // x coordinates
    public float x;
    // y coordinates
    public float y;
    // symbol for draw
    PaintContainerSymbol symbol;

    // paint object for fill
    public final Paint paintFill;
    // paint object for stroke
    final Paint paintStroke;
    // priority for draw
    final int priority;
    // stuff for background
    final BgRectangle bgRect;

    // boundary for text
    public final Rect boundary;
    // force draw rendering
    final boolean forceDraw;

    /**
     * Create a new point container, that holds the x-y coordinates of a point, a text variable and two paint objects.
     *
     * @param text       the text of the point.
     * @param x          the x coordinate of the point.
     * @param y          the y coordinate of the point.
     * @param paintFront the paintFront for the point.
     * @param paintBack  the paintBack for the point.
     */
    PaintContainerPointText(String text, float x, float y,
            Paint paintFill, Paint paintStroke,
            BgRectangle bgRect, int priority, boolean forceDraw) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.symbol = null;

        this.paintFill = paintFill;
        this.paintStroke = paintStroke;
        this.priority = priority;
        this.bgRect = bgRect;

        this.boundary = generateTextBoundary(text, paintFill, paintStroke);
        this.forceDraw = forceDraw;
    }

    /**
     * Create a new point container, that holds the x-y coordinates of a point, a text variable, two paint objects, and
     * a reference on a symbol, if the text is connected with a POI.
     *
     * @param text       the text of the point.
     * @param x          the x coordinate of the point.
     * @param y          the y coordinate of the point.
     * @param paintFront the paintFront for the point.
     * @param paintBack  the paintBack for the point.
     * @param symbol     the connected Symbol.
     */
    PaintContainerPointText(String text, float x, float y, PaintContainerSymbol symbol,
            Paint paintFill, Paint paintStroke, BgRectangle bgRect, int priority, boolean forceDraw) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.symbol = symbol;

        this.paintFill = paintFill;
        this.paintStroke = paintStroke;
        this.priority = priority;
        this.bgRect = bgRect;

        this.boundary = generateTextBoundary(text, paintFill, paintStroke);
        this.forceDraw = forceDraw;
    }

    private Rect generateTextBoundary(String text, Paint paintFill, Paint paintStroke) {
        Rect boundary = new Rect();
        if (paintStroke != null) {
            paintStroke.getTextBounds(text, 0, text.length(), boundary);
        } else {
            paintFill.getTextBounds(text, 0, text.length(), boundary);
        }
        return boundary;
    }

    @Override
    public int compareTo(PaintContainerPointText another) {
        // sort from item with lowest priority to highest
        if (priority < another.priority) {
            return -1;
        } else if (priority > another.priority) {
            return 1;
        } else {
            return 0;
        }
    }
}
