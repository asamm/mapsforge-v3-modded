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
package org.mapsforgeV3.map.reader;

import com.asamm.locus.mapsforge.utils.Utils;

import org.mapsforgeV3.core.model.Tag;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Reads from a {@link RandomAccessFile} into a buffer and decodes the data.
 */
public class ReadBuffer {

    // tag for logger
	private static final String TAG = "ReadBuffer";

	/**
	 * Maximum buffer size which is supported by this implementation. Incresed limit is used for
     * new devices with Android 4.0, otherwise we rather display empty tile (mainly large areas of
     * Germany), then crash app on OutOfMemory.
     */
    static final int MAXIMUM_BUFFER_SIZE = 5000000;

    private byte[] bufferData;
    private int bufferPosition;
    private final RandomAccessFile inputFile;

    ReadBuffer(RandomAccessFile inputFile) {
        this.inputFile = inputFile;
    }

    /**
     * Returns one signed byte from the read buffer.
     *
     * @return the byte value.
     */
    public byte readByte() {
        return this.bufferData[this.bufferPosition++];
    }

    /**
     * Reads the given amount of bytes from the file into the read buffer and resets the internal buffer position. If
     * the capacity of the read buffer is too small, a larger one is created automatically.
     *
     * @param length the amount of bytes to read from the file.
     * @return true if the whole data was read successfully, false otherwise.
     * @throws IOException if an error occurs while reading the file.
     */
    public boolean readFromFile(int length) throws IOException {
        // ensure that the read buffer is large enough
        if (this.bufferData == null || this.bufferData.length < length) {
            // ensure that the read buffer is not too large
            if (length > MAXIMUM_BUFFER_SIZE) {
                Utils.getHandler().logW(TAG, "invalid read length: " + length);
                return false;
            }
            this.bufferData = new byte[length];
        }

        // reset the buffer position and read the data into the buffer
        this.bufferPosition = 0;
        return this.inputFile.read(this.bufferData, 0, length) == length;
    }

    /**
     * Converts four bytes from the read buffer to a signed int.
     * <p>
     * The byte order is big-endian.
     *
     * @return the int value.
     */
    public int readInt() {
        this.bufferPosition += 4;
        return Deserializer.getInt(this.bufferData, this.bufferPosition - 4);
    }

    /**
     * Converts eight bytes from the read buffer to a signed long.
     * <p>
     * The byte order is big-endian.
     *
     * @return the long value.
     */
    public long readLong() {
        this.bufferPosition += 8;
        return Deserializer.getLong(this.bufferData, this.bufferPosition - 8);
    }

    /**
     * Converts two bytes from the read buffer to a signed int.
     * <p>
     * The byte order is big-endian.
     *
     * @return the int value.
     */
    public int readShort() {
        this.bufferPosition += 2;
        return Deserializer.getShort(this.bufferData, this.bufferPosition - 2);
    }

    /**
     * Converts a variable amount of bytes from the read buffer to a signed int.
     * <p>
     * The first bit is for continuation info, the other six (last byte) or seven (all other bytes) bits are for data.
     * The second bit in the last byte indicates the sign of the number.
     *
     * @return the int value.
     */
    public int readSignedInt() {
        int variableByteDecode = 0;
        byte variableByteShift = 0;

        // check if the continuation bit is set
        while ((this.bufferData[this.bufferPosition] & 0x80) != 0) {
            variableByteDecode |= (this.bufferData[this.bufferPosition++] & 0x7f) << variableByteShift;
            variableByteShift += 7;
        }

        // read the six data bits from the last byte
        if ((this.bufferData[this.bufferPosition] & 0x40) != 0) {
            // negative
            return -(variableByteDecode | ((this.bufferData[this.bufferPosition++] & 0x3f) << variableByteShift));
        }
        // positive
        return variableByteDecode | ((this.bufferData[this.bufferPosition++] & 0x3f) << variableByteShift);
    }

    /**
     * Converts a variable amount of bytes from the read buffer to an unsigned int.
     * <p>
     * The first bit is for continuation info, the other seven bits are for data.
     *
     * @return the int value.
     */
    public int readUnsignedInt() {
        int variableByteDecode = 0;
        byte variableByteShift = 0;

        // check if the continuation bit is set
        while ((this.bufferData[this.bufferPosition] & 0x80) != 0) {
            variableByteDecode |= (this.bufferData[this.bufferPosition++] & 0x7f) << variableByteShift;
            variableByteShift += 7;
        }

        // read the seven data bits from the last byte
        return variableByteDecode | (this.bufferData[this.bufferPosition++] << variableByteShift);
    }

    /**
     * Decodes a variable amount of bytes from the read buffer to a string.
     *
     * @return the UTF-8 decoded string (may be null).
     */
    public String readUTF8EncodedString() {
        return readUTF8EncodedString(readUnsignedInt());
    }

    /**
     * Decodes the given amount of bytes from the read buffer to a string.
     *
     * @param stringLength the length of the string in bytes.
     * @return the UTF-8 decoded string (may be null).
     */
    public String readUTF8EncodedString(int stringLength) {
        if (stringLength > 0 && this.bufferPosition + stringLength <= this.bufferData.length) {
            this.bufferPosition += stringLength;
            return newStringFromBytes(this.bufferData,
                    this.bufferPosition - stringLength,
                    stringLength);
        }
        Utils.getHandler().logW(TAG, "invalid string length: " + stringLength);
        return null;
    }

