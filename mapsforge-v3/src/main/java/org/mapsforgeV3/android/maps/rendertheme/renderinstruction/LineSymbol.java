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

import com.asamm.locus.mapsforge.utils.Utils;

import org.mapsforgeV3.android.maps.rendertheme.RenderTheme;
import org.mapsforgeV3.map.layer.renderer.WayDecorator;
import org.mapsforgeV3.android.maps.rendertheme.RenderCallback;
import org.mapsforgeV3.android.maps.rendertheme.tools.ImageSymbol;
import org.mapsforgeV3.core.model.Tag;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Represents an icon along a polyline on the map.
 */
public final class LineSymbol extends RenderInstruction {
	/**
	 * @param elementName
	 *            the name of the XML element.
	 * @param attrs
	 *            the attributes of the XML element.
	 * @param relativePathPrefix
	 *            the prefix for relative resource paths.
	 * @return a new LineSymbol with the given rendering attributes.
	 * @throws IOException
	 *             if an I/O error occurs while reading a resource.
	 */
	public static LineSymbol create(int indexInRules, String elementName, HashMap<String, String> attrs,
            String relativePathPrefix) throws IOException {
        String category = null;
		boolean alignCenter = false;
		float horOffset = WayDecorator.SEGMENT_SAFETY_DISTANCE;
		float verOffset = 0.0f;
		boolean repeat = false;
		float repeatGap = WayDecorator.DISTANCE_BETWEEN_SYMBOLS;

        if (attrs.containsKey(KEY_CAT)) {
            category = attrs.remove(KEY_CAT);
        }
        if (attrs.containsKey(KEY_ALIGN_CENTER)) {
            alignCenter = Boolean.parseBoolean(attrs.remove(KEY_ALIGN_CENTER));
        }
        if (attrs.containsKey(KEY_DX)) {
            horOffset = parseLengthUnits(attrs.remove(KEY_DX));
        }
        if (attrs.containsKey(KEY_DY)) {
            verOffset = parseLengthUnits(attrs.remove(KEY_DY));
        }
        if (attrs.containsKey(KEY_REPEAT)) {
            repeat = Boolean.parseBoolean(attrs.remove(KEY_REPEAT));
        }
        if (attrs.containsKey(KEY_REPEAT_GAP)) {
            repeatGap = parseLengthUnits(attrs.remove(KEY_REPEAT_GAP));
//			Utils.getHandler().logD("LineSymbol",
//					"index: " + indexInRules + ", gap: " + repeatGap + ", def: " + WayDecorator.DISTANCE_BETWEEN_SYMBOLS);
        }

        // read image
        ImageSymbol is = ImageSymbol.create(attrs, relativePathPrefix, true);

        // validate result
        validate(is);

        // create instruction
		return new LineSymbol(indexInRules, category,
                alignCenter, repeat, repeatGap,
                horOffset, verOffset, is);
	}

    /**
     * Validate all required parameters.
     * @param is image source
     */
    private static void validate(ImageSymbol is) {
        // check image
        if (is == null) {
            throw new IllegalArgumentException("Symbol has probably not defined 'src' parameter");
        }
    }

	private final boolean alignCenter;
	private final boolean repeat;
	private final float repeatGap;
	private final float horOffset;
	private final float verOffset;

    // symbol for a object
    private final ImageSymbol mImageSymbol;
	
	private LineSymbol(int indexInRules, String category,
            boolean alignCenter, boolean repeat, float repeatGap,
            float horOffset, float verOffset, ImageSymbol is) {
		super(indexInRules, category);

		// generate container for symbol
        this.mImageSymbol = is;
		this.alignCenter = alignCenter;
		this.repeat = repeat;
		this.repeatGap = repeatGap;
		this.horOffset = horOffset;
		this.verOffset = verOffset;
	}

	@Override
	public void destroy() {
        mImageSymbol.destroy();
	}

	@Override
	public void renderNode(RenderCallback renderCallback, Tag[] tags) {
		// do nothing
	}

	@Override
	public void renderWay(RenderCallback renderCallback, Tag[] tags) {
		renderCallback.renderWaySymbol(mImageSymbol.getBitmap(), this.alignCenter,
				this.repeat, mImageSymbol.getScale(), repeatGap, horOffset, verOffset);
	}

	@Override
	public void prepare(RenderTheme theme, float scaleStroke, float scaleText, byte zoomLevel) {
        mImageSymbol.scaleSize(zoomLevel);
	}
}
