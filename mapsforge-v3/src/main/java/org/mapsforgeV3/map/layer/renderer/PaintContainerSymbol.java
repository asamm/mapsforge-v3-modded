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

import android.graphics.Bitmap;

class PaintContainerSymbol implements Comparable<PaintContainerSymbol> {

    final boolean alignCenter;
    final float rotation;
    final Bitmap symbol;
    final float x;
    final float y;
    final float scale;
    final int priority;
    final boolean forceDraw;

    /**
     * Creates a new symbol container. The symbol will not be centered.
     *
     * @param symbol the symbol to render at the point
     * @param x      the x coordinate of the point.
     * @param y      the y coordinate of the point.
     */
    PaintContainerSymbol(Bitmap symbol, float x, float y,
            float scale, int priority, boolean forceDraw) {
        this(symbol, x, y, false, 0, scale, priority, forceDraw);
    }

    /**
     * Creates a new symbol container.
     *
     * @param symbol      the symbol to render at the point
     * @param x           the x coordinate of the point.
     * @param y           the y coordinate of the point.
     * @param alignCenter true if the symbol should be centered, false otherwise.
     * @param rotation    the rotation of the symbol.
     */
    PaintContainerSymbol(Bitmap symbol, float x, float y, boolean alignCenter,
            float rotation, float scale, int priority, boolean forceDraw) {
        this.symbol = symbol;
        this.x = x;
        this.y = y;
        this.alignCenter = alignCenter;
        this.rotation = rotation;
        this.scale = scale;
        this.priority = priority;
        this.forceDraw = forceDraw;
    }

    @Override
    public int compareTo(PaintContainerSymbol another) {
        if (priority < another.priority) {
            return -1;
        } else if (priority > another.priority) {
            return 1;
        } else {
            return 0;
        }
    }
}
