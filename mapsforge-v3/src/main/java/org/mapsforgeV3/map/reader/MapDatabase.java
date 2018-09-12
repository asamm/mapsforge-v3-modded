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
import org.mapsforgeV3.core.model.Tile;
import org.mapsforgeV3.core.util.MercatorProjection;
import org.mapsforgeV3.map.layer.renderer.DatabaseRenderer;
import org.mapsforgeV3.map.layer.renderer.DatabaseRenderer.TileRenderer;
import org.mapsforgeV3.map.reader.header.FileOpenResult;
import org.mapsforgeV3.map.reader.header.MapFileHeader;
import org.mapsforgeV3.map.reader.header.MapFileInfo;
import org.mapsforgeV3.map.reader.header.SubFileParameter;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * A class for reading binary map files.
 * <p>
 * This class is not thread-safe. Each thread should use its own instance.
 *
 * @see <a href="https://code.google.com/p/mapsforge/wiki/SpecificationBinaryMapFile">Specification</a>
 */
public class MapDatabase {

    // tag for logger
    private static final String TAG = "MapDatabase";

    /**
     * Bitmask to extract the block offset from an index entry.
     */
    private static final long BITMASK_INDEX_OFFSET = 0x7FFFFFFFFFL;

    /**
     * Bitmask to extract the water information from an index entry.
     */
    private static final long BITMASK_INDEX_WATER = 0x8000000000L;

    /**
     * Debug message prefix for the block signature.
     */
    private static final String DEBUG_SIGNATURE_BLOCK = "block signature: ";

    /**
     * Debug message prefix for the POI signature.
     */
    private static final String DEBUG_SIGNATURE_POI = "POI signature: ";

    /**
     * Debug message prefix for the way signature.
     */
    private static final String DEBUG_SIGNATURE_WAY = "way signature: ";

    /**
     * Amount of cache blocks that the index cache should store.
     */
    private static final int INDEX_CACHE_SIZE = 64;

    /**
     * Error message for an invalid first way offset.
     */
    private static final String INVALID_FIRST_WAY_OFFSET = "invalid first way offset: ";

    /**
     * Maximum number of map objects in the zoom table which is considered as valid.
     */
    private static final int MAXIMUM_ZOOM_TABLE_OBJECTS = 65536;

    /**
     * Bitmask for the optional POI feature "elevation".
     */
    private static final int POI_FEATURE_ELEVATION = 0x20;

    /**
     * Bitmask for the optional POI feature "house number".
     */
    private static final int POI_FEATURE_HOUSE_NUMBER = 0x40;

    /**
     * Bitmask for the optional POI feature "name".
     */
    private static final int POI_FEATURE_NAME = 0x80;

    /**
     * Bitmask for the POI layer.
     */
    private static final int POI_LAYER_BITMASK = 0xf0;

    /**
     * Bit shift for calculating the POI layer.
     */
    private static final int POI_LAYER_SHIFT = 4;

    /**
     * Bitmask for the number of POI tags.
     */
    private static final int POI_NUMBER_OF_TAGS_BITMASK = 0x0f;

    private static final String READ_ONLY_MODE = "r";

    /**
     * Length of the debug signature at the beginning of each block.
     */
    private static final byte SIGNATURE_LENGTH_BLOCK = 32;

    /**
     * Length of the debug signature at the beginning of each POI.
     */
    private static final byte SIGNATURE_LENGTH_POI = 32;

    /**
     * Length of the debug signature at the beginning of each way.
     */
    private static final byte SIGNATURE_LENGTH_WAY = 32;

    /**
     * The key in raw form of the elevation OpenStreetMap tag.
     */
    private static final String TAG_KEY_ELE_RAW = "ele";
    /**
     * The key of the elevation OpenStreetMap tag.
     */
    public static final int TAG_KEY_ELE =
            Utils.hashTagParameter(TAG_KEY_ELE_RAW);
    /**
     * The key in raw form of the elevation OpenStreetMap tag.
     */
    private static final String TAG_KEY_HOUSE_NUMBER_RAW = "addr:housenumber";
    /**
     * The key of the house number OpenStreetMap tag.
     */
    public static final int TAG_KEY_HOUSE_NUMBER =
            Utils.hashTagParameter(TAG_KEY_HOUSE_NUMBER_RAW);
    /**
     * The key in raw form of the elevation OpenStreetMap tag.
     */
    private static final String TAG_KEY_NAME_RAW = "name";
    /**
     * The key of the name OpenStreetMap tag.
     */
    public static final int TAG_KEY_NAME =
            Utils.hashTagParameter(TAG_KEY_NAME_RAW);
    /**
     * The key in raw form of the elevation OpenStreetMap tag.
     */
    private static final String TAG_KEY_REF_RAW = "ref";
    /**
     * The key of the reference OpenStreetMap tag.
     */
    public static final int TAG_KEY_REF =
            Utils.hashTagParameter(TAG_KEY_REF_RAW);

    /**
     * Bitmask for the optional way data blocks byte.
     */
    private static final int WAY_FEATURE_DATA_BLOCKS_BYTE = 0x08;

