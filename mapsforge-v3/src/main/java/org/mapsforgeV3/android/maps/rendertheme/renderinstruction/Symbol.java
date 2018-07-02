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
import com.asamm.locus.mapsforge.utils.Utils;

import org.mapsforgeV3.android.maps.rendertheme.RenderCallback;
import org.mapsforgeV3.android.maps.rendertheme.RenderTheme;
import org.mapsforgeV3.android.maps.rendertheme.tools.ImageSymbol;
import org.mapsforgeV3.core.model.Tag;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Represents an icon on the map.
 */
public final class Symbol extends RenderInstruction {

    // tag for logger
    private static final String TAG = "Symbol";

    /**
     * @param elementName        the name of the XML element.
     * @param attrs              the attributes of the XML element.
     * @param relativePathPrefix the prefix for relative resource paths.
     * @return a new Symbol with the given rendering attributes.
     * @throws IOException if an I/O error occurs while reading a resource.
     */
    public static Symbol create(int indexInRules, String elementName, HashMap<String, String> attrs,
            String relativePathPrefix) throws IOException {
        String category = null;
        int dbOutlineColor = Color.WHITE;
        float dbOutlineWidth = Utils.getHandler().getDpPixels(1.0f, true);
        boolean forceDraw = false;
        boolean renderDbOnly = false;

        // get data from attributes
        if (attrs.containsKey(KEY_CAT)) {
            category = attrs.remove(KEY_CAT);
        }
        if (attrs.containsKey(KEY_DB_OUTLINE_COLOR)) {
            dbOutlineColor = Color.parseColor(attrs.remove(KEY_DB_OUTLINE_COLOR));
        }
        if (attrs.containsKey(KEY_DB_OUTLINE_WIDTH)) {
            dbOutlineWidth = RenderInstruction
                    .parseLengthUnits(attrs.remove(KEY_DB_OUTLINE_WIDTH), true, true);
        }
        if (attrs.containsKey(KEY_FORCE_DRAW)) {
            forceDraw = Utils.getHandler().parseBoolean(
                    attrs.remove(KEY_FORCE_DRAW));
        }
        if (attrs.containsKey(KEY_RENDER_DB_ONLY)) {
            renderDbOnly = Utils.getHandler().parseBoolean(
                    attrs.remove(KEY_RENDER_DB_ONLY));
        }

        // read image
        ImageSymbol is = ImageSymbol.create(attrs, relativePathPrefix, false);

        // validate result
        validate(is);

        // create instruction
        return new Symbol(indexInRules, category, forceDraw, is, renderDbOnly,
                Math.max(1, (int) dbOutlineWidth), dbOutlineColor);
    }

    /**
     * Validate all required parameters.
     *
     * @param is image source
     */
    private static void validate(ImageSymbol is) {
        // check image
        if (is == null) {
            throw new IllegalArgumentException("Symbol has probably not defined 'src' parameter");
        }
    }

    private final boolean mForceDraw;
    // symbol for a object
    private final ImageSymbol mImageSymbol;
    // flag if we wants to draw symbol only over POI DB
    private boolean mRenderDbOnly;
    // width of outline of active points
    private int dbOutlineWidth;
    // color of outline of active points
    private int dbOutlineColor;

    private Symbol(int indexInRules, String category,
            boolean forceDraw, ImageSymbol is, boolean renderDbOnly,
            int dbOutlineWidth, int dbOutlineColor) {
        super(indexInRules, category);

        // generate container for symbol
        this.mImageSymbol = is;
        this.mForceDraw = forceDraw;
        this.mRenderDbOnly = renderDbOnly;
        this.dbOutlineWidth = dbOutlineWidth;
        this.dbOutlineColor = dbOutlineColor;
    }

    public ImageSymbol getImageSymbol() {
        return mImageSymbol;
    }

    // INSTRUCTION EXTENSION

    @Override
    public void destroy() {
        mImageSymbol.destroy();
    }

    @Override
    public void renderNode(RenderCallback renderCallback, List<Tag> tags) {
        renderCallback.renderPointOfInterestSymbol(
                mImageSymbol.getBitmap(), mImageSymbol.getScale(),
                this.indexInRules, this.mForceDraw, this.mRenderDbOnly,
                this.dbOutlineWidth, this.dbOutlineColor);
    }

    @Override
    public void renderWay(RenderCallback renderCallback, List<Tag> tags) {
        renderCallback.renderAreaSymbol(
                mImageSymbol.getBitmap(), mImageSymbol.getScale(),
                this.indexInRules, this.mForceDraw, this.mRenderDbOnly);
    }

    @Override
    public void prepare(RenderTheme theme, float scaleStroke, float scaleText, byte zoomLevel) {
        // scale icon
        mImageSymbol.scaleSize(zoomLevel);
    }
}
