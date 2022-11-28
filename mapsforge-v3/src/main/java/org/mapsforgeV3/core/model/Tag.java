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
package org.mapsforgeV3.core.model;

import com.asamm.locus.mapsforge.utils.Utils;

/**
 * A tag represents an immutable key-value pair.
 */
public class Tag {

    private static final char KEY_VALUE_SEPARATOR = '=';

	/**
	 * The key of this tag.
	 */
	public final int key;
    /**
     * Raw key used for DB poi
     */
    private final String keyText;
	/**
	 * The value of this tag.
	 */
	public final int value;
	/**
	 * Text value used for displaying
	 */
	private final String valueText;

	private final transient int hashCodeValue;

    public Tag(String tag) {
        this(tag, tag.indexOf(KEY_VALUE_SEPARATOR));
    }

    private Tag(String tag, int splitPosition) {
        this(tag.substring(0, splitPosition), tag.substring(splitPosition + 1));
    }

    public Tag(String keyText, String value) {
        this(keyText, Utils.hashTagParameter(keyText), value);
    }

    public Tag(String keyText, int key, String valueText) {
        // prepare key parameter
        this.keyText = keyText;
        this.key = key;

        // prepare value parameter
        this.valueText = valueText;
        this.value = Utils.hashTagParameter(valueText);

        // compute hash
        this.hashCodeValue = calculateHashCode();
    }

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof Tag)) {
			return false;
		}
		Tag other = (Tag) obj;
		if (key == 0 && other.key != 0) {
			return false;
		} else if (key != 0 && key != other.key) {
			return false;
		} else if (value == 0 && other.value != 0) {
			return false;
		} else {
			return value == 0 || value == other.value;
		}
    }

    public String getKeyAsString() {
        return keyText;
    }

    public String getValueAsString() {
        return valueText;
    }

	@Override
	public int hashCode() {
		return this.hashCodeValue;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Tag [key=");
		sb.append(this.key);
		sb.append(", value=");
		sb.append(this.value);
		sb.append("]");
		return sb.toString();
	}

	/**
	 * @return the hash code of this object.
	 */
	private int calculateHashCode() {
		int result = 7;
		result = 31 * result + key;
		result = 31 * result + value;
		return result;
	}}
