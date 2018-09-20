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
package org.mapsforgeV3.map.layer.renderer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.os.Build;

import com.asamm.locus.mapsforge.utils.Utils;

import org.mapsforgeV3.android.maps.mapgenerator.JobParameters;
import org.mapsforgeV3.android.maps.mapgenerator.MapGenerator;
import org.mapsforgeV3.android.maps.mapgenerator.MapGeneratorJob;
import org.mapsforgeV3.android.maps.mapgenerator.RenderThemeDefinition;
import org.mapsforgeV3.android.maps.rendertheme.RenderCallback;
import org.mapsforgeV3.android.maps.rendertheme.RenderTheme;
import org.mapsforgeV3.android.maps.rendertheme.RenderThemeHandler;
import org.mapsforgeV3.android.maps.rendertheme.tools.BgRectangle;
import org.mapsforgeV3.android.maps.rendertheme.tools.CurveStyle;
import org.mapsforgeV3.core.model.GeoPoint;
import org.mapsforgeV3.core.model.Tag;
import org.mapsforgeV3.core.model.Tile;
import org.mapsforgeV3.core.util.MercatorProjection;
import org.mapsforgeV3.map.reader.MapDatabase;
import org.mapsforgeV3.map.reader.Way;
import org.mapsforgeV3.map.reader.header.MapFileInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A DatabaseRenderer renders map tiles by reading from a {@link MapDatabase}.
 */
public class DatabaseRenderer implements MapGenerator {

    // tag for logger
    private static final String TAG = "DatabaseRenderer";

    // debug parameter
    private static final boolean DEBUG = false;

    // PARAMETERS

    private static final Byte DEFAULT_START_ZOOM_LEVEL = (byte) 12;
    private static final byte LAYERS = 11;
    private static final Paint PAINT_WATER_TILE_HIGHTLIGHT = new Paint(Paint.ANTI_ALIAS_FLAG);
    public static final Tag TAG_NATURAL_WATER = new Tag("natural", "water");
    public static final Tag TAG_NATURAL_NOSEA = new Tag("natural", "nosea");
    public static final Tag TAG_NATURAL_SEA = new Tag("natural", "sea");
    private static final byte ZOOM_MAX = 22;

    private static byte getValidLayer(byte layer) {
        if (layer < 0) {
            return 0;
        } else if (layer >= LAYERS) {
            return LAYERS - 1;
        } else {
            return layer;
        }
    }

    // flag if current selected theme is Locus extended
    public static boolean internalTheme;

    // lock for synchronization
    private final Object lock;
    // size of current tiles
    private final int mTileSize;
    // flag is fill or not, sea areas
    private boolean mFillSeaAreas;

    // maximum number of loaded maps at once
    private int maxMapDatabases;

    // container for map databases
    private List<MapDatabase> mMapDatabases;
    // path to current "base" map
    private File mMapFile;
    // path to current "world" map
    private MapDatabase mMapDbWorld;

    // currently used render theme
    private RenderTheme mRenderTheme;
    private RenderThemeDefinition mPreviousJobTheme;
    private LabelPlacement mLabelPlacement;

    // last requested zoom level
    private byte mCurrentZoomLevel;
    // last used text scale
    private float mCurrentTextScale;
    // last used language
    private String mCurrentLang;

    /**
     * Constructs a new DatabaseRenderer.
     *
     * @param tileSize size of tiles
     */
    public DatabaseRenderer(int tileSize) {
        this.lock = new Object();
        this.mTileSize = tileSize;
        this.mMapDatabases = new ArrayList<>();
        cleanup();

        PAINT_WATER_TILE_HIGHTLIGHT.setStyle(Paint.Style.FILL);
        PAINT_WATER_TILE_HIGHTLIGHT.setColor(Color.CYAN);
    }

    @Override
    public void cleanup() {
        synchronized (lock) {
            // destroy theme
            if (this.mRenderTheme != null) {
                this.mRenderTheme.destroy();
            }
            this.mRenderTheme = null;
            this.maxMapDatabases = Utils.getHandler().getScreenCategory() > 1 ? 5 : 3;
            this.mLabelPlacement = new LabelPlacement(mTileSize);
            this.mPreviousJobTheme = null;
            this.mCurrentZoomLevel = 0;
            this.mCurrentTextScale = 1.0f;
            this.mCurrentLang = "";
        }
    }

    /**
     * Destroy instance of renderer.
     */
    public void destroy() {
        // clean content
        cleanup();

        // remove all map databases
        setMapDatabaseMain(null);
        setMapDatabaseWorld(null);
    }

    /**
     * Get current loaded map theme.
     *
     * @return current theme
     */
    public RenderTheme getTheme() {
        return mRenderTheme;
    }

    /**
     * Get current used size of tiles in pixels.
     *
     * @return size of tiles
     */
    public int getTileSize() {
        return mTileSize;
    }