    /**
     * Read whole tag object directly from raw data.
     *
     * @return tag object or 'null' if any problem happen
     */
    public Tag readTagObject() {
        String tag = readUTF8EncodedString();
        if (tag != null) {
            return new Tag(tag);
        } else {
            return null;
        }
    }

    /**
     * @return the current buffer position.
     */
    int getBufferPosition() {
        return this.bufferPosition;
    }

    /**
     * @return the current size of the read buffer.
     */
    int getBufferSize() {
        return this.bufferData.length;
    }

    /**
     * Sets the buffer position to the given offset.
     *
     * @param bufferPosition the buffer position.
     */
    void setBufferPosition(int bufferPosition) {
        this.bufferPosition = bufferPosition;
    }

    /**
     * Skips the given number of bytes in the read buffer.
     *
     * @param bytes the number of bytes to skip.
     */
    void skipBytes(int bytes) {
        this.bufferPosition += bytes;
    }

    /**
     * Delete buffer data if arrays is not needed now
     */
    public void deleteBuffer() {
        this.bufferData = null;
    }

    // OPTIMIZED CHARSET GENERATOR
    // copy from StringFactory.java

    private static final char REPLACEMENT_CHAR = (char) 0xfffd;

    public static String newStringFromBytes(byte[] data, int offset, int byteCount) {
        if ((offset | byteCount) < 0 || byteCount > data.length - offset) {
            throw new StringIndexOutOfBoundsException(
                    "Invalid parameters: " + data.length + ", " + offset + ", " + byteCount);
        }

        char[] value;
        int length;

        // We inline UTF-8, ISO-8859-1, and US-ASCII decoders for speed.
        char[] v = new char[byteCount];
        int idx = offset;
        int last = offset + byteCount;
        int s = 0;
        outer:
        while (idx < last) {
            byte b0 = data[idx++];
            if ((b0 & 0x80) == 0) {
                // 0xxxxxxx
                // Range:  U-00000000 - U-0000007F
                int val = b0 & 0xff;
                v[s++] = (char) val;
            } else if (((b0 & 0xe0) == 0xc0) || ((b0 & 0xf0) == 0xe0) ||
                    ((b0 & 0xf8) == 0xf0) || ((b0 & 0xfc) == 0xf8) || ((b0 & 0xfe) == 0xfc)) {
                int utfCount = 1;
                if ((b0 & 0xf0) == 0xe0) utfCount = 2;
                else if ((b0 & 0xf8) == 0xf0) utfCount = 3;
                else if ((b0 & 0xfc) == 0xf8) utfCount = 4;
                else if ((b0 & 0xfe) == 0xfc) utfCount = 5;

                // 110xxxxx (10xxxxxx)+
                // Range:  U-00000080 - U-000007FF (count == 1)
                // Range:  U-00000800 - U-0000FFFF (count == 2)
                // Range:  U-00010000 - U-001FFFFF (count == 3)
                // Range:  U-00200000 - U-03FFFFFF (count == 4)
                // Range:  U-04000000 - U-7FFFFFFF (count == 5)

                if (idx + utfCount > last) {
                    v[s++] = REPLACEMENT_CHAR;
                    continue;
                }

                // Extract usable bits from b0
                int val = b0 & (0x1f >> (utfCount - 1));
                for (int i = 0; i < utfCount; ++i) {
                    byte b = data[idx++];
                    if ((b & 0xc0) != 0x80) {
                        v[s++] = REPLACEMENT_CHAR;
                        idx--; // Put the input char back
                        continue outer;
                    }
                    // Push new bits in from the right side
                    val <<= 6;
                    val |= b & 0x3f;
                }

                // Note: Java allows overlong char
                // specifications To disallow, check that val
                // is greater than or equal to the minimum
                // value for each count:
                //
                // count    min value
                // -----   ----------
                //   1           0x80
                //   2          0x800
                //   3        0x10000
                //   4       0x200000
                //   5      0x4000000

                // Allow surrogate values (0xD800 - 0xDFFF) to
                // be specified using 3-byte UTF values only
                if ((utfCount != 2) && (val >= 0xD800) && (val <= 0xDFFF)) {
                    v[s++] = REPLACEMENT_CHAR;
                    continue;
                }

                // Reject chars greater than the Unicode maximum of U+10FFFF.
                if (val > 0x10FFFF) {
                    v[s++] = REPLACEMENT_CHAR;
                    continue;
                }

                // Encode chars from U+10000 up as surrogate pairs
                if (val < 0x10000) {
                    v[s++] = (char) val;
                } else {
                    int x = val & 0xffff;
                    int u = (val >> 16) & 0x1f;
                    int w = (u - 1) & 0xffff;
                    int hi = 0xd800 | (w << 6) | (x >> 10);
                    int lo = 0xdc00 | (x & 0x3ff);
                    v[s++] = (char) hi;
                    v[s++] = (char) lo;
                }
            } else {
                // Illegal values 0x8*, 0x9*, 0xa*, 0xb*, 0xfd-0xff
                v[s++] = REPLACEMENT_CHAR;
            }
        }

        if (s == byteCount) {
            // We guessed right, so we can use our temporary array as-is.
            value = v;
            length = s;
        } else {
            // Our temporary array was too big, so reallocate and copy.
            value = new char[s];
            length = s;
            System.arraycopy(v, 0, value, 0, s);
        }
        return new String(value, 0, length);
    }

}