    /**
     * Bitmask for the optional way double delta encoding.
     */
    private static final int WAY_FEATURE_DOUBLE_DELTA_ENCODING = 0x04;

    /**
     * Bitmask for the optional way feature "house number".
     */
    private static final int WAY_FEATURE_HOUSE_NUMBER = 0x40;

    /**
     * Bitmask for the optional way feature "label position".
     */
    private static final int WAY_FEATURE_LABEL_POSITION = 0x10;

    /**
     * Bitmask for the optional way feature "name".
     */
    private static final int WAY_FEATURE_NAME = 0x80;

    /**
     * Bitmask for the optional way feature "reference".
     */
    private static final int WAY_FEATURE_REF = 0x20;

    /**
     * Bitmask for the way layer.
     */
    private static final int WAY_LAYER_BITMASK = 0xf0;

    /**
     * Bit shift for calculating the way layer.
     */
    private static final int WAY_LAYER_SHIFT = 4;

    /**
     * Bitmask for the number of way tags.
     */
    private static final int WAY_NUMBER_OF_TAGS_BITMASK = 0x0f;

    // main file itself
    private File mapFile;
    // size of file
    private long fileSize;

    // header for map file
    private MapFileHeader mapFileHeader;
    // flag if header is debug
    private boolean mapFileDebug;

    // stream for data reading
    private RandomAccessFile inputFile;
    // improved reader for map data
    private ReadBuffer readBuffer;

    // cache for database indexes
    private IndexCache databaseIndexCache;

    // private variables
    private String signatureBlock;
    private String signatureWay;
    private int tileLatitude;
    private int tileLongitude;
    // counter for checking of empty maps
    private int emptyTilesRendered;

    // counters for read data
    private int readNodes;
    private int readWays;

    // preferred language when extracting labels from this data store. The actual implementation is up to the
    // concrete implementation, which can also simply ignore this setting.
    private String mPreferredLanguage;
    // defined map country code
    private String mCountryCode;
    // POI database version, or '0' if not exists
    private int mDbPoiVersion;
    // container for loaded tags
    private List<Tag> tags = new ArrayList<>();

    /**
     * Base constructor.
     */
    public MapDatabase() {
        mPreferredLanguage = "";
        mCountryCode = "";
        mDbPoiVersion = 0;
    }

    /**
     * Closes the map file and destroys all internal caches. Has no effect if no map file is currently opened.
     */
    public void closeFile() {
        try {
            this.mapFileHeader = null;

            if (this.databaseIndexCache != null) {
                this.databaseIndexCache.destroy();
                this.databaseIndexCache = null;
            }

            if (this.inputFile != null) {
                this.inputFile.close();
                this.inputFile = null;
            }

            this.readBuffer = null;
        } catch (IOException e) {
            Utils.getHandler().logE(TAG, "closeFile()", e);
        }
    }

    public void clearCache() {
        if (readBuffer != null) {
            readBuffer.deleteBuffer();
        }
        if (databaseIndexCache != null) {
            databaseIndexCache.destroy();
        }
    }

    /**
     * @return the metadata for the current map file.
     * @throws IllegalStateException if no map is currently opened.
     */
    public MapFileInfo getMapFileInfo() {
        if (this.mapFileHeader == null) {
            throw new IllegalStateException("no map file is currently opened");
        }
        return this.mapFileHeader.getMapFileInfo();
    }

    public File getMapFile() {
        return mapFile;
    }

    /**
     * Check if file is still required. It means that it was used for at least some rendered
     * tiles during last usage.
     *
     * @return {@code true} if file is still needed for rendering
     */
    public boolean isFileRequired() {
        return emptyTilesRendered < (Utils.getHandler().getScreenCategory() + 1) * 10;
    }

    // LANGUAGE

    /**
     * Set preferred language code for this map
     *
     * @param lang language code
     */
    public void setPreferredLanguage(String lang) {
        mPreferredLanguage = lang != null ? lang.trim() : null;
        if (mPreferredLanguage != null && mPreferredLanguage.length() == 0) {
            mPreferredLanguage = null;
        }
    }

    /**
     * Extracts substring of preferred language from multilingual string.<br/>
     * Example multilingual string: "Base\ren\bEnglish\rjp\bJapan\rzh_py\bPin-yin".
     * <p/>
     * Use '\r' delimiter among names and '\b' delimiter between each language and name.
     */
    private String extract(String s) {
        // check string
        if (s == null || s.trim().isEmpty()) {
            return null;
        }

        // prepare data
        String lang = mPreferredLanguage;
        String[] langNames = s.split("\r", 0);

        // handle preferred language
        if (lang == null) {
            String[] langName = langNames[0].split("\b", 0);
            if (langName.length == 1) {
                return langName[0];
            }

            // set english as default
            lang = "en";
        }

        // check possible languages
        String fallback = null;
        for (int i = 1; i < langNames.length; i++) {
            String[] langName = langNames[i].split("\b", 0);
            if (langName.length != 2) {
                continue;
            }

            // Perfect match
            if (langName[0].equalsIgnoreCase(lang)) {
                return langName[1];
            }

            // Fall back to base, e.g. zh-min-lan -> zh
            if (fallback == null &&
                    !langName[0].contains("-") &&
                    (lang.contains("-") || lang.contains("_")) &&
                    lang.toLowerCase(Locale.ENGLISH).startsWith(langName[0].toLowerCase(Locale.ENGLISH))) {
                fallback = langName[1];
            }
        }

        // return best default value
        if (fallback != null && fallback.length() > 0) {
            return fallback;
        }
        String[] langName = langNames[0].split("\b", 0);
        if (langName.length == 2) {
            return langName[1];
        } else {
            return langNames[0];
        }
    }