    /**
     * Return draw color
     *
     * @return color for background
     */
    public int getBackgroundColor() {
        if (mRenderTheme == null) {
            return Color.TRANSPARENT;
        }
        return mRenderTheme.getMapBackground();
    }

    @Override
    public TileRenderer executeJob(MapGeneratorJob mapGeneratorJob, Bitmap preparedImg,
            ExtraRenderingHandler renderingHandler) {
        if (DEBUG) {
            Utils.getHandler().logW(TAG, "executeJob(" + mapGeneratorJob + ", " +
                    preparedImg + ", " + renderingHandler + ")");
        }

        // set render theme
        if (!prepareRenderTheme(mapGeneratorJob.jobParameters,
                mapGeneratorJob.tile.zoomLevel)) {
            return null;
        }

        // return empty image if no database is available
        int size = mMapDatabases.size();
        if (size == 0) {
            return new TileRenderer(mapGeneratorJob);
        }

        // create renderer
        TileRenderer tr = new TileRenderer(mapGeneratorJob,
                preparedImg, renderingHandler,
                mMapDatabases.get(0).getDbPoiVersion() > 0);

        // prepare builder
        synchronized (lock) {
            // store current main map file
            mMapFile = mMapDatabases.get(0).getMapFile();

            // read all data
            for (int i = size - 1; i >= 0; i--) {
                MapDatabase md = mMapDatabases.get(i);
                if (md.hasOpenFile()) {
                    md.readMapData(mapGeneratorJob.tile, tr);
                }

                // remove file if not required and at least one more file will remain
                if (!md.isFileRequired() && mMapDatabases.size() > 1) {
                    md.closeFile();
                    mMapDatabases.remove(md);
                }
            }

            // if still empty, close tasks
            boolean empty = tr.isEmpty();

            // handle world map
            if (mMapDbWorld != null) {
                mMapDbWorld.readMapData(mapGeneratorJob.tile, tr);
            }

            // finalize
            if (empty) {
                if (!tr.isEmpty()) {
                    tr.setRequestToWorldOnly();
                } else {
                    tr.setRequestToEmpty();
                }
            }
        }

        // return request
        return tr;
    }

    /**
     * Prepare render theme for next rendering operation.
     *
     * @param jobParameters parameters for job
     * @return {@code true} if theme was correctly set
     */
    public boolean prepareRenderTheme(JobParameters jobParameters, byte zoomLevel) {
        RenderThemeDefinition jobTheme = jobParameters.jobTheme;
        internalTheme = jobTheme.isInternalTheme();

        // compare jobThemes and generate new theme if job differs. We compare them by '==' and not
        // equals, because jobTheme is singleton and in case of changed layers (MapsForge 0.5), we
        // need to reload whole theme again
        boolean forceRefreshTheme = false;
        if (mPreviousJobTheme == null || jobTheme != mPreviousJobTheme) {
            cleanup();
            this.mRenderTheme = getRenderTheme(jobTheme);
            if (DEBUG) {
                Utils.getHandler().logW(TAG, "prepareRenderTheme(), " +
                        "loaded theme:" + mRenderTheme);
            }

            // test loaded theme
            if (mRenderTheme == null) {
                mPreviousJobTheme = null;
                return false;
            }

            // notify about new theme and set theme for usage
            Utils.getHandler().onNewThemeReadyToUse(mRenderTheme);
            mPreviousJobTheme = jobTheme;
            forceRefreshTheme = true;
        }

        // refresh theme if needed
        if (forceRefreshTheme ||
                mCurrentZoomLevel != zoomLevel ||
                mCurrentTextScale != jobParameters.textScale ||
                !mCurrentLang.equals(getFirstMapDatabaseCountryCode())) {
            if (DEBUG) {
                Utils.getHandler().logW(TAG, "prepareRenderTheme(), " +
                        "new zoom: " + mCurrentZoomLevel + " vs " + zoomLevel + ", " +
                        "text scale: " + mCurrentTextScale + " vs " + jobParameters.textScale);
            }
            // store new values
            mCurrentZoomLevel = zoomLevel;
            mCurrentTextScale = jobParameters.textScale;
            mCurrentLang = getFirstMapDatabaseCountryCode();

            // set Locus extended flag
            mFillSeaAreas = mRenderTheme.isFillSeaAreas();

            // scale theme
            mRenderTheme.prepareTheme(mCurrentZoomLevel, mCurrentTextScale, mCurrentLang);
        }

        // valid result
        return true;
    }

    /**
     * Parse new render theme based on defined parameters.
     *
     * @param jobTheme parameter for job
     * @return parsed theme
     */
    private RenderTheme getRenderTheme(RenderThemeDefinition jobTheme) {
        try {
            return RenderThemeHandler.getRenderTheme(jobTheme);
        } catch (Exception e) {
            Utils.getHandler().logE(TAG, "getRenderTheme(" + jobTheme + ")", e);
        }
        return null;
    }

