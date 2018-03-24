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

import com.asamm.locus.mapsforge.utils.Utils;

class NegativeMatcher implements AttributeMatcher {
	
	private final int[] mKeys;
	private final int[] mValues;

	NegativeMatcher(List<String> keyList, List<String> valueList) {
		mKeys = Utils.convertListString(keyList);
		mValues = Utils.convertListString(valueList);
	}

	@Override
	public boolean isCoveredBy(AttributeMatcher attributeMatcher) {
		return false;
	}

	@Override
	public boolean matches(int key, int value) {
		return Utils.contains(mValues, value);
	}

	public boolean keyListDoesNotContainKeys(List<Tag> tags) {
		for (int i = 0, n = tags.size(); i < n; ++i) {
			if (Utils.contains(mKeys, tags.get(i).key)) {
				return false;
			}
		}
		return true;
	}
}
