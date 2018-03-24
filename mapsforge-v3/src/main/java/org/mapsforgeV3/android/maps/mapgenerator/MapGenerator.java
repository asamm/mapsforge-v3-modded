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
package org.mapsforgeV3.android.maps.mapgenerator;

import org.mapsforgeV3.android.maps.rendertheme.RenderCallback;
import org.mapsforgeV3.core.model.GeoPoint;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;

/**
 * A MapGenerator provides map tiles either by downloading or rendering them.
 */
public interface MapGenerator {
	
	interface ExtraRenderingHandler {
		
		boolean drawBackground();
		
		/**
		 * This function check if ways will be rendered by theme renderer itself,
		 * or if will be handled in same cycle by {@link renderWay()} function
		 * @param zoomLevel
		 * @param layerLevel
		 * @return <code>false</code> if rendering will be handled by library (theme),
		 * otherwise return <code>true</code> 
		 */
		boolean handleRenderWay(int zoomLevel, int layerLevel);
		
		void renderWay(Canvas canvas, Path path, int layer, int level);
		
		void renderWayFinished(Canvas canvas, int zoomLevel, int layerLevel);
		
		boolean drawWaysSynchronized();
		
		void onTileRendered(Bitmap img);
	}
	
	/**
	 * Called once at the end of the MapGenerator lifecycle.
	 */
	void cleanup();

	/**
	 * Called when a job needs to be executed.
	 * 
	 * @param mapGeneratorJob
	 *            the job that should be executed.
	 * @return true if the job was executed successfully, false otherwise.
	 */
	RenderCallback executeJob(MapGeneratorJob mapGeneratorJob, Bitmap preparedImg,
			ExtraRenderingHandler extraRenderingHandler);

	/**
	 * @return the start point of this MapGenerator (may be null).
	 */
	GeoPoint getMapCenter();

	/**
	 * @return the start zoom level of this MapGenerator (may be null).
	 */
	Byte getStartZoomLevel();

	/**
	 * @return the maximum zoom level that this MapGenerator supports.
	 */
	byte getZoomLevelMax();
}
