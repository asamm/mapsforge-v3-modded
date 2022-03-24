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
            scale = Utils.parseFloat(attrs.remove(KEY_SCALE));
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
    private final float defWidth;
    // default height of icon
    private final float defHeight;
	// color used for SVG images
	private final int defColor;

    // scale value
	private final float scale;
    // level where scaling may start
	private final ScalableParameter scaleIcon;

    // already rendered bitmap
	private Bitmap bitmap;
    // current set scale value for current bitmap
    private float scaleCurrent;

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

        this.defWidth = defaultWidth;
        this.defHeight = defaultHeight;
		this.defColor = color;

        this.scale = scale;
        this.scaleIcon = scaleIcon;
    }

    /**
     * Get current rendered bitmap image.
     * @return current image
     */
	public Bitmap getBitmap() {
        // create image based on source
        synchronized (LOCK) {
            // return existing bitmap if it's already loaded
            if (bitmap != null) {
                return bitmap;
            }

            // finally create an image
            try {
                bitmap = BitmapUtils.createBitmap(relativePathPrefix, src,
                        scaleCurrent <= 0 ? scale : scaleCurrent,
                        defWidth, defHeight, defColor);
            } catch (Exception e) {
                Utils.getHandler().logE(TAG, "getBitmap(), problem with: " + src, e);
                bitmap = null;
            }

			// notify in case of any problem with bitmap loading
			if (bitmap == null) {
				Utils.getHandler().logW(TAG, "getBitmap(), problem with load, " +
						"relPath:" + relativePathPrefix + ", src:" + src);
			}
			return bitmap;
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
        float newScale = scale;
        if (scaleIcon != null) {
            newScale = scaleIcon.computeValue(scale, zoomLevel);
        }

        // store defined parameters
        if (newScale != scaleCurrent || bitmap == null) {
            // destroy previous image
            destroy();

            // set new parameters, image will be loaded in 'getBitmap()' later
            scaleCurrent = newScale;
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
            bitmap = null;
        }
	}

    @Override
    public String toString() {
        return "ImageSymbol[" +
                "relativePathPrefix:" + relativePathPrefix + ", " +
                "src:" + src + ", " +
                "mDefWidth:" + defWidth + ", " +
                "mDefHeight:" + defHeight + ", " +
                "mScale:" + scale + ", " +
                "mScaleIcon:" + scaleIcon + "]";
    }
}
