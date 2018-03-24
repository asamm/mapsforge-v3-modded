package org.mapsforgeV3.map.reader;

/**
 * A MapFileException is thrown if a file is opened as a MapFile that is somehow invalid.
 */
public class MapFileException extends IllegalArgumentException {
	private static final long serialVersionUID = 1L;

	public MapFileException(String errorMessage) {
		super(errorMessage);
	}
}

