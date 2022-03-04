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
package org.mapsforgeV3.android.maps.rendertheme;

import android.graphics.Color;

import com.asamm.locus.mapsforge.utils.Utils;

import org.mapsforgeV3.android.maps.rendertheme.renderinstruction.RenderInstruction;
import org.mapsforgeV3.android.maps.rendertheme.rules.Closed;
import org.mapsforgeV3.android.maps.rendertheme.rules.Rule;
import org.mapsforgeV3.core.model.Tag;
import org.mapsforgeV3.core.util.LRUCache;
import org.xml.sax.Attributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A RenderTheme defines how ways and nodes are drawn.
 */
public class RenderTheme {

	// tag for logger
	private static final String TAG = "RenderTheme";

	private static final int MATCHING_CACHE_SIZE = 8192;
	private static final int RENDER_THEME_VERSION = 4;

	private static void validate(String elementName, Integer version, float baseStrokeWidth, float baseTextSize) {
		if (version == null) {
			throw new IllegalArgumentException("missing attribute version for element:" + elementName);
		} else if (version > RENDER_THEME_VERSION) {
			throw new IllegalArgumentException("invalid render theme version:" + version);
		} else if (baseStrokeWidth < 0) {
			throw new IllegalArgumentException("base-stroke-width must not be negative: " + baseStrokeWidth);
		} else if (baseTextSize < 0) {
			throw new IllegalArgumentException("base-text-size must not be negative: " + baseTextSize);
		}
	}

	static RenderTheme create(String elementName, Attributes attributes) {
		Integer version = null;
		int mapBackground = Color.WHITE;
		float baseStrokeWidth = 1;
		float baseTextSize = 1;
		
		boolean locusExtended = false;
		boolean fillSeaAreas = true;
		boolean scaleLineDyByZoom = false;

		for (int i = 0; i < attributes.getLength(); ++i) {
			String name = attributes.getLocalName(i);
			String value = attributes.getValue(i);

			if ("schemaLocation".equals(name)) {
				// do nothing
			} else if ("version".equals(name)) {
				version = Integer.parseInt(value);
			} else if ("map-background".equals(name)) {
				mapBackground = Color.parseColor(value);
			} else if ("base-stroke-width".equals(name)) {
				baseStrokeWidth = Float.parseFloat(value);
			} else if ("base-text-size".equals(name)) {
				baseTextSize = Float.parseFloat(value);
			} else if ("locus-extended".equals(name)) {
				locusExtended = Integer.parseInt(value) == 1;
			} else if ("fill-sea-areas".equals(name)) {
				fillSeaAreas = Integer.parseInt(value) == 1;
			} else if ("scale-line-dy-by-zoom".equals(name)) {
				scaleLineDyByZoom = Integer.parseInt(value) == 1;
			} else {
				RenderThemeHandler.logUnknownAttribute(elementName, name, value, i);
			}
		}

		validate(elementName, version, baseStrokeWidth, baseTextSize);
		return new RenderTheme(mapBackground, baseStrokeWidth, baseTextSize, 
				locusExtended, fillSeaAreas, scaleLineDyByZoom);
	}

	// base parameters
	private final int mapBackground;
	private final float baseStrokeWidth;
	private final float baseTextSize;

	// extra parameters
	private final boolean locusExtended;
	private final boolean fillSeaAreas;
	private final boolean scaleLineDyByZoom;

	// cache for rendering instructions
	private final Map<Integer, List<RenderInstruction>> mMatchingCache;
	
	// list of active rules
	private Rule[] rulesList;
	private List<Rule> rulesListLoad;

	// private parameters
	private int levels;
	private float lastScaleStroke;
	private float lastScaleText;
	private float lastZoomLevel;

	// optimized version of theme
	private RenderTheme themeOptimized;
	
	// basic constructor
	private RenderTheme(int mapBackground, float baseStrokeWidth, float baseTextSize,
			boolean locusExtended, boolean fillSeaAreas, boolean scaleLineDyByZoom) {
		this.mapBackground = mapBackground;
		this.baseStrokeWidth = baseStrokeWidth;
		this.baseTextSize = baseTextSize;
		this.locusExtended = locusExtended;
		this.fillSeaAreas = fillSeaAreas;
		this.scaleLineDyByZoom = scaleLineDyByZoom;
		this.rulesListLoad = new ArrayList<>();

		// extra parameters
		this.mMatchingCache = Collections.synchronizedMap(
				new LRUCache<Integer, List<RenderInstruction>>(MATCHING_CACHE_SIZE));
		
		// private variables
		lastScaleStroke = 0.0f;
		lastScaleText = 0.0f;
		lastZoomLevel = Integer.MIN_VALUE;
	}

