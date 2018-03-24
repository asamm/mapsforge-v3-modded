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

import org.mapsforgeV3.android.maps.mapgenerator.RenderThemeDefinition;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * An ExternalRenderTheme allows for customizing the rendering style of the map via an XML file.
 */
public class ExternalRenderThemeDefinition implements RenderThemeDefinition {

    // file with theme
	private final File mThemeFile;
	// special 'theme' argument
	private final String mThemeStyle;

	/**
	 * @param renderThemeFile
	 *            the XML render theme file.
	 * @throws FileNotFoundException
	 *             if the file does not exist or cannot be read.
	 */
	public ExternalRenderThemeDefinition(File renderThemeFile, String themeStyle) throws FileNotFoundException {
		if (!renderThemeFile.exists()) {
			throw new FileNotFoundException("file does not exist: " + renderThemeFile);
		} else if (!renderThemeFile.isFile()) {
			throw new FileNotFoundException("not a file: " + renderThemeFile);
		} else if (!renderThemeFile.canRead()) {
			throw new FileNotFoundException("cannot read file: " + renderThemeFile);
		}

		// store parameters
		this.mThemeFile = renderThemeFile;
		this.mThemeStyle = themeStyle == null ? "" : themeStyle;
	}

	@Override
	public String getRelativePathPrefix() {
		return this.mThemeFile.getParent();
	}

	@Override
	public InputStream getAsStream() throws IOException {
		return new FileInputStream(this.mThemeFile);
	}

	@Override
	public String getThemeStyle() {
		return mThemeStyle;
	}

	@Override
	public boolean isInternalTheme() {
		return false;
	}
}
