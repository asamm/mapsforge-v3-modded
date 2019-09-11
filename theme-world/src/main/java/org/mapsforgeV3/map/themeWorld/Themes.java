package org.mapsforgeV3.map.themeWorld;

import org.mapsforgeV3.android.maps.mapgenerator.RenderThemeDefinition;

import java.io.InputStream;

public class Themes {

	// default prefix/path for internal themes
    private static final String DEFAULT_PREFIX = "/assets/themes/mapsforgeV3/";

    /**
     * Internal theme included in Locus application.
     */
	public static class LocusThemeDefinition implements RenderThemeDefinition {

        // prefix for map path
        private String mPathPrefix;
        // name of theme
		private String mName;
        // type of theme
        private String mType;
		
		public LocusThemeDefinition(String pathPrefix, String name, String type) {
            this.mPathPrefix = pathPrefix;
			this.mName = name;
            this.mType = type;
		}
		
		@Override
		public InputStream getAsStream() {
			return Themes.class.getResourceAsStream(DEFAULT_PREFIX + mName + ".xml");
		}

		@Override
		public String getThemeStyle() {
			return mType;
		}

		@Override
		public String getRelativePathPrefix() {
			return mPathPrefix;
		}

		@Override
		public boolean isInternalTheme() {
			return true;
		}
	}

	public static RenderThemeDefinition getThemeWorld() {

		return new LocusThemeDefinition(DEFAULT_PREFIX, "world", "");
	}
}