    // COUNTRY CODE

    /**
     * Get defined country code of current map.
     *
     * @return map country code
     */
    public String getCountryCode() {
        return mCountryCode;
    }

    /**
     * Set country code to current map.
     *
     * @param countryCode map country code
     */
    public void setCountryCode(String countryCode) {
        if (countryCode == null) {
            countryCode = "";
        }
        mCountryCode = countryCode.trim().toLowerCase();
    }

    // DB POI VERSION

    /**
     * Get version of DB POI attached to this database.
     *
     * @return DB poi version
     */
    public int getDbPoiVersion() {
        return mDbPoiVersion;
    }

    /**
     * Set new version of DB poi.
     *
     * @param dbPoiVersion DB poi version
     */
    public void setDbPoiVersion(int dbPoiVersion) {
        mDbPoiVersion = dbPoiVersion;
    }


    // OPEN METHODS

    /**
     * @return true if a map file is currently opened, false otherwise.
     */
    public boolean hasOpenFile() {
        return this.inputFile != null;
    }

    /**
     * Opens the given map file, reads its header data and validates them.
     *
     * @param mapFile the map file.
     * @return a FileOpenResult containing an error message in case of a failure.
     * @throws IllegalArgumentException if the given map file is null.
     */
    public FileOpenResult openFile(File mapFile) {
        try {
            if (mapFile == null) {
                throw new IllegalArgumentException("mapFile must not be null");
            }

            // make sure to close any previously opened file first
            closeFile();

            // check if the file exists and is readable
            this.mapFile = mapFile;
            if (!mapFile.exists()) {
                return new FileOpenResult("file does not exist: " + mapFile);
            } else if (!mapFile.isFile()) {
                return new FileOpenResult("not a file: " + mapFile);
            } else if (!mapFile.canRead()) {
                return new FileOpenResult("cannot read file: " + mapFile);
            }

            // open the file in read only mode
            this.inputFile = new RandomAccessFile(mapFile, READ_ONLY_MODE);
            this.fileSize = this.inputFile.length();

            this.readBuffer = new ReadBuffer(this.inputFile);
            this.mapFileHeader = new MapFileHeader();
            this.mapFileHeader.readHeader(this.readBuffer, this.fileSize);
            this.mapFileDebug = this.mapFileHeader.getMapFileInfo().debugFile;
            return FileOpenResult.SUCCESS;
        } catch (Exception e) {
            Utils.getHandler().logE(TAG, "openFile(" + mapFile + ")", e);
            // make sure that the file is closed
            closeFile();
            return new FileOpenResult(e.getMessage());
        }
    }

    /**
     * Reads all map data for the area covered by the given tile at the tile zoom level.
     *
     * @param tile defines area and zoom level of read map data.
     * @return the read map data.
     */
    public void readMapData(Tile tile, TileRenderer tr) {
        try {
            // prepare cache
            if (this.databaseIndexCache == null) {
                this.databaseIndexCache = new IndexCache(this.inputFile, INDEX_CACHE_SIZE);
            }

            // define parameters
            QueryParameters queryParameters = new QueryParameters();
            queryParameters.queryZoomLevel = this.mapFileHeader.getQueryZoomLevel(tile.zoomLevel);

            // get and check the sub-file for the query zoom level
            SubFileParameter subFileParameter = this.mapFileHeader.
                    getSubFileParameter(queryParameters.queryZoomLevel);
            if (subFileParameter == null) {
                Utils.getHandler().logW(TAG, "no sub-file for zoom level: " + queryParameters.queryZoomLevel);
                return;
            }

            // calculate tiles
            QueryCalculations.calculateBaseTiles(queryParameters, tile, subFileParameter);
            QueryCalculations.calculateBlocks(queryParameters, subFileParameter);

            // read data
            readNodes = 0;
            readWays = 0;
            processBlocks(queryParameters, subFileParameter, tr);
        } catch (IOException e) {
            Utils.getHandler().logE(TAG, "readMapData(" + tile + ")", e);
        } finally {
            if (readNodes == 0 && readWays == 0) {
                emptyTilesRendered++;
            } else {
                emptyTilesRendered = 0;
            }
        }
    }

