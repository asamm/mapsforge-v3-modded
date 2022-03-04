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

import org.mapsforgeV3.core.model.Tile;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * A MapGeneratorJob holds all immutable rendering parameters for a single map image together with a mutable priority
 * field, which indicates the importance of this job.
 */
public class MapGeneratorJob implements Serializable {
	
	private static final long serialVersionUID = 1L;

	/**
	 * The debug settings for this job.
	 */
	public final DebugSettings debugSettings;

	/**
	 * The rendering parameters for this job.
	 */
	public JobParameters jobParameters;

	/**
	 * The tile which should be generated.
	 */
	public final Tile tile;

	private transient int hashCodeValue;

	/**
	 * Creates a new job for a MapGenerator with the given parameters.
	 * 
	 * @param tile
	 *            the tile which should be generated.
	 * @param mapGeneratorId
	 *            the ID of the MapGenerator for this job.
	 * @param jobParameters
	 *            the rendering parameters for this job.
	 * @param debugSettings
	 *            the debug settings for this job.
	 */
	public MapGeneratorJob(Tile tile, JobParameters jobParameters, DebugSettings debugSettings) {
		this.tile = tile;
		this.jobParameters = jobParameters;
		this.debugSettings = debugSettings;
		calculateTransientValues();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof MapGeneratorJob)) {
			return false;
		}
		MapGeneratorJob other = (MapGeneratorJob) obj;
		if (this.debugSettings == null) {
			if (other.debugSettings != null) {
				return false;
			}
		} else if (!this.debugSettings.equals(other.debugSettings)) {
			return false;
		}
		if (this.jobParameters == null) {
			if (other.jobParameters != null) {
				return false;
			}
		} else if (!this.jobParameters.equals(other.jobParameters)) {
			return false;
		}
		if (this.tile == null) {
            return other.tile == null;
		} else return this.tile.equals(other.tile);
    }

	@Override
	public int hashCode() {
		return this.hashCodeValue;
	}

	/**
	 * @return the hash code of this object.
	 */
	private int calculateHashCode() {
		int result = 1;
		result = 31 * result + ((this.debugSettings == null) ? 0 : this.debugSettings.hashCode());
		result = 31 * result + ((this.jobParameters == null) ? 0 : this.jobParameters.hashCode());
		result = 31 * result + ((this.tile == null) ? 0 : this.tile.hashCode());
		return result;
	}

	/**
	 * Calculates the values of some transient variables.
	 */
	private void calculateTransientValues() {
		this.hashCodeValue = calculateHashCode();
	}

	private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
		objectInputStream.defaultReadObject();
		calculateTransientValues();
	}
}
