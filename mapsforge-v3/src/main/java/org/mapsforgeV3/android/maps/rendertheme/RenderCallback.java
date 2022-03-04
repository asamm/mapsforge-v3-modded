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
package org.mapsforgeV3.android.maps.rendertheme;

import android.graphics.Bitmap;
import android.graphics.Paint;

import org.mapsforgeV3.android.maps.rendertheme.tools.BgRectangle;
import org.mapsforgeV3.android.maps.rendertheme.tools.CurveStyle;

/**
 * Callback methods for rendering areas, ways and points of interest (POIs).
 */
public interface RenderCallback {

    /**
     * Flag if rendering of this tile is completed.
     */
    boolean isRenderingCompleted();

    /**
     * Flag if rendering of this tile is completed, but only background coverage is visible.
     */
    boolean isRenderingCompletedOnlyBg();

    /**
     * Rendered bitmap image.
     */
    Bitmap getRenderedBitmap();

    /**
     * Renders an area with the given parameters.
     *
     * @param paint the paint to be used for rendering the area.
     * @param level the drawing level on which the area should be rendered.
     */
    void renderArea(Paint paint, int level);

    /**
     * Renders an area caption with the given text.
     *
     * @param caption   the text to be rendered.
     * @param horOffset the horizontal offset of the caption.
     * @param verOffset the vertical offset of the caption.
     * @param paint     the paint to be used for rendering the text.
     * @param stroke    an optional paint for the text casing (may be null).
     */
    void renderAreaCaption(String caption,
            float horOffset, float verOffset,
            Paint paintFill, Paint paintStroke,
            BgRectangle bgRect, int priority, boolean forceDraw);

    /**
     * Renders an area symbol with the given bitmap.
     *
     * @param symbol the symbol to be rendered.
     */
    void renderAreaSymbol(Bitmap symbol, float scale, int priority, boolean forceDraw, boolean renderOnlyDb);

    /**
     * Renders a point of interest caption with the given text.
     *
     * @param caption     the text to be rendered.
     * @param horOffset   the horizontal offset of the caption.
     * @param verOffset   the vertical offset of the caption.
     * @param paintFill   the paint to be used for rendering the text.
     * @param paintStroke an optional paint for the text casing (may be null).
     */
    void renderPointOfInterestCaption(String caption,
            float horOffset, float verOffset,
            Paint paintFill, Paint paintStroke,
            BgRectangle bgRect, int priority, boolean forceDraw);

    /**
     * Renders a point of interest circle with the given parameters.
     *
     * @param radius the radius of the circle.
     * @param fill   the paint to be used for rendering the circle.
     * @param level  the drawing level on which the circle should be rendered.
     */
    void renderPointOfInterestCircle(float radius, Paint fill, int level);

    /**
     * Renders a point of interest symbol with the given bitmap.
     *
     * @param symbol the symbol to be rendered.
     */
    void renderPointOfInterestSymbol(Bitmap symbol, float scale, int priority, boolean forceDraw, boolean renderOnlyDb,
            int dbOutlineWidth, int dbOutlineColor);

    /**
     * Renders a way with the given parameters.
     *
     * @param paintLine   the paint to be used for rendering the way
     * @param paintBorder that paint to be used for rendering of border
     * @param vOffset     vertical offset that should be applied on line
     * @param curveStyle  style how track shape should be drawn
     * @param level       the drawing level on which the way should be rendered
     */
    void renderWay(Paint paintLine, Paint paintBorder,
            float vOffset, CurveStyle curveStyle, int level);

    /**
     * Renders a way with the given symbol along the way path.
     *
     * @param symbol      the symbol to be rendered.
     * @param alignCenter true if the symbol should be centered, false otherwise.
     * @param repeat      true if the symbol should be repeated, false otherwise.
     */
    void renderWaySymbol(Bitmap symbol, boolean alignCenter, boolean repeat,
            float scale, float repeatGap, float horOffset, float verOffset);

    /**
     * Renders a way with the given text along the way path.
     *
     * @param text   the text to be rendered.
     * @param paint  the paint to be used for rendering the text.
     * @param stroke an optional paint for the text casing (may be null).
     */
    void renderWayText(String text, float horOffset, float verOffset, boolean rotateUp,
            Paint paintFill, Paint paintStroke, BgRectangle bgRect);
}