    /**
     * Logs the debug signatures of the current way and block.
     */
    private void logDebugSignatures() {
        if (mapFileDebug) {
            Utils.getHandler().logW(TAG, DEBUG_SIGNATURE_WAY + this.signatureWay);
            Utils.getHandler().logW(TAG, DEBUG_SIGNATURE_BLOCK + this.signatureBlock);
        }
    }

    // HANDLE DATA

    private void processBlocks(QueryParameters queryParameters,
            SubFileParameter subFileParameter,
            TileRenderer tr) throws IOException {
        boolean queryIsWater = true;
        boolean queryReadWaterInfo = false;

        // read and process all blocks from top to bottom and from left to right
        for (long row = queryParameters.fromBlockY; row <= queryParameters.toBlockY; ++row) {
            for (long column = queryParameters.fromBlockX; column <= queryParameters.toBlockX; ++column) {

                // calculate the actual block number of the needed block in the file
                long blockNumber = row * subFileParameter.blocksWidth + column;

                // get the current index entry
                long currentBlockIndexEntry = this.databaseIndexCache.getIndexEntry(subFileParameter, blockNumber);

                // check if the current query would still return a water tile
                if (queryIsWater) {
                    // check the water flag of the current block in its index entry
                    queryIsWater &= (currentBlockIndexEntry & BITMASK_INDEX_WATER) != 0;
                    queryReadWaterInfo = true;
                }

                // get and check the current block pointer
                long currentBlockPointer = currentBlockIndexEntry & BITMASK_INDEX_OFFSET;
                if (currentBlockPointer < 1 || currentBlockPointer > subFileParameter.subFileSize) {
                    Utils.getHandler().logW(TAG, "invalid current block pointer: " + currentBlockPointer);
                    Utils.getHandler().logW(TAG, "subFileSize: " + subFileParameter.subFileSize);
                    return;
                }

                long nextBlockPointer;
                // check if the current block is the last block in the file
                if (blockNumber + 1 == subFileParameter.numberOfBlocks) {
                    // set the next block pointer to the end of the file
                    nextBlockPointer = subFileParameter.subFileSize;
                } else {
                    // get and check the next block pointer
                    nextBlockPointer = this.databaseIndexCache.getIndexEntry(subFileParameter, blockNumber + 1)
                            & BITMASK_INDEX_OFFSET;
                    if (nextBlockPointer < 1 || nextBlockPointer > subFileParameter.subFileSize) {
                        Utils.getHandler().logW(TAG, "invalid next block pointer: " + nextBlockPointer);
                        Utils.getHandler().logW(TAG, "sub-file size: " + subFileParameter.subFileSize);
                        return;
                    }
                }

                // calculate the size of the current block
                int currentBlockSize = (int) (nextBlockPointer - currentBlockPointer);
                if (currentBlockSize < 0) {
                    Utils.getHandler().logW(TAG, "current block size must not be negative: " + currentBlockSize);
                    return;
                } else if (currentBlockSize == 0) {
                    // the current block is empty, continue with the next block
                    continue;
                } else if (currentBlockSize > ReadBuffer.MAXIMUM_BUFFER_SIZE) {
                    // the current block is too large, continue with the next block
                    Utils.getHandler().logW(TAG, "current block size too large: " + currentBlockSize);
                    continue;
                } else if (currentBlockPointer + currentBlockSize > this.fileSize) {
                    Utils.getHandler().logW(TAG, "current block largher than file size: " + currentBlockSize);
                    return;
                }

                // seek to the current block in the map file
                this.inputFile.seek(subFileParameter.startAddress + currentBlockPointer);

                // read the current block into the buffer
                if (!this.readBuffer.readFromFile(currentBlockSize)) {
                    // skip the current block
                    Utils.getHandler().logW(TAG, "reading current block has failed: " + currentBlockSize);
                    return;
                }

                // calculate the top-left coordinates of the underlying tile
                double tileLatitudeDeg = MercatorProjection.tileYToLatitude(
                        subFileParameter.boundaryTileTop + row,
                        subFileParameter.baseZoomLevel,
                        subFileParameter.tilePixelSize);
                double tileLongitudeDeg = MercatorProjection.tileXToLongitude(
                        subFileParameter.boundaryTileLeft + column,
                        subFileParameter.baseZoomLevel,
                        subFileParameter.tilePixelSize);
                this.tileLatitude = (int) (tileLatitudeDeg * 1000000);
                this.tileLongitude = (int) (tileLongitudeDeg * 1000000);

                try {
                    processBlock(queryParameters, subFileParameter, tr);
                } catch (ArrayIndexOutOfBoundsException e) {
                    Utils.getHandler().logE(TAG, "", e);
                }
            }
        }

        // the query is finished, was the water flag set for all blocks?
        if (queryIsWater && queryReadWaterInfo) {
            tr.isWater = true;
        }

    }

