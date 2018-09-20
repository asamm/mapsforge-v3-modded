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
package org.mapsforgeV3.core.util;

/**
 * An implementation of the spherical Mercator projection.
 */
public final class MercatorProjection {
	/**
	 * The circumference of the earth at the equator in meters.
	 */
	public static final double EARTH_CIRCUMFERENCE = 40075016.686;

	/**
	 * Maximum possible latitude coordinate of the map.
	 */
	public static final double LATITUDE_MAX = 85.05112877980659;

	/**
	 * Minimum possible latitude coordinate of the map.
	 */
	public static final double LATITUDE_MIN = -LATITUDE_MAX;

	private static final double MATH_RHO = 180.0 / Math.PI;
	private static final double PI2 = 2 * Math.PI;
	private static final double PI4 = 4 * Math.PI;
	
//	/**
//	 * Calculates the distance on the ground that is represented by a single pixel on the map.
//	 * 
//	 * @param latitude
//	 *            the latitude coordinate at which the resolution should be calculated.
//	 * @param zoomLevel
//	 *            the zoom level at which the resolution should be calculated.
//	 * @return the ground resolution at the given latitude and zoom level.
//	 */
//	public static double calculateGroundResolution(double latitude, byte zoomLevel) {
//		long mapSize = getMapSize(zoomLevel);
//		return Math.cos(latitude / MATH_RHO) * EARTH_CIRCUMFERENCE / mapSize;
//	}
//
//	/**
//	 * Computes the amount of latitude degrees for a given distance in pixel at a given zoom level.
//	 * 
//	 * @param deltaPixel
//	 *            the delta in pixel
//	 * @param lat
//	 *            the latitude
//	 * @param zoom
//	 *            the zoom level
//	 * @return the delta in degrees
//	 */
//	public static double deltaLat(double deltaPixel, double lat, byte zoom) {
//		double pixelY = latitudeToPixelY(lat, zoom);
//		double lat2 = pixelYToLatitude(pixelY + deltaPixel, zoom);
//
//		return Math.abs(lat2 - lat);
//	}

	/**
	 * @param zoomLevel
	 *            the zoom level for which the size of the world map should be returned.
	 * @return the horizontal and vertical size of the map in pixel at the given zoom level.
	 * @throws IllegalArgumentException
	 *             if the given zoom level is negative.
	 */
	public static long getMapSize(byte zoomLevel, int tileSize) {
		if (zoomLevel < 0) {
			throw new IllegalArgumentException("zoom level must not be negative: " + zoomLevel);
		}
		return (long) Math.pow(2, zoomLevel + 8);
		//return (long) tileSize << zoomLevel;
	}

	/**
	 * Converts a latitude coordinate (in degrees) to a pixel Y coordinate at a certain zoom level.
	 * 
	 * @param latitude
	 *            the latitude coordinate that should be converted.
	 * @param mapSize
	 *            size of map in current zoom.
	 * @return the pixel Y coordinate of the latitude value.
	 */
	public static double latitudeToPixelY(double latitude, long mapSize) {
		double sinLatitude = Math.sin(latitude / MATH_RHO);
		// FIXME improve this formula so that it works correctly without the clipping
		double pixelY = (0.5 - Math.log((1 + sinLatitude) / (1.0 - sinLatitude)) / PI4) * mapSize;

		// improved system that remove calling Math.min/max
		double v1 = pixelY > 0.0 ? pixelY : 0.0;
		if (v1 < mapSize) {
			return v1;
		} else {
			return mapSize;
		}
	}

	/**
	 * Converts a latitude coordinate (in degrees) to a tile Y number at a certain zoom level.
	 * 
	 * @param latitude
	 *            the latitude coordinate that should be converted.
	 * @param zoomLevel
	 *            the zoom level at which the coordinate should be converted.
	 * @return the tile Y number of the latitude value.
	 */
	public static long latitudeToTileY(double latitude, byte zoomLevel, int tileSize) {
		return pixelYToTileY(latitudeToPixelY(latitude, 
				getMapSize(zoomLevel, tileSize)), zoomLevel, tileSize);
	}

	/**
	 * Converts a longitude coordinate (in degrees) to a pixel X coordinate at a certain zoom level.
	 * 
	 * @param longitude
	 *            the longitude coordinate that should be converted.
	 * @param mapSize
	 *            size of map in current zoom.
	 * @return the pixel X coordinate of the longitude value.
	 */
	public static double longitudeToPixelX(double longitude, long mapSize) {
		return (longitude + 180.0) / 360.0 * mapSize;
	}

