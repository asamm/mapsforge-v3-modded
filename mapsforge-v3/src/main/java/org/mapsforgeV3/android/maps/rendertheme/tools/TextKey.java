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
package org.mapsforgeV3.android.maps.rendertheme.tools;

import com.asamm.locus.mapsforge.utils.Utils;

import org.mapsforgeV3.core.model.Tag;
import org.mapsforgeV3.map.reader.MapDatabase;

import java.util.List;

public final class TextKey {
	
	private static final TextKey TEXT_KEY_ELEVATION = 
			new TextKey(MapDatabase.TAG_KEY_ELE);
	private static final TextKey TEXT_KEY_HOUSENUMBER =
			new TextKey(MapDatabase.TAG_KEY_HOUSE_NUMBER);
	private static final TextKey TEXT_KEY_NAME = 
			new TextKey(MapDatabase.TAG_KEY_NAME);
	private static final TextKey TEXT_KEY_REF = 
			new TextKey(MapDatabase.TAG_KEY_REF);

	public static TextKey getInstance(String key) {
		// check parameter
		if (key == null || key.length() == 0) {
			return null;
		}
		
		// find key
		int keyB = Utils.hashTagParameter(key);
		if (MapDatabase.TAG_KEY_ELE == keyB) {
			return TEXT_KEY_ELEVATION;
		} else if (MapDatabase.TAG_KEY_HOUSE_NUMBER == keyB) {
			return TEXT_KEY_HOUSENUMBER;
		} else if (MapDatabase.TAG_KEY_NAME == keyB) {
			return TEXT_KEY_NAME;
		} else if (MapDatabase.TAG_KEY_REF == keyB) {
			return TEXT_KEY_REF;
		} else {
			throw new IllegalArgumentException("invalid key: " + key);
		}
	}

	private final int mKey;

	private TextKey(int key) {
		this.mKey = key;
	}

    public String getValue(List<Tag> tags, boolean upperCase) {
        String result = null;
        for (int i = 0, n = tags.size(); i < n; ++i) {
            if (mKey == tags.get(i).key) {
                result = tags.get(i).getValueAsString();
                break;
            }
        }

        // check result
        if (result == null) {
            return null;
        }

        // convert to upper case if needed
        if (upperCase) {
            return result.toUpperCase();
        } else {
            return result;
        }
    }
}
