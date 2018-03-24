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
package org.mapsforgeV3.map.reader.header;

import org.mapsforgeV3.core.model.GeoPoint;
import org.mapsforgeV3.map.reader.MapFileException;
import org.mapsforgeV3.map.reader.ReadBuffer;

final class OptionalFields {

	// bitmask for the comment field in the file header.
	private static final int HEADER_BITMASK_COMMENT = 0x08;
	// bitmask for the created by field in the file header.
	private static final int HEADER_BITMASK_CREATED_BY = 0x04;
	// bitmask for the debug flag in the file header.
	private static final int HEADER_BITMASK_DEBUG = 0x80;
	// bitmask for the language preference field in the file header.
	private static final int HEADER_BITMASK_LANGUAGE_PREFERENCE = 0x10;
	// bitmask for the start position field in the file header.
	private static final int HEADER_BITMASK_START_POSITION = 0x40;
	// bitmask for the start zoom level field in the file header.
	private static final int HEADER_BITMASK_START_ZOOM_LEVEL = 0x20;
	// maximum valid start zoom level.
	private static final int START_ZOOM_LEVEL_MAX = 22;

	/**
	 * Read optional fields from map file.
	 * @param readBuffer buffer
	 * @param mapFileInfoBuilder builder
	 */
	static void readOptionalFields(ReadBuffer readBuffer, MapFileInfoBuilder mapFileInfoBuilder) {
		OptionalFields optionalFields = new OptionalFields(readBuffer.readByte());
		mapFileInfoBuilder.optionalFields = optionalFields;

		// finally read fields
		optionalFields.readOptionalFields(readBuffer);
	}

	// PRIVATE PART

	String comment;
	String createdBy;
	private final boolean hasComment;
	private final boolean hasCreatedBy;
	private final boolean hasLanguagePreference;
	private final boolean hasStartPosition;
	private final boolean hasStartZoomLevel;
	final boolean isDebugFile;
	String languagesPreference;
	GeoPoint startPosition;
	Byte startZoomLevel;

	/**
	 * Create new optional fields container.
	 * @param flags fields flags
	 */
	private OptionalFields(byte flags) {
		this.isDebugFile = (flags & HEADER_BITMASK_DEBUG) != 0;
		this.hasStartPosition = (flags & HEADER_BITMASK_START_POSITION) != 0;
		this.hasStartZoomLevel = (flags & HEADER_BITMASK_START_ZOOM_LEVEL) != 0;
		this.hasLanguagePreference = (flags & HEADER_BITMASK_LANGUAGE_PREFERENCE) != 0;
		this.hasComment = (flags & HEADER_BITMASK_COMMENT) != 0;
		this.hasCreatedBy = (flags & HEADER_BITMASK_CREATED_BY) != 0;
	}

	/**
	 * Read fields from buffer.
	 * @param readBuffer buffer
	 */
	private void readOptionalFields(ReadBuffer readBuffer) {
		readMapStartPosition(readBuffer);
		readMapStartZoomLevel(readBuffer);
		readLanguagesPreference(readBuffer);
		if (this.hasComment) {
			this.comment = readBuffer.readUTF8EncodedString();
		}
		if (this.hasCreatedBy) {
			this.createdBy = readBuffer.readUTF8EncodedString();
		}
	}

	private void readMapStartPosition(ReadBuffer readBuffer) {
		// V4 TODO fix
//		if (this.hasStartPosition) {
//			double mapStartLatitude = LatLongUtils.microdegreesToDegrees(readBuffer.readInt());
//			double mapStartLongitude = LatLongUtils.microdegreesToDegrees(readBuffer.readInt());
//			try {
//				this.startPosition = new LatLong(mapStartLatitude, mapStartLongitude);
//			} catch (IllegalArgumentException e) {
//				throw new MapFileException(e.getMessage());
//			}
//		}

		if (this.hasStartPosition) {
			// get and check the start position latitude (4 byte)
			int mapStartLatitude = readBuffer.readInt();
			if (mapStartLatitude < RequiredFields.LATITUDE_MIN || mapStartLatitude > RequiredFields.LATITUDE_MAX) {
				throw new MapFileException("invalid map start latitude: " + mapStartLatitude);
			}

			// get and check the start position longitude (4 byte)
			int mapStartLongitude = readBuffer.readInt();
			if (mapStartLongitude < RequiredFields.LONGITUDE_MIN || mapStartLongitude > RequiredFields.LONGITUDE_MAX) {
				throw new MapFileException("invalid map start longitude: " + mapStartLongitude);
			}

			this.startPosition = new GeoPoint(mapStartLatitude, mapStartLongitude);
		}
	}

	private void readMapStartZoomLevel(ReadBuffer readBuffer) {
		if (this.hasStartZoomLevel) {
			// get and check the start zoom level (1 byte)
			byte mapStartZoomLevel = readBuffer.readByte();
			if (mapStartZoomLevel < 0 || mapStartZoomLevel > START_ZOOM_LEVEL_MAX) {
				throw new MapFileException("invalid map start zoom level: " + mapStartZoomLevel);
			}
			this.startZoomLevel = mapStartZoomLevel;
		}
	}

	private void readLanguagesPreference(ReadBuffer readBuffer) {
		if (this.hasLanguagePreference) {
			this.languagesPreference = readBuffer.readUTF8EncodedString();
		}
	}


}
