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
import com.example.model.OfflineMapNode
import com.example.model.OfflineRoutePlan
import com.example.model.TravelMode
import com.google.android.gms.location.LocationServices

@Composable
fun InteractiveMapScreen(
    language: AppLanguage,
    attractions: List<Attraction>,
    selectedCategory: AttractionCategory,
    selectedAttraction: Attraction?,
    activeRoute: OfflineRoutePlan?,
    activeStepIndex: Int,
    travelMode: TravelMode,
    userLatLon: Pair<Double, Double>,
    offlinePackReady: Boolean,
    isEffectiveOffline: Boolean = false,
    isFullscreen: Boolean = false,
    onToggleFullscreen: (Boolean) -> Unit = {},
    onCategorySelected: (AttractionCategory) -> Unit,
    onSelectAttraction: (Attraction?) -> Unit,
    onStartNavigation: (Attraction) -> Unit,
    onClearNavigation: () -> Unit,
    onStepNavigation: (Int) -> Unit,
    onTravelModeChange: (TravelMode) -> Unit,
    onUpdateLocation: (Double, Double) -> Unit,
    onOpenDetail: (Attraction) -> Unit
) {
    BackHandler(enabled = isFullscreen) {
        onToggleFullscreen(false)
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
    var showLayerPanel by remember { mutableStateOf(false) }
    var showRoadNetwork by rememberSaveable { mutableStateOf(true) }
    var showJunctionNodes by rememberSaveable { mutableStateOf(true) }
    var showElevationAndHubs by rememberSaveable { mutableStateOf(true) }
    var selectedJunctionNode by remember { mutableStateOf<OfflineMapNode?>(null) }

    val mapFilteredAttractions = remember(attractions, mapSearchQuery, language) {
        val trimmed = mapSearchQuery.trim().lowercase()
        if (trimmed.isEmpty()) {
            attractions
        } else {
            attractions.filter { attr ->
                attr.name.get(language).lowercase().contains(trimmed) ||
                    attr.name.en.lowercase().contains(trimmed) ||
                    attr.barangay.lowercase().contains(trimmed)
            }
        }
    }

    LaunchedEffect(selectedAttraction?.id) {
        selectedAttraction?.let {
            selectedJunctionNode = null
            centerLat = it.latitude
            centerLon = it.longitude
            if (zoomLevel < 12.8f) zoomLevel = 13.5f
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchGpsLocation() {
        val fused = LocationServices.getFusedLocationProviderClient(context)
        fused.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                val inZamboangaDelSur = loc.latitude in OsmProjection.MIN_ZDS_LAT..OsmProjection.MAX_ZDS_LAT &&
                    loc.longitude in OsmProjection.MIN_ZDS_LON..OsmProjection.MAX_ZDS_LON
                if (inZamboangaDelSur) {
                    onUpdateLocation(loc.latitude, loc.longitude)
                    centerLat = loc.latitude
                    centerLon = loc.longitude
                    gpsStatusNote = "GPS locked (${"%.4f".format(loc.latitude)}°N)"
                } else {
                    onUpdateLocation(DumingagSeedData.CENTER_LAT, DumingagSeedData.CENTER_LON)
                    centerLat = DumingagSeedData.CENTER_LAT
                    centerLon = DumingagSeedData.CENTER_LON
                    gpsStatusNote = "Focused on Dumingag, Zamboanga del Sur"
                }
            } else {
                centerLat = DumingagSeedData.CENTER_LAT
                centerLon = DumingagSeedData.CENTER_LON
                gpsStatusNote = "Centered at Poblacion, Dumingag"
            }
        }.addOnFailureListener {
            gpsStatusNote = "Offline Dumingag center active"
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchGpsLocation()
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
        centerLat = OsmProjection.DUMINGAG_LAT
        centerLon = OsmProjection.DUMINGAG_LON
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (fineGranted || coarseGranted) {
            fetchGpsLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    val layersActiveCount = listOf(showRoadNetwork, showJunctionNodes, showElevationAndHubs).count { it }
    val hasFullscreenOverlay = isFullscreen && (activeRoute != null || selectedAttraction != null)

    if (isFullscreen) {
        Box(modifier = Modifier.fillMaxSize()) {
            OsmMapViewport(
                language = language,
                attractions = mapFilteredAttractions,
                selectedAttraction = selectedAttraction,
                activeRoute = activeRoute,
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
                onSelectAttraction = {
                    selectedJunctionNode = null
                    onSelectAttraction(it)
                },
                onSelectJunction = { node ->
                    selectedJunctionNode = if (selectedJunctionNode?.id == node.id) null else node
                    centerLat = node.latitude
                    centerLon = node.longitude
                },
                onToggleRoads = { showRoadNetwork = !showRoadNetwork },
                onToggleJunctions = { showJunctionNodes = !showJunctionNodes },
                onToggleContours = { showElevationAndHubs = !showElevationAndHubs },
                onCloseLayers = { showLayerPanel = false },
                modifier = Modifier.fillMaxSize()
            )

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
            } else if (selectedAttraction != null) {
                SelectedPinCard(
                    attraction = selectedAttraction,
                    userLatLon = userLatLon,
                    language = language,
                    onDismiss = { onSelectAttraction(null) },
                    onNavigateOffline = { onStartNavigation(selectedAttraction) },
                    onOpenGuide = { onOpenDetail(selectedAttraction) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .displayCutoutPadding()
                        .widthIn(max = 560.dp)
                )
            }
        }
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useSupportingSidePane = maxWidth >= 680.dp

        Column(modifier = Modifier.fillMaxSize()) {
            MapSearchAndFilterHeader(
                query = mapSearchQuery,
                selectedCategory = selectedCategory,
                language = language,
                layersActiveCount = layersActiveCount,
                showLayerPanel = showLayerPanel,
                onQueryChange = { mapSearchQuery = it },
                onCategorySelected = onCategorySelected,
                onToggleLayerPanel = { showLayerPanel = !showLayerPanel }
            )

            if (useSupportingSidePane) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OsmMapViewport(
                        language = language,
                        attractions = mapFilteredAttractions,
                        selectedAttraction = selectedAttraction,
                        activeRoute = activeRoute,
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
                        onSelectAttraction = {
                            selectedJunctionNode = null
                            onSelectAttraction(it)
                        },
                        onSelectJunction = { node ->
                            selectedJunctionNode = if (selectedJunctionNode?.id == node.id) null else node
                            centerLat = node.latitude
                            centerLon = node.longitude
                        },
                        onToggleRoads = { showRoadNetwork = !showRoadNetwork },
                        onToggleJunctions = { showJunctionNodes = !showJunctionNodes },
                        onToggleContours = { showElevationAndHubs = !showElevationAndHubs },
                        onCloseLayers = { showLayerPanel = false },
                        modifier = Modifier
                            .weight(1.45f)
                            .fillMaxHeight()
                    )

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
                            selectedAttraction != null -> {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    SelectedPinCard(
                                        attraction = selectedAttraction,
                                        userLatLon = userLatLon,
                                        language = language,
                                        onDismiss = { onSelectAttraction(null) },
                                        onNavigateOffline = { onStartNavigation(selectedAttraction) },
                                        onOpenGuide = { onOpenDetail(selectedAttraction) }
                                    )
                                }
                            }
                            else -> {
                                MapLandmarkSideList(
                                    attractions = mapFilteredAttractions,
                                    userLatLon = userLatLon,
                                    language = language,
                                    onSelectAttraction = { onSelectAttraction(it) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            } else {
                OsmMapViewport(
                    language = language,
                    attractions = mapFilteredAttractions,
                    selectedAttraction = selectedAttraction,
                    activeRoute = activeRoute,
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
                    onSelectAttraction = {
                        selectedJunctionNode = null
                        onSelectAttraction(it)
                    },
                    onSelectJunction = { node ->
                        selectedJunctionNode = if (selectedJunctionNode?.id == node.id) null else node
                        centerLat = node.latitude
                        centerLon = node.longitude
                    },
                    onToggleRoads = { showRoadNetwork = !showRoadNetwork },
                    onToggleJunctions = { showJunctionNodes = !showJunctionNodes },
                    onToggleContours = { showElevationAndHubs = !showElevationAndHubs },
                    onCloseLayers = { showLayerPanel = false },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )

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
                    selectedAttraction != null -> {
                        SelectedPinCard(
                            attraction = selectedAttraction,
                            userLatLon = userLatLon,
                            language = language,
                            onDismiss = { onSelectAttraction(null) },
                            onNavigateOffline = { onStartNavigation(selectedAttraction) },
                            onOpenGuide = { onOpenDetail(selectedAttraction) }
                        )
                    }
                    else -> {
                        MapQuickLandmarkStrip(
                            attractions = mapFilteredAttractions,
                            userLatLon = userLatLon,
                            language = language,
                            onSelectAttraction = { onSelectAttraction(it) }
                        )
                    }
                }
            }
        }
    }
}