    @Override
    public GeoPoint getMapCenter() {
        MapDatabase md = getFirstMapDatabase();
        if (md != null) {
            MapFileInfo mapFileInfo = md.getMapFileInfo();
            if (mapFileInfo.mapCenter != null) {
                return mapFileInfo.mapCenter;
            } else if (mapFileInfo.startPosition != null) {
                return mapFileInfo.startPosition;
            }
        }
        return null;
    }

    @Override
    public Byte getStartZoomLevel() {
        MapDatabase md = getFirstMapDatabase();
        if (md != null) {
            MapFileInfo mapFileInfo = md.getMapFileInfo();
            if (mapFileInfo.startZoomLevel != null) {
                return mapFileInfo.startZoomLevel;
            }
        }
        return DEFAULT_START_ZOOM_LEVEL;
    }

    private MapDatabase getFirstMapDatabase() {
        synchronized (lock) {
            for (MapDatabase md : mMapDatabases) {
                if (md.hasOpenFile()) {
                    return md;
                }
            }
            return null;
        }
    }

    /**
     * Get country code of main active map.
     *
     * @return map country code
     */
    public String getFirstMapDatabaseCountryCode() {
        MapDatabase md = getFirstMapDatabase();
        if (md == null) {
            return "";
        }
        return md.getCountryCode();
    }

    public File getCurrentMapFile() {
        return mMapFile;
    }

    @Override
    public byte getZoomLevelMax() {
        return ZOOM_MAX;
    }

    /**************************************************/
    /*                  VARIOUS TOOLS                 */
    /**************************************************/

    /**
     * @param mapDatabase the MapDatabase from which the map data will be read.
     */
    public void setMapDatabaseMain(MapDatabase mapDatabase) {
        synchronized (lock) {
            // remove extra maps
            for (int i = 0, n = mMapDatabases.size(); i < n; i++) {
                mMapDatabases.get(i).closeFile();
            }
            mMapDatabases.clear();

            // set base map
            if (mapDatabase != null) {
                mMapFile = mapDatabase.getMapFile();
                mMapDatabases.add(mapDatabase);
            }
        }
    }

    public boolean setMapDatabasesExtra(List<MapDatabase> maps) {
        synchronized (lock) {
            boolean newAdded = false;
            for (int i = 0, n = maps.size(); i < n; i++) {
                MapDatabase map = maps.get(i);
                if (!mMapDatabases.contains(map)) {
                    mMapDatabases.add(map);
                    newAdded = true;
                }

                // limit number of loaded maps
                if (mMapDatabases.size() >= maxMapDatabases) {
                    break;
                }
            }
            return newAdded;
        }
    }

    public void setMapDatabaseWorld(MapDatabase mapDatabase) {
        synchronized (lock) {
            // close existing file
            if (mMapDbWorld != null) {
                mMapDbWorld.closeFile();
            }
            mMapDbWorld = null;

            // set new map
            if (mapDatabase != null) {
                mMapDbWorld = mapDatabase;
            }
        }
    }

