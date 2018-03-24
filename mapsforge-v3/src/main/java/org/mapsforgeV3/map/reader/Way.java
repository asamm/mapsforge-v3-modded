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
package org.mapsforgeV3.map.reader;

import java.util.ArrayList;
import java.util.List;

import org.mapsforgeV3.core.model.Tag;

/**
 * An immutable container for all data associated with a single way or area (closed way).
 */
public class Way {
	/**
	 * The position of the area label (may be null).
	 */
	public int latitudeE6;
	public int longitudeE6;

	/**
	 * The layer of this way + 5 (to avoid negative values).
	 */
	public byte layer;

	/**
	 * The tags of this way.
	 */
	public final List<Tag> tags;

	/**
	 * The geographical coordinates of the way nodes in the order longitude/latitude.
	 */
	public float[][] wayNodes;

    // bbox most top coordinate
	public int bboxTopE6;
    // bbox most bottom coordinate
	public int bboxBottomE6;
    // bbox most left coordinate
	public int bboxLeftE6;
    // bbox most right coordinate
	public int bboxRightE6;

	// flag if way if just background way
	public boolean isFillBackground;

    /**
     * Default constructor.
     */
	Way() {
        tags = new ArrayList<>();
        prepareToNewStep();
	}
	
	public void set(byte layer, int latitudeE6, int longitudeE6) {
		this.layer = layer;
		this.latitudeE6 = latitudeE6;
		this.longitudeE6 = longitudeE6;
	}

    /**
     * Clear content of way object so it may be reused.
     */
    void prepareToNewStep() {
        latitudeE6 = 0;
        longitudeE6 = 0;
        layer = 0;
        tags.clear();

        // clear data with coordinates
        clearNodesData();
    }
	
	void clearNodesData() {
		wayNodes = null;
		this.bboxTopE6 = Integer.MIN_VALUE;
		this.bboxBottomE6 = Integer.MAX_VALUE;
		this.bboxLeftE6 = Integer.MAX_VALUE;
		this.bboxRightE6 = Integer.MIN_VALUE;
		this.isFillBackground = false;
	}
}
