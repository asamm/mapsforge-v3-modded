package org.mapsforgeV3.android.maps.rendertheme;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Entry class for automatically building menus from rendertheme V4+ files.
 * This class holds all the defined layers and allows to retrieve them by name
 * or through iteration.
 * This class is Serializable to be able to pass an instance of it through the
 * Android Intent mechanism.
 */
public class XmlRenderThemeStyleMenu {

    private final String mId;
    private final String mDefaultLanguage;
    private final String mDefaultValue;
    private final Map<String, XmlRenderThemeStyleLayer> mLayers;

    public XmlRenderThemeStyleMenu(String id, String defaultLanguage, String defaultValue) {
        this.mId = id;
        this.mDefaultLanguage = defaultLanguage;
        this.mDefaultValue = defaultValue;
        this.mLayers = new LinkedHashMap<>();
    }

    public String getId() {
        return this.mId;
    }

    public String getDefaultLanguage() {
        return this.mDefaultLanguage;
    }

    public String getDefaultValue() {
        return this.mDefaultValue;
    }

    public XmlRenderThemeStyleLayer createLayer(String id, boolean visible, boolean enabled) {
        XmlRenderThemeStyleLayer style = new XmlRenderThemeStyleLayer(
                id, visible, enabled, this.mDefaultLanguage);
        this.mLayers.put(id, style);
        return style;
    }

    public XmlRenderThemeStyleLayer getLayer(String id) {
        return this.mLayers.get(id);
    }

    public Map<String, XmlRenderThemeStyleLayer> getLayers() {
        return this.mLayers;
    }
}
