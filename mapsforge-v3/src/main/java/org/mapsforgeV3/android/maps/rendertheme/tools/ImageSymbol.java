package org.mapsforgeV3.android.maps.rendertheme.tools;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.asamm.locus.mapsforge.utils.Utils;

import org.mapsforgeV3.android.maps.rendertheme.renderinstruction.RenderInstruction;

import java.util.HashMap;

import static org.mapsforgeV3.android.maps.rendertheme.renderinstruction.RenderInstruction.KEY_SCALE;
import static org.mapsforgeV3.android.maps.rendertheme.renderinstruction.RenderInstruction.KEY_SCALE_ICON_SIZE;
import static org.mapsforgeV3.android.maps.rendertheme.renderinstruction.RenderInstruction.KEY_SRC;
import static org.mapsforgeV3.android.maps.rendertheme.renderinstruction.RenderInstruction.KEY_SYMBOL_COLOR;
import static org.mapsforgeV3.android.maps.rendertheme.renderinstruction.RenderInstruction.KEY_SYMBOL_HEIGHT;
import static org.mapsforgeV3.android.maps.rendertheme.renderinstruction.RenderInstruction.KEY_SYMBOL_WIDTH;

public class ImageSymbol {

    private static final String TAG = ImageSymbol.class.getSimpleName();

    /**
     * Create image object from list of attributes.
     * @param attrs attributes
     * @param relativePathPrefix path prefix
     * @return image object
     */
    public static ImageSymbol create(HashMap<String, String> attrs, String relativePathPrefix, boolean roundDensity) {
        // BASIC
        String src;

        // ICON SIZE
        float defaultWidth = -1.0f;
        float defaultHeight = -1.0f;
		int defaultColor = 0;

        // ICON SCALE
        float scale = 1.0f;
        ScalableParameter scaleIcon = null;

        // BASIC
        src = attrs.remove(KEY_SRC);

        // ICON SIZE
        if (attrs.containsKey(KEY_SYMBOL_WIDTH)) {
            defaultWidth = RenderInstruction.
                    parseLengthUnits(attrs.remove(KEY_SYMBOL_WIDTH), true, roundDensity);
        }
        if (attrs.containsKey(KEY_SYMBOL_HEIGHT)) {
            defaultHeight = RenderInstruction.
                    parseLengthUnits(attrs.remove(KEY_SYMBOL_HEIGHT), true, roundDensity);
        }

		// SYMBOL COLOR
		if (attrs.containsKey(KEY_SYMBOL_COLOR)) {
			defaultColor = Color.parseColor(attrs.remove(KEY_SYMBOL_COLOR));
		}

        // ICON SCALE
        if (attrs.containsKey(KEY_SCALE)) {
            scale = Utils.getHandler().parseFloat(
                    attrs.remove(KEY_SCALE));
        }
        if (attrs.containsKey(KEY_SCALE_ICON_SIZE)) {
            scaleIcon = ScalableParameter.create(attrs.remove(KEY_SCALE_ICON_SIZE));
        }

        // validate source
        if (src == null || src.length() == 0) {
            return null;
        }

        // finally return generated icon
        return new ImageSymbol(relativePathPrefix, src,
                defaultWidth, defaultHeight, defaultColor, scale, scaleIcon);
    }

    private final Object LOCK = new Object();

    // prefix for relative icon path
	private final String relativePathPrefix;
    // src value for icon
	private final String src;

    // default width of icon
    private final float mDefWidth;
    // default height of icon
    private final float mDefHeight;
	// color used for SVG images
	private final int mDefColor;

    // scale value
	private final float mScale;
    // level where scaling may start
	private final ScalableParameter mScaleIcon;

    // already rendered bitmap
	private Bitmap mBitmap;
    // current set scale value for current bitmap
    private float mScaleCurrent;

    /**
     * Construct icon with all base parameters.
     * @param relativePathPrefix prefix for relative path
     * @param src path to icon itself
     * @param defaultWidth default icon width
     * @param defaultHeight default icon height
     * @param scale scale value
     * @param scaleIcon scale increment
     */
    private ImageSymbol(String relativePathPrefix, String src,
			float defaultWidth, float defaultHeight, int color,
			float scale, ScalableParameter scaleIcon) {
        this.relativePathPrefix = relativePathPrefix == null ? "" : relativePathPrefix;
        this.src = src;

        this.mDefWidth = defaultWidth;
        this.mDefHeight = defaultHeight;
		this.mDefColor = color;

        this.mScale = scale;
        this.mScaleIcon = scaleIcon;
    }

    /**
     * Get current rendered bitmap image.
     * @return current image
     */
	public Bitmap getBitmap() {
        // create image based on source
        synchronized (LOCK) {
            // return existing bitmap if it's already loaded
            if (mBitmap != null) {
                return mBitmap;
            }

            // finally create an image
            try {
                mBitmap = BitmapUtils.createBitmap(relativePathPrefix, src,
                        mScaleCurrent <= 0 ? mScale : mScaleCurrent,
                        mDefWidth, mDefHeight, mDefColor);
            } catch (Exception e) {
                Utils.getHandler().logE(TAG, "getBitmap()", e);
                mBitmap = null;
            }

			// notify in case of any problem with bitmap loading
			if (mBitmap == null) {
				Utils.getHandler().logW(TAG, "getBitmap(), problem with load, " +
						"relPath:" + relativePathPrefix + ", src:" + src);
			}
			return mBitmap;
        }
	}

    /**
     * Get current defined scale value.
     * @return current scale value
     */
	public float getScale() {
		return 1.0f;
	}

    /**
     * Set new scale to current image symbol object.
     * @param zoomLevel current zoom level
     */
	public void scaleSize(byte zoomLevel) {
        // prepare scale value
        float newScale = mScale;
        if (mScaleIcon != null) {
            newScale = mScaleIcon.computeValue(mScale, zoomLevel);
        }

        // store defined parameters
        if (newScale != mScaleCurrent || mBitmap == null) {
            // destroy previous image
            destroy();

            // set new parameters, image will be loaded in 'getBitmap()' later
            mScaleCurrent = newScale;
        }
    }

    /**
     * Destroy current image. This clear existing image from memory.
     */
	public void destroy() {
        synchronized (LOCK) {
            // because we need to re-use these images during draw of Db POI,
            // do not recycle them and keep them alive
//            if (mBitmap != null) {
//                this.mBitmap.recycle();
//            }
            mBitmap = null;
        }
	}

    @Override
    public String toString() {
        return "ImageSymbol[" +
                "relativePathPrefix:" + relativePathPrefix + ", " +
                "src:" + src + ", " +
                "mDefWidth:" + mDefWidth + ", " +
                "mDefHeight:" + mDefHeight + ", " +
                "mScale:" + mScale + ", " +
                "mScaleIcon:" + mScaleIcon + "]";
    }
}