	/**
	 * Check if current theme contain extended features special for Locus
	 * @return <code>true</code> if theme is "Locus extended"
	 */
	public boolean isLocusExtended() {
		return locusExtended;
	}
	
	public boolean isFillSeaAreas() {
		return fillSeaAreas;
	}

	public boolean isScaleLineDyByZoom() {
		return scaleLineDyByZoom;
	}

	/**
	 * Must be called when this RenderTheme gets destroyed to clean up and free resources.
	 */
	public void destroy() {
		this.mMatchingCache.clear();
		Rule[] oldRules = rulesList;
		rulesList = null;
        for (Rule rule : oldRules) {
            rule.onDestroy();
        }
	}
	
	// MAIN SETTERS

    // number of tested nodes
	public static int countTestNodes = 0;
    // number of tested ways
	public static int countTestWays = 0;

	//private static int countMatchWay;
	//private static int countMatchWayCached;

	private static final double STROKE_INCREASE = 1.4;
	private static final byte STROKE_MIN_ZOOM_LEVEL = 12;

	/**
	 * Prepare theme for next rendering
	 * @param currentZoomLevel current zoom level value
	 * @param textScale scale that should apply on texts
	 */
	public void prepareTheme(byte currentZoomLevel, float textScale, String mapCountryCode) {
		int zoomLevelDiff = Math.max(currentZoomLevel - STROKE_MIN_ZOOM_LEVEL, 0);
		prepareTheme((float) Math.pow(STROKE_INCREASE, zoomLevelDiff),
				textScale, currentZoomLevel, true, mapCountryCode);
	}

	/**
	 * Prepare theme for another rendering. Call this function after every important change
     * in rendering settings, like zoom etc.
	 */
	private synchronized void prepareTheme(float scaleStroke, float scaleText, byte zoomLevel,
			boolean generateOptimizedTheme, String mapCountryCode) {
		// check previous values
		if (scaleStroke == this.lastScaleStroke &&
				scaleText == this.lastScaleText &&
				zoomLevel == this.lastZoomLevel) {
			// all correctly set
			return;
		}

        // prepare parameters
		countTestNodes = 0;
		countTestWays = 0;
		//countMatchWay = 0;
		//countMatchWayCached = 0;

		// set values and rescale
		this.lastScaleStroke = scaleStroke;
		this.lastScaleText = scaleText;
		this.lastZoomLevel = zoomLevel;
		
		// prepare optimized theme
		if (generateOptimizedTheme) {
			themeOptimized = new RenderTheme(mapBackground,
					baseStrokeWidth, baseTextSize, locusExtended, fillSeaAreas, scaleLineDyByZoom);
            for (Rule aRulesList : rulesList) {
                Rule rule = aRulesList.createOptimized(zoomLevel, mapCountryCode);
                if (rule != null) {
                    themeOptimized.addRule(rule);
                }
            }
			
			themeOptimized.setLevels(levels);
			themeOptimized.complete();
			themeOptimized.prepareTheme(scaleStroke, scaleText, zoomLevel, false, mapCountryCode);
		} else {
			themeOptimized = null;
			
			// prepare theme
            for (Rule aRulesList : rulesList) {
                aRulesList.prepareRule(
                        scaleStroke * this.baseStrokeWidth,
                        scaleText * this.baseTextSize,
                        zoomLevel);
            }
		}
//		Utils.getHandler().logW("RenderTheme",
//				"prepareTheme(" + scaleStroke + ", " + scaleText + ", " + zoomLevel + ", " + generateOptimizedTheme + "), done");
	}

    /**
     * Check if theme is already prepared and ready tu use.
     * @return <code>true</code> if ready
     */
    public boolean isPrepared() {
        return rulesList != null;
    }
	
	/**
	 * @return the number of distinct drawing levels required by this RenderTheme.
	 */
	public int getLevels() {
		return this.levels;
	}

	/**
	 * @return the map background color of this RenderTheme.
	 * @see Color
	 */
	public int getMapBackground() {
		return this.mapBackground;
	}

