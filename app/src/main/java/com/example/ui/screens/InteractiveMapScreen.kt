package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.DumingagSeedData
import com.example.data.remote.OsmProjection
import com.example.data.remote.OsmTileLoader
import com.example.model.AppLanguage
import com.example.model.Attraction
import com.example.model.AttractionCategory
import com.example.model.CategoryCatalog
import com.example.model.CommunityReview
import com.example.model.ManagedCategory
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.model.OfflineMapNode
import com.example.model.OfflineRoutePlan
import com.example.model.TourismEvent
import com.example.model.TourismEventSeverity
import com.example.model.TourismEventType
import com.example.model.TravelMode
import com.example.ui.components.ComposeReviewDialog
import com.example.ui.components.ComprehensiveAttractionReviewsSheet
import com.example.ui.components.buildReviewSummariesByAttraction
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlin.math.abs
import kotlin.math.max

@Composable
fun InteractiveMapScreen(
    language: AppLanguage,
    attractions: List<Attraction>,
    selectedCategory: AttractionCategory,
    selectedCategoryCode: String = selectedCategory.name,
    managedCategories: List<ManagedCategory> = CategoryCatalog.defaultCategories,
    selectedAttraction: Attraction?,
    markedAttractions: List<Attraction> = listOfNotNull(selectedAttraction),
    activeRoute: OfflineRoutePlan?,
    activeStepIndex: Int,
    travelMode: TravelMode,
    userLatLon: Pair<Double, Double>,
    offlinePackReady: Boolean,
    isEffectiveOffline: Boolean = false,
    isFullscreen: Boolean = false,
    favoriteIds: Set<String> = emptySet(),
    onToggleFullscreen: (Boolean) -> Unit = {},
    onToggleFavorite: (Attraction) -> Unit = {},
    onCategorySelected: (AttractionCategory) -> Unit,
    onCategoryCodeSelected: (String) -> Unit = { code ->
        val enumMatch = enumValues<AttractionCategory>().firstOrNull { it.name == code } ?: AttractionCategory.ALL
        onCategorySelected(enumMatch)
    },
    onSelectAttraction: (Attraction?) -> Unit,
    onToggleMarkAttraction: (Attraction) -> Unit = { onSelectAttraction(it) },
    onRemoveMarkedAttraction: (Attraction) -> Unit = { onSelectAttraction(null) },
    onClearAllMarks: () -> Unit = { onSelectAttraction(null) },
    onStartNavigation: (Attraction) -> Unit,
    onClearNavigation: () -> Unit,
    onStepNavigation: (Int) -> Unit,
    onTravelModeChange: (TravelMode) -> Unit,
    onUpdateLocation: (Double, Double) -> Unit,
    onOpenDetail: (Attraction) -> Unit,
    reviews: List<CommunityReview> = emptyList(),
    currentUserId: String = "",
    isAdmin: Boolean = false,
    tourismEvents: List<TourismEvent> = emptyList(),
    onSubmitReview: (Attraction, Int, String, String, String?) -> Unit = { _, _, _, _, _ -> },
    onUpdateReview: (String, Int, String, String, String?, Boolean) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteReview: (String) -> Unit = {},
    onToggleReviewHelpful: (CommunityReview) -> Unit = {},
    onAdminReplyReview: (String, String) -> Unit = { _, _ -> }
) {
    var is3DMode by rememberSaveable { mutableStateOf(false) }
    val reviewSummaries = remember(reviews) { buildReviewSummariesByAttraction(reviews) }
    var reviewsSheetAttractionId by rememberSaveable { mutableStateOf<String?>(null) }
    var composeReviewAttractionId by remember { mutableStateOf<String?>(null) }
    var editingReview by remember { mutableStateOf<CommunityReview?>(null) }
    var dismissedAdvisoryId by rememberSaveable { mutableStateOf<String?>(null) }
    val handleOpenAttractionReviews: (Attraction) -> Unit = { spot ->
        reviewsSheetAttractionId = spot.id
    }

    BackHandler(enabled = isFullscreen) {
        onToggleFullscreen(false)
    }
    BackHandler(enabled = is3DMode && !isFullscreen) {
        is3DMode = false
    }

    val context = LocalContext.current
    val osmTileLoader = remember(context) { OsmTileLoader(context) }
    val loadedTiles = remember { mutableStateMapOf<String, ImageBitmap>() }

    // Centered on Dumingag, Zamboanga del Sur (8.1650° N, 123.3465° E)
    var centerLat by rememberSaveable { mutableDoubleStateOf(OsmProjection.DUMINGAG_LAT) }
    var centerLon by rememberSaveable { mutableDoubleStateOf(OsmProjection.DUMINGAG_LON) }
    var zoomLevel by rememberSaveable { mutableFloatStateOf(13.2f) }
    var gpsStatusNote by remember { mutableStateOf<String?>(null) }
    var tileTrigger by remember { mutableIntStateOf(0) }

    var mapSearchQuery by rememberSaveable { mutableStateOf("") }
    var mapQuickFilter by rememberSaveable { mutableStateOf("ALL") }
    var showLayerPanel by remember { mutableStateOf(false) }
    var showRoadNetwork by rememberSaveable { mutableStateOf(true) }
    var showJunctionNodes by rememberSaveable { mutableStateOf(true) }
    var showElevationAndHubs by rememberSaveable { mutableStateOf(true) }
    var selectedJunctionNode by remember { mutableStateOf<OfflineMapNode?>(null) }

    val mapFilteredAttractions = remember(attractions, mapSearchQuery, mapQuickFilter, reviewSummaries, favoriteIds, language) {
        val trimmed = mapSearchQuery.trim().lowercase()
        attractions.filter { attr ->
            val matchesQuery = trimmed.isEmpty() ||
                attr.name.get(language).lowercase().contains(trimmed) ||
                attr.name.en.lowercase().contains(trimmed) ||
                attr.barangay.lowercase().contains(trimmed)
            val summary = reviewSummaries[attr.id]
            val matchesQuick = when (mapQuickFilter) {
                "REVIEWED" -> summary != null && summary.reviewCount > 0
                "TOP_RATED" -> summary != null && summary.averageRating >= 4.0
                "FAVORITES" -> attr.id in favoriteIds
                else -> true
            }
            matchesQuery && matchesQuick
        }
    }

    val activeMapAdvisory = remember(tourismEvents, dismissedAdvisoryId) {
        tourismEvents.firstOrNull { evt ->
            evt.active &&
                evt.id != dismissedAdvisoryId &&
                (evt.eventType == TourismEventType.TRAIL_ADVISORY.code ||
                    evt.eventType == TourismEventType.WEATHER_ALERT.code ||
                    evt.severity == TourismEventSeverity.ALERT.code)
        }
    }

    val effectiveMarkedAttractions = remember(markedAttractions, selectedAttraction) {
        markedAttractions.ifEmpty { listOfNotNull(selectedAttraction) }
    }
    val focusedAttraction = selectedAttraction ?: effectiveMarkedAttractions.lastOrNull()
    var showDetailsCard by rememberSaveable { mutableStateOf(effectiveMarkedAttractions.isNotEmpty()) }
    var prevMarkedCount by rememberSaveable { mutableIntStateOf(effectiveMarkedAttractions.size) }

    LaunchedEffect(effectiveMarkedAttractions.size) {
        val currentCount = effectiveMarkedAttractions.size
        if (currentCount == 0) {
            showDetailsCard = false
        } else if (prevMarkedCount == 0) {
            showDetailsCard = true
        }
        prevMarkedCount = currentCount
    }

    LaunchedEffect(effectiveMarkedAttractions.map { it.id }, selectedAttraction?.id, isFullscreen, showDetailsCard) {
        if (effectiveMarkedAttractions.isNotEmpty()) {
            selectedJunctionNode = null
            val allLats = buildList {
                add(userLatLon.first)
                effectiveMarkedAttractions.forEach { add(it.latitude) }
            }
            val allLons = buildList {
                add(userLatLon.second)
                effectiveMarkedAttractions.forEach { add(it.longitude) }
            }
            val minLat = allLats.min()
            val maxLat = allLats.max()
            val minLon = allLons.min()
            val maxLon = allLons.max()
            val latSpan = abs(maxLat - minLat)
            val lonSpan = abs(maxLon - minLon)
            val maxSpan = max(latSpan, lonSpan)
            val bottomCardBias = if (isFullscreen && showDetailsCard) {
                (maxSpan * 0.22).coerceIn(0.004, 0.016)
            } else {
                0.0
            }
            val midLat = (((minLat + maxLat) / 2.0) - bottomCardBias)
                .coerceIn(OsmProjection.MIN_ZDS_LAT, OsmProjection.MAX_ZDS_LAT)
            val midLon = ((minLon + maxLon) / 2.0)
                .coerceIn(OsmProjection.MIN_ZDS_LON, OsmProjection.MAX_ZDS_LON)
            centerLat = midLat
            centerLon = midLon
            zoomLevel = when {
                maxSpan < 0.004 -> 14.8f
                maxSpan < 0.012 -> 13.6f
                maxSpan < 0.030 -> 12.6f
                maxSpan < 0.065 -> 11.7f
                maxSpan < 0.140 -> 10.9f
                maxSpan < 0.280 -> 10.2f
                else -> 9.7f
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun applyGpsCoordinates(lat: Double, lon: Double, recenterCamera: Boolean) {
        val inZamboangaDelSur = lat in OsmProjection.MIN_ZDS_LAT..OsmProjection.MAX_ZDS_LAT &&
            lon in OsmProjection.MIN_ZDS_LON..OsmProjection.MAX_ZDS_LON
        if (inZamboangaDelSur) {
            onUpdateLocation(lat, lon)
            if (recenterCamera) {
                centerLat = lat
                centerLon = lon
            }
            gpsStatusNote = "GPS locked (${"%.4f".format(lat)}°N)"
        } else {
            onUpdateLocation(DumingagSeedData.CENTER_LAT, DumingagSeedData.CENTER_LON)
            if (recenterCamera) {
                centerLat = DumingagSeedData.CENTER_LAT
                centerLon = DumingagSeedData.CENTER_LON
            }
            gpsStatusNote = "Focused on Dumingag, Zamboanga del Sur"
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchGpsLocation(recenterCamera: Boolean = true) {
        val fused = LocationServices.getFusedLocationProviderClient(context)
        fused.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                applyGpsCoordinates(loc.latitude, loc.longitude, recenterCamera)
            } else {
                fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .addOnSuccessListener { fresh ->
                        if (fresh != null) {
                            applyGpsCoordinates(fresh.latitude, fresh.longitude, recenterCamera)
                        } else if (recenterCamera) {
                            centerLat = DumingagSeedData.CENTER_LAT
                            centerLon = DumingagSeedData.CENTER_LON
                            gpsStatusNote = "Centered at Poblacion, Dumingag"
                        }
                    }
                    .addOnFailureListener {
                        if (recenterCamera) {
                            gpsStatusNote = "Offline Dumingag center active"
                        }
                    }
            }
        }.addOnFailureListener {
            if (recenterCamera) {
                gpsStatusNote = "Offline Dumingag center active"
            }
        }
    }

    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (fineGranted || coarseGranted) {
            fetchGpsLocation(recenterCamera = selectedAttraction == null)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchGpsLocation(recenterCamera = true)
        } else {
            gpsStatusNote = "Location denied • Centered on Dumingag"
        }
    }

    val handleFocusDumingag = {
        centerLat = OsmProjection.DUMINGAG_LAT
        centerLon = OsmProjection.DUMINGAG_LON
        zoomLevel = 13.4f
        gpsStatusNote = "Focused on Dumingag Town & Eco-Sites"
    }

    val handleFocusZds = {
        centerLat = 8.05
        centerLon = 123.38
        zoomLevel = 10.9f
        gpsStatusNote = "Viewing Zamboanga del Sur Region"
    }

    val handleRecenterGps = {
        zoomLevel = 13.4f
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (fineGranted || coarseGranted) {
            fetchGpsLocation(recenterCamera = true)
        } else {
            centerLat = OsmProjection.DUMINGAG_LAT
            centerLon = OsmProjection.DUMINGAG_LON
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    val handleSpotSelected: (Attraction) -> Unit = { spot ->
        selectedJunctionNode = null
        if (activeRoute != null && activeRoute.destination.id != spot.id) {
            onClearNavigation()
        }
        onToggleMarkAttraction(spot)
    }

    val handleCycleTravelMode = {
        val modes = TravelMode.entries
        val nextIndex = (modes.indexOf(travelMode) + 1) % modes.size
        onTravelModeChange(modes[nextIndex])
    }

    val layersActiveCount = listOf(showRoadNetwork, showJunctionNodes, showElevationAndHubs).count { it }
    val hasFullscreenOverlay = isFullscreen && (activeRoute != null || (focusedAttraction != null && showDetailsCard))

    if (isFullscreen) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (is3DMode) {
                Holographic3DMapSlab(
                    language = language,
                    attractions = mapFilteredAttractions,
                    selectedAttraction = focusedAttraction,
                    markedAttractions = effectiveMarkedAttractions,
                    activeRoute = activeRoute,
                    travelMode = travelMode,
                    userLatLon = userLatLon,
                    isFullscreen = true,
                    hasBottomOverlay = hasFullscreenOverlay,
                    loadedTiles = loadedTiles,
                    tileTrigger = tileTrigger,
                    osmTileLoader = osmTileLoader,
                    isEffectiveOffline = isEffectiveOffline,
                    onTileLoaded = { tileTrigger++ },
                    onToggle3DMode = { is3DMode = false },
                    onToggleFullscreen = { onToggleFullscreen(false) },
                    onSelectAttraction = handleSpotSelected,
                    reviewSummaries = reviewSummaries,
                    onOpenAttractionReviews = handleOpenAttractionReviews,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                OsmMapViewport(
                    language = language,
                    attractions = mapFilteredAttractions,
                    selectedAttraction = focusedAttraction,
                    markedAttractions = effectiveMarkedAttractions,
                    activeRoute = activeRoute,
                    travelMode = travelMode,
                    userLatLon = userLatLon,
                    centerLat = centerLat,
                    centerLon = centerLon,
                    zoomLevel = zoomLevel,
                    gpsStatusNote = gpsStatusNote,
                    offlinePackReady = offlinePackReady,
                    isEffectiveOffline = isEffectiveOffline,
                    isFullscreen = true,
                    hasBottomOverlay = hasFullscreenOverlay,
                    showRoadNetwork = showRoadNetwork,
                    showJunctionNodes = showJunctionNodes,
                    showElevationAndHubs = showElevationAndHubs,
                    showLayerPanel = showLayerPanel,
                    selectedJunctionNode = selectedJunctionNode,
                    loadedTiles = loadedTiles,
                    tileTrigger = tileTrigger,
                    osmTileLoader = osmTileLoader,
                    onPanZoom = { lat, lon, z ->
                        centerLat = lat
                        centerLon = lon
                        zoomLevel = z
                    },
                    onTileLoaded = { tileTrigger++ },
                    onFocusDumingag = handleFocusDumingag,
                    onFocusZds = handleFocusZds,
                    onRecenterGps = handleRecenterGps,
                    onToggleFullscreen = { onToggleFullscreen(false) },
                    onToggleLayerPanel = { showLayerPanel = !showLayerPanel },
                    onSelectAttraction = handleSpotSelected,
                    onSelectJunction = { node ->
                        selectedJunctionNode = if (selectedJunctionNode?.id == node.id) null else node
                        centerLat = node.latitude
                        centerLon = node.longitude
                    },
                    onToggleRoads = { showRoadNetwork = !showRoadNetwork },
                    onToggleJunctions = { showJunctionNodes = !showJunctionNodes },
                    onToggleContours = { showElevationAndHubs = !showElevationAndHubs },
                    onCloseLayers = { showLayerPanel = false },
                    is3DMode = false,
                    onToggle3DMode = { is3DMode = true },
                    reviewSummaries = reviewSummaries,
                    onOpenAttractionReviews = handleOpenAttractionReviews,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (activeRoute != null) {
                OfflineNavigationDrawer(
                    route = activeRoute,
                    stepIndex = activeStepIndex,
                    language = language,
                    travelMode = travelMode,
                    onTravelModeChange = onTravelModeChange,
                    onStepDelta = onStepNavigation,
                    onSelectStep = { targetIdx ->
                        onStepNavigation(targetIdx - activeStepIndex)
                    },
                    onClose = onClearNavigation,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .displayCutoutPadding()
                        .widthIn(max = 560.dp)
                )
            } else if (focusedAttraction != null && showDetailsCard) {
                SelectedPinCard(
                    attraction = focusedAttraction,
                    userLatLon = userLatLon,
                    language = language,
                    managedCategories = managedCategories,
                    travelMode = travelMode,
                    markedAttractions = effectiveMarkedAttractions,
                    reviewSummary = reviewSummaries[focusedAttraction.id],
                    onOpenReviews = { handleOpenAttractionReviews(focusedAttraction) },
                    onTravelModeChange = onTravelModeChange,
                    isFavorite = focusedAttraction.id in favoriteIds,
                    onToggleFavorite = { onToggleFavorite(focusedAttraction) },
                    onRemoveMarkedAttraction = onRemoveMarkedAttraction,
                    onClearAllMarks = {
                        showDetailsCard = false
                        onClearAllMarks()
                    },
                    onDismiss = { showDetailsCard = false },
                    onNavigateOffline = { onStartNavigation(focusedAttraction) },
                    onOpenGuide = { onOpenDetail(focusedAttraction) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .displayCutoutPadding()
                        .widthIn(max = 560.dp)
                )
            } else if (effectiveMarkedAttractions.isNotEmpty()) {
                CollapsedRouteSummaryBar(
                    markedAttractions = effectiveMarkedAttractions,
                    userLatLon = userLatLon,
                    language = language,
                    travelMode = travelMode,
                    onCycleTravelMode = handleCycleTravelMode,
                    onReopenDetails = { showDetailsCard = true },
                    onClearAllMarks = {
                        showDetailsCard = false
                        onClearAllMarks()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .displayCutoutPadding()
                        .widthIn(max = 560.dp)
                )
            }
        }
    } else {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useSupportingSidePane = maxWidth >= 680.dp

        Column(modifier = Modifier.fillMaxSize()) {
            MapSearchAndFilterHeader(
                query = mapSearchQuery,
                selectedCategory = selectedCategory,
                selectedCategoryCode = selectedCategoryCode,
                managedCategories = managedCategories,
                language = language,
                layersActiveCount = layersActiveCount,
                showLayerPanel = showLayerPanel,
                mapQuickFilter = mapQuickFilter,
                onMapQuickFilterChange = { mapQuickFilter = it },
                onQueryChange = { mapSearchQuery = it },
                onCategorySelected = onCategorySelected,
                onCategoryCodeSelected = onCategoryCodeSelected,
                onToggleLayerPanel = { showLayerPanel = !showLayerPanel }
            )

            activeMapAdvisory?.let { advisory ->
                val isWarning = advisory.severity == TourismEventSeverity.ALERT.code
                Surface(
                    color = if (isWarning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .testTag("map_active_advisory_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Active trail advisory",
                            tint = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = advisory.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isWarning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = advisory.summary,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isWarning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { dismissedAdvisoryId = advisory.id },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss advisory",
                                tint = if (isWarning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (useSupportingSidePane) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (is3DMode) {
                        Holographic3DMapSlab(
                            language = language,
                            attractions = mapFilteredAttractions,
                            selectedAttraction = focusedAttraction,
                            markedAttractions = effectiveMarkedAttractions,
                            activeRoute = activeRoute,
                            travelMode = travelMode,
                            userLatLon = userLatLon,
                            isFullscreen = false,
                            hasBottomOverlay = false,
                            loadedTiles = loadedTiles,
                            tileTrigger = tileTrigger,
                            osmTileLoader = osmTileLoader,
                            isEffectiveOffline = isEffectiveOffline,
                            onTileLoaded = { tileTrigger++ },
                            onToggle3DMode = { is3DMode = false },
                            onToggleFullscreen = { onToggleFullscreen(true) },
                            onSelectAttraction = handleSpotSelected,
                            reviewSummaries = reviewSummaries,
                            onOpenAttractionReviews = handleOpenAttractionReviews,
                            modifier = Modifier
                                .weight(1.45f)
                                .fillMaxHeight()
                        )
                    } else {
                        OsmMapViewport(
                            language = language,
                            attractions = mapFilteredAttractions,
                            selectedAttraction = focusedAttraction,
                            markedAttractions = effectiveMarkedAttractions,
                            activeRoute = activeRoute,
                            travelMode = travelMode,
                            userLatLon = userLatLon,
                            centerLat = centerLat,
                            centerLon = centerLon,
                            zoomLevel = zoomLevel,
                            gpsStatusNote = gpsStatusNote,
                            offlinePackReady = offlinePackReady,
                            isEffectiveOffline = isEffectiveOffline,
                            isFullscreen = false,
                            hasBottomOverlay = false,
                            showRoadNetwork = showRoadNetwork,
                            showJunctionNodes = showJunctionNodes,
                            showElevationAndHubs = showElevationAndHubs,
                            showLayerPanel = showLayerPanel,
                            selectedJunctionNode = selectedJunctionNode,
                            loadedTiles = loadedTiles,
                            tileTrigger = tileTrigger,
                            osmTileLoader = osmTileLoader,
                            onPanZoom = { lat, lon, z ->
                                centerLat = lat
                                centerLon = lon
                                zoomLevel = z
                            },
                            onTileLoaded = { tileTrigger++ },
                            onFocusDumingag = handleFocusDumingag,
                            onFocusZds = handleFocusZds,
                            onRecenterGps = handleRecenterGps,
                            onToggleFullscreen = { onToggleFullscreen(true) },
                            onToggleLayerPanel = { showLayerPanel = !showLayerPanel },
                            onSelectAttraction = handleSpotSelected,
                            onSelectJunction = { node ->
                                selectedJunctionNode = if (selectedJunctionNode?.id == node.id) null else node
                                centerLat = node.latitude
                                centerLon = node.longitude
                            },
                            onToggleRoads = { showRoadNetwork = !showRoadNetwork },
                            onToggleJunctions = { showJunctionNodes = !showJunctionNodes },
                            onToggleContours = { showElevationAndHubs = !showElevationAndHubs },
                            onCloseLayers = { showLayerPanel = false },
                            is3DMode = false,
                            onToggle3DMode = { is3DMode = true },
                            reviewSummaries = reviewSummaries,
                            onOpenAttractionReviews = handleOpenAttractionReviews,
                            modifier = Modifier
                                .weight(1.45f)
                                .fillMaxHeight()
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        when {
                            activeRoute != null -> {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    OfflineNavigationDrawer(
                                        route = activeRoute,
                                        stepIndex = activeStepIndex,
                                        language = language,
                                        travelMode = travelMode,
                                        onTravelModeChange = onTravelModeChange,
                                        onStepDelta = onStepNavigation,
                                        onSelectStep = { targetIdx ->
                                            onStepNavigation(targetIdx - activeStepIndex)
                                        },
                                        onClose = onClearNavigation
                                    )
                                }
                            }
                            focusedAttraction != null && showDetailsCard -> {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    SelectedPinCard(
                                        attraction = focusedAttraction,
                                        userLatLon = userLatLon,
                                        language = language,
                                        managedCategories = managedCategories,
                                        travelMode = travelMode,
                                        markedAttractions = effectiveMarkedAttractions,
                                        reviewSummary = reviewSummaries[focusedAttraction.id],
                                        onOpenReviews = { handleOpenAttractionReviews(focusedAttraction) },
                                        onTravelModeChange = onTravelModeChange,
                                        isFavorite = focusedAttraction.id in favoriteIds,
                                        onToggleFavorite = { onToggleFavorite(focusedAttraction) },
                                        onRemoveMarkedAttraction = onRemoveMarkedAttraction,
                                        onClearAllMarks = {
                                            showDetailsCard = false
                                            onClearAllMarks()
                                        },
                                        onDismiss = { showDetailsCard = false },
                                        onNavigateOffline = { onStartNavigation(focusedAttraction) },
                                        onOpenGuide = { onOpenDetail(focusedAttraction) }
                                    )
                                }
                            }
                            else -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    if (effectiveMarkedAttractions.isNotEmpty()) {
                                        CollapsedRouteSummaryBar(
                                            markedAttractions = effectiveMarkedAttractions,
                                            userLatLon = userLatLon,
                                            language = language,
                                            travelMode = travelMode,
                                            onCycleTravelMode = handleCycleTravelMode,
                                            onReopenDetails = { showDetailsCard = true },
                                            onClearAllMarks = {
                                                showDetailsCard = false
                                                onClearAllMarks()
                                            }
                                        )
                                    }
                                    MapLandmarkSideList(
                                        attractions = mapFilteredAttractions,
                                        userLatLon = userLatLon,
                                        language = language,
                                        managedCategories = managedCategories,
                                        travelMode = travelMode,
                                        markedAttractions = effectiveMarkedAttractions,
                                        reviewSummaries = reviewSummaries,
                                        onOpenAttractionReviews = handleOpenAttractionReviews,
                                        onSelectAttraction = handleSpotSelected,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                if (is3DMode) {
                    Holographic3DMapSlab(
                        language = language,
                        attractions = mapFilteredAttractions,
                        selectedAttraction = focusedAttraction,
                        markedAttractions = effectiveMarkedAttractions,
                        activeRoute = activeRoute,
                        travelMode = travelMode,
                        userLatLon = userLatLon,
                        isFullscreen = false,
                        hasBottomOverlay = false,
                        loadedTiles = loadedTiles,
                        tileTrigger = tileTrigger,
                        osmTileLoader = osmTileLoader,
                        isEffectiveOffline = isEffectiveOffline,
                        onTileLoaded = { tileTrigger++ },
                        onToggle3DMode = { is3DMode = false },
                        onToggleFullscreen = { onToggleFullscreen(true) },
                        onSelectAttraction = handleSpotSelected,
                        reviewSummaries = reviewSummaries,
                        onOpenAttractionReviews = handleOpenAttractionReviews,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    )
                } else {
                    OsmMapViewport(
                        language = language,
                        attractions = mapFilteredAttractions,
                        selectedAttraction = focusedAttraction,
                        markedAttractions = effectiveMarkedAttractions,
                        activeRoute = activeRoute,
                        travelMode = travelMode,
                        userLatLon = userLatLon,
                        centerLat = centerLat,
                        centerLon = centerLon,
                        zoomLevel = zoomLevel,
                        gpsStatusNote = gpsStatusNote,
                        offlinePackReady = offlinePackReady,
                        isEffectiveOffline = isEffectiveOffline,
                        isFullscreen = false,
                        hasBottomOverlay = false,
                        showRoadNetwork = showRoadNetwork,
                        showJunctionNodes = showJunctionNodes,
                        showElevationAndHubs = showElevationAndHubs,
                        showLayerPanel = showLayerPanel,
                        selectedJunctionNode = selectedJunctionNode,
                        loadedTiles = loadedTiles,
                        tileTrigger = tileTrigger,
                        osmTileLoader = osmTileLoader,
                        onPanZoom = { lat, lon, z ->
                            centerLat = lat
                            centerLon = lon
                            zoomLevel = z
                        },
                        onTileLoaded = { tileTrigger++ },
                        onFocusDumingag = handleFocusDumingag,
                        onFocusZds = handleFocusZds,
                        onRecenterGps = handleRecenterGps,
                        onToggleFullscreen = { onToggleFullscreen(true) },
                        onToggleLayerPanel = { showLayerPanel = !showLayerPanel },
                        onSelectAttraction = handleSpotSelected,
                        onSelectJunction = { node ->
                            selectedJunctionNode = if (selectedJunctionNode?.id == node.id) null else node
                            centerLat = node.latitude
                            centerLon = node.longitude
                        },
                        onToggleRoads = { showRoadNetwork = !showRoadNetwork },
                        onToggleJunctions = { showJunctionNodes = !showJunctionNodes },
                        onToggleContours = { showElevationAndHubs = !showElevationAndHubs },
                        onCloseLayers = { showLayerPanel = false },
                        is3DMode = false,
                        onToggle3DMode = { is3DMode = true },
                        reviewSummaries = reviewSummaries,
                        onOpenAttractionReviews = handleOpenAttractionReviews,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    )
                }

                when {
                    activeRoute != null -> {
                        OfflineNavigationDrawer(
                            route = activeRoute,
                            stepIndex = activeStepIndex,
                            language = language,
                            travelMode = travelMode,
                            onTravelModeChange = onTravelModeChange,
                            onStepDelta = onStepNavigation,
                            onSelectStep = { targetIdx ->
                                onStepNavigation(targetIdx - activeStepIndex)
                            },
                            onClose = onClearNavigation
                        )
                    }
                    focusedAttraction != null && showDetailsCard -> {
                        SelectedPinCard(
                            attraction = focusedAttraction,
                            userLatLon = userLatLon,
                            language = language,
                            managedCategories = managedCategories,
                            travelMode = travelMode,
                            markedAttractions = effectiveMarkedAttractions,
                            reviewSummary = reviewSummaries[focusedAttraction.id],
                            onOpenReviews = { handleOpenAttractionReviews(focusedAttraction) },
                            onTravelModeChange = onTravelModeChange,
                            isFavorite = focusedAttraction.id in favoriteIds,
                            onToggleFavorite = { onToggleFavorite(focusedAttraction) },
                            onRemoveMarkedAttraction = onRemoveMarkedAttraction,
                            onClearAllMarks = {
                                showDetailsCard = false
                                onClearAllMarks()
                            },
                            onDismiss = { showDetailsCard = false },
                            onNavigateOffline = { onStartNavigation(focusedAttraction) },
                            onOpenGuide = { onOpenDetail(focusedAttraction) }
                        )
                    }
                    effectiveMarkedAttractions.isNotEmpty() -> {
                        CollapsedRouteSummaryBar(
                            markedAttractions = effectiveMarkedAttractions,
                            userLatLon = userLatLon,
                            language = language,
                            travelMode = travelMode,
                            onCycleTravelMode = handleCycleTravelMode,
                            onReopenDetails = { showDetailsCard = true },
                            onClearAllMarks = {
                                showDetailsCard = false
                                onClearAllMarks()
                            }
                        )
                    }
                    else -> {
                        MapQuickLandmarkStrip(
                            attractions = mapFilteredAttractions,
                            userLatLon = userLatLon,
                            language = language,
                            managedCategories = managedCategories,
                            travelMode = travelMode,
                            markedAttractions = effectiveMarkedAttractions,
                            reviewSummaries = reviewSummaries,
                            onOpenAttractionReviews = handleOpenAttractionReviews,
                            onSelectAttraction = handleSpotSelected
                        )
                    }
                }
            }
        }
    }
    }

    val reviewsSheetAttraction = remember(reviewsSheetAttractionId, attractions, effectiveMarkedAttractions, focusedAttraction) {
        val targetId = reviewsSheetAttractionId ?: return@remember null
        attractions.firstOrNull { it.id == targetId }
            ?: effectiveMarkedAttractions.firstOrNull { it.id == targetId }
            ?: focusedAttraction?.takeIf { it.id == targetId }
    }
    if (reviewsSheetAttraction != null) {
        ComprehensiveAttractionReviewsSheet(
            attraction = reviewsSheetAttraction,
            summary = reviewSummaries[reviewsSheetAttraction.id],
            language = language,
            managedCategories = managedCategories,
            userLatLon = userLatLon,
            travelMode = travelMode,
            currentUserId = currentUserId,
            isAdmin = isAdmin,
            onDismiss = { reviewsSheetAttractionId = null },
            onWriteReview = { composeReviewAttractionId = reviewsSheetAttraction.id },
            onEditReview = { rev -> editingReview = rev },
            onDeleteReview = onDeleteReview,
            onToggleHelpful = onToggleReviewHelpful,
            onAdminReply = onAdminReplyReview,
            onOpenGuide = {
                val target = reviewsSheetAttraction
                reviewsSheetAttractionId = null
                onOpenDetail(target)
            }
        )
    }

    val composeTarget = remember(composeReviewAttractionId, attractions, reviewsSheetAttraction) {
        val id = composeReviewAttractionId ?: return@remember null
        attractions.firstOrNull { it.id == id } ?: reviewsSheetAttraction
    }
    if (composeTarget != null) {
        ComposeReviewDialog(
            language = language,
            attractions = attractions.ifEmpty { listOf(composeTarget) },
            initialReview = null,
            initialAttraction = composeTarget,
            onDismiss = { composeReviewAttractionId = null },
            onSave = { target, rating, comment, tag, photoData ->
                onSubmitReview(target, rating, comment, tag, photoData)
                composeReviewAttractionId = null
            }
        )
    }

    editingReview?.let { rev ->
        val editAttr = attractions.firstOrNull { it.id == rev.attractionId } ?: reviewsSheetAttraction
        if (editAttr != null) {
            ComposeReviewDialog(
                language = language,
                attractions = attractions.ifEmpty { listOf(editAttr) },
                initialReview = rev,
                initialAttraction = editAttr,
                onDismiss = { editingReview = null },
                onSave = { _, rating, comment, tag, photoData ->
                    val clearPhoto = photoData == null && rev.photoData.isNotBlank()
                    onUpdateReview(rev.id, rating, comment, tag, photoData, clearPhoto)
                    editingReview = null
                }
            )
        }
    }
}