    private void processBlock(QueryParameters queryParameters, SubFileParameter subFileParameter,
            TileRenderer tr) {
        if (!processBlockSignature()) {
            return;
        }

        int[][] zoomTable = readZoomTable(subFileParameter);
        if (zoomTable == null) {
            return;
        }
        int zoomTableRow = queryParameters.queryZoomLevel - subFileParameter.zoomLevelMin;
        int poisOnQueryZoomLevel = zoomTable[zoomTableRow][0];
        int waysOnQueryZoomLevel = zoomTable[zoomTableRow][1];

        // get the relative offset to the first stored way in the block
        int firstWayOffset = this.readBuffer.readUnsignedInt();
        if (firstWayOffset < 0) {
            Utils.getHandler().logW(TAG, INVALID_FIRST_WAY_OFFSET + firstWayOffset);
            if (mapFileDebug) {
                Utils.getHandler().logW(TAG, DEBUG_SIGNATURE_BLOCK + this.signatureBlock);
            }
            return;
        }

        // add the current buffer position to the relative first way offset
        firstWayOffset += this.readBuffer.getBufferPosition();
        if (firstWayOffset > this.readBuffer.getBufferSize()) {
            Utils.getHandler().logW(TAG, INVALID_FIRST_WAY_OFFSET + firstWayOffset);
            if (mapFileDebug) {
                Utils.getHandler().logW(TAG, DEBUG_SIGNATURE_BLOCK + this.signatureBlock);
            }
            return;
        }

        // attempt to read points
        boolean resPois = processPOIs(poisOnQueryZoomLevel, tr);
        if (!resPois) {
            return;
        }
        readNodes = poisOnQueryZoomLevel;

        // finished reading POIs, check if the current buffer position is valid
        if (this.readBuffer.getBufferPosition() > firstWayOffset) {
            Utils.getHandler().logW(TAG, "invalid buffer position: " + this.readBuffer.getBufferPosition());
            if (mapFileDebug) {
                Utils.getHandler().logW(TAG, DEBUG_SIGNATURE_BLOCK + this.signatureBlock);
            }
            return;
        }

        // move the pointer to the first way
        this.readBuffer.setBufferPosition(firstWayOffset);

        // finally handle a ways
        int resWays = processWays(queryParameters, waysOnQueryZoomLevel, tr);
        if (resWays < 0) {
            return;
        }
        readWays = resWays;
    }

    /**
     * Processes the block signature, if present.
     *
     * @return true if the block signature could be processed successfully, false otherwise.
     */
    private boolean processBlockSignature() {
        if (mapFileDebug) {
            // get and check the block signature
            this.signatureBlock = this.readBuffer.readUTF8EncodedString(SIGNATURE_LENGTH_BLOCK);
            if (!this.signatureBlock.startsWith("###TileStart")) {
                Utils.getHandler().logW(TAG, "invalid block signature: " + this.signatureBlock);
                return false;
            }
        }
        return true;
    }

    private int[][] readZoomTable(SubFileParameter subFileParameter) {
        int rows = subFileParameter.zoomLevelMax - subFileParameter.zoomLevelMin + 1;
        int[][] zoomTable = new int[rows][2];
//Logger.d(TAG, "readZoomTable(), rows:" + rows);
        int cumulatedNumberOfPois = 0;
        int cumulatedNumberOfWays = 0;

        for (int row = 0; row < rows; ++row) {
            cumulatedNumberOfPois += this.readBuffer.readUnsignedInt();
            cumulatedNumberOfWays += this.readBuffer.readUnsignedInt();

//			if (cumulatedNumberOfPois < 0 || cumulatedNumberOfPois > MAXIMUM_ZOOM_TABLE_OBJECTS) {
//				Utils.getHandler().logW(TAG, "invalid cumulated number of POIs in row " + row + ' ' + cumulatedNumberOfPois);
//				if (mapFileDebug) {
//					Utils.getHandler().logW(TAG, DEBUG_SIGNATURE_BLOCK + this.signatureBlock);
//				}
//				return null;
//			} else if (cumulatedNumberOfWays < 0 || cumulatedNumberOfWays > MAXIMUM_ZOOM_TABLE_OBJECTS) {
//				Utils.getHandler().logW(TAG, "invalid cumulated number of ways in row " + row + ' ' + cumulatedNumberOfWays);
//				if (mapFileDebug) {
//					Utils.getHandler().logW(TAG, DEBUG_SIGNATURE_BLOCK + this.signatureBlock);
//				}
//				return null;
//			}

            zoomTable[row][0] = cumulatedNumberOfPois;
            zoomTable[row][1] = cumulatedNumberOfWays;
        }

        return zoomTable;
    }

    // HANDLE POIS

