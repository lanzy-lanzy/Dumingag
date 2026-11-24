package com.example.ui.screens

import androidx.compose.ui.geometry.Offset
import com.example.data.remote.OsmProjection
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh

internal data class Vec3(
    val x: Float,
    val y: Float,
    val z: Float
)

internal data class Projected3D(
    val screen: Offset,
    val depth: Float,
    val perspectiveScale: Float
)

internal data class HoloGeoBounds(
    val centerLat: Double,
    val centerLon: Double,
    val halfLatSpan: Double,
    val halfLonSpan: Double
)

internal data class SlabTileGrid(
    val tileZoom: Int,
    val minTx: Int,
    val maxTx: Int,
    val minTy: Int,
    val maxTy: Int
)

internal data class SlabTextureRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

internal const val HOLO_SLAB_HALF_EXTENT = 1.04f
internal const val HOLO_SLAB_THICKNESS = 0.11f
internal const val HOLO_DEFAULT_YAW_DEG = -24f
internal const val HOLO_DEFAULT_PITCH_DEG = 55f
internal const val HOLO_MIN_PITCH_DEG = 24f
internal const val HOLO_MAX_PITCH_DEG = 75f
internal const val HOLO_MIN_ZOOM = 0.65f
internal const val HOLO_MAX_ZOOM = 1.85f
private const val HOLO_CAMERA_DISTANCE = 3.9f

/**
 * Computes aspect-ratio-preserving geographic bounds around the linked points so both
 * START and END pins sit attractively inside the floating 3D holographic slab.
 */
internal fun computeHoloGeoBounds(points: List<Pair<Double, Double>>): HoloGeoBounds {
    if (points.isEmpty()) {
        return HoloGeoBounds(
            centerLat = 8.1650,
            centerLon = 123.3465,
            halfLatSpan = 0.025,
            halfLonSpan = 0.025
        )
    }
    val minLat = points.minOf { it.first }
    val maxLat = points.maxOf { it.first }
    val minLon = points.minOf { it.second }
    val maxLon = points.maxOf { it.second }
    val centerLat = (minLat + maxLat) / 2.0
    val centerLon = (minLon + maxLon) / 2.0
    val cosLat = cos(Math.toRadians(centerLat)).coerceAtLeast(0.2)

    val halfLatRaw = abs(maxLat - minLat) / 2.0
    val halfLonMetricRaw = (abs(maxLon - minLon) / 2.0) * cosLat
    val paddedMetricHalf = max(halfLatRaw, halfLonMetricRaw).coerceAtLeast(0.011) * 1.52

    return HoloGeoBounds(
        centerLat = centerLat,
        centerLon = centerLon,
        halfLatSpan = paddedMetricHalf,
        halfLonSpan = paddedMetricHalf / cosLat
    )
}

/**
 * Chooses a sharp OpenStreetMap tile zoom level (targeting ~3-4 tiles across the floating slab)
 * so real streets, rivers, and place labels stay crisp in 3D perspective without excessive downloads.
 */
internal fun computeSlabTileZoom(bounds: HoloGeoBounds): Int {
    val lonSpanDeg = (bounds.halfLonSpan * 2.0 * HOLO_SLAB_HALF_EXTENT).coerceAtLeast(0.004)
    val desiredTileLonDeg = lonSpanDeg / 3.2
    return floor(log2(360.0 / desiredTileLonDeg)).toInt().coerceIn(11, 16)
}

/**
 * Computes the exact OpenStreetMap tile index grid needed to cover the full floating slab deck.
 */
internal fun computeSlabTileRange(bounds: HoloGeoBounds, tileZoom: Int): SlabTileGrid {
    val northLat = bounds.centerLat + HOLO_SLAB_HALF_EXTENT * bounds.halfLatSpan
    val southLat = bounds.centerLat - HOLO_SLAB_HALF_EXTENT * bounds.halfLatSpan
    val westLon = bounds.centerLon - HOLO_SLAB_HALF_EXTENT * bounds.halfLonSpan
    val eastLon = bounds.centerLon + HOLO_SLAB_HALF_EXTENT * bounds.halfLonSpan

    val (nwX, nwY) = OsmProjection.latLonToTileCoord(northLat, westLon, tileZoom)
    val (seX, seY) = OsmProjection.latLonToTileCoord(southLat, eastLon, tileZoom)
    val maxIdx = (1 shl tileZoom) - 1

    return SlabTileGrid(
        tileZoom = tileZoom,
        minTx = floor(min(nwX, seX)).toInt().coerceIn(0, maxIdx),
        maxTx = floor(max(nwX, seX)).toInt().coerceIn(0, maxIdx),
        minTy = floor(min(nwY, seY)).toInt().coerceIn(0, maxIdx),
        maxTy = floor(max(nwY, seY)).toInt().coerceIn(0, maxIdx)
    )
}

/**
 * Computes the exact destination rectangle of an OpenStreetMap tile (z, tx, ty) inside the
 * square slab map texture bitmap so real map roads match pin coordinates sub-pixel accurately.
 */
