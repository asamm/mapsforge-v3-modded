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
package org.mapsforgeV3.android.maps.rendertheme.tools;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Shader.TileMode;

import com.asamm.locus.mapsforge.utils.Utils;

import org.mapsforgeV3.map.layer.renderer.DatabaseRenderer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public final class BitmapUtils {

	// tag for logger
	private static final String TAG = "BitmapUtils";

    public static final int ICONS_MAX_LIMIT = (int) Utils.getHandler().getDpPixels(64.0f, false);
    private static final float ICON_DEFAULT_SIZE = Utils.getHandler().getDpPixels(16.0f, false);

    private static InputStream createInputStream(String relativePathPrefix, String src) throws FileNotFoundException {
		if (DatabaseRenderer.internalTheme || relativePathPrefix.startsWith("/assets/themes/mapsforge")) {
			String absoluteName = getAbsoluteName(relativePathPrefix,
					src.substring("file:/".length()));
			InputStream inputStream = BitmapUtils.class.getResourceAsStream(absoluteName);
			if (inputStream == null) {
				throw new FileNotFoundException("resource not found: " + absoluteName);
			}
			return inputStream;
		} else if (src.startsWith("file:")) {
			File file = getFile(relativePathPrefix, src.substring("file:".length()));
			if (!file.exists()) {
				throw new IllegalArgumentException("file does not exist: " + file);
			} else if (!file.isFile()) {
				throw new IllegalArgumentException("not a file: " + file);
			} else if (!file.canRead()) {
				throw new IllegalArgumentException("cannot read file: " + file);
			}
			return new FileInputStream(file);
		}

		throw new IllegalArgumentException("invalid bitmap source: " + src);
	}
	
	private static String getAbsoluteName(String relativePathPrefix, String name) {
		if (name.charAt(0) == '/') {
			return name;
		}
		return relativePathPrefix + name;
	}

	private static File getFile(String parentPath, String pathName) {
		return new File(parentPath, pathName);
	}

    /**
     * Check if source image is SVG image.
     * @param src path to image
     * @return <code>true</code> if image is SVG
     */
	public static boolean isSvg(String src) {
        // tet path
		if (src == null || src.length() == 0) {
			return false;
		}

        // test icon name
		return src.toLowerCase().indexOf(".svg") > 0;
	}

    /**
     * Create bitmap object from supplied parameters.
     * @param relativePathPrefix relative prefix for path to image
     * @param src source of image
     * @param scale current scale
     * @param requestedWidth default width for new image
     * @param requestedHeight default height for new image
	 * @param color color that should be applied on images
     * @return generated bitmap or 'null' in case of invalid parameters
     * @throws IOException
     */
	public static Bitmap createBitmap(String relativePathPrefix, String src, float scale,
            float requestedWidth, float requestedHeight, int color) throws Exception {
		if (src == null || src.length() == 0) {
			Utils.getHandler().logW(TAG, "createBitmap(" + relativePathPrefix + ", " + src + "), " +
					"no image source defined");
			return null;
		}

        // create stream and load icon with correct size
		InputStream inputStream = createInputStream(relativePathPrefix, src);
		if (inputStream == null) {
            Utils.getHandler().logW(TAG, "createBitmap(" + relativePathPrefix + ", " + src + "), " +
                    "unable to load for: " + relativePathPrefix + ", " + src);
		    return null;
        }

		// load image
		Bitmap bitmap;
		if (isSvg(src)) {
            bitmap = Utils.getHandler().
                    createSVGBitmap(inputStream, scale,
							requestedWidth, requestedHeight, color);
        } else {
            bitmap = createRasterBitmap(inputStream, scale, requestedWidth, requestedHeight);
		}

        // close stream and return bitmap
		inputStream.close();
		return bitmap;
	}

    /**
     * Generate shader for certain bitmap image.
     * @param bitmap image for shader
     * @return generated shader or 'null' for invalid bitmap
     */
    public static BitmapShader createBitmapShader(Bitmap bitmap) {
        // check bitmap
        if (bitmap == null) {
            return null;
        }

        // create shader
        return new BitmapShader(bitmap, TileMode.REPEAT, TileMode.REPEAT);
    }

    /**
     * Create SVG image from stream with defined scale.
     * @param inputStream stream with image
     * @param scale new scale of image
     * @return created SVG image
     * @throws IOException
     */
    private static Bitmap createRasterBitmap(InputStream inputStream, float scale,
            float requestedWidth, float requestedHeight) throws IOException {
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

        // check loaded image
        if (bitmap == null) {
            return null;
        }

        // check sizes
        float[] dims = computeNewSizes(bitmap.getWidth(), bitmap.getHeight(),
                requestedWidth, requestedHeight, scale);

        // finally scale icon
        if (dims[0] != bitmap.getWidth() || dims[1] != bitmap.getHeight()) {
            return Bitmap.createScaledBitmap(bitmap,
                    (int) dims[0], (int) dims[1], true);
        } else {
            return bitmap;
        }
    }

    private static float[] computeNewSizes(float currentWidth, float currentHeight,
            float requestedWidth, float requestedHeight, float scale) {
        // check sizes
        float width = currentWidth;
        float height = currentHeight;
        if (requestedWidth > 0 && requestedHeight <= 0) {
            // only width is defined
            float ratio = requestedWidth / width;
            width = requestedWidth;
            height = ratio * height;
        } else if (requestedWidth <= 0 && requestedHeight > 0) {
            // only height is defined
            float ratio = requestedHeight / width;
            width = ratio * width;
            height = requestedHeight;
        } else if (requestedWidth > 0 && requestedHeight > 0) {
            // both sizes are defined
            width = requestedWidth;
            height = requestedHeight;
        } else {
            // no size is defined
//            float dimScale = Utils.getHandler().getDpPixels(1.0f);
//            width *= dimScale;
//            height *= dimScale;
        }

        // test max value
        float max = Math.max(width, height);
        if (max * scale > ICONS_MAX_LIMIT) {
            scale = ICONS_MAX_LIMIT / max;
        }

        // compute new values
        width = width * scale;
        height = height * scale;
        if (width <= 0 || height <= 0) {
            width = ICON_DEFAULT_SIZE * scale;
            height = width;
        }

        // return generated values
        return new float[] {width, height};
    }
}
