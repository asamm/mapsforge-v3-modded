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

import java.util.List;

import org.mapsforgeV3.android.maps.rendertheme.tools.BgRectangle;

import com.asamm.locus.mapsforge.utils.Utils;

import android.graphics.Bitmap;
import android.graphics.Paint;

public final class WayDecorator {
	/**
	 * Minimum distance in pixels before the symbol is repeated.
	 */
	public static final int DISTANCE_BETWEEN_SYMBOLS = (int) Utils.getHandler().getDpPixels(100.0f, false);

	/**
	 * Minimum distance in pixels before the way name is repeated.
	 */
	private static final int DISTANCE_BETWEEN_WAY_NAMES = (int) Utils.getHandler().getDpPixels(250.0f, false);
	
	/**
	 * Distance in pixels (square) to skip from both ends of a segment.
	 */
	public static final int SEGMENT_SAFETY_DISTANCE = (int) Utils.getHandler().getDpPixels(30.0f, false);

	static void renderSymbol(Bitmap symbolBitmap, boolean alignCenter, boolean repeatSymbol, 
			float[][] coordinates, List<PaintContainerSymbol> waySymbols,
			float scale, float horOffset, float verOffset, float repeatGap) {
//		Utils.getHandler().logD("WayDecorator", "renderSymbol(" + symbolBitmap + ", " + alignCenter + ", " +
//				repeatSymbol + ", " + coordinates + ", " + waySymbols + ", " + scale + ", " +
//				horOffset + ", " + verOffset + ", " + repeatGap);
		int skipPixels = (int) (horOffset * scale);

		// get the first way point coordinates
		float previousX = coordinates[0][0];
		float previousY = coordinates[0][1];
		int imgWidth = (int) (symbolBitmap.getWidth() * scale);
		
		// draw the symbol on each way segment
		float segmentLengthRemaining;
		float segmentSkipPercentage;
		float symbolAngle;
		for (int i = 2; i < coordinates[0].length; i += 2) {
			// get the current way point coordinates
			float currentX = coordinates[0][i];
			float currentY = coordinates[0][i + 1];

			// calculate the length of the current segment (Euclidian distance)
			float diffX = currentX - previousX;
			float diffY = currentY - previousY;
			double segmentLengthInPixel = Math.sqrt(diffX * diffX + diffY * diffY);
			segmentLengthRemaining = (float) segmentLengthInPixel;

			// test till there is enough space for image
			while ((segmentLengthRemaining - skipPixels) > imgWidth) {
				// calculate the percentage of the current segment to skip
				segmentSkipPercentage = skipPixels / segmentLengthRemaining;

				// move the previous point forward towards the current point
				previousX += diffX * segmentSkipPercentage;
				previousY += diffY * segmentSkipPercentage;
				symbolAngle = (float) Math.toDegrees(Math.atan2(currentY - previousY, currentX - previousX));

				waySymbols.add(new PaintContainerSymbol(symbolBitmap,
						previousX, previousY, alignCenter, symbolAngle, scale, 0, false));

				// check if the symbol should only be rendered once
				if (!repeatSymbol) {
					return;
				}

				// recalculate the distances
				diffX = currentX - previousX;
				diffY = currentY - previousY;

				// recalculate the remaining length of the current segment
				segmentLengthRemaining -= skipPixels;

				// set the amount of pixels to skip before repeating the symbol
				skipPixels = (int) (repeatGap + imgWidth);

//Log.d("XXX", "  generated, remain:" + segmentLengthRemaining + ", len:" + segmentLengthInPixel + ", skip:" + skipPixels + ", imgW:" + imgWidth);
			}

			skipPixels -= segmentLengthRemaining;
			if (skipPixels < 0) {
				skipPixels = 0;
			}

			// set the previous way point coordinates for the next loop
			previousX = currentX;
			previousY = currentY;
		}
	}

	static void renderText(String textKey, float[][] coordinates,
			List<PaintContainerWayText> wayNames, float horOffset, float verOffset, boolean rotateUp,
			Paint paintFill, Paint paintStroke, BgRectangle bgRect) {
		// calculate the way name length plus some margin of safety
		float wayNameWidth = paintFill.measureText(textKey) + 10;
		wayNameWidth *= wayNameWidth;
		
		int skipPixels = 0;

		// get the first way point coordinates
		float previousX = coordinates[0][0];
		float previousY = coordinates[0][1];

		// find way segments long enough to draw the way name on them
		for (int i = 2; i < coordinates[0].length; i += 2) {
			// get the current way point coordinates
			float currentX = coordinates[0][i];
			float currentY = coordinates[0][i + 1];

			// calculate the length of the current segment (Euclidian distance)
			float diffX = currentX - previousX;
			float diffY = currentY - previousY;
			double segmentLengthInPixel = diffX * diffX + diffY * diffY;

			if (skipPixels > 0) {
				skipPixels -= segmentLengthInPixel;
			} else if (segmentLengthInPixel > wayNameWidth) {
				float[] wayNamePath = new float[4];
				// check to prevent inverted way names
				if (!rotateUp || previousX > currentX) {
					wayNamePath[0] = currentX;
					wayNamePath[1] = currentY;
					wayNamePath[2] = previousX;
					wayNamePath[3] = previousY;
				} else {
					wayNamePath[0] = previousX;
					wayNamePath[1] = previousY;
					wayNamePath[2] = currentX;
					wayNamePath[3] = currentY;
				}

				// add text container
				wayNames.add(new PaintContainerWayText(wayNamePath, textKey, horOffset, verOffset,
						paintFill, paintStroke, bgRect));
				skipPixels = DISTANCE_BETWEEN_WAY_NAMES;
			}

			// store the previous way point coordinates
			previousX = currentX;
			previousY = currentY;
		}
	}

	/**
	 * Unsupported constructor.
	 */
	private WayDecorator() {
		throw new IllegalStateException();
	}
}
