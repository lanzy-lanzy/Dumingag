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
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.ViewInAr
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DumingagSeedData
import com.example.data.remote.OsmProjection
import com.example.data.remote.OsmTileLoader
import com.example.model.AppLanguage
import com.example.model.Attraction
import com.example.model.OfflineMapNode
import com.example.model.OfflineRoutePlan
import com.example.model.TravelMode
import com.example.ui.components.AttractionCoverImage
import com.example.ui.components.AttractionReviewSummary
import com.example.ui.components.MapPinStarBadge
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
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
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
    markedAttractions: List<Attraction> = listOfNotNull(selectedAttraction),
    activeRoute: OfflineRoutePlan?,
    travelMode: TravelMode = TravelMode.HABAL_HABAL,
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
    is3DMode: Boolean = false,
    onToggle3DMode: () -> Unit = {},
    reviewSummaries: Map<String, AttractionReviewSummary> = emptyMap(),
    onOpenAttractionReviews: (Attraction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showCompassHud by remember { mutableStateOf(false) }
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
    val connectionDashPhase by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = -28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "connectionDashPhase"
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

                    val userPt = latLonToScreen(userLatLon.first, userLatLon.second)
                    val effectiveMarks = markedAttractions.ifEmpty { listOfNotNull(selectedAttraction) }
                    val connectionPoints = buildPointToPointOffsets(
                        userPt = userPt,
                        activeRoute = activeRoute,
                        markedAttractions = effectiveMarks,
                        nodesById = nodesById,
                        latLonToScreen = ::latLonToScreen
                    )

                    if (connectionPoints.size >= 2) {
                        val isGuidedRoute = activeRoute != null && activeRoute.nodePathIds.isNotEmpty()
                        val lineColor = if (isGuidedRoute) MapRouteCore else MapUserPin
                        val selectedPinRadiusPx = 27.dp.toPx()

                        for (i in 0 until connectionPoints.size - 1) {
                            val p1 = connectionPoints[i]
                            val p2 = connectionPoints[i + 1]
                            val dx = p2.x - p1.x
                            val dy = p2.y - p1.y
                            val segLen = hypot(dx, dy)
                            if (segLen < 1f) continue

                            drawLine(
                                color = MapRouteGlow,
                                start = p1,
                                end = p2,
                                strokeWidth = 11.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = Color.White.copy(alpha = 0.92f),
                                start = p1,
                                end = p2,
                                strokeWidth = 6.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = lineColor,
                                start = p1,
                                end = p2,
                                strokeWidth = 4.2.dp.toPx(),
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f), connectionDashPhase)
                            )

                            val angleRad = atan2(dy, dx)
                            val isLastSegment = i == connectionPoints.size - 2
                            val pointsIntoLandmarkPin = !isGuidedRoute || isLastSegment

                            // Mid-segment directional pointer on longer segments
                            if (segLen >= 115.dp.toPx()) {
                                val midT = if (pointsIntoLandmarkPin) 0.42f else 0.55f
                                val midTip = Offset(p1.x + dx * midT, p1.y + dy * midT)
                                drawDirectionalArrowHead(
                                    tip = midTip,
                                    directionAngleRad = angleRad,
                                    arrowLengthPx = 11.dp.toPx(),
                                    arrowHalfWidthPx = 6.5.dp.toPx(),
                                    fillColor = lineColor
                                )
                            }

                            // Crisp pointer arrowhead aimed directly into each marked landmark pin rim
                            if (pointsIntoLandmarkPin && segLen > selectedPinRadiusPx + 12.dp.toPx()) {
                                val ux = dx / segLen
                                val uy = dy / segLen
                                val tipOffset = Offset(
                                    x = p2.x - ux * (selectedPinRadiusPx + 3.dp.toPx()),
                                    y = p2.y - uy * (selectedPinRadiusPx + 3.dp.toPx())
                                )
                                drawDirectionalArrowHead(
                                    tip = tipOffset,
                                    directionAngleRad = angleRad,
                                    arrowLengthPx = 13.dp.toPx(),
                                    arrowHalfWidthPx = 8.dp.toPx(),
                                    fillColor = lineColor
                                )
                            }
                        }

                        if (isGuidedRoute) {
                            // Highlight intermediate road junction vertices
                            for (i in 1 until connectionPoints.size - 1) {
                                val pt = connectionPoints[i]
                                drawCircle(color = Color.White, radius = 6.dp.toPx(), center = pt)
                                drawCircle(color = lineColor, radius = 3.8.dp.toPx(), center = pt)
                            }
                            drawCircle(
                                color = lineColor.copy(alpha = 0.22f),
                                radius = (selectedPinRadiusPx + 7.dp.toPx()),
                                center = connectionPoints.last()
                            )
                        } else {
                            // Highlight every marked landmark pin anchor halo in the chain
                            for (i in 1 until connectionPoints.size) {
                                drawCircle(
                                    color = lineColor.copy(alpha = 0.22f),
                                    radius = (selectedPinRadiusPx + 7.dp.toPx()),
                                    center = connectionPoints[i]
                                )
                            }
                        }
                    }

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
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(pt.x.roundToInt(), pt.y.roundToInt() - with(density) { 14.dp.roundToPx() })
                            }
                            .width(0.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                            modifier = Modifier.wrapContentWidth(
                                align = Alignment.CenterHorizontally,
                                unbounded = true
                            )
                        ) {
                            Text(
                                text = hub.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
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

            // Landmark Photo Pins — anchored pixel-exact on (screenPos.x, screenPos.y)
            // Zoom-aware anti-overlap decluttering: selected/marked and higher-rated pins get badge priority
            val effectiveMarks = markedAttractions.ifEmpty { listOfNotNull(selectedAttraction) }
            val minLabelGapX = with(density) { 86.dp.toPx() }
            val minLabelGapY = with(density) { 46.dp.toPx() }
            val applyDeclutter = zoomLevel < 14.2f

            val prioritizedPins = attractions.map { attraction ->
                val screenPos = latLonToScreen(attraction.latitude, attraction.longitude)
                val markIndex = effectiveMarks.indexOfFirst { it.id == attraction.id }
                val isMarked = markIndex >= 0
                val isSelected = isMarked ||
                    selectedAttraction?.id == attraction.id ||
                    activeRoute?.destination?.id == attraction.id
                val summary = reviewSummaries[attraction.id]
                Triple(attraction, screenPos, Triple(markIndex, isSelected, summary))
            }.sortedWith(
                compareByDescending<Triple<Attraction, Offset, Triple<Int, Boolean, AttractionReviewSummary?>>> { it.third.second }
                    .thenByDescending { it.third.third?.averageRating ?: 0.0 }
                    .thenByDescending { it.third.third?.reviewCount ?: 0 }
            )

            val occupiedLabelCenters = mutableListOf<Offset>()
            val showDecorationsById = mutableMapOf<String, Boolean>()
            prioritizedPins.forEach { (attraction, screenPos, meta) ->
                val isSelected = meta.second
                if (isSelected || !applyDeclutter) {
                    showDecorationsById[attraction.id] = true
                    occupiedLabelCenters.add(screenPos)
                } else {
                    val collides = occupiedLabelCenters.any { existing ->
                        kotlin.math.abs(existing.x - screenPos.x) < minLabelGapX &&
                            kotlin.math.abs(existing.y - screenPos.y) < minLabelGapY
                    }
                    if (!collides) {
                        showDecorationsById[attraction.id] = true
                        occupiedLabelCenters.add(screenPos)
                    } else {
                        showDecorationsById[attraction.id] = false
                    }
                }
            }

            // Render unselected pins first so selected/marked pins sit cleanly on top
            prioritizedPins.asReversed().forEach { (attraction, screenPos, meta) ->
                val markIndex = meta.first
                val isMarked = markIndex >= 0
                val isSelected = meta.second
                val reviewSummary = meta.third
                val showDecorations = showDecorationsById[attraction.id] != false

                val pinSizeDp = if (isSelected) 54.dp else if (showDecorations) 48.dp else 40.dp
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
                        .width(pinSizeDp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onSelectAttraction(attraction) }
                            .testTag("map_pin_${attraction.id}")
                    ) {
                        Box(modifier = Modifier.size(pinSizeDp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
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
                            if (isMarked && activeRoute == null) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(MapUserPin)
                                        .border(1.5.dp, Color.White, CircleShape)
                                        .semantics { contentDescription = "Stop ${markIndex + 1}" }
                                        .testTag("map_pin_badge_${attraction.id}")
                                ) {
                                    Text(
                                        text = "${markIndex + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            } else if (!showDecorations && reviewSummary != null) {
                                // Compact amber star dot when zoomed out and decluttered
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFB300))
                                        .border(1.5.dp, Color.White, CircleShape)
                                        .clickable { onOpenAttractionReviews(attraction) }
                                        .testTag("map_pin_compact_star_${attraction.id}")
                                ) {
                                    Text(
                                        text = "★",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF1F1600),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        if (showDecorations) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Surface(
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                shape = RoundedCornerShape(8.dp),
                                shadowElevation = 2.dp,
                                modifier = Modifier.wrapContentWidth(
                                    align = Alignment.CenterHorizontally,
                                    unbounded = true
                                )
                            ) {
                                Text(
                                    text = attraction.name.get(language).split(" ").take(2).joinToString(" "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (reviewSummary != null && showDecorations) {
                        Spacer(modifier = Modifier.height(3.dp))
                        MapPinStarBadge(
                            summary = reviewSummary,
                            attractionName = attraction.name.get(language),
                            onClick = { onOpenAttractionReviews(attraction) },
                            modifier = Modifier.wrapContentWidth(
                                align = Alignment.CenterHorizontally,
                                unbounded = true
                            )
                        )
                    }
                }
            }

            // Crisp Current-Location Origin Anchor Dot (always visible as Point A even over a nearby town pin)
            run {
                val userPt = latLonToScreen(userLatLon.first, userLatLon.second)
                val dotSize = 18.dp
                val halfDotPx = with(density) { (dotSize / 2).roundToPx() }
                Box(
                    modifier = Modifier
                        .offset { IntOffset(userPt.x.roundToInt() - halfDotPx, userPt.y.roundToInt() - halfDotPx) }
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(MapUserPin)
                        .testTag("map_user_origin_dot")
                )
            }

            // Floating Distance (km) & Hours (hr) Badges along the Connected Line(s)
            val minPadX = with(density) { 14.dp.toPx() }
            val minPadY = with(density) { 58.dp.toPx() }
            val maxPadY = with(density) { if (hasBottomOverlay) 210.dp.toPx() else 72.dp.toPx() }
            val perpShiftPx = with(density) { 22.dp.toPx() }

            if (activeRoute != null) {
                val nodesById = DumingagSeedData.mapNodes.associateBy { it.id }
                val userPt = latLonToScreen(userLatLon.first, userLatLon.second)
                val connectionPoints = buildPointToPointOffsets(
                    userPt = userPt,
                    activeRoute = activeRoute,
                    markedAttractions = effectiveMarks,
                    nodesById = nodesById,
                    latLonToScreen = ::latLonToScreen
                )
                val connectionMetrics = calculateMetricsForDistance(activeRoute.totalDistanceKm, travelMode)
                val (midPt, tangent) = pointAlongPolylineAtFraction(connectionPoints, 0.5f)
                val perpX = -tangent.y
                val perpY = tangent.x
                val preferredSide = if (perpY <= 0f) 1f else -1f
                val anchorX = midPt.x + perpX * perpShiftPx * preferredSide
                val anchorY = midPt.y + perpY * perpShiftPx * preferredSide

                var pillSize by remember { mutableStateOf(IntSize.Zero) }
                val halfW = if (pillSize.width > 0) pillSize.width / 2f else with(density) { 82.dp.toPx() }
                val halfH = if (pillSize.height > 0) pillSize.height / 2f else with(density) { 16.dp.toPx() }
                val clampedX = (anchorX - halfW).coerceIn(
                    minPadX,
                    (widthPx - halfW * 2f - minPadX).coerceAtLeast(minPadX)
                )
                val clampedY = (anchorY - halfH).coerceIn(
                    minPadY,
                    (heightPx - maxPadY).coerceAtLeast(minPadY)
                )

                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.25.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)),
                    shadowElevation = 5.dp,
                    modifier = Modifier
                        .offset { IntOffset(clampedX.roundToInt(), clampedY.roundToInt()) }
                        .onSizeChanged { pillSize = it }
                        .semantics(mergeDescendants = true) {}
                        .testTag("map_connection_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Route,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${formatDistanceDisplay(connectionMetrics.distanceKm)} · ${formatTravelHoursSummary(connectionMetrics)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else if (effectiveMarks.isNotEmpty()) {
                val multiSummary = calculateMultiPointRoute(
                    userLatLon = userLatLon,
                    markedAttractions = effectiveMarks,
                    travelMode = travelMode
                )
                val isMultiStop = multiSummary.legs.size > 1
                val placedPillCenters = mutableListOf<Offset>()
                val minPillSeparationY = with(density) { 36.dp.toPx() }
                val minPillSeparationX = with(density) { 148.dp.toPx() }
                multiSummary.legs.forEach { leg ->
                    val p1 = latLonToScreen(leg.fromLatLon.first, leg.fromLatLon.second)
                    val p2 = latLonToScreen(leg.toLatLon.first, leg.toLatLon.second)
                    val (midPt, tangent) = pointAlongPolylineAtFraction(listOf(p1, p2), 0.5f)
                    val perpX = -tangent.y
                    val perpY = tangent.x
                    val preferredSide = if (perpY <= 0f) 1f else -1f
                    val anchorX = midPt.x + perpX * perpShiftPx * preferredSide
                    val anchorY = midPt.y + perpY * perpShiftPx * preferredSide

                    var pillSize by remember(leg.legIndex, leg.toAttraction.id) { mutableStateOf(IntSize.Zero) }
                    val halfW = if (pillSize.width > 0) pillSize.width / 2f else with(density) { 80.dp.toPx() }
                    val halfH = if (pillSize.height > 0) pillSize.height / 2f else with(density) { 15.dp.toPx() }
                    val clampedX = (anchorX - halfW).coerceIn(
                        minPadX,
                        (widthPx - halfW * 2f - minPadX).coerceAtLeast(minPadX)
                    )
                    val maxAllowedY = (heightPx - maxPadY).coerceAtLeast(minPadY)
                    var clampedY = (anchorY - halfH).coerceIn(minPadY, maxAllowedY)
                    for (existing in placedPillCenters) {
                        val centerX = clampedX + halfW
                        val centerY = clampedY + halfH
                        if (kotlin.math.abs(centerX - existing.x) < minPillSeparationX &&
                            kotlin.math.abs(centerY - existing.y) < minPillSeparationY
                        ) {
                            val tryBelow = existing.y + minPillSeparationY - halfH
                            val tryAbove = existing.y - minPillSeparationY - halfH
                            clampedY = if (tryBelow <= maxAllowedY) {
                                tryBelow.coerceIn(minPadY, maxAllowedY)
                            } else {
                                tryAbove.coerceIn(minPadY, maxAllowedY)
                            }
                        }
                    }
                    placedPillCenters.add(Offset(clampedX + halfW, clampedY + halfH))

                    val pillLabel = if (isMultiStop) {
                        "#${leg.legIndex + 1} · ${formatDistanceDisplay(leg.metrics.distanceKm)} · ${formatHoursDecimal(leg.metrics.estimatedHours)}"
                    } else {
                        "${formatDistanceDisplay(leg.metrics.distanceKm)} · ${formatTravelHoursSummary(leg.metrics)}"
                    }
                    val pillTag = if (leg.legIndex == 0) {
                        "map_connection_pill"
                    } else {
                        "map_connection_leg_${leg.legIndex}"
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.25.dp, MapUserPin.copy(alpha = 0.80f)),
                        shadowElevation = 5.dp,
                        modifier = Modifier
                            .offset { IntOffset(clampedX.roundToInt(), clampedY.roundToInt()) }
                            .onSizeChanged { pillSize = it }
                            .semantics(mergeDescendants = true) {}
                            .testTag(pillTag)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Route,
                                contentDescription = null,
                                tint = MapUserPin,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = pillLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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

        AnimatedVisibility(
            visible = showCompassHud,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .then(topSafeModifier)
                .padding(top = 52.dp, start = 10.dp, end = 64.dp)
        ) {
            CompassBearingHud(
                userLatLon = userLatLon,
                targetAttraction = activeRoute?.destination ?: selectedAttraction,
                language = language,
                onClose = { showCompassHud = false }
            )
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

            Surface(
                shape = CircleShape,
                color = if (is3DMode) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.96f)
                },
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                ),
                shadowElevation = 3.dp
            ) {
                IconButton(
                    onClick = onToggle3DMode,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("map_3d_toggle_button")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = "Switch to 3D holographic slab view",
                            tint = if (is3DMode) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "3D",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (is3DMode) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }
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
                color = if (showCompassHud) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                },
                shadowElevation = 2.dp
            ) {
                IconButton(
                    onClick = { showCompassHud = !showCompassHud },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("map_compass_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = if (showCompassHud) "Hide live compass HUD" else "Show live compass HUD",
                        tint = if (showCompassHud) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                    )
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

/**
 * Builds deduplicated screen-space polyline vertices connecting [userPt] point-to-point
 * either through every guided waypoint in [activeRoute] to its destination, or sequentially
 * across all [markedAttractions] (`userPt -> stop[0] -> stop[1] -> ...`).
 */
internal fun buildPointToPointOffsets(
    userPt: Offset,
    activeRoute: OfflineRoutePlan?,
    markedAttractions: List<Attraction>,
    nodesById: Map<String, OfflineMapNode>,
    latLonToScreen: (Double, Double) -> Offset,
    minPointSpacingPx: Float = 4f
): List<Offset> {
    val rawPoints = mutableListOf<Offset>()
    if (activeRoute != null && activeRoute.nodePathIds.isNotEmpty()) {
        rawPoints.add(userPt)
        activeRoute.nodePathIds.forEach { nodeId ->
            nodesById[nodeId]?.let { node ->
                rawPoints.add(latLonToScreen(node.latitude, node.longitude))
            }
        }
        rawPoints.add(
            latLonToScreen(
                activeRoute.destination.latitude,
                activeRoute.destination.longitude
            )
        )
    } else if (markedAttractions.isNotEmpty()) {
        rawPoints.add(userPt)
        markedAttractions.forEach { spot ->
            rawPoints.add(latLonToScreen(spot.latitude, spot.longitude))
        }
    }

    if (rawPoints.isEmpty()) return emptyList()
    val deduped = mutableListOf(rawPoints.first())
    for (i in 1 until rawPoints.size) {
        val candidate = rawPoints[i]
        val isFinal = i == rawPoints.lastIndex
        val dist = hypot(candidate.x - deduped.last().x, candidate.y - deduped.last().y)
        if (dist >= minPointSpacingPx || (isFinal && deduped.size == 1 && dist >= 1f)) {
            deduped.add(candidate)
        } else if (isFinal && deduped.size >= 2) {
            deduped[deduped.lastIndex] = candidate
        }
    }
    return deduped
}

/**
 * Finds the point and unit tangent vector at [fraction] (0f..1f) of total polyline arc length.
 */
internal fun pointAlongPolylineAtFraction(
    points: List<Offset>,
    fraction: Float
): Pair<Offset, Offset> {
    if (points.isEmpty()) return Offset.Zero to Offset(1f, 0f)
    if (points.size == 1) return points.first() to Offset(1f, 0f)

    val lengths = FloatArray(points.size - 1)
    var totalLen = 0f
    for (i in 0 until points.size - 1) {
        val len = hypot(points[i + 1].x - points[i].x, points[i + 1].y - points[i].y)
        lengths[i] = len
        totalLen += len
    }
    if (totalLen < 1f) return points.first() to Offset(1f, 0f)

    val target = totalLen * fraction.coerceIn(0f, 1f)
    var acc = 0f
    for (i in lengths.indices) {
        val segLen = lengths[i]
        if (acc + segLen >= target && segLen > 0f) {
            val t = ((target - acc) / segLen).coerceIn(0f, 1f)
            val p1 = points[i]
            val p2 = points[i + 1]
            val pt = Offset(p1.x + (p2.x - p1.x) * t, p1.y + (p2.y - p1.y) * t)
            val tangent = Offset((p2.x - p1.x) / segLen, (p2.y - p1.y) / segLen)
            return pt to tangent
        }
        acc += segLen
    }
    val p1 = points[points.size - 2]
    val p2 = points.last()
    val segLen = hypot(p2.x - p1.x, p2.y - p1.y).coerceAtLeast(1f)
    return p2 to Offset((p2.x - p1.x) / segLen, (p2.y - p1.y) / segLen)
}

/**
 * Draws a crisp, white-cased directional arrowhead pointing along [directionAngleRad] with its apex at [tip].
 */
private fun DrawScope.drawDirectionalArrowHead(
    tip: Offset,
    directionAngleRad: Float,
    arrowLengthPx: Float,
    arrowHalfWidthPx: Float,
    fillColor: Color,
    casingColor: Color = Color.White
) {
    val cosA = cos(directionAngleRad)
    val sinA = sin(directionAngleRad)
    val baseCenterX = tip.x - cosA * arrowLengthPx
    val baseCenterY = tip.y - sinA * arrowLengthPx
    val notchX = tip.x - cosA * (arrowLengthPx * 0.72f)
    val notchY = tip.y - sinA * (arrowLengthPx * 0.72f)
    val perpX = -sinA * arrowHalfWidthPx
    val perpY = cosA * arrowHalfWidthPx

    val arrowPath = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(baseCenterX + perpX, baseCenterY + perpY)
        lineTo(notchX, notchY)
        lineTo(baseCenterX - perpX, baseCenterY - perpY)
        close()
    }
    drawPath(
        path = arrowPath,
        color = casingColor,
        style = Stroke(width = 3.dp.toPx(), join = StrokeJoin.Round)
    )
    drawPath(
        path = arrowPath,
        color = fillColor
    )
}