internal fun tileRectInSlabTexture(
    tileZoom: Int,
    tx: Int,
    ty: Int,
    bounds: HoloGeoBounds,
    textureSizePx: Float
): SlabTextureRect {
    val n = 2.0.pow(tileZoom)
    val westLon = (tx / n) * 360.0 - 180.0
    val eastLon = ((tx + 1) / n) * 360.0 - 180.0
    val northLat = Math.toDegrees(atan(sinh(PI * (1.0 - 2.0 * ty / n))))
    val southLat = Math.toDegrees(atan(sinh(PI * (1.0 - 2.0 * (ty + 1) / n))))

    val nwLocal = geoToSlabLocal(northLat, westLon, bounds)
    val seLocal = geoToSlabLocal(southLat, eastLon, bounds)
    val span = 2f * HOLO_SLAB_HALF_EXTENT

    return SlabTextureRect(
        left = ((nwLocal.x + HOLO_SLAB_HALF_EXTENT) / span) * textureSizePx,
        top = ((nwLocal.y + HOLO_SLAB_HALF_EXTENT) / span) * textureSizePx,
        right = ((seLocal.x + HOLO_SLAB_HALF_EXTENT) / span) * textureSizePx,
        bottom = ((seLocal.y + HOLO_SLAB_HALF_EXTENT) / span) * textureSizePx
    )
}

/**
 * Maps geographic (latitude, longitude) to local 3D slab coordinates where:
 * x in [-1, 1] runs West -> East, y in [-1, 1] runs North -> South, z is elevation above slab.
 */
internal fun geoToSlabLocal(
    lat: Double,
    lon: Double,
    bounds: HoloGeoBounds,
    z: Float = 0f
): Vec3 {
    val nx = ((lon - bounds.centerLon) / bounds.halfLonSpan).toFloat()
    val ny = ((bounds.centerLat - lat) / bounds.halfLatSpan).toFloat()
    return Vec3(nx, ny, z)
}

/**
 * True perspective projection from local 3D slab coordinates to 2D Compose viewport coordinates.
 * Higher [Vec3.z] projects upward (smaller screen Y) and slightly closer to the camera.
 */
internal fun project3DPoint(
    point: Vec3,
    yawDeg: Float,
    pitchDeg: Float,
    zoom: Float,
    floatOffsetY: Float,
    viewportWidth: Float,
    viewportHeight: Float
): Projected3D {
    val yawRad = Math.toRadians(yawDeg.toDouble())
    val pitchRad = Math.toRadians(pitchDeg.toDouble())
    val cosYaw = cos(yawRad).toFloat()
    val sinYaw = sin(yawRad).toFloat()
    val cosPitch = cos(pitchRad).toFloat()
    val sinPitch = sin(pitchRad).toFloat()

    // Rotate around slab vertical Z-axis (orbit)
    val rx = point.x * cosYaw - point.y * sinYaw
    val ry = point.x * sinYaw + point.y * cosYaw
    val rz = point.z

    // Tilt around horizontal X-axis (pitch)
    val camX = rx
    val camY = ry * cosPitch - rz * sinPitch
    val camZ = ry * sinPitch + rz * cosPitch

    val denom = (HOLO_CAMERA_DISTANCE - camZ).coerceAtLeast(0.65f)
    val persp = HOLO_CAMERA_DISTANCE / denom
    val baseScale = min(viewportWidth, viewportHeight) * 0.34f * zoom

    val screenX = viewportWidth * 0.5f + camX * baseScale * persp
    val screenY = viewportHeight * 0.54f + floatOffsetY + camY * baseScale * persp

    return Projected3D(
        screen = Offset(screenX, screenY),
        depth = camZ,
        perspectiveScale = persp
    )
}

/**
 * Generates an elevated 3D parabolic polyline/arc connecting a sequence of slab waypoints.
 * Each segment smoothly rises above the slab surface to highlight the point-to-point link.
 */
internal fun buildElevated3DPolyline(
    waypoints: List<Vec3>,
    subdivisionsPerSegment: Int = 22,
    maxArcHeight: Float = 0.34f
): List<Vec3> {
    if (waypoints.size < 2) return waypoints
    val steps = subdivisionsPerSegment.coerceAtLeast(4)
    val out = ArrayList<Vec3>((waypoints.size - 1) * steps + 1)

    for (seg in 0 until waypoints.lastIndex) {
        val a = waypoints[seg]
        val b = waypoints[seg + 1]
        val dist = hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble()).toFloat()
        val segArcHeight = (0.14f + dist * 0.17f).coerceIn(0.14f, maxArcHeight)

        val startIndex = if (seg == 0) 0 else 1
        for (i in startIndex..steps) {
            val t = i.toFloat() / steps.toFloat()
            val x = a.x + (b.x - a.x) * t
            val y = a.y + (b.y - a.y) * t
            val baseZ = a.z + (b.z - a.z) * t
            val lift = sin(PI * t).toFloat() * segArcHeight
            out.add(Vec3(x, y, baseZ + lift))
        }
    }
    return out
}

internal fun sample3DPolylineAtFraction(points: List<Vec3>, fraction: Float): Vec3 {
    if (points.isEmpty()) return Vec3(0f, 0f, 0f)
    if (points.size == 1) return points.first()
    val clamped = fraction.coerceIn(0f, 1f)
    val scaled = clamped * (points.size - 1)
    val idx = scaled.toInt().coerceIn(0, points.lastIndex - 1)
    val localT = scaled - idx
    val a = points[idx]
    val b = points[idx + 1]
    return Vec3(
        x = a.x + (b.x - a.x) * localT,
        y = a.y + (b.y - a.y) * localT,
        z = a.z + (b.z - a.z) * localT
    )
}

internal fun isInsideSlab(point: Vec3, margin: Float = 0.02f): Boolean {
    val limit = HOLO_SLAB_HALF_EXTENT - margin
    return abs(point.x) <= limit && abs(point.y) <= limit
}

internal fun formatCoordLabel(lat: Double, lon: Double): String {
    return "${"%.4f".format(lat)}°N, ${"%.4f".format(lon)}°E"
}

