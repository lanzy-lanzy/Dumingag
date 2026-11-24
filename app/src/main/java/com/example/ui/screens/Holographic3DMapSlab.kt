package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DumingagSeedData
import com.example.data.TravelCompanionEngines
import com.example.data.remote.OsmTileLoader
import com.example.model.AppLanguage
import com.example.model.Attraction
import com.example.model.OfflineRoutePlan
import com.example.model.TravelMode
import com.example.ui.components.AttractionReviewSummary
import com.example.ui.components.MapPinStarBadge
import com.example.ui.theme.HoloArcCore
import com.example.ui.theme.HoloArcGlow
import com.example.ui.theme.HoloContourLine
import com.example.ui.theme.HoloEndPinAccent
import com.example.ui.theme.HoloGridLine
import com.example.ui.theme.HoloHudBorder
import com.example.ui.theme.HoloHudSurface
import com.example.ui.theme.HoloRoadLine
import com.example.ui.theme.HoloScanlineGlow
import com.example.ui.theme.HoloShadowColor
import com.example.ui.theme.HoloSlabBottomFace
import com.example.ui.theme.HoloSlabEdgeGlow
import com.example.ui.theme.HoloSlabSpecular
import com.example.ui.theme.HoloSlabTopEnd
import com.example.ui.theme.HoloSlabTopStart
import com.example.ui.theme.HoloSlabWallLeft
import com.example.ui.theme.HoloSlabWallRight
import com.example.ui.theme.HoloSpaceBgBottom
import com.example.ui.theme.HoloSpaceBgMid
import com.example.ui.theme.HoloSpaceBgTop
import com.example.ui.theme.HoloStartPinAccent
import com.example.ui.theme.HoloWaypointAccent
import com.example.ui.theme.MapContourRing
import com.example.ui.theme.MapHighwayCasing
import com.example.ui.theme.MapHighwayFill
import com.example.ui.theme.MapTerrainFallbackBg
import com.example.ui.theme.MapTrailStroke
import com.example.ui.theme.SplashGlassTextPrimary
import com.example.ui.theme.SplashGlassTextSecondary
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

private const val SLAB_TEXTURE_SIZE_PX = 768

private data class HoloPinAnchor(
    val label: String,
    val badgeTag: String,
    val lat: Double,
    val lon: Double,
    val accent: Color
)

private data class SlabWallFace(
    val corners: List<Vec3>,
    val tint: Color,
    val averageDepth: Float
)

private data class CompositedRealMapSlab(
    val bitmap: Bitmap?,
    val loadedTileCount: Int
)

