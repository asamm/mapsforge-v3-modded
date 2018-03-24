package com.asamm.mapsforgeV3.sample

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.asamm.locus.mapsforge.utils.MapsForgeHandler
import com.asamm.locus.mapsforge.utils.Utils
import org.mapsforgeV3.android.maps.mapgenerator.DebugSettings
import org.mapsforgeV3.android.maps.mapgenerator.JobParameters
import org.mapsforgeV3.android.maps.mapgenerator.MapGeneratorJob
import org.mapsforgeV3.android.maps.rendertheme.RenderTheme
import org.mapsforgeV3.android.maps.rendertheme.XmlRenderThemeStyleMenu
import org.mapsforgeV3.core.model.Tile
import org.mapsforgeV3.core.util.MercatorProjection
import org.mapsforgeV3.map.layer.renderer.DatabaseRenderer
import org.mapsforgeV3.map.reader.MapDatabase
import org.mapsforgeV3.map.themeBase.Themes
import java.io.File
import java.io.InputStream
import java.lang.Exception
import java.util.ArrayList
import java.util.HashSet

class MainActivity : AppCompatActivity() {

    // map view component
    private lateinit var mapView: MapView
    // map renderer
    private lateinit var mapRenderer: DatabaseRenderer
    // settings for debug
    private val debugSettings = DebugSettings(false, false, false)
    // default job parameters
    private val jobParameters = JobParameters(Themes.getThemeHikeBike(), 1.5f)

    // current map path
    private val mapPath = "/storage/4245-AD86/Locus SD/mapsVector/europe/czech_republic.osm.map"
    // default zoom level
    private var zoomLevel: Byte = 14
    // current map tileSize
    private var tileSize = 0
    // starting longitude coordinate
    private val baseLongitude = 14.42974
    // starting latitude coordinate
    private val baseLatitude = 50.07978

