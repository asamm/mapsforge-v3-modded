package com.asamm.locus.mapsforge.utils;

import android.content.Context;
import android.graphics.Bitmap;

import org.mapsforgeV3.android.maps.mapgenerator.RenderThemeDefinition;
import org.mapsforgeV3.android.maps.rendertheme.RenderTheme;
import org.mapsforgeV3.android.maps.rendertheme.XmlRenderThemeStyleMenu;

import java.io.InputStream;
import java.util.Set;

public interface MapsForgeHandler {

	// CORE IMAGES
	
	Bitmap getEmptyImage();

    /**
     * Function allows to check if source bitmap is usable for specific
     * map tile. In case not, it creates new empty bitmap object
     * @param bitmap existing bitmap
     * @param pixelSizeX size in X pixels
     * @param pixelSizeY size in Y pixels
     * @return valid bitmap usable for a tile
     */
    Bitmap getValidImage(Bitmap bitmap, int pixelSizeX, int pixelSizeY);

	// VARIOUS PARAMETERS

	/**
	 * Get current app context.
	 * @return context
	 */
    Context getContext();

	/**
	 * Get screen category parameter.
	 * @return current screen parameter
	 */
	int getScreenCategory();

	/**
	 * Get converted DP value by device density.
	 * @param parseFloat float value
	 * @param roundDensity {@code true} use rounded density to 1, 2, 4 values
	 * @return converted value
	 */
	float getDpPixels(float parseFloat, boolean roundDensity);

    // HANDLER FOR THEMES

    /**
     * Handler for styling vector themes. Allows to define which categories will be visible
     * and which hidden.
     * @return render theme or null if not yet ready
     */
    RenderTheme getRenderTheme(RenderThemeDefinition jobTheme);

    // GRAPHICS

    /**
     * Create SVG image from certain stream.
     * @param inputStream stream with image
     * @param scale base scale for image
     * @param requestedWidth expected width
     * @param requestedHeight expected height
	 * @param color color that should be applied on images
     * @return generated (drawn) bitmap image
     * @throws Exception possible various exceptions
     */
    Bitmap createSVGBitmap(InputStream inputStream, float scale,
            float requestedWidth, float requestedHeight, int color) throws Exception;

	// LOGGER
	
	void logI(String tag, String msg);

	void logD(String tag, String msg);

	void logW(String tag, String msg);
	
	void logE(String tag, String msg, Exception e);
}