	/**
	 * Matches a closed way with the given parameters against this RenderTheme.
	 * 
	 * @param renderCallback
	 *            the callback implementation which will be executed on each match.
	 * @param tags
	 *            the tags of the way.
	 * @param zoomLevel
	 *            the zoom level at which the way should be matched.
	 */
	public void matchClosedWay(RenderCallback renderCallback, Tag[] tags, byte zoomLevel) {
		matchWay(renderCallback, tags, zoomLevel, Closed.YES);
	}

	/**
	 * Matches a linear way with the given parameters against this RenderTheme.
	 * 
	 * @param renderCallback
	 *            the callback implementation which will be executed on each match.
	 * @param tags
	 *            the tags of the way.
	 * @param zoomLevel
	 *            the zoom level at which the way should be matched.
	 */
	public void matchLinearWay(RenderCallback renderCallback, Tag[] tags, byte zoomLevel) {
		matchWay(renderCallback, tags, zoomLevel, Closed.NO);
	}

	/**
	 * Matches a node with the given parameters against this RenderTheme.
	 * 
	 * @param renderCallback
	 *            the callback implementation which will be executed on each match.
	 * @param tags
	 *            the tags of the node.
	 * @param zoomLevel
	 *            the zoom level at which the node should be matched.
	 */
	public void matchNode(RenderCallback renderCallback, Tag[] tags, byte zoomLevel) {
		// use optimized theme
		if (themeOptimized != null) {
			themeOptimized.matchNode(renderCallback, tags, zoomLevel);
			return;
		}

		// match nodes
		if (isPrepared()) {
			for (Rule rule : rulesList) {
				rule.matchNode(renderCallback, tags, zoomLevel);
			}
		} else {
			Utils.getHandler().logD(TAG,
					"theme is not prepared to matchNode");
		}
	}

	private void matchWay(RenderCallback renderCallback, Tag[] tags, byte zoomLevel, Closed closed) {
		if (themeOptimized != null) {
			themeOptimized.matchWay(renderCallback, tags, zoomLevel, closed);
			return;
		}
//        if (countMatchWay % 10000 == 0) {
//            Utils.getHandler().logW("XXX", "count:" + countMatchWay + ", cache:" + countMatchWayCached + ", cacheS:" + mMatchingCache.size());
//        }
//        countMatchWay++;
		
		// check cache
		int matchingKey = calculateMatchKey(tags, zoomLevel, closed);
		List<RenderInstruction> matchingList = this.mMatchingCache.get(matchingKey);
		if (matchingList != null) {
            //countMatchWayCached++;
			// cache hit
			for (int i = 0, n = matchingList.size(); i < n; ++i) {
				matchingList.get(i).renderWay(renderCallback, tags);
			}
			return;
		}

		// cache miss
		if (isPrepared()) {
			matchingList = new ArrayList<>();
			for (Rule aRulesList : rulesList) {
				aRulesList.matchWay(renderCallback, tags, zoomLevel, closed, matchingList);
			}
			this.mMatchingCache.put(matchingKey, matchingList);
		} else {
			Utils.getHandler().logD(TAG,
					"theme is not prepared to matchWay");
		}
	}

	private int calculateMatchKey(Tag[] tags, byte zoomLevel, Closed closed) {
		int result = 7;
		result = 31 * result + ((closed == null) ? 0 : closed.hashCode());
		result = 31 * result + ((tags == null) ? 0 : Arrays.hashCode(tags));
		result = 31 * result + zoomLevel;
		return result;
	}

	/**
	 * Add certain rule to current theme.
	 * @param rule created rule
	 */
	void addRule(Rule rule) {
		this.rulesListLoad.add(rule);
	}

	/**
	 * Complete initializing of theme render and prepare all necessary rules.
	 */
	void complete() {
		// complete list of rules
		Rule[] rules = new Rule[rulesListLoad.size()];
		rulesListLoad.toArray(rules);
		for (Rule rule : rules) {
			rule.onComplete(this);
		}

		// clear temp list
		rulesList = rules;
		rulesListLoad.clear();
		rulesListLoad = null;
	}

	void setLevels(int levels) {
		this.levels = levels;
	}

	// HELPING TOOLS

	public Rule[] getRulesList() {
		return rulesList;
	}
}