    // visible images
    private val mapTiles = arrayListOf<MapTile>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        // insert map view
        mapView = MapView(this)
        findViewById<FrameLayout>(R.id.frame_layout_inner).addView(mapView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        // setup mapsForge
        initializeMapsForgeLib()

        // fab button
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initializeMapsForgeLib() {
        // setup main handler
        Utils.registerHandler(object : MapsForgeHandler {

            override fun getEmptyImage(): Bitmap {
                return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            }

            override fun getValidImage(bitmap: Bitmap?, pixelSizeX: Int, pixelSizeY: Int): Bitmap {
                // prepare bitmap itself
                if (bitmap == null || !bitmap.isMutable ||
                        bitmap.width != pixelSizeX ||
                        bitmap.height != pixelSizeY) {
                    return Bitmap.createBitmap(
                            pixelSizeX, pixelSizeY, Bitmap.Config.ARGB_8888)
                }
                return bitmap
            }

            override fun getContext(): Context {
                return this@MainActivity
            }

            override fun getScreenCategory(): Int {
                return 2
            }

            // optimized density value
            private var mDensity: Float = 0.toFloat()

            override fun getDpPixels(value: Float, roundDensity: Boolean): Float {
                // return basic value
                val density = resources.displayMetrics.density
                if (!roundDensity) {
                    return density * value
                }

                // round density
                if (mDensity == 0.0f || mDensity != density) {
                    mDensity = density
                    if (mDensity > 1.0f && mDensity < 2.0f) {
                        mDensity = 1.0f // 1.5
                    } else if (mDensity > 2.0f && mDensity <= 3.0f) {
                        mDensity = 2.0f // 2.5, 3.0
                    } else if (mDensity > 3.0f && mDensity < 4.0f) {
                        mDensity = 4.0f // 3.5
                    }
                }

                // return converted value
                return mDensity * value
            }

            override fun onNewThemeReadyToUse(newTheme: RenderTheme?) {}

            override fun getThemeCategories(menuStyle: XmlRenderThemeStyleMenu?): MutableSet<String>? {
                return getStyleMenuCategories(menuStyle)
            }

            override fun createSVGBitmap(inputStream: InputStream?, scale: Float,
                    requestedWidth: Float, requestedHeight: Float, color: Int): Bitmap {
                return Bitmap.createBitmap(
                        if (requestedWidth <= 0) 10 else requestedWidth.toInt(),
                        if (requestedHeight <= 0) 10 else requestedHeight.toInt(),
                        Bitmap.Config.ARGB_8888)
            }

            override fun parseBoolean(text: String?): Boolean {
                return text?.trim()?.toBoolean() ?: false
            }

            override fun parseInt(text: String?): Int {
                return text?.trim()?.toInt() ?: 0
            }

            override fun parseFloat(text: String?): Float {
                return text?.trim()?.toFloat() ?: 0.0f
            }

            override fun logI(tag: String?, msg: String?) {
                Log.i(tag, msg)
            }

            override fun logD(tag: String?, msg: String?) {
                Log.d(tag, msg)
            }

            override fun logW(tag: String?, msg: String?) {
                Log.w(tag, msg)
            }

            override fun logE(tag: String?, msg: String?, e: Exception?) {
                Log.e(tag, msg, e)
            }
        })

        // setup map
        val mapDatabase = MapDatabase().apply {
            openFile(File(mapPath))
            setPreferredLanguage("cs")
            countryCode = "cz"
            // we do not have DbPoi, so draw all points in map
            dbPoiVersion = 0
        }
        tileSize = mapDatabase.mapFileInfo.tilePixelSize
        mapRenderer = DatabaseRenderer(tileSize).apply {
            setMapDatabaseMain(mapDatabase)
            prepareRenderTheme(jobParameters, zoomLevel)
        }

        // prepare base inner tile coordinates
        val centerTileX = MercatorProjection.longitudeToTileX(baseLongitude, zoomLevel, tileSize)
        val centerTileY = MercatorProjection.latitudeToTileY(baseLatitude, zoomLevel, tileSize)

        // load all images
        for (i in (centerTileX - 1) .. (centerTileX + 1)) {
            for (j in (centerTileY - 1) .. (centerTileY + 1)) {
                MapTile(i, j, zoomLevel).let {
                    mapTiles.add(it)
                    loadImage(it)
                }
            }
        }
    }

    /**
     * Load certain map tile (sync) defined by [mapTile] container.
     */
    private fun loadImage(mapTile: MapTile) {
        val tile = Tile(mapTile.tileX, mapTile.tileY, mapTile.zoom)
        val mgj = MapGeneratorJob(tile, jobParameters, debugSettings)
        val tileRenderer = mapRenderer.executeJob(mgj, null, null)
        tileRenderer.startRender()

        // store image
        mapTile.image = tileRenderer.renderedBitmap

        // refresh view
        mapView.invalidate()
    }

    /**
     * Get current configuration of menu styles.
     * @param menuStyle current active style
     * @return list of allowed categories
     */
    fun getStyleMenuCategories(menuStyle: XmlRenderThemeStyleMenu?): MutableSet<String>? {
        // check style
        if (menuStyle == null ||
                menuStyle.layers.isEmpty() ||
                menuStyle.id == null ||
                menuStyle.id.isEmpty()) {
            return null
        }


        // get and check last defined layer
        val lastLayerId = jobParameters.jobTheme.themeStyle
        if (lastLayerId == null || lastLayerId.isEmpty()
                || menuStyle.getLayer(lastLayerId) == null) {
            return null
        }

        // add categories from selected layer
        val categories = HashSet<String>()
        categories.addAll(menuStyle.getLayer(lastLayerId).categories)

        // now load previous configuration and check them
        val parameters = getStyleMenuEnabledOverlays(menuStyle, lastLayerId)
        for (i in 0 until parameters.size) {
            val overlayId = parameters[i]
            val overlay = menuStyle.getLayer(overlayId)
            if (overlay != null) {
                categories.addAll(overlay.categories)
            }
        }
        return categories
    }

    /**
     * Get enabled overlays for current active layer.
     * @return list of enabled categories or default values if layer is not yet defined
     */
    private fun getStyleMenuEnabledOverlays(menu: XmlRenderThemeStyleMenu, lastLayerId: String): List<String> {
        // get active layer
        val overlays = ArrayList<String>()
        val layer = menu.getLayer(lastLayerId) ?: return overlays

        // add overlays to list
        for (overlay in layer.overlays) {
            if (overlay.isEnabled) {
                overlays.add(overlay.id)
            }
        }
        return overlays
    }

    private inner class MapTile(val tileX: Long, val tileY: Long, val zoom: Byte) {

        // tile longitude coordinate of top/left corner
        val tileLon = MercatorProjection.tileXToLongitude(tileX, zoom, tileSize)
        // tile latitude coordinate of top/left corner
        val tileLan = MercatorProjection.tileYToLatitude(tileY, zoom, tileSize)
        // loaded image
        var image: Bitmap? = null
    }

    inner class MapView(ctx: Context) : View(ctx) {

        private var mapSize = MercatorProjection.getMapSize(zoomLevel)
        private var offsetX = MercatorProjection.longitudeToPixelX(baseLongitude, mapSize)
        private var offsetY = MercatorProjection.latitudeToPixelY(baseLatitude, mapSize)
        // line draw
        private val paintLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.RED
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            // prepare canvas
            val centerX = offsetX.toFloat() - width / 2.0f
            val centerY = offsetY.toFloat() - height / 2.0f

            // draw image
            for (mapTile in mapTiles) {
                val pxLeft = MercatorProjection.longitudeToPixelX(mapTile.tileLon, mapSize)
                val pxTop = MercatorProjection.latitudeToPixelY(mapTile.tileLan, mapSize)

                // draw tile
                canvas.save()
                canvas.translate((pxLeft - centerX).toFloat(), (pxTop - centerY).toFloat())
                canvas.drawBitmap(mapTile.image, 0.0f, 0.0f, null)
                canvas.restore()
            }

            // draw lines
            canvas.drawLine(0.0f, height / 2.0f, width.toFloat(), height / 2.0f, paintLine)
            canvas.drawLine(width / 2.0f, 0.0f, width / 2.0f, height.toFloat(), paintLine)
        }
    }
}