    private boolean processPOIs(int numberOfPois, TileRenderer tr) {
        Tag[] poiTags = this.mapFileHeader.getMapFileInfo().poiTags;
        String signaturePoi = null;
        for (int elementCounter = numberOfPois; elementCounter != 0; --elementCounter) {
            if (mapFileDebug) {
                // get and check the POI signature
                signaturePoi = this.readBuffer.readUTF8EncodedString(SIGNATURE_LENGTH_POI);
                if (!signaturePoi.startsWith("***POIStart")) {
                    Utils.getHandler().logW(TAG, "invalid POI signature: " + signaturePoi);
                    Utils.getHandler().logW(TAG, DEBUG_SIGNATURE_BLOCK + this.signatureBlock);
                    return false;
                }
            }

            // get the POI latitude offset (VBE-S)
            int latitude = this.tileLatitude + this.readBuffer.readSignedInt();

            // get the POI longitude offset (VBE-S)
            int longitude = this.tileLongitude + this.readBuffer.readSignedInt();

            // get the special byte which encodes multiple flags
            byte specialByte = this.readBuffer.readByte();

            // bit 1-4 represent the layer
            byte layer = (byte) ((specialByte & POI_LAYER_BITMASK) >>> POI_LAYER_SHIFT);
            // bit 5-8 represent the number of tag IDs
            byte numberOfTags = (byte) (specialByte & POI_NUMBER_OF_TAGS_BITMASK);

            // get the tag IDs (VBE-U)
            tags.clear();
            for (byte tagIndex = numberOfTags; tagIndex != 0; --tagIndex) {
                int tagId = this.readBuffer.readUnsignedInt();
                if (tagId < 0 || tagId >= poiTags.length) {
                    Utils.getHandler().logW(TAG, "invalid POI tag ID: " + tagId);
                    if (mapFileDebug) {
                        Utils.getHandler().logW(TAG, DEBUG_SIGNATURE_POI + signaturePoi);
                        Utils.getHandler().logW(TAG, DEBUG_SIGNATURE_BLOCK + this.signatureBlock);
                    }
                    return false;
                }
                tags.add(poiTags[tagId]);
            }

            // get the feature bitmask (1 byte)
            byte featureByte = this.readBuffer.readByte();

            // bit 1-3 enable optional features
            boolean featureName = (featureByte & POI_FEATURE_NAME) != 0;
            boolean featureHouseNumber = (featureByte & POI_FEATURE_HOUSE_NUMBER) != 0;
            boolean featureElevation = (featureByte & POI_FEATURE_ELEVATION) != 0;

            // check if the POI has a name
            if (featureName) {
                String name = extract(readBuffer.readUTF8EncodedString());
                if (name != null) {
                    tags.add(new Tag(TAG_KEY_NAME_RAW, TAG_KEY_NAME, name));
                }
            }

            // check if the POI has a house number
            if (featureHouseNumber) {
                tags.add(new Tag(TAG_KEY_HOUSE_NUMBER_RAW, TAG_KEY_HOUSE_NUMBER,
                        readBuffer.readUTF8EncodedString()));
            }

            // check if the POI has an elevation
            if (featureElevation) {
                tags.add(new Tag(TAG_KEY_ELE_RAW, TAG_KEY_ELE,
                        Integer.toString(readBuffer.readSignedInt())));
            }
            Tag[] tagsA = new Tag[tags.size()];
            tags.toArray(tagsA);
            tr.renderNode(layer, tagsA, latitude, longitude);
        }
        return true;
    }

    // HANDLE WAYS

    // comparator for tags
    private static Comparator<Tag> tagsComparator
            = (lhs, rhs) -> Integer.compare(lhs.hashCode(), rhs.hashCode());