    public boolean existsMapDatabase(File file) {
        for (int i = 0, n = mMapDatabases.size(); i < n; i++) {
            MapDatabase map = mMapDatabases.get(i);
            if (map.getMapFile().getAbsolutePath().equals(file.getAbsolutePath())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get number of currently loaded map databases.
     *
     * @return number of databases
     */
    public int getMapDatabasesSize() {
        return mMapDatabases.size();
    }

    /**************************************************/
    /*                 RENDERING PART                 */

    /**************************************************/

    public class TileRenderer implements RenderCallback {

        // DEFINITION

        // current generator job
        private MapGeneratorJob mapGeneratorJob;
        // current handled tile
        private Tile currentMapTile;
        // handler for special events after rendering
        private ExtraRenderingHandler extraRenderingHandler;
        // flag if DB POI for main map exists
        private boolean mExistsDbPoi;

        // PARAMETERS

        // drawing canvas
        private CanvasRasterer canvasRasterer;
        // generated bitmap
        private Bitmap bitmap;
        // X coordinates of currently handled POI
        private float poiX;
        // Y coordinates of currently handled POI
        private float poiY;

        // CONTAINERS

        // container for nodes
        private List<PaintContainerPointText> nodes;
        // container for labels on areas
        private List<PaintContainerPointText> areaLabels;
        // container for simple map icons
        private List<PaintContainerSymbol> pointSymbols;
        // lazy-loaded container for names along ways
        private List<PaintContainerWayText> wayNames;
        // lazy-loaded container for symbols along paths
        private List<PaintContainerSymbol> waySymbols;
        // some crazy "ways" container
        private List<PaintContainerShape>[][] waysArray;

        // TEMP OBJECT SPECIFIC PARAMETERS

        // current container for draw objects
        private List<PaintContainerShape>[] mCurrentLayer;
        // current shape object
        private ContainerShape mShapeContainer;
        // flag if shape container is "no sea/background"
        private boolean mShapeContainerBg;
        // current coordinates
        private float[][] coordinates;
        // are current coordinates closed
        private boolean mCoordinatesClosed;

        // BASIC PARAMETERS

        // is current tile water
        public boolean isWater;
        // rendering completed
        private boolean renderingComplete;
        // is renderer still valid
        private boolean isStillValid;
        // world map only
        private boolean isWorldDataOnly;

        // current tile values
        private byte cZoomLevel;
        private long cPixelX;
        private long cPixelY;

        private int bboxTopE6;
        private int bboxBottomE6;
        private int bboxLeftE6;
        private int bboxRightE6;

        // size of map in pixels in current zoom
        private long mapSize;

        // COUNTERS

        private int mCounterLoadedPoi;
        private int mCounterLoadedWay;

        private int mCounterRenderArea;
        private int mCounterRenderAreaBg;
        private int mCounterRenderAreaCaption;
        private int mCounterRenderAreaSymbol;
        private int mCounterRenderPoiCaption;
        private int mCounterRenderPoiCircle;
        private int mCounterRenderPoiSymbol;
        private int mCounterRenderWay;
        private int mCounterRenderWaySymbol;
        private int mCounterRenderWayText;

        /**
         * Construct finished empty container
         */
        TileRenderer(MapGeneratorJob mapGeneratorJob) {
            this.mapGeneratorJob = mapGeneratorJob;
            setRequestToEmpty();
        }

        TileRenderer(MapGeneratorJob mapGeneratorJob, Bitmap preparedImg,
                ExtraRenderingHandler extraRenderingHandler, boolean existDbPoi) {
            // definition
            this.mapGeneratorJob = mapGeneratorJob;
            this.currentMapTile = mapGeneratorJob.tile;
            this.extraRenderingHandler = extraRenderingHandler;
            this.mExistsDbPoi = existDbPoi;

            // parameters
            canvasRasterer = new CanvasRasterer(mTileSize);
            bitmap = preparedImg;

            // containers
            nodes = new ArrayList<>(16);
            areaLabels = new ArrayList<>(16);
            pointSymbols = new ArrayList<>(16);

            // generate ways container
            this.waysArray = getWayContainerFromCache(mRenderTheme.getLevels());

            // basic parameters
            this.isWater = false;
            this.isWorldDataOnly = false;
            this.renderingComplete = false;
            this.isStillValid = true;
            this.cZoomLevel = currentMapTile.zoomLevel;
            this.cPixelX = currentMapTile.tileX * mTileSize;
            this.cPixelY = currentMapTile.tileY * mTileSize;
            this.mapSize = MercatorProjection.getMapSize(cZoomLevel, mTileSize);

            // prepare tile BBOX
            bboxTopE6 = (int) (MercatorProjection.pixelYToLatitude(
                    cPixelY - mTileSize / 4, cZoomLevel, mTileSize,false) * 1000000.0);
            bboxBottomE6 = (int) (MercatorProjection.pixelYToLatitude(
                    cPixelY + mTileSize + mTileSize / 4, cZoomLevel, mTileSize, false) * 1000000.0);
            bboxLeftE6 = (int) (MercatorProjection.pixelXToLongitude(
                    cPixelX - mTileSize / 4, cZoomLevel, mTileSize, false) * 1000000.0);
            bboxRightE6 = (int) (MercatorProjection.pixelXToLongitude(
                    cPixelX + mTileSize + mTileSize / 4, cZoomLevel, mTileSize, false) * 1000000.0);
        }

        /**
         * Check if at least some data were loaded and rendered.
         *
         * @return {@code true} if really no data were rendered
         */
        public boolean isEmpty() {
            return mCounterRenderArea == 0 &&
                    mCounterRenderAreaCaption == 0 &&
                    mCounterRenderAreaSymbol == 0 &&
                    mCounterRenderPoiCaption == 0 &&
                    mCounterRenderPoiCircle == 0 &&
                    mCounterRenderPoiSymbol == 0 &&
                    mCounterRenderWay == 0 &&
                    mCounterRenderWaySymbol == 0 &&
                    mCounterRenderWayText == 0;
        }

        /**
         * Check if rendered tile is really only background tile.
         *
         * @return {@code true} if tile is background only
         */
        public boolean isResultOnlyBg() {
            // true for "world map" only
            if (isWorldDataOnly) {
                return true;
            }

            // check data
            return mCounterRenderAreaBg != 0 &&
                    mCounterRenderAreaCaption == 0 &&
                    mCounterRenderAreaSymbol == 0 &&
                    mCounterRenderPoiCaption == 0 &&
                    mCounterRenderPoiCircle == 0 &&
                    mCounterRenderPoiSymbol == 0 &&
                    mCounterRenderWay == 0 &&
                    mCounterRenderWaySymbol == 0 &&
                    mCounterRenderWayText == 0;
        }

        private void setRequestToEmpty() {
            this.renderingComplete = true;
            this.bitmap = Utils.getHandler().getEmptyImage();
        }

        private void setRequestToWorldOnly() {
            this.isWorldDataOnly = true;
        }

        /**
         * Finally start rendering. This method is already called from separate thread
         */
        public void startRender() {
            if (DEBUG) {
                Utils.getHandler().logW(TAG, "startRender(), tile:" +
                        currentMapTile.tileX + ", " + currentMapTile.tileY + ", " + currentMapTile.zoomLevel);
            }
            // check if tile is already rendered
            if (renderingComplete) {
                return;
            }

            // render tile
            renderData();

            if (DEBUG) {
                Utils.getHandler().logW(TAG, "render done!");
                printCurrentContent();
            }

            // clear containers in the end
            clearLists();

            // set completed
            renderingComplete = true;
        }

        @Override
        public boolean isRenderingCompleted() {
            return renderingComplete;
        }

        @Override
        public Bitmap getRenderedBitmap() {
            return bitmap;
        }

        private boolean isStillValid() {
            if (isStillValid && mCurrentZoomLevel == cZoomLevel) {
                return true;
            } else {
                bitmap = null;
                return false;
            }
        }

        /**
         * Get info if current renderer is still valid. It means if renderer still works on a
         * new tile image.
         *
         * @param isStillValid <code>true</code> if still valid
         */
        public void setStillValid(boolean isStillValid) {
            this.isStillValid = isStillValid;
        }

        /**
         * Finally render data itself.
         */
        private void renderData() {
            // do something with water
            if (mFillSeaAreas && isWater) {
                renderWaterBackground();
            }

            // draw content
            synchronized (lock) {
                // handle placement
                this.nodes = mLabelPlacement.placeLabels(
                        this.nodes, this.pointSymbols,
                        this.areaLabels, this.currentMapTile);
            }

            // check validity
            if (!isStillValid()) return;

            // prepare bitmap itself
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                bitmap = Utils.getHandler().getValidImage(bitmap, mTileSize, mTileSize);
                Canvas canvas = new Canvas(bitmap);
                clearCanvas(canvas);
                canvasRasterer.setCustomCanvas(canvas);
            } else {
                // nothing to do, canvas is already ready
            }

            // check validity
            if (!isStillValid()) return;

            // draw ways
            this.canvasRasterer.drawWays(cZoomLevel, waysArray, extraRenderingHandler);

            // check validity
            if (!isStillValid()) return;

            // draw symbols along way
            if (waySymbols != null) {
                canvasRasterer.drawSymbols(waySymbols);
            }

            // check validity
            if (!isStillValid()) return;

            this.canvasRasterer.drawSymbols(this.pointSymbols);

            // check validity
            if (!isStillValid()) return;

            // draw names along way
            if (wayNames != null) {
                canvasRasterer.drawWayNames(wayNames);
            }

            // check validity
            if (!isStillValid()) return;

            this.canvasRasterer.drawNodes(this.nodes, this.areaLabels);

            // draw frame in debug mode
            if (mapGeneratorJob.debugSettings.drawTileFrames) {
                this.canvasRasterer.drawTileFrame(mTileSize);
            }

            // draw coordinate labels in debug mode
            if (mapGeneratorJob.debugSettings.drawTileCoordinates) {
                this.canvasRasterer.drawTileCoordinates(currentMapTile);
            }

            // check validity
            if (!isStillValid()) return;

            // prepare bitmap itself
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // nothing to do, map tile ready
            } else {
                // prepare bitmap itself
                bitmap = Utils.getHandler().getValidImage(bitmap, mTileSize, mTileSize);
                Canvas canvas = new Canvas(bitmap);

                // finally draw items
                clearCanvas(canvas);

                // finally draw canvas to bitmap
                canvasRasterer.finish(canvas);
            }

            // do some special actions if required
            if (extraRenderingHandler != null) {
                extraRenderingHandler.onTileRendered(bitmap);
            }
        }

        private void clearCanvas(Canvas canvas) {
            // prepare color user for clear of canvas
            int color = getBackgroundColor();
            if (extraRenderingHandler != null &&
                    !extraRenderingHandler.drawBackground()) {
                color = Color.TRANSPARENT;
            }

            // clear itself
            if (color == Color.TRANSPARENT) {
                canvas.drawColor(Color.TRANSPARENT, Mode.CLEAR);
            } else {
                canvas.drawColor(color);
            }
        }

        /**
         * Basic call to render point data (call from data loader).
         */
        public void renderNode(byte layer, Tag[] tags, int latitudeE6, int longitudeE6) {
            mCounterLoadedPoi++;

            // quick coordinates test on place
            if (longitudeE6 < bboxLeftE6 ||
                    longitudeE6 > bboxRightE6 ||
                    latitudeE6 > bboxTopE6 ||
                    latitudeE6 < bboxBottomE6) {
                return;
            }

            // now check node
            this.mCurrentLayer = waysArray[getValidLayer(layer)];
            this.poiX = scaleLongitude(longitudeE6);
            this.poiY = scaleLatitude(latitudeE6);
            mRenderTheme.matchNode(this, tags, cZoomLevel);
        }

        /**
         * Basic call to render track data (call from data loader)
         *
         * @param way loaded data
         */
        public void renderWay(Way way) {
            mCounterLoadedWay++;

            // quick coordinates test on place
            if (cZoomLevel >= 15 &&
                    (way.bboxRightE6 < bboxLeftE6 ||
                            way.bboxLeftE6 > bboxRightE6 ||
                            way.bboxTopE6 < bboxBottomE6 ||
                            way.bboxBottomE6 > bboxTopE6)) {
                return;
            }

            // prepare transferred coordinates
            this.coordinates = new float[way.wayNodes.length][];
            for (int i = 0; i < way.wayNodes.length; ++i) {
                float[] coor = way.wayNodes[i];
                this.coordinates[i] = new float[coor.length];
                for (int j = 0; j < coor.length; j += 2) {
                    this.coordinates[i][j] = scaleLongitude(coor[j]);
                    this.coordinates[i][j + 1] = scaleLatitude(coor[j + 1]);
                }
            }

            // now check way
            this.mCurrentLayer = waysArray[getValidLayer(way.layer)];
            this.mShapeContainer = new ContainerWay(this.coordinates);
            mShapeContainerBg = way.isFillBackground;
            mCoordinatesClosed = GeometryUtils.isClosedWay(this.coordinates[0]);
            if (mCoordinatesClosed) {
                mRenderTheme.matchClosedWay(this, way.tags, cZoomLevel);
            } else {
                mRenderTheme.matchLinearWay(this, way.tags, cZoomLevel);
            }
        }

        /**
         * Prepare way (and add for rendering), that works as water background.
         */
        private void renderWaterBackground() {
            // set parameters
            this.coordinates = new float[][]{{
                    0, 0, mTileSize, 0, mTileSize, mTileSize, 0, mTileSize, 0, 0}};
            this.mShapeContainer = new ContainerWay(this.coordinates);

            // generate tile
            mRenderTheme.matchClosedWay(this,
                    new Tag[] {TAG_NATURAL_WATER}, cZoomLevel);
        }

        private void clearLists() {
            // clear containers
            nodes.clear();
            areaLabels.clear();
            pointSymbols.clear();
            if (wayNames != null) {
                wayNames.clear();
            }
            if (waySymbols != null) {
                waySymbols.clear();
            }

            // clear ways
            for (int i = waysArray.length - 1; i >= 0; --i) {
                for (int j = waysArray[i].length - 1; j >= 0; --j) {
                    waysArray[i][j].clear();
                }
            }
            addWayContainerCache(waysArray);
            waysArray = null;
        }

        /**
         * Print currently loaded content of this tile renderer.
         */
        private void printCurrentContent() {
            Utils.getHandler().logW(TAG, "printCurrentContent()");
            Utils.getHandler().logI(TAG, "Loaded: " +
                    "POI:" + mCounterLoadedPoi + ", " +
                    "Way:" + mCounterLoadedWay);
            Utils.getHandler().logI(TAG, "Rendered: " +
                    "Area:" + mCounterRenderArea + ", " + mCounterRenderAreaBg + ", " +
                    "AreaCaption:" + mCounterRenderAreaCaption + ", " +
                    "AreaSymbol:" + mCounterRenderAreaSymbol + ", " +
                    "PoiCaption:" + mCounterRenderPoiCaption + ", " +
                    "PoiCircle:" + mCounterRenderPoiCircle + ", " +
                    "PoiSymbol:" + mCounterRenderPoiSymbol + ", " +
                    "Way:" + mCounterRenderWay + ", " +
                    "WaySymbol:" + mCounterRenderWaySymbol + ", " +
                    "WayText:" + mCounterRenderWayText);
        }

        /**
         * Converts a latitude value into an Y coordinate on the current tile.
         *
         * @param latitude the latitude value.
         * @return the Y coordinate on the current tile.
         */
        private float scaleLatitude(float latitude) {
            return (float) (MercatorProjection.latitudeToPixelY(latitude / 1000000.0,
                    mapSize) - cPixelY);
        }

        /**
         * Converts a longitude value into an X coordinate on the current tile.
         *
         * @param longitude the longitude value.
         * @return the X coordinate on the current tile.
         */
        private float scaleLongitude(float longitude) {
            return (float) (MercatorProjection.longitudeToPixelX(longitude / 1000000.0,
                    mapSize) - cPixelX);
        }

        /**************************************************/
        // RENDER CALLBACK

        /**************************************************/

        // AREA
        @Override
        public void renderArea(Paint paint, int level) {
            // count rendered ways
            if (mShapeContainerBg) {
                mShapeContainerBg = false;
                mCounterRenderAreaBg++;
            }
            mCounterRenderArea++;

            // add to layer
            this.mCurrentLayer[level].add(
                    new PaintContainerShape(this.mShapeContainer, paint));
        }

        @Override
        public void renderAreaCaption(String caption,
                float horOffset, float verOffset,
                Paint paintFill, Paint paintStroke,
                BgRectangle bgRect, int priority, boolean forceDraw) {
            mCounterRenderAreaCaption++;
            float[] centerPosition = GeometryUtils.
                    computeAreaCenterPosition(mCoordinatesClosed, coordinates);
            this.areaLabels.add(new PaintContainerPointText(
                    caption, centerPosition[0] + horOffset, centerPosition[1] + verOffset,
                    paintFill, paintStroke, bgRect, priority, forceDraw));
        }

        @Override
        public void renderAreaSymbol(Bitmap symbol,
                float scale, int priority, boolean forceDraw, boolean renderOnlyDb) {
            // check if we wants to draw points
            if (renderOnlyDb && mExistsDbPoi) {
                return;
            }

            // draw symbol
            mCounterRenderAreaSymbol++;
            float[] centerPosition = GeometryUtils.
                    computeAreaCenterPosition(mCoordinatesClosed, coordinates);
            pointSymbols.add(new PaintContainerSymbol(symbol,
                    centerPosition[0] - (symbol.getWidth() >> 1),
                    centerPosition[1] - (symbol.getHeight() >> 1),
                    scale, priority, forceDraw));
        }

        // POINT OF INTEREST

        @Override
        public void renderPointOfInterestCaption(String caption,
                float horOffset, float verOffset,
                Paint paintFill, Paint paintStroke,
                BgRectangle bgRect, int priority, boolean forceDraw) {
            mCounterRenderPoiCaption++;
            this.nodes.add(new PaintContainerPointText(
                    caption, this.poiX + horOffset, this.poiY + verOffset,
                    paintFill, paintStroke, bgRect, priority, forceDraw));
        }

        @Override
        public void renderPointOfInterestCircle(float radius, Paint outline, int level) {
            mCounterRenderPoiCircle++;
            this.mCurrentLayer[level].add(
                    new PaintContainerShape(new ContainerCircle(this.poiX, this.poiY, radius), outline));
        }

        @Override
        public void renderPointOfInterestSymbol(Bitmap symbol, float scale,
                int priority, boolean forceDraw, boolean renderOnlyDb,
                int dbOutlineWidth, int dbOutlineColor) {
            // check if we wants to draw points
            if (renderOnlyDb && mExistsDbPoi) {
                return;
            }

            // render point
            mCounterRenderPoiSymbol++;
            pointSymbols.add(new PaintContainerSymbol(symbol,
                    this.poiX - (symbol.getWidth() >> 1),
                    this.poiY - (symbol.getHeight() >> 1),
                    scale, priority, forceDraw));
        }

        // WAY

        @Override
        public void renderWay(Paint paintLine, Paint paintBorder,
                float vOffset, CurveStyle curveStyle, int level) {
            // increase counter
            mCounterRenderWay++;

            // container with shape
            PaintContainerShape containerShape = new PaintContainerShape(
                    mShapeContainer, paintLine, paintBorder, vOffset, curveStyle);

            // add container to layer
            this.mCurrentLayer[level].add(containerShape);
        }

        @Override
        public void renderWaySymbol(Bitmap symbolBitmap, boolean alignCenter, boolean repeatSymbol,
                float scale, float repeatGap, float horOffset, float verOffset) {
            mCounterRenderWaySymbol++;
            // prepare container
            if (waySymbols == null) {
                waySymbols = new ArrayList<>(16);
            }

            // generate symbols
            WayDecorator.renderSymbol(symbolBitmap, alignCenter,
                    repeatSymbol, this.coordinates, this.waySymbols,
                    scale, horOffset, verOffset, repeatGap);
        }

        @Override
        public void renderWayText(String textKey, float dx, float dy, boolean rotateUp,
                Paint paintFill, Paint paintStroke, BgRectangle bgRect) {
            mCounterRenderWayText++;
            // prepare container
            if (wayNames == null) {
                wayNames = new ArrayList<>(16);
            }

            // generate texts
            WayDecorator.renderText(textKey, this.coordinates, this.wayNames, dx, dy, rotateUp,
                    paintFill, paintStroke, bgRect);
        }

//        // LABELS EXPERIMENTS
//
//        private Set<MapElementContainer> processLabels() {
//            // if we are drawing the labels per tile, we need to establish which tile-overlapping
//            // elements need to be drawn.
//
//            Set<MapElementContainer> labelsToDraw = new HashSet<>();
//
//            synchronized (mTileDependencies) {
//                // first we need to get the labels from the adjacent tiles if they have already been drawn
//                // as those overlapping items must also be drawn on the current tile. They must be drawn regardless
//                // of priority clashes as a part of them has alread been drawn.
//                Set<Tile> neighbours = currentMapTile.getNeighbours();
//                Iterator<Tile> tileIterator = neighbours.iterator();
//                Set<MapElementContainer> undrawableElements = new HashSet<>();
//
//                mTileDependencies.addTileInProgress(currentMapTile);
//                while (tileIterator.hasNext()) {
//                    Tile neighbour = tileIterator.next();
//
//                    if (mTileDependencies.isTileInProgress(neighbour) || tileCache.containsKey(renderContext.rendererJob.otherTile(neighbour))) {
//                        // if a tile has already been drawn, the elements drawn that overlap onto the
//                        // current tile should be in the tile dependencies, we add them to the labels that
//                        // need to be drawn onto this tile. For the multi-threaded renderer we also need to take
//                        // those tiles into account that are not yet in the TileCache: this is taken care of by the
//                        // set of tilesInProgress inside the TileDependencies.
//                        labelsToDraw.addAll(mTileDependencies.getOverlappingElements(neighbour, currentMapTile));
//
//                        // but we need to remove the labels for this tile that overlap onto a tile that has been drawn
//                        for (MapElementContainer current : renderContext.labels) {
//                            if (current.intersects(neighbour.getBoundaryAbsolute())) {
//                                undrawableElements.add(current);
//                            }
//                        }
//                        // since we already have the data from that tile, we do not need to get the data for
//                        // it, so remove it from the neighbours list.
//                        tileIterator.remove();
//                    } else {
//                        mTileDependencies.removeTileData(neighbour);
//                    }
//                }
//
//                // now we remove the elements that overlap onto a drawn tile from the list of labels
//                // for this tile
//                renderContext.labels.removeAll(undrawableElements);
//
//                // at this point we have two lists: one is the list of labels that must be drawn because
//                // they already overlap from other tiles. The second one is currentLabels that contains
//                // the elements on this tile that do not overlap onto a drawn tile. Now we sort this list and
//                // remove those elements that clash in this list already.
//                List<MapElementContainer> currentElementsOrdered = LayerUtil.collisionFreeOrdered(renderContext.labels);
//
//                // now we go through this list, ordered by priority, to see which can be drawn without clashing.
//                Iterator<MapElementContainer> currentMapElementsIterator = currentElementsOrdered.iterator();
//                while (currentMapElementsIterator.hasNext()) {
//                    MapElementContainer current = currentMapElementsIterator.next();
//                    for (MapElementContainer label : labelsToDraw) {
//                        if (label.clashesWith(current)) {
//                            currentMapElementsIterator.remove();
//                            break;
//                        }
//                    }
//                }
//
//                labelsToDraw.addAll(currentElementsOrdered);
//
//                // update dependencies, add to the dependencies list all the elements that overlap to the
//                // neighbouring tiles, first clearing out the cache for this relation.
//                for (Tile tile : neighbours) {
//                    mTileDependencies.removeTileData(currentMapTile, tile);
//                    for (MapElementContainer element : labelsToDraw) {
//                        if (element.intersects(tile.getBoundaryAbsolute())) {
//                            mTileDependencies.addOverlappingElement(currentMapTile, tile, element);
//                        }
//                    }
//                }
//            }
//            return labelsToDraw;
//        }
    }

    // CACHE WAYS CONTAINERS

    private static List<List<PaintContainerShape>[][]> mCacheWayContainers =
            Collections.synchronizedList(new ArrayList<List<PaintContainerShape>[][]>());

    private static void addWayContainerCache(List<PaintContainerShape>[][] container) {
        // add item to cache
        if (mCacheWayContainers.size() >= 100) {
            return;
        }
        mCacheWayContainers.add(container);
    }

    private static List<PaintContainerShape>[][] getWayContainerFromCache(int levels) {
        // get data from cache
        if (mCacheWayContainers.size() > 0) {
            List<PaintContainerShape>[][] waysArray = mCacheWayContainers.remove(0);
            if (waysArray != null && waysArray.length == LAYERS && waysArray[0].length == levels) {
                return waysArray;
            }
        }

        // create a new array, http://stackoverflow.com/a/217093/836138
        @SuppressWarnings("unchecked")
        List<PaintContainerShape>[][] waysArray =
                (List<PaintContainerShape>[][]) new List[LAYERS][levels];
        for (byte i = LAYERS - 1; i >= 0; i--) {
            for (int j = levels - 1; j >= 0; j--) {
                waysArray[i][j] = new ArrayList<>(0);
            }
        }
        return waysArray;
    }
}
