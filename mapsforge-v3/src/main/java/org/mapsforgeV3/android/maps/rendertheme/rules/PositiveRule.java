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
package org.mapsforgeV3.android.maps.rendertheme.rules;

import java.util.List;

import org.mapsforgeV3.core.model.Tag;

class PositiveRule extends Rule {
	
	// matcher by keys
	final AttributeMatcher keyMatcher;
	// matched by values
	final AttributeMatcher valueMatcher;
	
	PositiveRule(ElementMatcher elementMatcher, ClosedMatcher closedMatcher, byte zoomMin, byte zoomMax,
			AttributeMatcher keyMatcher, AttributeMatcher valueMatcher, String category, String[] countryCodes) {
		super(elementMatcher, closedMatcher, zoomMin, zoomMax, category, countryCodes);

		this.keyMatcher = keyMatcher;
		this.valueMatcher = valueMatcher;
	}

	@Override
	boolean matchesNode(List<Tag> tags, byte zoomLevel) {
//Utils.getHandler().logW("PositiveRule", "matchNode(" + tags + ", " + zoomLevel + "), " + 
//		", keyMatcher:" + keyMatcher + 
//		", valueMatcher:" + valueMatcher + 
//		", zooms:" + zoomMin + ", " + zoomMax);
		// basic checks
		if (zoomLevel == Byte.MIN_VALUE) {
			// do not test zoom level
		} else if (this.zoomMin > zoomLevel || 
			this.zoomMax < zoomLevel) {
			return false;
		}
		
		// check element
		if (!this.elementMatcher.matches(Element.NODE)) {
			return false;
		}
		
		// check key/values
		int size = tags.size();
		for (int i = 0; i < size; i++) {
			Tag tag = tags.get(i);
			if (keyMatcher.matches(tag.key, tag.value) && 
					valueMatcher.matches(tag.key, tag.value)) {
				return true;
			}
		}
		return false;
	}

	@Override
	boolean matchesWay(List<Tag> tags, byte zoomLevel, Closed closed) {
//Utils.getHandler().logW("PositiveRule", "matchesWay(" + tags + ", " + zoomLevel + ", " + closed + "), " + 
//		", keyMatcher:" + keyMatcher + 
//		", valueMatcher:" + valueMatcher +
//		", zooms:" + zoomMin + ", " + zoomMax);
		
		// basic checks
		if (this.zoomMin > zoomLevel || 
				this.zoomMax < zoomLevel ||
				!this.elementMatcher.matches(Element.WAY) || 
				!this.closedMatcher.matches(closed)) {
			return false;
		}
		
		// check key/values
		int size = tags.size();
		for (int i = 0; i < size; i++) {
			Tag tag = tags.get(i);
			if (keyMatcher.matches(tag.key, tag.value) &&
					valueMatcher.matches(tag.key, tag.value)) {
				return true;
			}
		}
		return false;
	}
}