    /**
     * Process all ways for a block.
     *
     * @param queryParameters ?
     * @param numberOfWays    number of ways in block
     * @param tr              rendered handler
     * @return number of valid ways in block or '-1' in case of any problem
     */
    private int processWays(QueryParameters queryParameters, int numberOfWays, TileRenderer tr) {
        Tag[] wayTags = this.mapFileHeader.getMapFileInfo().wayTags;

        // container for a way
        int validWays = 0;
        Way way = new Way();

        // handle all ways
        for (int elementCounter = numberOfWays; elementCounter != 0; --elementCounter) {
            if (mapFileDebug) {
                // get and check the way signature
                this.signatureWay = this.readBuffer.readUTF8EncodedString(SIGNATURE_LENGTH_WAY);
                if (!this.signatureWay.startsWith("---WayStart")) {
                    Utils.getHandler().logW(TAG, "invalid way signature: " + this.signatureWay);
                    Utils.getHandler().logW(TAG, DEBUG_SIGNATURE_BLOCK + this.signatureBlock);
                    return -1;
                }
            }

            // prepare container
            way.prepareToNewStep();
            tags.clear();

            // get the size of the way (VBE-U)
            int wayDataSize = this.readBuffer.readUnsignedInt();
            if (wayDataSize < 0) {
                Utils.getHandler().logW(TAG, "invalid way data size: " + wayDataSize +
                        ", amount:" + numberOfWays + ", index:" + elementCounter);
                if (mapFileDebug) {
                    Utils.getHandler().logW(TAG, DEBUG_SIGNATURE_BLOCK + this.signatureBlock);
                }
                return -1;
            }

            if (queryParameters.useTileBitmask) {
                // get the way tile bitmask (2 bytes)
                int tileBitmask = this.readBuffer.readShort();
                // check if the way is inside the requested tile
                if ((queryParameters.queryTileBitmask & tileBitmask) == 0) {
                    // skip the rest of the way and continue with the next way
                    this.readBuffer.skipBytes(wayDataSize - 2);
                    continue;
                }
            } else {
                // ignore the way tile bitmask (2 bytes)
                this.readBuffer.skipBytes(2);
            }

            // get the special byte which encodes multiple flags
            byte specialByte = this.readBuffer.readByte();

            // bit 1-4 represent the layer
            byte layer = (byte) ((specialByte & WAY_LAYER_BITMASK) >>> WAY_LAYER_SHIFT);

            // bit 5-8 represent the number of tag IDs
            byte numberOfTags = (byte) (specialByte & WAY_NUMBER_OF_TAGS_BITMASK);

            // add all tags to way
            for (byte tagIndex = numberOfTags; tagIndex != 0; --tagIndex) {
                int tagId = this.readBuffer.readUnsignedInt();
                if (tagId < 0 || tagId >= wayTags.length) {
                    Utils.getHandler().logW(TAG, "invalid way tag ID: " + tagId);
                    logDebugSignatures();
                    return -1;
                }
                tags.add(wayTags[tagId]);
            }

            // sort tags so comparing of ways match
            Collections.sort(tags, tagsComparator);

            // get the feature bitmask (1 byte)
            byte featureByte = this.readBuffer.readByte();

            // bit 1-6 enable optional features
            boolean featureName = (featureByte & WAY_FEATURE_NAME) != 0;
            boolean featureHouseNumber = (featureByte & WAY_FEATURE_HOUSE_NUMBER) != 0;
            boolean featureRef = (featureByte & WAY_FEATURE_REF) != 0;
            boolean featureLabelPosition = (featureByte & WAY_FEATURE_LABEL_POSITION) != 0;
            boolean featureWayDataBlocksByte = (featureByte & WAY_FEATURE_DATA_BLOCKS_BYTE) != 0;
            boolean featureWayDoubleDeltaEncoding = (featureByte & WAY_FEATURE_DOUBLE_DELTA_ENCODING) != 0;

            // check if the way has a name
            if (featureName) {
                String name = extract(readBuffer.readUTF8EncodedString());
                if (name != null) {
                    tags.add(new Tag(TAG_KEY_NAME_RAW, TAG_KEY_NAME, name));
                }
            }

            // check if the way has a house number
            if (featureHouseNumber) {
                tags.add(new Tag(TAG_KEY_HOUSE_NUMBER_RAW, TAG_KEY_HOUSE_NUMBER,
                        readBuffer.readUTF8EncodedString()));
            }

            // check if the way has a reference
            if (featureRef) {
                tags.add(new Tag(TAG_KEY_REF_RAW, TAG_KEY_REF,
                        readBuffer.readUTF8EncodedString()));
            }

            int latitude = 0;
            int longitude = 0;
            if (featureLabelPosition) {
                latitude = this.tileLatitude + this.readBuffer.readSignedInt();
                longitude = this.tileLongitude + this.readBuffer.readSignedInt();
            }

            int wayDataBlocks = readOptionalWayDataBlocksByte(featureWayDataBlocksByte);
            if (wayDataBlocks < 1) {
                Utils.getHandler().logW(TAG, "invalid number of way data blocks: " + wayDataBlocks);
                logDebugSignatures();
                return -1;
            }

            // handle way
            way.set(layer, latitude, longitude);
            for (int wayDataBlock = 0; wayDataBlock < wayDataBlocks; ++wayDataBlock) {
                way.wayNodes = processWayDataBlock(way, featureWayDoubleDeltaEncoding);
                if (way.wayNodes == null || way.wayNodes.length == 0) {
                    return -1;
                }

                // check if way is just a empty background
                if (tags.size() == 1) {
                    Tag tag = tags.get(0);
                    way.isFillBackground =
                            tag.equals(DatabaseRenderer.TAG_NATURAL_WATER) ||
                                    tag.equals(DatabaseRenderer.TAG_NATURAL_NOSEA) ||
                                    tag.equals(DatabaseRenderer.TAG_NATURAL_SEA);
                }

                // finally render way
                way.tags = new Tag[tags.size()];
                tags.toArray(way.tags);
                tr.renderWay(way);

                // add valid way to counter
                if (!way.isFillBackground) {
                    validWays += 1;
                }
            }
        }
        return validWays;
    }

    private int readOptionalWayDataBlocksByte(boolean featureWayDataBlocksByte) {
        if (featureWayDataBlocksByte) {
            // get and check the number of way data blocks (VBE-U)
            return this.readBuffer.readUnsignedInt();
        }
        // only one way data block exists
        return 1;
    }