	/**
	 * Converts a longitude coordinate (in degrees) to the tile X number at a certain zoom level.
	 * 
	 * @param longitude
	 *            the longitude coordinate that should be converted.
	 * @param zoomLevel
	 *            the zoom level at which the coordinate should be converted.
	 * @return the tile X number of the longitude value.
	 */
	public static long longitudeToTileX(double longitude, byte zoomLevel, int tileSize) {
		return pixelXToTileX(longitudeToPixelX(longitude, 
				getMapSize(zoomLevel, tileSize)), zoomLevel, tileSize);
	}

	/**
	 * Converts a pixel X coordinate at a certain zoom level to a longitude coordinate.
	 * 
	 * @param pixelX
	 *            the pixel X coordinate that should be converted.
	 * @param zoomLevel
	 *            the zoom level at which the coordinate should be converted.
	 * @return the longitude value of the pixel X coordinate.
	 * @throws IllegalArgumentException
	 *             if the given pixelX coordinate is invalid.
	 */
	public static double pixelXToLongitude(double pixelX, byte zoomLevel, int tileSize, boolean checkDim) {
		long mapSize = getMapSize(zoomLevel, tileSize);
		if (checkDim && (pixelX < 0 || pixelX > mapSize)) {
			throw new IllegalArgumentException("invalid pixelX coordinate at zoom level " + zoomLevel + ": " + pixelX);
		}
		return 360.0 * ((pixelX / mapSize) - 0.5);
	}

	/**
	 * Converts a pixel X coordinate to the tile X number.
	 * 
	 * @param pixelX
	 *            the pixel X coordinate that should be converted.
	 * @param zoomLevel
	 *            the zoom level at which the coordinate should be converted.
	 * @return the tile X number.
	 */
	public static long pixelXToTileX(double pixelX, byte zoomLevel, int tileSize) {
		return (long) Math.min(Math.max(pixelX / tileSize, 0), Math.pow(2, zoomLevel) - 1);
	}

	/**
	 * Converts a pixel Y coordinate at a certain zoom level to a latitude coordinate.
	 * 
	 * @param pixelY
	 *            the pixel Y coordinate that should be converted.
	 * @param zoomLevel
	 *            the zoom level at which the coordinate should be converted.
	 * @return the latitude value of the pixel Y coordinate.
	 * @throws IllegalArgumentException
	 *             if the given pixelY coordinate is invalid.
	 */
	public static double pixelYToLatitude(double pixelY, byte zoomLevel, int tileSize, boolean checkDim) {
		long mapSize = getMapSize(zoomLevel, tileSize);
		if (checkDim && (pixelY < 0 || pixelY > mapSize)) {
			throw new IllegalArgumentException("invalid pixelY coordinate at zoom level " + zoomLevel + ": " + pixelY);
		}
		double y = 0.5 - (pixelY / mapSize);
		return 90.0 - 360.0 * Math.atan(Math.exp(-y * PI2)) / Math.PI;
	}

	/**
	 * Converts a pixel Y coordinate to the tile Y number.
	 * 
	 * @param pixelY
	 *            the pixel Y coordinate that should be converted.
	 * @param zoomLevel
	 *            the zoom level at which the coordinate should be converted.
	 * @return the tile Y number.
	 */
	public static long pixelYToTileY(double pixelY, byte zoomLevel, int tileSize) {
		return (long) Math.min(Math.max(pixelY / tileSize, 0), Math.pow(2, zoomLevel) - 1);
	}

	/**
	 * Converts a tile X number at a certain zoom level to a longitude coordinate.
	 * 
	 * @param tileX
	 *            the tile X number that should be converted.
	 * @param zoomLevel
	 *            the zoom level at which the number should be converted.
	 * @return the longitude value of the tile X number.
	 */
	public static double tileXToLongitude(long tileX, byte zoomLevel, int tileSize) {
		return pixelXToLongitude(tileX * tileSize, zoomLevel, tileSize, false);
	}

	/**
	 * Converts a tile Y number at a certain zoom level to a latitude coordinate.
	 * 
	 * @param tileY
	 *            the tile Y number that should be converted.
	 * @param zoomLevel
	 *            the zoom level at which the number should be converted.
	 * @return the latitude value of the tile Y number.
	 */
	public static double tileYToLatitude(long tileY, byte zoomLevel, int tileSize) {
		return pixelYToLatitude(tileY * tileSize, zoomLevel, tileSize, false);
	}

	private MercatorProjection() {
		throw new IllegalStateException();
	}
}