@Composable
fun Holographic3DMapSlab(
    language: AppLanguage,
    attractions: List<Attraction>,
    selectedAttraction: Attraction?,
    markedAttractions: List<Attraction>,
    activeRoute: OfflineRoutePlan?,
    travelMode: TravelMode,
    userLatLon: Pair<Double, Double>,
    isFullscreen: Boolean,
    hasBottomOverlay: Boolean,
    loadedTiles: MutableMap<String, ImageBitmap>,
    tileTrigger: Int,
    osmTileLoader: OsmTileLoader,
    isEffectiveOffline: Boolean,
    onTileLoaded: () -> Unit,
    onToggle3DMode: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onSelectAttraction: (Attraction) -> Unit,
    reviewSummaries: Map<String, AttractionReviewSummary> = emptyMap(),
    onOpenAttractionReviews: (Attraction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Interactive 3D camera state
    var yawDeg by remember { mutableFloatStateOf(HOLO_DEFAULT_YAW_DEG) }
    var pitchDeg by remember { mutableFloatStateOf(HOLO_DEFAULT_PITCH_DEG) }
    var zoomScale by remember { mutableFloatStateOf(1.06f) }
    var autoOrbit by remember { mutableStateOf(false) }
    var isFlyingRoute by remember { mutableStateOf(false) }
    var flyoverProgress by remember { mutableFloatStateOf(0f) }
    // Default false = Vivid Real OpenStreetMap Cartography on floating 3D glass slab; true = Holographic Cyan Tint
    var useHoloTint by remember { mutableStateOf(false) }

    // Optional custom Start pin override so users can link any 2 coordinates/spots point-to-point
    var customStartAttraction by remember { mutableStateOf<Attraction?>(null) }
    var tapSetsStartPin by remember { mutableStateOf(false) }

    LaunchedEffect(autoOrbit) {
        if (!autoOrbit) return@LaunchedEffect
        var lastNanos = 0L
        while (autoOrbit) {
            withFrameNanos { frameNanos ->
                if (lastNanos != 0L) {
                    val dt = ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    yawDeg = (yawDeg + dt * 14f) % 360f
                }
                lastNanos = frameNanos
            }
        }
    }

    LaunchedEffect(isFlyingRoute) {
        if (!isFlyingRoute) return@LaunchedEffect
        autoOrbit = false
        flyoverProgress = 0f
        val startYaw = yawDeg
        var elapsedSec = 0f
        val totalDurationSec = 7.5f
        var lastNanos = 0L
        while (isFlyingRoute && elapsedSec < totalDurationSec) {
            withFrameNanos { frameNanos ->
                if (lastNanos != 0L) {
                    val dt = ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    elapsedSec += dt
                    val p = (elapsedSec / totalDurationSec).coerceIn(0f, 1f)
                    flyoverProgress = p
                    yawDeg = (startYaw + p * 115f) % 360f
                    pitchDeg = (HOLO_DEFAULT_PITCH_DEG - 10f * sin(p * PI).toFloat())
                        .coerceIn(HOLO_MIN_PITCH_DEG, HOLO_MAX_PITCH_DEG)
                    zoomScale = (1.08f + 0.34f * sin(p * PI).toFloat())
                        .coerceIn(HOLO_MIN_ZOOM, HOLO_MAX_ZOOM)
                }
                lastNanos = frameNanos
            }
        }
        isFlyingRoute = false
    }

    val holoTransition = rememberInfiniteTransition(label = "holoSlabMotion")
    val floatPhase by holoTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "floatPhase"
    )
    val energyPhase by holoTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "energyPhase"
    )
    val scanlineY by holoTransition.animateFloat(
        initialValue = -HOLO_SLAB_HALF_EXTENT,
        targetValue = HOLO_SLAB_HALF_EXTENT,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanlineY"
    )
    val pulseRadius by holoTransition.animateFloat(
        initialValue = 0.045f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )

    // Resolve Start & End pins and intermediate polyline route nodes
    val effectiveMarks = remember(markedAttractions, selectedAttraction) {
        markedAttractions.ifEmpty { listOfNotNull(selectedAttraction) }
    }
    val startLatLon = customStartAttraction?.let { it.latitude to it.longitude } ?: userLatLon
    val startName = customStartAttraction?.name?.get(language) ?: "Current Location"

    val defaultEndAttraction = remember(attractions, startLatLon) {
        attractions.firstOrNull {
            hypot(it.latitude - startLatLon.first, it.longitude - startLatLon.second) > 0.002
        } ?: attractions.firstOrNull()
    }
    val endAttraction = activeRoute?.destination
        ?: effectiveMarks.lastOrNull()
        ?: selectedAttraction
        ?: defaultEndAttraction

    val endLatLon = endAttraction?.let { it.latitude to it.longitude }
        ?: (DumingagSeedData.CENTER_LAT + 0.022 to DumingagSeedData.CENTER_LON - 0.018)
    val endName = endAttraction?.name?.get(language) ?: "Dumingag Peak"

    val nodesById = remember { DumingagSeedData.mapNodes.associateBy { it.id } }

    // Ordered geographic polyline path from START -> intermediate stops/junctions -> END
    val routeGeoPoints = remember(startLatLon, endLatLon, effectiveMarks, activeRoute, customStartAttraction) {
        buildList {
            add(startLatLon)
            if (activeRoute != null && activeRoute.nodePathIds.isNotEmpty() && customStartAttraction == null) {
                activeRoute.nodePathIds.forEachIndexed { idx, nodeId ->
                    val node = nodesById[nodeId] ?: return@forEachIndexed
                    val pt = node.latitude to node.longitude
                    val isDupStart = idx == 0 && hypot(pt.first - startLatLon.first, pt.second - startLatLon.second) < 0.001
                    val isDupEnd = hypot(pt.first - endLatLon.first, pt.second - endLatLon.second) < 0.001
                    if (!isDupStart && !isDupEnd) add(pt)
                }
            } else if (effectiveMarks.size > 1) {
                effectiveMarks.dropLast(1).forEach { stop ->
                    val pt = stop.latitude to stop.longitude
                    if (hypot(pt.first - startLatLon.first, pt.second - startLatLon.second) > 0.0005) {
                        add(pt)
                    }
                }
            }
            add(endLatLon)
        }
    }

    val linkDistanceKm = remember(routeGeoPoints, activeRoute, customStartAttraction) {
        if (activeRoute != null && customStartAttraction == null) {
            activeRoute.totalDistanceKm
        } else {
            var sum = 0.0
            for (i in 0 until routeGeoPoints.lastIndex) {
                sum += haversineDistanceKmExact(
                    routeGeoPoints[i].first,
                    routeGeoPoints[i].second,
                    routeGeoPoints[i + 1].first,
                    routeGeoPoints[i + 1].second
                )
            }
            sum
        }
    }
    val linkMetrics = remember(linkDistanceKm, travelMode) {
        calculateMetricsForDistance(linkDistanceKm, travelMode)
    }

    val bounds = remember(routeGeoPoints) {
        computeHoloGeoBounds(routeGeoPoints)
    }
    val elevationProfile = remember(routeGeoPoints) {
        TravelCompanionEngines.buildElevationProfile(routeGeoPoints, samplesPerSegment = 8)
    }

    // Load real OpenStreetMap tiles covering the exact floating 3D slab bounds
    val tileGrid = remember(bounds) {
        val z = computeSlabTileZoom(bounds)
        computeSlabTileRange(bounds, z)
    }

    LaunchedEffect(tileGrid, isEffectiveOffline) {
        for (tx in tileGrid.minTx..tileGrid.maxTx) {
            for (ty in tileGrid.minTy..tileGrid.maxTy) {
                val key = "${tileGrid.tileZoom}_${tx}_$ty"
                if (!loadedTiles.containsKey(key)) {
                    osmTileLoader.getCachedInMemory(tileGrid.tileZoom, tx, ty)?.let {
                        loadedTiles[key] = it
                        onTileLoaded()
                    } ?: launch {
                        val loaded = osmTileLoader.loadTile(
                            z = tileGrid.tileZoom,
                            x = tx,
                            y = ty,
                            allowNetwork = !isEffectiveOffline
                        )
                        if (loaded != null) {
                            loadedTiles[key] = loaded
                            onTileLoaded()
                        }
                    }
                }
            }
        }
    }

    // Composite all available real OSM tiles into a single seamless square slab texture
    val compositedSlab = remember(tileGrid, bounds, tileTrigger, loadedTiles.size) {
        buildRealMapSlabTexture(
            tileGrid = tileGrid,
            bounds = bounds,
            loadedTiles = loadedTiles
        )
    }

    val realMapPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            alpha = 248
        }
    }
    val holoTintPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            alpha = 215
            // Shifts real OSM map tiles into a cool cyan-emerald holographic palette while preserving roads & labels
            colorFilter = ColorMatrixColorFilter(
                ColorMatrix(
                    floatArrayOf(
                        0.28f, 0.08f, 0.12f, 0f, -12f,
                        0.16f, 0.72f, 0.24f, 0f, 18f,
                        0.18f, 0.28f, 0.85f, 0f, 26f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
    }
    val polyMatrix = remember { Matrix() }

    val startAnchor = HoloPinAnchor(
        label = startName,
        badgeTag = "START",
        lat = startLatLon.first,
        lon = startLatLon.second,
        accent = HoloStartPinAccent
    )
    val endAnchor = HoloPinAnchor(
        label = endName,
        badgeTag = "END",
        lat = endLatLon.first,
        lon = endLatLon.second,
        accent = HoloEndPinAccent
    )

    val containerShape = if (isFullscreen) RoundedCornerShape(0.dp) else RoundedCornerShape(24.dp)
    val frameModifier = if (isFullscreen) {
        modifier
            .background(Brush.verticalGradient(listOf(HoloSpaceBgTop, HoloSpaceBgMid, HoloSpaceBgBottom)))
            .clipToBounds()
    } else {
        modifier
            .clip(containerShape)
            .background(Brush.verticalGradient(listOf(HoloSpaceBgTop, HoloSpaceBgMid, HoloSpaceBgBottom)))
            .border(1.dp, HoloHudBorder, containerShape)
            .clipToBounds()
    }

    BoxWithConstraints(
        modifier = frameModifier.testTag("map_3d_holographic_slab")
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val heightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        val floatBobPx = sin(floatPhase) * with(density) { 5.dp.toPx() }

        fun project(v: Vec3): Projected3D = project3DPoint(
            point = v,
            yawDeg = yawDeg,
            pitchDeg = pitchDeg,
            zoom = zoomScale,
            floatOffsetY = floatBobPx,
            viewportWidth = widthPx,
            viewportHeight = heightPx
        )

        val localWaypoints = remember(routeGeoPoints, bounds) {
            routeGeoPoints.map { (lat, lon) -> geoToSlabLocal(lat, lon, bounds, z = 0.008f) }
        }
        val elevatedArc = remember(localWaypoints) {
            buildElevated3DPolyline(localWaypoints, subdivisionsPerSegment = 24, maxArcHeight = 0.36f)
        }

        // Interactive 3D orbit, pitch-tilt & pinch-zoom surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        if (pan != Offset.Zero) {
                            autoOrbit = false
                            isFlyingRoute = false
                            yawDeg = (yawDeg + pan.x * 0.32f) % 360f
                            pitchDeg = (pitchDeg + pan.y * 0.24f).coerceIn(
                                HOLO_MIN_PITCH_DEG,
                                HOLO_MAX_PITCH_DEG
                            )
                        }
                        if (gestureZoom != 1f && gestureZoom > 0f) {
                            zoomScale = (zoomScale * gestureZoom).coerceIn(
                                HOLO_MIN_ZOOM,
                                HOLO_MAX_ZOOM
                            )
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val hasRealTiles = compositedSlab.loadedTileCount > 0 && compositedSlab.bitmap != null

                // 1. Ambient holographic radial aura behind levitating slab
                val centerOffset = Offset(size.width * 0.5f, size.height * 0.54f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            HoloSlabEdgeGlow.copy(alpha = 0.16f),
                            HoloStartPinAccent.copy(alpha = 0.07f),
                            Color.Transparent
                        ),
                        center = centerOffset,
                        radius = size.minDimension * 0.62f
                    ),
                    radius = size.minDimension * 0.62f,
                    center = centerOffset
                )

                // 2. Soft levitation cast shadow underneath the floating slab
                val shadowScale = (1f - sin(floatPhase) * 0.045f) * zoomScale
                val shadowW = size.minDimension * 0.72f * shadowScale
                val shadowH = shadowW * (0.24f + (1f - pitchDeg / 90f) * 0.28f)
                val shadowCenter = Offset(size.width * 0.5f, size.height * 0.54f + size.minDimension * 0.27f)
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            HoloShadowColor,
                            HoloSlabEdgeGlow.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = shadowCenter,
                        radius = shadowW * 0.6f
                    ),
                    topLeft = Offset(shadowCenter.x - shadowW * 0.5f, shadowCenter.y - shadowH * 0.5f),
                    size = Size(shadowW, shadowH)
                )

                // 3. Extruded 3D Floating Glass Slab Geometry
                val e = HOLO_SLAB_HALF_EXTENT
                val zBot = -HOLO_SLAB_THICKNESS
                val topCorners = listOf(
                    Vec3(-e, -e, 0f),
                    Vec3(e, -e, 0f),
                    Vec3(e, e, 0f),
                    Vec3(-e, e, 0f)
                )
                val botCorners = topCorners.map { it.copy(z = zBot) }
                val projectedTop = topCorners.map(::project)
                val projectedBot = botCorners.map(::project)

                // Bottom frosted face
                drawQuadPath(
                    points = projectedBot.map { it.screen },
                    fillBrush = Brush.linearGradient(
                        colors = listOf(HoloSlabBottomFace, HoloSpaceBgTop.copy(alpha = 0.72f)),
                        start = projectedBot[0].screen,
                        end = projectedBot[2].screen
                    ),
                    borderColor = HoloSlabEdgeGlow.copy(alpha = 0.35f),
                    borderWidthPx = 1.dp.toPx()
                )

                // Depth-sorted extruded translucent glass side walls
                val walls = listOf(
                    SlabWallFace(
                        corners = listOf(topCorners[0], topCorners[1], botCorners[1], botCorners[0]),
                        tint = HoloSlabWallRight,
                        averageDepth = (projectedTop[0].depth + projectedTop[1].depth) / 2f
                    ),
                    SlabWallFace(
                        corners = listOf(topCorners[1], topCorners[2], botCorners[2], botCorners[1]),
                        tint = HoloSlabWallLeft,
                        averageDepth = (projectedTop[1].depth + projectedTop[2].depth) / 2f
                    ),
                    SlabWallFace(
                        corners = listOf(topCorners[2], topCorners[3], botCorners[3], botCorners[2]),
                        tint = HoloSlabWallLeft,
                        averageDepth = (projectedTop[2].depth + projectedTop[3].depth) / 2f
                    ),
                    SlabWallFace(
                        corners = listOf(topCorners[3], topCorners[0], botCorners[0], botCorners[3]),
                        tint = HoloSlabWallRight,
                        averageDepth = (projectedTop[3].depth + projectedTop[0].depth) / 2f
                    )
                ).sortedBy { it.averageDepth }

                walls.forEach { wall ->
                    val proj = wall.corners.map(::project).map { it.screen }
                    drawQuadPath(
                        points = proj,
                        fillBrush = Brush.verticalGradient(
                            colors = listOf(
                                wall.tint.copy(alpha = 0.78f),
                                HoloSlabBottomFace.copy(alpha = 0.90f)
                            )
                        ),
                        borderColor = HoloSlabEdgeGlow.copy(alpha = 0.62f),
                        borderWidthPx = 1.1.dp.toPx()
                    )
                }

                // 4. Real OpenStreetMap Cartography Projected in True 3D Perspective onto the Floating Slab Deck
                val topScreen = projectedTop.map { it.screen }
                val topSlabPath = Path().apply {
                    moveTo(topScreen[0].x, topScreen[0].y)
                    lineTo(topScreen[1].x, topScreen[1].y)
                    lineTo(topScreen[2].x, topScreen[2].y)
                    lineTo(topScreen[3].x, topScreen[3].y)
                    close()
                }

                // Base slab deck fill (natural cartography terrain in Real mode, deep glass in Holo Tint mode)
                drawPath(
                    path = topSlabPath,
                    brush = if (useHoloTint) {
                        Brush.linearGradient(
                            colors = listOf(HoloSlabTopStart, HoloSlabTopEnd),
                            start = topScreen[0],
                            end = topScreen[2]
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(MapTerrainFallbackBg, MapTerrainFallbackBg.copy(alpha = 0.94f)),
                            start = topScreen[0],
                            end = topScreen[2]
                        )
                    }
                )

                // Draw perspective-warped real OpenStreetMap texture inside the slab deck
                val slabBitmap = compositedSlab.bitmap
                if (hasRealTiles && slabBitmap != null) {
                    val texMax = SLAB_TEXTURE_SIZE_PX.toFloat()
                    val srcQuad = floatArrayOf(
                        0f, 0f,
                        texMax, 0f,
                        texMax, texMax,
                        0f, texMax
                    )
                    val dstQuad = floatArrayOf(
                        topScreen[0].x, topScreen[0].y,
                        topScreen[1].x, topScreen[1].y,
                        topScreen[2].x, topScreen[2].y,
                        topScreen[3].x, topScreen[3].y
                    )
                    polyMatrix.reset()
                    if (polyMatrix.setPolyToPoly(srcQuad, 0, dstQuad, 0, 4)) {
                        clipPath(topSlabPath) {
                            drawIntoCanvas { canvas ->
                                canvas.nativeCanvas.drawBitmap(
                                    slabBitmap,
                                    polyMatrix,
                                    if (useHoloTint) holoTintPaint else realMapPaint
                                )
                            }
                        }
                    }
                }

                // Offline / pre-load fallback: draw real Dumingag road network & terrain contours onto slab
                if (!hasRealTiles || useHoloTint) {
                    val timolanLocal = geoToSlabLocal(8.1360, 123.3110, bounds, z = 0.003f)
                    val contourColor = if (useHoloTint) HoloContourLine else MapContourRing
                    listOf(0.24f, 0.42f, 0.62f).forEach { r ->
                        drawProjectedSlabRing(
                            center = timolanLocal,
                            radius = r,
                            color = contourColor,
                            strokeWidthPx = 1.2.dp.toPx(),
                            project = ::project
                        )
                    }
                    DumingagSeedData.mapEdges.forEach { edge ->
                        val n1 = nodesById[edge.fromId] ?: return@forEach
                        val n2 = nodesById[edge.toId] ?: return@forEach
                        val v1 = geoToSlabLocal(n1.latitude, n1.longitude, bounds, z = 0.004f)
                        val v2 = geoToSlabLocal(n2.latitude, n2.longitude, bounds, z = 0.004f)
                        if (isInsideSlab(v1) && isInsideSlab(v2)) {
                            val p1 = project(v1).screen
                            val p2 = project(v2).screen
                            val isHighway = edge.roadType.contains("Highway") || edge.roadType.contains("Town")
                            if (useHoloTint) {
                                drawLine(
                                    color = HoloRoadLine,
                                    start = p1,
                                    end = p2,
                                    strokeWidth = if (isHighway) 2.2.dp.toPx() else 1.4.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            } else if (isHighway) {
                                drawLine(MapHighwayCasing, p1, p2, strokeWidth = 4.2.dp.toPx(), cap = StrokeCap.Round)
                                drawLine(MapHighwayFill, p1, p2, strokeWidth = 2.4.dp.toPx(), cap = StrokeCap.Round)
                            } else {
                                drawLine(MapTrailStroke, p1, p2, strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round)
                            }
                        }
                    }
                }

                // Subtle glass sheen + holographic grid/scanline overlay (stronger in Holo mode, whisper-light in Real mode)
                clipPath(topSlabPath) {
                    drawPath(
                        path = topSlabPath,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                HoloSlabSpecular.copy(alpha = if (useHoloTint) 0.14f else 0.16f),
                                Color.Transparent,
                                HoloSlabEdgeGlow.copy(alpha = if (useHoloTint) 0.12f else 0.05f)
                            ),
                            start = topScreen[0],
                            end = topScreen[2]
                        )
                    )

                    if (useHoloTint) {
                        val gridDivisions = 8
                        for (i in 1 until gridDivisions) {
                            val t = -e + (2f * e * i / gridDivisions)
                            drawLine(
                                HoloGridLine,
                                project(Vec3(t, -e, 0.002f)).screen,
                                project(Vec3(t, e, 0.002f)).screen,
                                strokeWidth = 1.dp.toPx()
                            )
                            drawLine(
                                HoloGridLine,
                                project(Vec3(-e, t, 0.002f)).screen,
                                project(Vec3(e, t, 0.002f)).screen,
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }

                    // Animated subtle holographic scanline sweeping across the floating map glass
                    drawLine(
                        color = HoloScanlineGlow.copy(alpha = if (useHoloTint) 0.42f else 0.18f),
                        start = project(Vec3(-e, scanlineY, 0.004f)).screen,
                        end = project(Vec3(e, scanlineY, 0.004f)).screen,
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Crisp luminous aquamarine glass rim framing the floating 3D map slab
                drawPath(
                    path = topSlabPath,
                    color = HoloSlabSpecular.copy(alpha = 0.42f),
                    style = Stroke(width = 4.dp.toPx(), join = StrokeJoin.Round)
                )
                drawPath(
                    path = topSlabPath,
                    color = HoloSlabEdgeGlow.copy(alpha = 0.94f),
                    style = Stroke(width = 1.9.dp.toPx(), join = StrokeJoin.Round)
                )

                // 5. Ground-Projected Point-to-Point Polyline on Real Map Surface (with dark casing for legibility)
                if (localWaypoints.size >= 2) {
                    val groundPath = Path().apply {
                        val first = project(localWaypoints.first()).screen
                        moveTo(first.x, first.y)
                        for (i in 1 until localWaypoints.size) {
                            val pt = project(localWaypoints[i]).screen
                            lineTo(pt.x, pt.y)
                        }
                    }
                    // High-contrast casing so route line pops vividly over real bright map tiles
                    drawPath(
                        path = groundPath,
                        color = HoloSpaceBgTop.copy(alpha = 0.62f),
                        style = Stroke(
                            width = 5.2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                    drawPath(
                        path = groundPath,
                        color = HoloSlabEdgeGlow.copy(alpha = 0.92f),
                        style = Stroke(
                            width = 2.6.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), -energyPhase * 40f)
                        )
                    )
                }

                // 6. Elevated 3D Holographic Polyline / Arc + Height Drop-Lines + Animated Pulses
                if (elevatedArc.size >= 2) {
                    val curtainStep = (elevatedArc.size / 7).coerceAtLeast(3)
                    for (i in curtainStep until elevatedArc.lastIndex step curtainStep) {
                        val topPt = project(elevatedArc[i]).screen
                        val botPt = project(elevatedArc[i].copy(z = 0.005f)).screen
                        drawLine(
                            color = HoloStartPinAccent.copy(alpha = 0.34f),
                            start = botPt,
                            end = topPt,
                            strokeWidth = 1.1.dp.toPx()
                        )
                    }

                    val arcScreenPts = elevatedArc.map { project(it).screen }
                    val arcPath = Path().apply {
                        moveTo(arcScreenPts.first().x, arcScreenPts.first().y)
                        for (i in 1 until arcScreenPts.size) {
                            lineTo(arcScreenPts[i].x, arcScreenPts[i].y)
                        }
                    }
                    val startScreen = arcScreenPts.first()
                    val endScreen = arcScreenPts.last()

                    // Soft dark halo so the elevated 3D polyline stays crisp over bright OSM tiles
                    drawPath(
                        path = arcPath,
                        color = HoloSpaceBgTop.copy(alpha = 0.38f),
                        style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    // Outer neon holographic bloom
                    drawPath(
                        path = arcPath,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                HoloStartPinAccent.copy(alpha = 0.50f),
                                HoloArcGlow,
                                HoloEndPinAccent.copy(alpha = 0.52f)
                            ),
                            start = startScreen,
                            end = endScreen
                        ),
                        style = Stroke(width = 8.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    // Mid luminous beam
                    drawPath(
                        path = arcPath,
                        brush = Brush.linearGradient(
                            colors = listOf(HoloStartPinAccent, HoloSlabEdgeGlow, HoloEndPinAccent),
                            start = startScreen,
                            end = endScreen
                        ),
                        style = Stroke(width = 4.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    // Razor-crisp bright holographic core
                    drawPath(
                        path = arcPath,
                        color = HoloArcCore,
                        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // 3 animated traveling light pulses flowing START -> END
                    listOf(0f, 0.33f, 0.66f).forEach { offset ->
                        val frac = (energyPhase + offset) % 1f
                        val packetPt = project(sample3DPolylineAtFraction(elevatedArc, frac)).screen
                        drawCircle(
                            color = HoloSpaceBgTop.copy(alpha = 0.45f),
                            radius = 7.5.dp.toPx(),
                            center = packetPt
                        )
                        drawCircle(
                            color = HoloSlabEdgeGlow.copy(alpha = 0.62f),
                            radius = 6.dp.toPx(),
                            center = packetPt
                        )
                        drawCircle(
                            color = HoloArcCore,
                            radius = 3.dp.toPx(),
                            center = packetPt
                        )
                    }

                    // Active One-Tap 3D Route Flyover Beacon gliding along the elevated polyline
                    if (isFlyingRoute) {
                        val flyPt = sample3DPolylineAtFraction(elevatedArc, flyoverProgress)
                        val flyScreen = project(flyPt).screen
                        val flyGround = project(flyPt.copy(z = 0.006f)).screen
                        drawLine(
                            color = HoloArcCore.copy(alpha = 0.85f),
                            start = flyGround,
                            end = flyScreen,
                            strokeWidth = 2.dp.toPx()
                        )
                        drawCircle(color = HoloEndPinAccent.copy(alpha = 0.35f), radius = 15.dp.toPx(), center = flyScreen)
                        drawCircle(color = HoloSlabEdgeGlow, radius = 8.dp.toPx(), center = flyScreen)
                        drawCircle(color = HoloArcCore, radius = 4.dp.toPx(), center = flyScreen)
                    }
                }

                // 7. Intermediate Waypoint Beacons (for multi-stop routes)
                if (localWaypoints.size > 2) {
                    for (i in 1 until localWaypoints.lastIndex) {
                        drawHoloPinBeacon(
                            baseLocal = localWaypoints[i],
                            beaconHeight = 0.14f,
                            accent = HoloWaypointAccent,
                            pulseRadius = pulseRadius * 0.65f,
                            project = ::project
                        )
                    }
                }

                // 8. START & END 3D Pin Beacons (sorted back-to-front by camera depth)
                val startLocal = geoToSlabLocal(startAnchor.lat, startAnchor.lon, bounds, z = 0.008f)
                val endLocal = geoToSlabLocal(endAnchor.lat, endAnchor.lon, bounds, z = 0.008f)
                val sortedBeacons = listOf(
                    startLocal to HoloStartPinAccent,
                    endLocal to HoloEndPinAccent
                ).sortedBy { (pt, _) -> project(pt).depth }

                sortedBeacons.forEach { (pt, color) ->
                    drawHoloPinBeacon(
                        baseLocal = pt,
                        beaconHeight = 0.25f,
                        accent = color,
                        pulseRadius = pulseRadius,
                        project = ::project
                    )
                }
            }

            // Clickable nearby Dumingag landmarks projected onto the real 3D map slab for live repinning + review stars
            attractions.forEach { spot ->
                val isCurrentEnd = endAttraction?.id == spot.id
                val isCurrentStart = customStartAttraction?.id == spot.id
                if (isCurrentEnd || isCurrentStart) return@forEach

                val localPt = geoToSlabLocal(spot.latitude, spot.longitude, bounds, z = 0.01f)
                if (!isInsideSlab(localPt, margin = 0.06f)) return@forEach
                val projected = project(localPt)
                val dotSize = 24.dp
                val halfDotPx = with(density) { (dotSize / 2).roundToPx() }
                val spotSummary = reviewSummaries[spot.id]

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset {
                        IntOffset(
                            x = projected.screen.x.roundToInt() - halfDotPx,
                            y = projected.screen.y.roundToInt() - halfDotPx
                        )
                    }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(dotSize)
                            .clip(CircleShape)
                            .clickable {
                                if (tapSetsStartPin) {
                                    customStartAttraction = spot
                                } else {
                                    onSelectAttraction(spot)
                                }
                            }
                            .semantics {
                                contentDescription = "Select ${spot.name.get(language)} on 3D map"
                            }
                            .testTag("map_3d_spot_${spot.id}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(HoloSpaceBgTop.copy(alpha = 0.88f))
                                .padding(1.5.dp)
                                .clip(CircleShape)
                                .background(HoloWaypointAccent)
                        )
                    }
                    if (spotSummary != null) {
                        MapPinStarBadge(
                            summary = spotSummary,
                            attractionName = spot.name.get(language),
                            onClick = { onOpenAttractionReviews(spot) },
                            compact = true,
                            darkSurface = true
                        )
                    }
                }
            }

            // Floating 3D START & END Pin Callout Badges anchored to the 3D beacon apexes
            val startApex = project(
                geoToSlabLocal(startAnchor.lat, startAnchor.lon, bounds, z = 0.27f)
            ).screen
            val endApex = project(
                geoToSlabLocal(endAnchor.lat, endAnchor.lon, bounds, z = 0.27f)
            ).screen

            HoloEndpointPinBadge(
                anchor = startAnchor,
                screenAnchor = startApex,
                viewportWidthPx = widthPx,
                viewportHeightPx = heightPx,
                testTagName = "map_3d_start_pin",
                reviewSummary = customStartAttraction?.let { reviewSummaries[it.id] },
                onOpenReviews = customStartAttraction?.let { target -> { onOpenAttractionReviews(target) } }
            )
            HoloEndpointPinBadge(
                anchor = endAnchor,
                screenAnchor = endApex,
                viewportWidthPx = widthPx,
                viewportHeightPx = heightPx,
                testTagName = "map_3d_end_pin",
                reviewSummary = endAttraction?.let { reviewSummaries[it.id] },
                onOpenReviews = endAttraction?.let { target -> { onOpenAttractionReviews(target) } }
            )
        }

        val topSafeModifier = if (isFullscreen) {
            Modifier
                .statusBarsPadding()
                .displayCutoutPadding()
        } else {
            Modifier
        }
        val bottomSafeModifier = if (isFullscreen) {
            Modifier
                .navigationBarsPadding()
                .displayCutoutPadding()
        } else {
            Modifier
        }

        // Top-Left Frosted Liquid Glass Start <-> End Coordinates & Telemetry HUD
        Surface(
            color = HoloHudSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, HoloHudBorder),
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .then(topSafeModifier)
                .padding(10.dp)
                .widthIn(max = 264.dp)
                .semantics(mergeDescendants = true) {}
                .testTag("map_3d_coords_hud")
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Route,
                        contentDescription = null,
                        tint = HoloSlabEdgeGlow,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (useHoloTint) "3D HOLOGRAPHIC MAP" else "3D REAL MAP SLAB",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                        color = HoloSlabEdgeGlow,
                        maxLines = 1
                    )
                }

                Text(
                    text = "${formatDistanceDisplay(linkMetrics.distanceKm)} · ${formatTravelHoursSummary(linkMetrics)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SplashGlassTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                HoloCoordinateRow(
                    tag = "START",
                    name = startAnchor.label,
                    coords = formatCoordLabel(startAnchor.lat, startAnchor.lon),
                    accent = HoloStartPinAccent
                )
                HoloCoordinateRow(
                    tag = "END",
                    name = endAnchor.label,
                    coords = formatCoordLabel(endAnchor.lat, endAnchor.lon),
                    accent = HoloEndPinAccent
                )
            }
        }

        // Top-Right Floating 3D Controls Stack
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .then(topSafeModifier)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = HoloSlabEdgeGlow.copy(alpha = 0.92f),
                border = BorderStroke(1.dp, HoloArcCore),
                shadowElevation = 4.dp
            ) {
                IconButton(
                    onClick = onToggle3DMode,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("map_3d_toggle_button")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Switch to 2D map view",
                            tint = HoloSpaceBgTop,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "2D",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = HoloSpaceBgTop
                        )
                    }
                }
            }

            HoloControlCircleButton(
                onClick = onToggleFullscreen,
                tag = "map_fullscreen_toggle_button",
                contentDescription = if (isFullscreen) "Exit fullscreen map" else "Enter fullscreen map",
                highlighted = isFullscreen
            ) {
                Icon(
                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = null,
                    tint = if (isFullscreen) HoloSlabEdgeGlow else SplashGlassTextPrimary
                )
            }

            // Toggle between vivid Real OSM Map and Holographic Cyan Tint
            HoloControlCircleButton(
                onClick = { useHoloTint = !useHoloTint },
                tag = "map_3d_style_toggle",
                contentDescription = if (useHoloTint) "Switch to vivid real map colors" else "Switch to holographic tint",
                highlighted = useHoloTint
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = if (useHoloTint) HoloSlabEdgeGlow else SplashGlassTextPrimary
                )
            }

            HoloControlCircleButton(
                onClick = { isFlyingRoute = !isFlyingRoute },
                tag = "map_3d_flyover_button",
                contentDescription = if (isFlyingRoute) "Stop 3D route flyover" else "Fly along 3D route",
                highlighted = isFlyingRoute
            ) {
                Icon(
                    imageVector = if (isFlyingRoute) Icons.Default.Stop else Icons.Default.FlightTakeoff,
                    contentDescription = null,
                    tint = if (isFlyingRoute) HoloSlabEdgeGlow else SplashGlassTextPrimary
                )
            }

            HoloControlCircleButton(
                onClick = {
                    isFlyingRoute = false
                    autoOrbit = !autoOrbit
                },
                tag = "map_3d_orbit_button",
                contentDescription = if (autoOrbit) "Pause 3D orbit" else "Auto-orbit 3D slab",
                highlighted = autoOrbit
            ) {
                Icon(
                    imageVector = Icons.Default.Autorenew,
                    contentDescription = null,
                    tint = if (autoOrbit) HoloSlabEdgeGlow else SplashGlassTextPrimary
                )
            }

            HoloControlCircleButton(
                onClick = {
                    autoOrbit = false
                    isFlyingRoute = false
                    yawDeg = HOLO_DEFAULT_YAW_DEG
                    pitchDeg = HOLO_DEFAULT_PITCH_DEG
                    zoomScale = 1.06f
                },
                tag = "map_3d_reset_button",
                contentDescription = "Reset 3D camera angle"
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = SplashGlassTextPrimary
                )
            }

            HoloControlCircleButton(
                onClick = { zoomScale = (zoomScale + 0.18f).coerceAtMost(HOLO_MAX_ZOOM) },
                tag = "map_3d_zoom_in_button",
                contentDescription = "Zoom in 3D slab"
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = null,
                    tint = SplashGlassTextPrimary
                )
            }

            HoloControlCircleButton(
                onClick = { zoomScale = (zoomScale - 0.18f).coerceAtLeast(HOLO_MIN_ZOOM) },
                tag = "map_3d_zoom_out_button",
                contentDescription = "Zoom out 3D slab"
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomOut,
                    contentDescription = null,
                    tint = SplashGlassTextPrimary
                )
            }
        }

        // Bottom-Left Interactive Pin Assignment & Gesture Hint Controls
        if (!hasBottomOverlay) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .then(bottomSafeModifier)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = HoloHudSurface,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(
                            1.dp,
                            if (tapSetsStartPin) HoloStartPinAccent else HoloEndPinAccent
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(50))
                            .clickable { tapSetsStartPin = !tapSetsStartPin }
                            .testTag("map_3d_pin_mode_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = null,
                                tint = if (tapSetsStartPin) HoloStartPinAccent else HoloEndPinAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (tapSetsStartPin) "Tap pin: Set START" else "Tap pin: Set END",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SplashGlassTextPrimary
                            )
                        }
                    }

                    if (customStartAttraction != null) {
                        Surface(
                            color = HoloHudSurface,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, HoloHudBorder),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(50))
                                .clickable { customStartAttraction = null }
                                .testTag("map_3d_reset_start_gps")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = null,
                                    tint = HoloStartPinAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "GPS Start",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SplashGlassTextPrimary
                                )
                            }
                        }
                    }
                }

                Holographic3DElevationChart(
                    profile = elevationProfile,
                    flyoverProgress = if (isFlyingRoute) flyoverProgress else null
                )

                Surface(
                    color = HoloHudSurface.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.75.dp, HoloHudBorder.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = "Drag to orbit & tilt · Pinch to zoom · © OpenStreetMap",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = SplashGlassTextSecondary,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

private fun buildRealMapSlabTexture(
    tileGrid: SlabTileGrid,
    bounds: HoloGeoBounds,
    loadedTiles: Map<String, ImageBitmap>
): CompositedRealMapSlab {
    var count = 0
    for (tx in tileGrid.minTx..tileGrid.maxTx) {
        for (ty in tileGrid.minTy..tileGrid.maxTy) {
            if (loadedTiles.containsKey("${tileGrid.tileZoom}_${tx}_$ty")) {
                count++
            }
        }
    }
    if (count == 0) {
        return CompositedRealMapSlab(bitmap = null, loadedTileCount = 0)
    }

    val bitmap = runCatching {
        Bitmap.createBitmap(SLAB_TEXTURE_SIZE_PX, SLAB_TEXTURE_SIZE_PX, Bitmap.Config.ARGB_8888)
    }.getOrNull() ?: return CompositedRealMapSlab(bitmap = null, loadedTileCount = 0)

    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(MapTerrainFallbackBg.toArgb())
    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val dstRect = RectF()
    val texSizeF = SLAB_TEXTURE_SIZE_PX.toFloat()

    var drawn = 0
    for (tx in tileGrid.minTx..tileGrid.maxTx) {
        for (ty in tileGrid.minTy..tileGrid.maxTy) {
            val img = loadedTiles["${tileGrid.tileZoom}_${tx}_$ty"] ?: continue
            val rect = tileRectInSlabTexture(
                tileZoom = tileGrid.tileZoom,
                tx = tx,
                ty = ty,
                bounds = bounds,
                textureSizePx = texSizeF
            )
            // Slight 0.6px bleed eliminates sub-pixel hairline seams between adjacent OSM tiles
            dstRect.set(rect.left - 0.6f, rect.top - 0.6f, rect.right + 0.6f, rect.bottom + 0.6f)
            runCatching {
                canvas.drawBitmap(img.asAndroidBitmap(), null, dstRect, tilePaint)
                drawn++
            }
        }
    }

    return CompositedRealMapSlab(bitmap = bitmap, loadedTileCount = drawn)
}

@Composable
private fun HoloEndpointPinBadge(
    anchor: HoloPinAnchor,
    screenAnchor: Offset,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    testTagName: String,
    reviewSummary: AttractionReviewSummary? = null,
    onOpenReviews: (() -> Unit)? = null
) {
    val density = LocalDensity.current
    val padX = with(density) { 12.dp.toPx() }
    val estHalfW = with(density) { 64.dp.toPx() }
    val badgeH = with(density) { if (reviewSummary != null) 66.dp.toPx() else 42.dp.toPx() }
    val clampedX = (screenAnchor.x - estHalfW).coerceIn(
        padX,
        (viewportWidthPx - estHalfW * 2f - padX).coerceAtLeast(padX)
    )
    val clampedY = (screenAnchor.y - badgeH - with(density) { 8.dp.toPx() }).coerceIn(
        with(density) { 12.dp.toPx() },
        (viewportHeightPx - badgeH - with(density) { 16.dp.toPx() }).coerceAtLeast(12f)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.offset { IntOffset(clampedX.roundToInt(), clampedY.roundToInt()) }
    ) {
        Surface(
            color = HoloHudSurface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.25.dp, anchor.accent.copy(alpha = 0.92f)),
            shadowElevation = 6.dp,
            modifier = Modifier
                .widthIn(max = 152.dp)
                .semantics(mergeDescendants = true) {}
                .testTag(testTagName)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(anchor.accent)
                    )
                    Text(
                        text = anchor.badgeTag,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp,
                        color = anchor.accent
                    )
                }
                Text(
                    text = anchor.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SplashGlassTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (reviewSummary != null && onOpenReviews != null) {
            MapPinStarBadge(
                summary = reviewSummary,
                attractionName = anchor.label,
                onClick = onOpenReviews,
                compact = false,
                darkSurface = true
            )
        }
    }
}