    private float[][] processWayDataBlock(Way way, boolean doubleDeltaEncoding) {
        // prepare way
        way.clearNodesData();

        // get and check the number of way coordinate blocks (VBE-U)
        int numberOfWayCoordinateBlocks = this.readBuffer.readUnsignedInt();
        if (numberOfWayCoordinateBlocks < 1 || numberOfWayCoordinateBlocks > Short.MAX_VALUE) {
            Utils.getHandler().logW(TAG, "invalid number of way coordinate blocks: " + numberOfWayCoordinateBlocks);
            logDebugSignatures();
            return null;
        }

        // create the array which will store the different way coordinate blocks
        float[][] wayCoordinates = new float[numberOfWayCoordinateBlocks][];

        // read the way coordinate blocks
        for (int cooBlock = 0; cooBlock < numberOfWayCoordinateBlocks; ++cooBlock) {
            // get and check the number of way nodes (VBE-U)
            int numberOfWayNodes = this.readBuffer.readUnsignedInt();
            if (numberOfWayNodes < 2 || numberOfWayNodes > Short.MAX_VALUE) {
                Utils.getHandler().logW(TAG, "invalid number of way nodes: " + numberOfWayNodes +
                        ", cooBlock:" + cooBlock + ", total:" + numberOfWayCoordinateBlocks);
                logDebugSignatures();
                // returning null here will actually leave the tile blank as the
                // position on the ReadBuffer will not be advanced correctly. However,
                // it will not crash the app.
                return null;
            }

            // each way node consists of latitude and longitude
            int wayNodesSequenceLength = numberOfWayNodes * 2;

            // create the array which will store the current way segment
            float[] waySegment = new float[wayNodesSequenceLength];

            if (doubleDeltaEncoding) {
                decodeWayNodesDoubleDelta(way, waySegment);
            } else {
                decodeWayNodesSingleDelta(way, waySegment);
            }

            wayCoordinates[cooBlock] = waySegment;
        }

        return wayCoordinates;
    }

    private void decodeWayNodesSingleDelta(Way way, float[] waySegment) {
        // get the first way node latitude single-delta offset (VBE-S)
        int wayNodeLatitude = this.tileLatitude + this.readBuffer.readSignedInt();

        // get the first way node longitude single-delta offset (VBE-S)
        int wayNodeLongitude = this.tileLongitude + this.readBuffer.readSignedInt();

        // store the first way node
        waySegment[1] = wayNodeLatitude;
        waySegment[0] = wayNodeLongitude;
        recomputeWayBBox(way, wayNodeLatitude, wayNodeLongitude);

        for (int wayNodesIndex = 2; wayNodesIndex < waySegment.length; wayNodesIndex += 2) {
            // get the way node latitude offset (VBE-S)
            wayNodeLatitude = wayNodeLatitude + this.readBuffer.readSignedInt();

            // get the way node longitude offset (VBE-S)
            wayNodeLongitude = wayNodeLongitude + this.readBuffer.readSignedInt();

            waySegment[wayNodesIndex + 1] = wayNodeLatitude;
            waySegment[wayNodesIndex] = wayNodeLongitude;
            recomputeWayBBox(way, wayNodeLatitude, wayNodeLongitude);
        }
    }

    private void decodeWayNodesDoubleDelta(Way way, float[] waySegment) {
        // get the first way node latitude offset (VBE-S)
        int wayNodeLatitude = this.tileLatitude + this.readBuffer.readSignedInt();

        // get the first way node longitude offset (VBE-S)
        int wayNodeLongitude = this.tileLongitude + this.readBuffer.readSignedInt();

        // store the first way node
        waySegment[1] = wayNodeLatitude;
        waySegment[0] = wayNodeLongitude;
        recomputeWayBBox(way, wayNodeLatitude, wayNodeLongitude);

        int previousSingleDeltaLatitude = 0;
        int previousSingleDeltaLongitude = 0;

        for (int wayNodesIndex = 2; wayNodesIndex < waySegment.length; wayNodesIndex += 2) {
            // get the way node latitude double-delta offset (VBE-S)
            int doubleDeltaLatitude = this.readBuffer.readSignedInt();

            // get the way node longitude double-delta offset (VBE-S)
            int doubleDeltaLongitude = this.readBuffer.readSignedInt();

            int singleDeltaLatitude = doubleDeltaLatitude + previousSingleDeltaLatitude;
            int singleDeltaLongitude = doubleDeltaLongitude + previousSingleDeltaLongitude;

            wayNodeLatitude = wayNodeLatitude + singleDeltaLatitude;
            wayNodeLongitude = wayNodeLongitude + singleDeltaLongitude;

            // add data to way
            waySegment[wayNodesIndex + 1] = wayNodeLatitude;
            waySegment[wayNodesIndex] = wayNodeLongitude;
            recomputeWayBBox(way, wayNodeLatitude, wayNodeLongitude);

            previousSingleDeltaLatitude = singleDeltaLatitude;
            previousSingleDeltaLongitude = singleDeltaLongitude;
        }
    }

    private void recomputeWayBBox(Way way, int wayNodeLatitude, int wayNodeLongitude) {
        if (wayNodeLatitude < way.bboxBottomE6) {
            way.bboxBottomE6 = wayNodeLatitude;
        }
        if (wayNodeLatitude > way.bboxTopE6) {
            way.bboxTopE6 = wayNodeLatitude;
        }
        if (wayNodeLongitude < way.bboxLeftE6) {
            way.bboxLeftE6 = wayNodeLongitude;
        }
        if (wayNodeLongitude > way.bboxRightE6) {
            way.bboxRightE6 = wayNodeLongitude;
        }
    }
}
