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

final class GeometryUtils {

	private float[] computeAreaCenterPosition(float[][] coordinates) {
		return computeAreaCenterPosition(isClosedWay(coordinates[0]), coordinates);
	}

	static float[] computeAreaCenterPosition(boolean coordinatesClosed, float[][] coordinates) {
		float[] centerPosition;
		if (coordinatesClosed || coordinates[0].length < 4) {
			centerPosition = calculateCenterOfBoundingBox(coordinates[0]);
		} else {
			int index = (coordinates[0].length / 2) / 2 * 2;
			centerPosition = new float[] {
					coordinates[0][index],
					coordinates[0][index + 1]};
		}
		return centerPosition;
	}

	/**
	 * Calculates the center of the minimum bounding rectangle for the given coordinates.
	 * 
	 * @param coordinates
	 *            the coordinates for which calculation should be done.
	 * @return the center coordinates of the minimum bounding rectangle.
	 */
	static float[] calculateCenterOfBoundingBox(float[] coordinates) {
		float longitudeMin = coordinates[0];
		float longitudeMax = coordinates[0];
		float latitudeMax = coordinates[1];
		float latitudeMin = coordinates[1];

		for (int i = 2; i < coordinates.length; i += 2) {
			if (coordinates[i] < longitudeMin) {
				longitudeMin = coordinates[i];
			} else if (coordinates[i] > longitudeMax) {
				longitudeMax = coordinates[i];
			}

			if (coordinates[i + 1] < latitudeMin) {
				latitudeMin = coordinates[i + 1];
			} else if (coordinates[i + 1] > latitudeMax) {
				latitudeMax = coordinates[i + 1];
			}
		}

		return new float[] { (longitudeMin + longitudeMax) / 2, (latitudeMax + latitudeMin) / 2 };
	}

	/**
	 * @param way
	 *            the coordinates of the way.
	 * @return true if the given way is closed, false otherwise.
	 */
	static boolean isClosedWay(float[] way) {
		return Float.compare(way[0], way[way.length - 2]) == 0 && 
				Float.compare(way[1], way[way.length - 1]) == 0;
	}
}
