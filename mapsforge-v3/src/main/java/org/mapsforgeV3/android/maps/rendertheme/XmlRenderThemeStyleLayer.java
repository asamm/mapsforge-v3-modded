package org.mapsforgeV3.android.maps.rendertheme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * An individual layer in the rendertheme V4+ menu system.
 * A layer can have translations, categories that will always be enabled
 * when the layer is selected as well as optional overlays.
 */
public class XmlRenderThemeStyleLayer {

    // ID of layer
    private final String mId;
    private final boolean visible;
    private final boolean enabled;
    private final String defaultLanguage;
    // defined categories
    private final Set<String> mCategories;
    private final List<XmlRenderThemeStyleLayer> mOverlays;
    private final Map<String, String> mTitles;

    XmlRenderThemeStyleLayer(String id, boolean visible, boolean enabled, String defaultLanguage) {
        this.mId = id;
        this.visible = visible;
        this.enabled = enabled;
        this.defaultLanguage = defaultLanguage;
        this.mCategories = new LinkedHashSet<>();
        this.mOverlays = new ArrayList<>();
        this.mTitles = new HashMap<>();
    }

    // BASIC GETTERS

    public String getId() {
        return this.mId;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    // CATEGORIES

    public void addCategory(String category) {
        this.mCategories.add(category);
    }

    public Set<String> getCategories() {
        return this.mCategories;
    }

    // OVERLAYS

    public void addOverlay(XmlRenderThemeStyleLayer overlay) {
        this.mOverlays.add(overlay);
    }

    public List<XmlRenderThemeStyleLayer> getOverlays() {
        return this.mOverlays;
    }

    // TRANSLATIONS

    public void addTranslation(String language, String name) {
        this.mTitles.put(language, name);
    }

    public String getTitle(String language) {
        String result = this.mTitles.get(language);
        if (result == null) {
            return this.mTitles.get(this.defaultLanguage);
        }
        return result;
    }

    public Map<String, String> getTitles() {
        return this.mTitles;
    }
}