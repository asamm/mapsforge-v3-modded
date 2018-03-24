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

import org.mapsforgeV3.core.model.BoundingBox;
import org.mapsforgeV3.core.model.Tag;
import org.mapsforgeV3.map.reader.MapFileException;
import org.mapsforgeV3.map.reader.ReadBuffer;

import java.io.IOException;

final class RequiredFields {

	// magic byte at the beginning of a valid binary map file.
	private static final String BINARY_OSM_MAGIC_BYTE = "mapsforge binary OSM";
	// maximum size of the file header in bytes.
	private static final int HEADER_SIZE_MAX = 1000000;
	// minimum size of the file header in bytes.
	private static final int HEADER_SIZE_MIN = 70;
	// the name of the Mercator projection as stored in the file header.
	private static final String MERCATOR = "Mercator";
	// the maximum latitude values in microdegrees.
	static final int LATITUDE_MAX = 90000000;
	// the minimum latitude values in microdegrees.
	static final int LATITUDE_MIN = -90000000;
	// the maximum longitude values in microdegrees.
	static final int LONGITUDE_MAX = 180000000;
	// the minimum longitude values in microdegrees.
	static final int LONGITUDE_MIN = -180000000;

	// lowest version of the map file format supported by this implementation.
	private static final int SUPPORTED_FILE_VERSION_MIN = 3;
	// highest version of the map file format supported by this implementation.
	private static final int SUPPORTED_FILE_VERSION_MAX = 4;

	/**
	 * Read core magic byte.
	 * @param readBuffer buffer
	 * @throws IOException
	 */
	static void readMagicByte(ReadBuffer readBuffer) throws IOException {
		// read the the magic byte and the file header size into the buffer
		int magicByteLength = BINARY_OSM_MAGIC_BYTE.length();
		if (!readBuffer.readFromFile(magicByteLength + 4)) {
			throw new MapFileException("reading magic byte has failed");
		}

		// get and check the magic byte
		String magicByte = readBuffer.readUTF8EncodedString(magicByteLength);
		if (!BINARY_OSM_MAGIC_BYTE.equals(magicByte)) {
			throw new MapFileException("invalid magic byte: " + magicByte);
		}
	}

	/**
	 * Read remaining header size.
	 * @param readBuffer buffer
	 * @throws IOException
	 */
	static void readRemainingHeader(ReadBuffer readBuffer) throws IOException {
		// get and check the size of the remaining file header (4 bytes)
		int remainingHeaderSize = readBuffer.readInt();
		if (remainingHeaderSize < HEADER_SIZE_MIN || remainingHeaderSize > HEADER_SIZE_MAX) {
			throw new MapFileException("invalid remaining header size: " + remainingHeaderSize);
		}

		// read the header data into the buffer
		if (!readBuffer.readFromFile(remainingHeaderSize)) {
			throw new MapFileException("reading header data has failed: " + remainingHeaderSize);
		}
	}

	/**
	 * Read version of current map file.
	 * @param readBuffer buffer
	 * @param mapFileInfoBuilder current info builder
	 */
	static void readFileVersion(ReadBuffer readBuffer, MapFileInfoBuilder mapFileInfoBuilder) {
		// get and check the file version (4 bytes)
		int fileVersion = readBuffer.readInt();
		if (fileVersion < SUPPORTED_FILE_VERSION_MIN || fileVersion > SUPPORTED_FILE_VERSION_MAX) {
			throw new MapFileException("unsupported file version: " + fileVersion);
		}
		mapFileInfoBuilder.fileVersion = fileVersion;
	}

	/**
	 * Read/verify size of map file.
	 * @param readBuffer buffer
	 * @param fileSize real file size on disk
	 * @param mapFileInfoBuilder current info builder
	 */
	static void readFileSize(ReadBuffer readBuffer, long fileSize, MapFileInfoBuilder mapFileInfoBuilder) {
		// get and check the file size (8 bytes)
		long headerFileSize = readBuffer.readLong();
		if (headerFileSize != fileSize) {
			throw new MapFileException("invalid file size: " + headerFileSize);
		}
		mapFileInfoBuilder.fileSize = fileSize;
	}