@Composable
private fun HoloCoordinateRow(
    tag: String,
    name: String,
    coords: String,
    accent: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(accent)
        )
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = "$tag · $name",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = SplashGlassTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = coords,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = SplashGlassTextSecondary.copy(alpha = 0.85f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HoloControlCircleButton(
    onClick: () -> Unit,
    tag: String,
    contentDescription: String,
    highlighted: Boolean = false,
    content: @Composable () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (highlighted) HoloSlabWallLeft.copy(alpha = 0.92f) else HoloHudSurface,
        border = BorderStroke(
            width = 1.dp,
            color = if (highlighted) HoloSlabEdgeGlow else HoloHudBorder
        ),
        shadowElevation = 3.dp
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .semantics { this.contentDescription = contentDescription }
                .testTag(tag)
        ) {
            content()
        }
    }
}

private fun DrawScope.drawQuadPath(
    points: List<Offset>,
    fillBrush: Brush,
    borderColor: Color,
    borderWidthPx: Float
) {
    if (points.size < 4) return
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        lineTo(points[1].x, points[1].y)
        lineTo(points[2].x, points[2].y)
        lineTo(points[3].x, points[3].y)
        close()
    }
    drawPath(path = path, brush = fillBrush)
    drawPath(
        path = path,
        color = borderColor,
        style = Stroke(width = borderWidthPx, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawProjectedSlabRing(
    center: Vec3,
    radius: Float,
    color: Color,
    strokeWidthPx: Float,
    project: (Vec3) -> Projected3D
) {
    val segments = 32
    val path = Path()
    var started = false
    for (i in 0..segments) {
        val angle = (2.0 * PI * i / segments)
        val candidate = Vec3(
            x = (center.x + cos(angle).toFloat() * radius).coerceIn(-HOLO_SLAB_HALF_EXTENT, HOLO_SLAB_HALF_EXTENT),
            y = (center.y + sin(angle).toFloat() * radius).coerceIn(-HOLO_SLAB_HALF_EXTENT, HOLO_SLAB_HALF_EXTENT),
            z = center.z
        )
        val pt = project(candidate).screen
        if (!started) {
            path.moveTo(pt.x, pt.y)
            started = true
        } else {
            path.lineTo(pt.x, pt.y)
        }
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidthPx)
    )
}

private fun DrawScope.drawHoloPinBeacon(
    baseLocal: Vec3,
    beaconHeight: Float,
    accent: Color,
    pulseRadius: Float,
    project: (Vec3) -> Projected3D
) {
    val baseProj = project(baseLocal)
    val apexProj = project(baseLocal.copy(z = baseLocal.z + beaconHeight))

    // Expanding pulse ring on the real map floor
    drawProjectedSlabRing(
        center = baseLocal,
        radius = pulseRadius,
        color = accent.copy(alpha = (1f - pulseRadius / 0.18f).coerceIn(0.22f, 0.85f)),
        strokeWidthPx = 2.dp.toPx(),
        project = project
    )
    // Dark-cased locked coordinate anchor ring so pins stand out crisply on real OSM map tiles
    drawProjectedSlabRing(
        center = baseLocal,
        radius = 0.048f,
        color = HoloSpaceBgTop.copy(alpha = 0.72f),
        strokeWidthPx = 3.5.dp.toPx(),
        project = project
    )
    drawProjectedSlabRing(
        center = baseLocal,
        radius = 0.048f,
        color = accent,
        strokeWidthPx = 2.dp.toPx(),
        project = project
    )

    // Dark-cased glowing vertical 3D beacon stem rising from exact map coordinate
    drawLine(
        color = HoloSpaceBgTop.copy(alpha = 0.58f),
        start = baseProj.screen,
        end = apexProj.screen,
        strokeWidth = 5.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(accent.copy(alpha = 0.72f), accent, HoloArcCore),
            start = baseProj.screen,
            end = apexProj.screen
        ),
        start = baseProj.screen,
        end = apexProj.screen,
        strokeWidth = 2.8.dp.toPx(),
        cap = StrokeCap.Round
    )

    // Base coordinate dot on the map surface
    drawCircle(color = HoloSpaceBgTop.copy(alpha = 0.85f), radius = 5.dp.toPx(), center = baseProj.screen)
    drawCircle(color = HoloArcCore, radius = 3.2.dp.toPx(), center = baseProj.screen)

    // Floating crystalline 3D pin head orb with luminous halo
    val orbScale = apexProj.perspectiveScale.coerceIn(0.8f, 1.3f)
    drawCircle(color = HoloSpaceBgTop.copy(alpha = 0.48f), radius = 11.dp.toPx() * orbScale, center = apexProj.screen)
    drawCircle(color = accent.copy(alpha = 0.36f), radius = 13.dp.toPx() * orbScale, center = apexProj.screen)
    drawCircle(color = accent, radius = 6.8.dp.toPx() * orbScale, center = apexProj.screen)
    drawCircle(color = HoloArcCore, radius = 2.8.dp.toPx() * orbScale, center = apexProj.screen)
}
