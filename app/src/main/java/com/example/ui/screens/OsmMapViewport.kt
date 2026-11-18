package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.data.DumingagSeedData
import com.example.data.remote.OsmProjection
import com.example.data.remote.OsmTileLoader
import com.example.model.AppLanguage
import com.example.model.Attraction
import com.example.model.OfflineMapNode
import com.example.model.OfflineRoutePlan
import com.example.ui.components.AttractionCoverImage
import com.example.ui.theme.MapContourRing
import com.example.ui.theme.MapHighwayCasing
import com.example.ui.theme.MapHighwayFill
import com.example.ui.theme.MapRouteCore
import com.example.ui.theme.MapRouteGlow
import com.example.ui.theme.MapTerrainFallbackBg
import com.example.ui.theme.MapTrailStroke
import com.example.ui.theme.MapUserPin
import com.example.ui.theme.MapUserPulse
import kotlinx.coroutines.launch
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sinh

private const val MIN_MAP_ZOOM = 9.5f
private const val MAX_MAP_ZOOM = 17.5f
private const val MIN_TILE_ZOOM = 9
private const val MAX_TILE_ZOOM = 17

private data class RegionalHub(val name: String, val lat: Double, val lon: Double)

private val RegionalZdsHubs = listOf(
    RegionalHub("Pagadian City", 7.8257, 123.4370),
    RegionalHub("Molave", 8.0942, 123.4892),
    RegionalHub("Mahayag", 8.1333, 123.4333),
    RegionalHub("Tambulig", 8.0744, 123.5419)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OsmMapViewport(
    language: AppLanguage,
    attractions: List<Attraction>,
    selectedAttraction: Attraction?,
    activeRoute: OfflineRoutePlan?,
    userLatLon: Pair<Double, Double>,
    centerLat: Double,
    centerLon: Double,
    zoomLevel: Float,
    gpsStatusNote: String?,
    offlinePackReady: Boolean,
    isEffectiveOffline: Boolean,
    isFullscreen: Boolean,
    hasBottomOverlay: Boolean,
    showRoadNetwork: Boolean,
    showJunctionNodes: Boolean,
    showElevationAndHubs: Boolean,
    showLayerPanel: Boolean,
    selectedJunctionNode: OfflineMapNode?,
    loadedTiles: MutableMap<String, ImageBitmap>,
    tileTrigger: Int,
    osmTileLoader: OsmTileLoader,
    onPanZoom: (Double, Double, Float) -> Unit,
    onTileLoaded: () -> Unit,
    onFocusDumingag: () -> Unit,
    onFocusZds: () -> Unit,
    onRecenterGps: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onToggleLayerPanel: () -> Unit,
    onSelectAttraction: (Attraction) -> Unit,
    onSelectJunction: (OfflineMapNode) -> Unit,
    onToggleRoads: () -> Unit,
    onToggleJunctions: () -> Unit,
    onToggleContours: () -> Unit,
    onCloseLayers: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pulseTransition = rememberInfiniteTransition(label = "gpsPulse")
    val gpsPulseScale by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gpsPulseScale"
    )

    val containerShape = if (isFullscreen) RoundedCornerShape(0.dp) else RoundedCornerShape(24.dp)
    val viewportModifier = if (isFullscreen) {
        modifier
            .background(MapTerrainFallbackBg)
            .clipToBounds()
    } else {
        modifier
            .clip(containerShape)
            .background(MapTerrainFallbackBg)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), containerShape)
            .clipToBounds()
    }

    BoxWithConstraints(modifier = viewportModifier) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val currentCenterLat by rememberUpdatedState(centerLat)
        val currentCenterLon by rememberUpdatedState(centerLon)
        val currentZoom by rememberUpdatedState(zoomLevel)
        val currentWidthPx by rememberUpdatedState(widthPx)
        val currentHeightPx by rememberUpdatedState(heightPx)
        val currentOnPanZoom by rememberUpdatedState(onPanZoom)

        val tileZoom = floor(zoomLevel).toInt().coerceIn(MIN_TILE_ZOOM, MAX_TILE_ZOOM)
        val scale = 2.0.pow((zoomLevel - tileZoom).toDouble())
        val tileSizePx = (256.0 * density.density * scale).toFloat()
        val (centerWorldX, centerWorldY) = OsmProjection.latLonToTileCoord(centerLat, centerLon, tileZoom)

        fun latLonToScreen(lat: Double, lon: Double): Offset {
            val (wx, wy) = OsmProjection.latLonToTileCoord(lat, lon, tileZoom)
            return Offset(
                x = widthPx / 2f + ((wx - centerWorldX) * tileSizePx).toFloat(),
                y = heightPx / 2f + ((wy - centerWorldY) * tileSizePx).toFloat()
            )
        }

        // Dynamic scale bar calculation (meters per pixel at centerLat)
        val metersPerPixel = (156543.03392 * cos(Math.toRadians(centerLat))) /
            (2.0.pow(zoomLevel.toDouble()) * density.density)
        val targetScaleMeters = metersPerPixel * with(density) { 64.dp.toPx() }
        val scaleLabel = if (targetScaleMeters >= 1000.0) {
            "${((targetScaleMeters / 1000.0) * 10.0).roundToInt() / 10.0} km"
        } else {
            "${((targetScaleMeters / 50.0).roundToInt() * 50).coerceAtLeast(100)} m"
        }

        val halfTilesX = (widthPx / 2f) / tileSizePx
        val halfTilesY = (heightPx / 2f) / tileSizePx
        val maxTileIndex = (1 shl tileZoom) - 1
        val minTx = floor(centerWorldX - halfTilesX).toInt().coerceIn(0, maxTileIndex)
        val maxTx = ceil(centerWorldX + halfTilesX).toInt().coerceIn(0, maxTileIndex)
        val minTy = floor(centerWorldY - halfTilesY).toInt().coerceIn(0, maxTileIndex)
        val maxTy = ceil(centerWorldY + halfTilesY).toInt().coerceIn(0, maxTileIndex)

        LaunchedEffect(tileZoom, minTx, maxTx, minTy, maxTy, isEffectiveOffline) {
            for (tx in minTx..maxTx) {
                for (ty in minTy..maxTy) {
                    val key = "${tileZoom}_${tx}_$ty"
                    if (!loadedTiles.containsKey(key)) {
                        osmTileLoader.getCachedInMemory(tileZoom, tx, ty)?.let {
                            loadedTiles[key] = it
                        } ?: launch {
                            val bmp = osmTileLoader.loadTile(
                                z = tileZoom,
                                x = tx,
                                y = ty,
                                allowNetwork = !isEffectiveOffline
                            )
                            if (bmp != null) {
                                loadedTiles[key] = bmp
                                onTileLoaded()
                            }
                        }
                    }
                }
            }
        }

        fun applyFocalTransform(centroid: Offset, pan: Offset, newZoom: Float) {
            val wPx = currentWidthPx
            val hPx = currentHeightPx
            if (wPx <= 0f || hPx <= 0f) return

            val curZoom = currentZoom
            val curTileZ = floor(curZoom).toInt().coerceIn(MIN_TILE_ZOOM, MAX_TILE_ZOOM)
            val curScale = 2.0.pow((curZoom - curTileZ).toDouble())
            val curTilePx = 256.0 * density.density * curScale
            val (curCx, curCy) = OsmProjection.latLonToTileCoord(
                currentCenterLat,
                currentCenterLon,
                curTileZ
            )

            // Unclamped geographic point currently beneath the pinch/tap focal point
            val focalWx = curCx + ((centroid.x - wPx / 2f) / curTilePx)
            val focalWy = curCy + ((centroid.y - hPx / 2f) / curTilePx)
            val curN = 2.0.pow(curTileZ)
            val focalLon = (focalWx / curN) * 360.0 - 180.0
            val focalLat = Math.toDegrees(atan(sinh(Math.PI * (1.0 - 2.0 * focalWy / curN))))

            val clampedZoom = newZoom.coerceIn(MIN_MAP_ZOOM, MAX_MAP_ZOOM)
            val nextTileZ = floor(clampedZoom).toInt().coerceIn(MIN_TILE_ZOOM, MAX_TILE_ZOOM)
            val nextScale = 2.0.pow((clampedZoom - nextTileZ).toDouble())
            val nextTilePx = 256.0 * density.density * nextScale

            val (nextFocalWx, nextFocalWy) = OsmProjection.latLonToTileCoord(focalLat, focalLon, nextTileZ)
            val targetOffsetX = centroid.x + pan.x - wPx / 2f
            val targetOffsetY = centroid.y + pan.y - hPx / 2f
            val nextCenterWx = nextFocalWx - (targetOffsetX / nextTilePx)
            val nextCenterWy = nextFocalWy - (targetOffsetY / nextTilePx)
            val (newLat, newLon) = OsmProjection.tileCoordToLatLon(nextCenterWx, nextCenterWy, nextTileZ)
            currentOnPanZoom(newLat, newLon, clampedZoom)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                // Constant keys keep gesture recognizers alive across zoom-tile transitions
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, gestureZoom, _ ->
                        val zoomDelta = if (gestureZoom > 0f && gestureZoom != 1f) {
                            log2(gestureZoom.toDouble()).toFloat()
                        } else {
                            0f
                        }
                        val nextZoom = (currentZoom + zoomDelta).coerceIn(MIN_MAP_ZOOM, MAX_MAP_ZOOM)
                        applyFocalTransform(centroid = centroid, pan = pan, newZoom = nextZoom)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            val targetZoom = if (currentZoom >= 16.4f) {
                                13.2f
                            } else {
                                (currentZoom + 1.2f).coerceAtMost(MAX_MAP_ZOOM)
                            }
                            applyFocalTransform(centroid = tapOffset, pan = Offset.Zero, newZoom = targetZoom)
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val redrawTick = tileTrigger
                if (redrawTick >= 0) {
                    if (showElevationAndHubs) {
                        val timolanPt = latLonToScreen(8.1360, 123.3110)
                        listOf(68f, 122f, 178f, 232f).forEach { r ->
                            drawCircle(
                                color = MapContourRing,
                                radius = r * (zoomLevel / 13.2f),
                                center = timolanPt,
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                    }

                    val tilePxInt = ceil(tileSizePx).toInt().coerceAtLeast(1)
                    for (tx in minTx..maxTx) {
                        for (ty in minTy..maxTy) {
                            val bmp = loadedTiles["${tileZoom}_${tx}_$ty"]
                            if (bmp != null) {
                                val left = (widthPx / 2f + ((tx - centerWorldX) * tileSizePx)).roundToInt()
                                val top = (heightPx / 2f + ((ty - centerWorldY) * tileSizePx)).roundToInt()
                                drawImage(
                                    image = bmp,
                                    dstOffset = IntOffset(left, top),
                                    dstSize = IntSize(tilePxInt, tilePxInt)
                                )
                            }
                        }
                    }

                    val nodesById = DumingagSeedData.mapNodes.associateBy { it.id }
                    if (showRoadNetwork) {
                        DumingagSeedData.mapEdges.forEach { edge ->
                            val n1 = nodesById[edge.fromId] ?: return@forEach
                            val n2 = nodesById[edge.toId] ?: return@forEach
                            val s1 = latLonToScreen(n1.latitude, n1.longitude)
                            val s2 = latLonToScreen(n2.latitude, n2.longitude)
                            val isHighway = edge.roadType.contains("Highway") || edge.roadType.contains("Town")
                            if (isHighway) {
                                drawLine(
                                    color = MapHighwayCasing,
                                    start = s1,
                                    end = s2,
                                    strokeWidth = 6.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                                drawLine(
                                    color = MapHighwayFill,
                                    start = s1,
                                    end = s2,
                                    strokeWidth = 3.2.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            } else {
                                drawLine(
                                    color = MapTrailStroke,
                                    start = s1,
                                    end = s2,
                                    strokeWidth = 2.5.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f), 0f)
                                )
                            }
                        }
                    }

                    if (activeRoute != null && activeRoute.nodePathIds.size >= 2) {
                        for (i in 0 until activeRoute.nodePathIds.size - 1) {
                            val n1 = nodesById[activeRoute.nodePathIds[i]] ?: continue
                            val n2 = nodesById[activeRoute.nodePathIds[i + 1]] ?: continue
                            val p1 = latLonToScreen(n1.latitude, n1.longitude)
                            val p2 = latLonToScreen(n2.latitude, n2.longitude)
                            drawLine(
                                color = MapRouteGlow,
                                start = p1,
                                end = p2,
                                strokeWidth = 12.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = MapRouteCore,
                                start = p1,
                                end = p2,
                                strokeWidth = 5.5.dp.toPx(),
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f), 0f)
                            )
                            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = p1)
                            drawCircle(color = MapRouteCore, radius = 3.2.dp.toPx(), center = p1)
                        }
                    }

                    val userPt = latLonToScreen(userLatLon.first, userLatLon.second)
                    drawCircle(
                        color = MapUserPulse,
                        radius = (15.dp * gpsPulseScale).toPx(),
                        center = userPt
                    )
                    drawCircle(color = Color.White, radius = 8.5.dp.toPx(), center = userPt)
                    drawCircle(color = MapUserPin, radius = 5.5.dp.toPx(), center = userPt)
                }
            }

            // Regional Zamboanga del Sur town hubs when zoomed out
            if (showElevationAndHubs && zoomLevel < 12.3f) {
                RegionalZdsHubs.forEach { hub ->
                    val pt = latLonToScreen(hub.lat, hub.lon)
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                        modifier = Modifier.offset {
                            IntOffset(pt.x.roundToInt() - 40, pt.y.roundToInt() - 14)
                        }
                    ) {
                        Text(
                            text = hub.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Transit & Road Junction Markers
            if (showJunctionNodes) {
                DumingagSeedData.mapNodes.filter { !it.isLandmark }.forEach { node ->
                    val pt = latLonToScreen(node.latitude, node.longitude)
                    val isSelectedNode = selectedJunctionNode?.id == node.id
                    val dotSize = if (isSelectedNode) 20.dp else 14.dp
                    val halfDotPx = with(density) { (dotSize / 2).roundToPx() }
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(pt.x.roundToInt() - halfDotPx, pt.y.roundToInt() - halfDotPx) }
                            .size(dotSize)
                            .clip(CircleShape)
                            .background(if (isSelectedNode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable { onSelectJunction(node) }
                    )
                }
            }

            // Landmark Photo Pins
            attractions.forEach { attraction ->
                val screenPos = latLonToScreen(attraction.latitude, attraction.longitude)
                val isSelected = selectedAttraction?.id == attraction.id ||
                    activeRoute?.destination?.id == attraction.id
                val pinSizeDp = if (isSelected) 54.dp else 44.dp
                val halfPinPx = with(density) { (pinSizeDp / 2).roundToPx() }
                val ringColor = if (isSelected) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    categoryPinAccent(
                        attraction.category,
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.tertiary
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = screenPos.x.roundToInt() - halfPinPx,
                                y = screenPos.y.roundToInt() - halfPinPx
                            )
                        }
                        .clickable { onSelectAttraction(attraction) }
                        .testTag("map_pin_${attraction.id}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(pinSizeDp)
                            .clip(CircleShape)
                            .border(
                                width = if (isSelected) 3.dp else 2.5.dp,
                                color = ringColor,
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = Color.White,
                                shape = CircleShape
                            )
                    ) {
                        AttractionCoverImage(
                            attraction = attraction,
                            contentDescription = attraction.name.get(language),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        shape = RoundedCornerShape(8.dp),
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = attraction.name.get(language).split(" ").take(2).joinToString(" "),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
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

        // Top-Left Status + Compass Badge
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 2.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .then(topSafeModifier)
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "North indicator",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = gpsStatusNote ?: when {
                        zoomLevel < 12.2f -> "Zamboanga del Sur · OSM"
                        isEffectiveOffline -> if (offlinePackReady) "Dumingag Offline OSM" else "Offline Trail Map"
                        else -> "Dumingag, Zamboanga del Sur"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Junction Callout Tooltip if user taps a road/transit node
        selectedJunctionNode?.let { node ->
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .then(topSafeModifier)
                    .padding(top = 52.dp, start = 16.dp, end = 64.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${node.name.get(language)} · ${"%.4f".format(node.latitude)}°N, ${"%.4f".format(node.longitude)}°E",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Expandable Layers & Legend Overlay
        AnimatedVisibility(
            visible = showLayerPanel,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .then(topSafeModifier)
                .padding(top = 52.dp, start = 12.dp, end = 64.dp)
        ) {
            MapLegendAndLayerCard(
                showRoads = showRoadNetwork,
                showJunctions = showJunctionNodes,
                showContours = showElevationAndHubs,
                onToggleRoads = onToggleRoads,
                onToggleJunctions = onToggleJunctions,
                onToggleContours = onToggleContours,
                onClose = onCloseLayers
            )
        }

        // Top-Right Fullscreen, Layers (fullscreen), Zoom & GPS Controls
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
                color = if (isFullscreen) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.96f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                },
                shadowElevation = 3.dp
            ) {
                IconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("map_fullscreen_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isFullscreen) "Exit fullscreen map" else "Enter fullscreen map",
                        tint = if (isFullscreen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (isFullscreen) {
                Surface(
                    shape = CircleShape,
                    color = if (showLayerPanel) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    },
                    shadowElevation = 2.dp
                ) {
                    IconButton(
                        onClick = onToggleLayerPanel,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("map_fullscreen_layers_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Toggle map layers",
                            tint = if (showLayerPanel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 2.dp
            ) {
                IconButton(
                    onClick = { onPanZoom(centerLat, centerLon, (zoomLevel + 0.8f).coerceAtMost(MAX_MAP_ZOOM)) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("map_zoom_in_button")
                ) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom in")
                }
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 2.dp
            ) {
                IconButton(
                    onClick = { onPanZoom(centerLat, centerLon, (zoomLevel - 0.8f).coerceAtLeast(MIN_MAP_ZOOM)) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("map_zoom_out_button")
                ) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom out")
                }
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 2.dp
            ) {
                IconButton(
                    onClick = onRecenterGps,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("map_recenter_gps_button")
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Center on my location")
                }
            }
        }

        // Bottom-Left Focus Presets, Dynamic Scale Bar & OSM Attribution
        if (!hasBottomOverlay) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .then(bottomSafeModifier)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(50),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onFocusDumingag)
                            .testTag("map_focus_dumingag_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Dumingag", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(50),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onFocusZds)
                            .testTag("map_focus_zds_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Zamboanga del Sur", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(3.dp)
                                    .background(MaterialTheme.colorScheme.onSurface)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$scaleLabel · z${"%.1f".format(zoomLevel)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "© OpenStreetMap",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
