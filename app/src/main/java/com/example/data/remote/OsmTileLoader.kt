package com.example.data.remote

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sinh
import kotlin.math.tan

object OsmProjection {
    // Zamboanga del Sur focus bounds centered around Dumingag (8.1650° N, 123.3465° E)
    const val DUMINGAG_LAT = 8.1650
    const val DUMINGAG_LON = 123.3465
    const val MIN_ZDS_LAT = 7.45
    const val MAX_ZDS_LAT = 8.65
    const val MIN_ZDS_LON = 122.75
    const val MAX_ZDS_LON = 123.95

    fun latLonToTileCoord(lat: Double, lon: Double, tileZoom: Int): Pair<Double, Double> {
        val n = 2.0.pow(tileZoom)
        val x = ((lon + 180.0) / 360.0) * n
        val clampedLat = lat.coerceIn(-85.0511, 85.0511)
        val latRad = Math.toRadians(clampedLat)
        val y = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / Math.PI) / 2.0 * n
        return x to y
    }

    fun tileCoordToLatLon(worldX: Double, worldY: Double, tileZoom: Int): Pair<Double, Double> {
        val n = 2.0.pow(tileZoom)
        val lon = (worldX / n) * 360.0 - 180.0
        val latRad = atan(sinh(Math.PI * (1.0 - 2.0 * worldY / n)))
        val lat = Math.toDegrees(latRad)
        return lat.coerceIn(MIN_ZDS_LAT, MAX_ZDS_LAT) to lon.coerceIn(MIN_ZDS_LON, MAX_ZDS_LON)
    }
}

class OsmTileLoader(context: Context) {
    private val cacheDir = File(context.applicationContext.cacheDir, "osm_tiles").apply { mkdirs() }
    private val memoryCache = LruCache<String, ImageBitmap>(110)

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    fun getCachedInMemory(z: Int, x: Int, y: Int): ImageBitmap? =
        memoryCache.get("${z}_${x}_$y")

    suspend fun loadTile(z: Int, x: Int, y: Int, allowNetwork: Boolean): ImageBitmap? {
        val key = "${z}_${x}_$y"
        memoryCache.get(key)?.let { return it }

        val n = 1 shl z
        if (x !in 0 until n || y !in 0 until n) return null

        return withContext(Dispatchers.IO) {
            val diskFile = File(cacheDir, "$z/$x/$y.png")
            if (diskFile.exists() && diskFile.length() > 0L) {
                val bmp = BitmapFactory.decodeFile(diskFile.absolutePath)?.asImageBitmap()
                if (bmp != null) {
                    memoryCache.put(key, bmp)
                    return@withContext bmp
                }
            }

            if (!allowNetwork) return@withContext null

            try {
                val url = "https://tile.openstreetmap.org/$z/$x/$y.png"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "DumingagZamboangaDelSurGuide/1.0 (https://dumingag.com)")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val bytes = response.body?.bytes() ?: return@withContext null
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                        ?: return@withContext null
                    diskFile.parentFile?.mkdirs()
                    diskFile.writeBytes(bytes)
                    memoryCache.put(key, bmp)
                    bmp
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun countCachedDiskTiles(): Int = withContext(Dispatchers.IO) {
        cacheDir.walkTopDown().count { it.isFile && it.extension == "png" && it.length() > 0L }
    }

    /**
     * Pre-downloads real OpenStreetMap tiles covering Dumingag Poblacion, Mt. Timolan,
     * Danlugan Falls, and organic farm corridors across zoom levels 12..14 so 2D and 3D
     * maps work smoothly with zero cellular signal.
     */
    suspend fun preDownloadDumingagTiles(
        onProgress: (downloaded: Int, total: Int) -> Unit = { _, _ -> }
    ): Int = withContext(Dispatchers.IO) {
        val minLat = 8.115
        val maxLat = 8.215
        val minLon = 123.295
        val maxLon = 123.395
        val zoomLevels = listOf(12, 13, 14)

        val tileCoords = buildList {
            for (z in zoomLevels) {
                val (minX, minY) = OsmProjection.latLonToTileCoord(maxLat, minLon, z)
                val (maxX, maxY) = OsmProjection.latLonToTileCoord(minLat, maxLon, z)
                val maxIdx = (1 shl z) - 1
                val xStart = kotlin.math.floor(minX).toInt().coerceIn(0, maxIdx)
                val xEnd = kotlin.math.floor(maxX).toInt().coerceIn(0, maxIdx)
                val yStart = kotlin.math.floor(minY).toInt().coerceIn(0, maxIdx)
                val yEnd = kotlin.math.floor(maxY).toInt().coerceIn(0, maxIdx)
                for (tx in xStart..xEnd) {
                    for (ty in yStart..yEnd) {
                        add(Triple(z, tx, ty))
                    }
                }
            }
        }

        val total = tileCoords.size
        var completed = 0
        var availableCount = 0
        onProgress(0, total)

        for ((z, x, y) in tileCoords) {
            val diskFile = File(cacheDir, "$z/$x/$y.png")
            if (diskFile.exists() && diskFile.length() > 0L) {
                availableCount++
            } else {
                val loaded = loadTile(z = z, x = x, y = y, allowNetwork = true)
                if (loaded != null) availableCount++
            }
            completed++
            onProgress(completed, total)
        }
        availableCount
    }
}