	/**
	 * Read date when map was created.
	 * @param readBuffer buffer
	 * @param mapFileInfoBuilder current info builder
	 */
	static void readMapDate(ReadBuffer readBuffer, MapFileInfoBuilder mapFileInfoBuilder) {
		// get and check the the map date (8 bytes)
		long mapDate = readBuffer.readLong();
		// is the map date before 2010-01-10 ?
		if (mapDate < 1200000000000L) {
			throw new MapFileException("invalid map date: " + mapDate);
		}
		mapFileInfoBuilder.mapDate = mapDate;
	}

	/**
	 * Read coordinates from map file.
	 * @param readBuffer buffer
	 * @param mapFileInfoBuilder builder
	 */
	static void readBoundingBox(ReadBuffer readBuffer, MapFileInfoBuilder mapFileInfoBuilder) {
		// V4 TODO fix
//		double minLatitude = CoordinatesUtils.microdegreesToDegrees(readBuffer.readInt());
//		double minLongitude = CoordinatesUtils.microdegreesToDegrees(readBuffer.readInt());
//		double maxLatitude = CoordinatesUtils.microdegreesToDegrees(readBuffer.readInt());
//		double maxLongitude = CoordinatesUtils.microdegreesToDegrees(readBuffer.readInt());
//
//		try {
//			mapFileInfoBuilder.boundingBox = new BoundingBox(minLatitude, minLongitude, maxLatitude, maxLongitude);
//		} catch (IllegalArgumentException e) {
//			throw new MapFileException(e.getMessage());
//		}

		// get coordinates (4 bytes)
		int minLatitude = readBuffer.readInt();
		int minLongitude = readBuffer.readInt();
		int maxLatitude = readBuffer.readInt();
		int maxLongitude = readBuffer.readInt();

		try {
			mapFileInfoBuilder.boundingBox = new BoundingBox(minLatitude, minLongitude, maxLatitude, maxLongitude);
		} catch (IllegalArgumentException e) {
			throw new MapFileException(e.getMessage());
		}
	}

	/**
	 * Read size of map tiles.
	 * @param readBuffer buffer
	 * @param mapFileInfoBuilder current info builder
	 */
	static void readTilePixelSize(ReadBuffer readBuffer, MapFileInfoBuilder mapFileInfoBuilder) {
		// get the tile pixel size (2 bytes)
		mapFileInfoBuilder.tilePixelSize = readBuffer.readShort();
	}

	/**
	 * Read name of map projection.
	 * @param readBuffer buffer
	 * @param mapFileInfoBuilder current info builder
	 */
	static void readProjectionName(ReadBuffer readBuffer, MapFileInfoBuilder mapFileInfoBuilder) {
		// get and check the projection name
		String projectionName = readBuffer.readUTF8EncodedString();
		if (!MERCATOR.equals(projectionName)) {
			throw new MapFileException("unsupported projection: " + projectionName);
		}
		mapFileInfoBuilder.projectionName = projectionName;
	}

	static void readPoiTags(ReadBuffer readBuffer, MapFileInfoBuilder mapFileInfoBuilder) {
		// get and check the number of POI tags (2 bytes)
		int numberOfPoiTags = readBuffer.readShort();
		if (numberOfPoiTags < 0) {
			throw new MapFileException("invalid number of POI tags: " + numberOfPoiTags);
		}

		Tag[] poiTags = new Tag[numberOfPoiTags];
		for (int currentTagId = 0; currentTagId < numberOfPoiTags; ++currentTagId) {
			// get and check the POI tag
			poiTags[currentTagId] = readBuffer.readTagObject();
            if (poiTags[currentTagId] == null) {
                throw new MapFileException("POI tag must not be null: " + currentTagId);
            }
        }
		mapFileInfoBuilder.poiTags = poiTags;
	}

	static void readWayTags(ReadBuffer readBuffer, MapFileInfoBuilder mapFileInfoBuilder) {
		// get and check the number of way tags (2 bytes)
		int numberOfWayTags = readBuffer.readShort();
		if (numberOfWayTags < 0) {
			throw new MapFileException("invalid number of way tags: " + numberOfWayTags);
		}

		Tag[] wayTags = new Tag[numberOfWayTags];

		for (int currentTagId = 0; currentTagId < numberOfWayTags; ++currentTagId) {
			// get and check the way tag
            wayTags[currentTagId] = readBuffer.readTagObject();
			if (wayTags[currentTagId] == null) {
				throw new MapFileException("way tag must not be null: " + currentTagId);
			}
		}
		mapFileInfoBuilder.wayTags = wayTags;
	}

	private RequiredFields() {
		throw new IllegalStateException();
	}
}
