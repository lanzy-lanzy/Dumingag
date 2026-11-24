package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.Attraction
import com.example.model.AttractionCategory
import com.example.model.OfflineRoutePlan
import com.example.model.TravelMode
import com.example.ui.components.AttractionCoverImage
import com.example.ui.components.AttractionReviewSummary
import com.example.ui.components.CategoryFilterChips
import com.example.ui.components.MapPinStarBadge
import com.example.ui.theme.MapHighwayFill
import com.example.ui.theme.MapRatingGold
import com.example.ui.theme.MapRouteCore
import com.example.ui.theme.MapTrailStroke
import com.example.ui.theme.MapUserPin
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class SpotTravelMetrics(
    val distanceKm: Double,
    val estimatedHours: Double,
    val estimatedMinutes: Int,
    val travelMode: TravelMode
)

fun haversineDistanceKmExact(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
        sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return (earthRadiusKm * c).coerceAtLeast(0.0)
}

fun haversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    return (haversineDistanceKmExact(lat1, lon1, lat2, lon2) * 10.0).roundToInt() / 10.0
}

fun calculateSpotTravelMetrics(
    fromLat: Double,
    fromLon: Double,
    toLat: Double,
    toLon: Double,
    travelMode: TravelMode
): SpotTravelMetrics {
    val distKm = haversineDistanceKmExact(fromLat, fromLon, toLat, toLon)
    return calculateMetricsForDistance(distKm, travelMode)
}

fun calculateMetricsForDistance(
    distanceKm: Double,
    travelMode: TravelMode
): SpotTravelMetrics {
    val safeDist = distanceKm.coerceAtLeast(0.0)
    val speed = travelMode.speedKmH.coerceAtLeast(1.0)
    val hours = safeDist / speed
    val mins = if (safeDist < 0.01) 1 else ceil(hours * 60.0).toInt().coerceAtLeast(1)
    return SpotTravelMetrics(
        distanceKm = safeDist,
        estimatedHours = hours,
        estimatedMinutes = mins,
        travelMode = travelMode
    )
}

data class RouteLegMetrics(
    val legIndex: Int,
    val fromLabel: String,
    val toAttraction: Attraction,
    val fromLatLon: Pair<Double, Double>,
    val toLatLon: Pair<Double, Double>,
    val metrics: SpotTravelMetrics
)

data class MultiPointRouteSummary(
    val legs: List<RouteLegMetrics>,
    val totalDistanceKm: Double,
    val totalHours: Double,
    val totalMinutes: Int,
    val travelMode: TravelMode
)

fun calculateMultiPointRoute(
    userLatLon: Pair<Double, Double>,
    markedAttractions: List<Attraction>,
    travelMode: TravelMode
): MultiPointRouteSummary {
    if (markedAttractions.isEmpty()) {
        return MultiPointRouteSummary(
            legs = emptyList(),
            totalDistanceKm = 0.0,
            totalHours = 0.0,
            totalMinutes = 0,
            travelMode = travelMode
        )
    }
    val legs = mutableListOf<RouteLegMetrics>()
    var currentFrom = userLatLon
    var currentLabel = "Current Location"
    var totalKm = 0.0

    markedAttractions.forEachIndexed { idx, spot ->
        val spotLatLon = spot.latitude to spot.longitude
        val legMetrics = calculateSpotTravelMetrics(
            fromLat = currentFrom.first,
            fromLon = currentFrom.second,
            toLat = spotLatLon.first,
            toLon = spotLatLon.second,
            travelMode = travelMode
        )
        totalKm += legMetrics.distanceKm
        legs.add(
            RouteLegMetrics(
                legIndex = idx,
                fromLabel = currentLabel,
                toAttraction = spot,
                fromLatLon = currentFrom,
                toLatLon = spotLatLon,
                metrics = legMetrics
            )
        )
        currentFrom = spotLatLon
        currentLabel = spot.name.en
    }

    val totalMetrics = calculateMetricsForDistance(totalKm, travelMode)
    return MultiPointRouteSummary(
        legs = legs,
        totalDistanceKm = totalMetrics.distanceKm,
        totalHours = totalMetrics.estimatedHours,
        totalMinutes = totalMetrics.estimatedMinutes,
        travelMode = travelMode
    )
}

fun formatDistanceDisplay(distanceKm: Double): String {
    return if (distanceKm < 1.0) {
        val meters = (distanceKm * 1000.0).roundToInt().coerceAtLeast(1)
        "$meters m"
    } else {
        "${String.format(Locale.US, "%.1f", distanceKm)} km"
    }
}

fun formatHoursDecimal(hours: Double): String {
    val safeHours = hours.coerceAtLeast(0.0)
    return when {
        safeHours >= 10.0 -> "${String.format(Locale.US, "%.1f", safeHours)} hrs"
        safeHours >= 1.0 -> "${String.format(Locale.US, "%.2f", safeHours)} hrs"
        else -> "${String.format(Locale.US, "%.2f", safeHours)} hr"
    }
}

fun formatHoursAndMinutes(totalMinutes: Int): String {
    val safeMins = totalMinutes.coerceAtLeast(1)
    val hrs = safeMins / 60
    val remMins = safeMins % 60
    return when {
        hrs > 0 && remMins > 0 -> "${hrs}h ${remMins}m"
        hrs > 0 -> "${hrs}h"
        else -> "$remMins min"
    }
}

fun formatTravelHoursSummary(metrics: SpotTravelMetrics): String {
    return "${formatHoursDecimal(metrics.estimatedHours)} (${formatHoursAndMinutes(metrics.estimatedMinutes)})"
}

fun categoryPinAccent(category: AttractionCategory, primary: Color, secondary: Color, tertiary: Color): Color =
    when (category) {
        AttractionCategory.NATURE -> primary
        AttractionCategory.CULTURAL -> secondary
        AttractionCategory.LANDMARK -> tertiary
        AttractionCategory.ORGANIC_FARM -> primary
        AttractionCategory.HISTORICAL -> secondary
        AttractionCategory.ALL -> primary
    }

@Composable
fun MapSearchAndFilterHeader(
    query: String,
    selectedCategory: AttractionCategory,
    selectedCategoryCode: String = selectedCategory.name,
    managedCategories: List<com.example.model.ManagedCategory> = com.example.model.CategoryCatalog.defaultCategories,
    language: AppLanguage,
    layersActiveCount: Int,
    showLayerPanel: Boolean,
    mapQuickFilter: String = "ALL",
    onMapQuickFilterChange: (String) -> Unit = {},
    onQueryChange: (String) -> Unit,
    onCategorySelected: (AttractionCategory) -> Unit,
    onCategoryCodeSelected: (String) -> Unit = { code ->
        val enumMatch = enumValues<AttractionCategory>().firstOrNull { it.name == code } ?: AttractionCategory.ALL
        onCategorySelected(enumMatch)
    },
    onToggleLayerPanel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = "Find falls, farms, plazas...",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search map landmarks") },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear map search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("map_search_input")
            )

            Surface(
                color = if (showLayerPanel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    if (showLayerPanel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onToggleLayerPanel)
                    .testTag("map_layers_toggle_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Map layers and legend",
                        tint = if (showLayerPanel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$layersActiveCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (showLayerPanel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        com.example.ui.components.ManagedCategoryFilterChips(
            selectedCategoryCode = selectedCategoryCode,
            onCategoryCodeSelected = onCategoryCodeSelected,
            language = language,
            managedCategories = managedCategories
        )

        val quickFilters = listOf(
            "ALL" to "All Pins",
            "REVIEWED" to "★ Reviewed",
            "TOP_RATED" to "4★+ Rated",
            "FAVORITES" to "♥ Favorites"
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(quickFilters, key = { it.first }) { (code, label) ->
                FilterChip(
                    selected = mapQuickFilter == code,
                    onClick = { onMapQuickFilterChange(if (mapQuickFilter == code && code != "ALL") "ALL" else code) },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    },
                    modifier = Modifier.testTag("map_quick_filter_${code.lowercase()}")
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MapLegendAndLayerCard(
    showRoads: Boolean,
    showJunctions: Boolean,
    showContours: Boolean,
    onToggleRoads: () -> Unit,
    onToggleJunctions: () -> Unit,
    onToggleContours: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)),
        border = BorderStroke(1.3.dp, Color.White.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Map Layers & Legend",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close layers", modifier = Modifier.size(18.dp))
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = showRoads,
                    onClick = onToggleRoads,
                    label = { Text("Roads & Trails", maxLines = 1) },
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .heightIn(min = 48.dp)
                )
                FilterChip(
                    selected = showJunctions,
                    onClick = onToggleJunctions,
                    label = { Text("Transit Nodes", maxLines = 1) },
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .heightIn(min = 48.dp)
                )
                FilterChip(
                    selected = showContours,
                    onClick = onToggleContours,
                    label = { Text("Elevation & Hubs", maxLines = 1) },
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .heightIn(min = 48.dp)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LegendSwatch("Highway", MapHighwayFill)
                LegendSwatch("Eco-Trail", MapTrailStroke)
                LegendSwatch("Active Route", MapRouteCore)
                LegendSwatch("Your GPS", MapUserPin)
            }
        }
    }
}

@Composable
private fun LegendSwatch(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(11.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MapQuickLandmarkStrip(
    attractions: List<Attraction>,
    userLatLon: Pair<Double, Double>,
    language: AppLanguage,
    managedCategories: List<com.example.model.ManagedCategory> = com.example.model.CategoryCatalog.defaultCategories,
    travelMode: TravelMode = TravelMode.HABAL_HABAL,
    markedAttractions: List<Attraction> = emptyList(),
    reviewSummaries: Map<String, AttractionReviewSummary> = emptyMap(),
    onOpenAttractionReviews: (Attraction) -> Unit = {},
    onSelectAttraction: (Attraction) -> Unit,
    modifier: Modifier = Modifier
) {
    if (attractions.isEmpty()) return
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(attractions, key = { it.id }) { attraction ->
            val metrics = calculateSpotTravelMetrics(
                fromLat = userLatLon.first,
                fromLon = userLatLon.second,
                toLat = attraction.latitude,
                toLon = attraction.longitude,
                travelMode = travelMode
            )
            val markIndex = markedAttractions.indexOfFirst { it.id == attraction.id }
            val isMarked = markIndex >= 0
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isMarked) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                border = BorderStroke(
                    width = if (isMarked) 1.5.dp else 1.dp,
                    color = if (isMarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .width(248.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onSelectAttraction(attraction) }
                    .testTag("map_strip_card_${attraction.id}")
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        AttractionCoverImage(
                            attraction = attraction,
                            contentDescription = attraction.name.get(language),
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        if (isMarked) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(20.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${markIndex + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = attraction.name.get(language),
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${AppStrings.categoryLabelForCode(attraction.categoryCode, managedCategories, language)} · Brgy. ${attraction.barangay}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${formatDistanceDisplay(metrics.distanceKm)} · ${formatHoursDecimal(metrics.estimatedHours)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            val cardReviewSummary = reviewSummaries[attraction.id]
                            if (cardReviewSummary != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                MapPinStarBadge(
                                    summary = cardReviewSummary,
                                    attractionName = attraction.name.get(language),
                                    onClick = { onOpenAttractionReviews(attraction) },
                                    compact = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MapLandmarkSideList(
    attractions: List<Attraction>,
    userLatLon: Pair<Double, Double>,
    language: AppLanguage,
    managedCategories: List<com.example.model.ManagedCategory> = com.example.model.CategoryCatalog.defaultCategories,
    travelMode: TravelMode = TravelMode.HABAL_HABAL,
    markedAttractions: List<Attraction> = emptyList(),
    reviewSummaries: Map<String, AttractionReviewSummary> = emptyMap(),
    onOpenAttractionReviews: (Attraction) -> Unit = {},
    onSelectAttraction: (Attraction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Dumingag Landmarks (${attractions.size})",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (attractions.isEmpty()) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Text(
                    text = "No landmarks match your current search or category filter.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(attractions, key = { it.id }) { attraction ->
                    val metrics = calculateSpotTravelMetrics(
                        fromLat = userLatLon.first,
                        fromLon = userLatLon.second,
                        toLat = attraction.latitude,
                        toLon = attraction.longitude,
                        travelMode = travelMode
                    )
                    val markIndex = markedAttractions.indexOfFirst { it.id == attraction.id }
                    val isMarked = markIndex >= 0
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMarked) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = BorderStroke(
                            width = if (isMarked) 1.5.dp else 1.dp,
                            color = if (isMarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onSelectAttraction(attraction) }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                AttractionCoverImage(
                                    attraction = attraction,
                                    contentDescription = attraction.name.get(language),
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                if (isMarked) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(20.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${markIndex + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = attraction.name.get(language),
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${AppStrings.categoryLabelForCode(attraction.categoryCode, managedCategories, language)} · Brgy. ${attraction.barangay}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${formatDistanceDisplay(metrics.distanceKm)} · ${formatHoursDecimal(metrics.estimatedHours)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    val sideReviewSummary = reviewSummaries[attraction.id]
                                    if (sideReviewSummary != null) {
                                        MapPinStarBadge(
                                            summary = sideReviewSummary,
                                            attractionName = attraction.name.get(language),
                                            onClick = { onOpenAttractionReviews(attraction) },
                                            compact = true
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectedPinCard(
    attraction: Attraction,
    userLatLon: Pair<Double, Double>,
    language: AppLanguage,
    managedCategories: List<com.example.model.ManagedCategory> = com.example.model.CategoryCatalog.defaultCategories,
    travelMode: TravelMode = TravelMode.HABAL_HABAL,
    markedAttractions: List<Attraction> = listOf(attraction),
    reviewSummary: AttractionReviewSummary? = null,
    onOpenReviews: () -> Unit = {},
    onTravelModeChange: (TravelMode) -> Unit = {},
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onRemoveMarkedAttraction: (Attraction) -> Unit = {},
    onClearAllMarks: () -> Unit = {},
    onDismiss: () -> Unit,
    onNavigateOffline: () -> Unit,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeStops = remember(markedAttractions, attraction) {
        markedAttractions.ifEmpty { listOf(attraction) }
    }
    val multiSummary = remember(userLatLon, activeStops, travelMode) {
        calculateMultiPointRoute(
            userLatLon = userLatLon,
            markedAttractions = activeStops,
            travelMode = travelMode
        )
    }
    val distLabel = formatDistanceDisplay(multiSummary.totalDistanceKm)
    val hoursLabel = formatHoursDecimal(multiSummary.totalHours)
    val hmLabel = formatHoursAndMinutes(multiSummary.totalMinutes)
    val isMultiStop = activeStops.size > 1

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)),
        border = BorderStroke(1.3.dp, Color.White.copy(alpha = 0.52f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AttractionCoverImage(
                    attraction = attraction,
                    contentDescription = attraction.name.get(language),
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = AppStrings.categoryLabelForCode(attraction.categoryCode, managedCategories, language),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("map_pin_favorite_${attraction.id}")
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                                    tint = if (isFavorite) Color(0xFFE11D48) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("map_close_details_card")
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Hide details card and view map",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = attraction.name.get(language),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Brgy. ${attraction.barangay} • ${attraction.entryFee.get(language)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (reviewSummary != null) {
                            MapPinStarBadge(
                                summary = reviewSummary,
                                attractionName = attraction.name.get(language),
                                onClick = onOpenReviews,
                                compact = false
                            )
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable(onClick = onOpenReviews)
                                    .testTag("map_selected_rate_${attraction.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Rate ${attraction.name.get(language)}",
                                        tint = MapRatingGold,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Rate",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Live Distance & Travel Time (in Hours) from Current Location
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("map_distance_duration_card")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(0.95f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                shape = CircleShape,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Route,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isMultiStop) "Total Distance" else "Distance",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                Text(
                                    text = distLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("map_pin_distance_value")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.weight(1.35f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                                shape = CircleShape,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isMultiStop) "Total Travel Time" else "Est. Travel Time",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                Text(
                                    text = "$hoursLabel ($hmLabel)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("map_pin_hours_value")
                                )
                            }
                        }
                    }

                    // Connected Point-to-Point Landmarks Chain
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isMultiStop) {
                                "${activeStops.size} connected landmarks • Tap pins to add/remove"
                            } else {
                                "Connected from your location • Tap more landmarks to chain"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = onClearAllMarks,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .heightIn(min = 36.dp)
                                .testTag("map_clear_all_marks")
                        ) {
                            Text(
                                text = if (isMultiStop) "Clear All" else "Clear Line",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (isMultiStop) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("map_marked_stops_chain")
                        ) {
                            item {
                                Surface(
                                    color = MapUserPin.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, MapUserPin.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MyLocation,
                                            contentDescription = null,
                                            tint = MapUserPin,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "You",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MapUserPin
                                        )
                                    }
                                }
                            }
                            itemsIndexed(activeStops, key = { _, stop -> stop.id }) { idx, stop ->
                                val leg = multiSummary.legs.getOrNull(idx)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .clickable { onRemoveMarkedAttraction(stop) }
                                            .testTag("map_chain_chip_${stop.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "${idx + 1}. ${stop.name.get(language).split(" ").take(2).joinToString(" ")}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                            if (leg != null) {
                                                Text(
                                                    text = "(${formatDistanceDisplay(leg.metrics.distanceKm)})",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    maxLines = 1
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove ${stop.name.get(language)}",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TravelMode.entries.forEach { mode ->
                            val selected = mode == travelMode
                            val modeIcon = when (mode) {
                                TravelMode.WALK -> Icons.AutoMirrored.Filled.DirectionsWalk
                                TravelMode.HABAL_HABAL -> Icons.Default.TwoWheeler
                                TravelMode.JEEPNEY_CAR -> Icons.Default.DirectionsCar
                            }
                            Surface(
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(50))
                                    .clickable { onTravelModeChange(mode) }
                                    .testTag("pin_travel_mode_${mode.name}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = modeIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = AppStrings.travelModeLabel(mode, language),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onNavigateOffline,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .heightIn(min = 48.dp)
                        .testTag("map_start_nav_button")
                ) {
                    Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = AppStrings.navigateOffline(language),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = onOpenGuide,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Full Guide",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun CollapsedRouteSummaryBar(
    markedAttractions: List<Attraction>,
    userLatLon: Pair<Double, Double>,
    language: AppLanguage,
    travelMode: TravelMode,
    onCycleTravelMode: () -> Unit,
    onReopenDetails: () -> Unit,
    onClearAllMarks: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (markedAttractions.isEmpty()) return
    val multiSummary = remember(userLatLon, markedAttractions, travelMode) {
        calculateMultiPointRoute(
            userLatLon = userLatLon,
            markedAttractions = markedAttractions,
            travelMode = travelMode
        )
    }
    val distLabel = formatDistanceDisplay(multiSummary.totalDistanceKm)
    val hoursLabel = formatHoursDecimal(multiSummary.totalHours)
    val hmLabel = formatHoursAndMinutes(multiSummary.totalMinutes)
    val stopsText = if (markedAttractions.size == 1) "1 stop" else "${markedAttractions.size} stops"
    val modeIcon = when (travelMode) {
        TravelMode.WALK -> Icons.AutoMirrored.Filled.DirectionsWalk
        TravelMode.HABAL_HABAL -> Icons.Default.TwoWheeler
        TravelMode.JEEPNEY_CAR -> Icons.Default.DirectionsCar
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("map_collapsed_route_bar"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)),
        border = BorderStroke(1.3.dp, MapUserPin.copy(alpha = 0.62f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = MapUserPin.copy(alpha = 0.14f),
                shape = CircleShape,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Route,
                        contentDescription = null,
                        tint = MapUserPin,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onReopenDetails)
            ) {
                Text(
                    text = "$stopsText · $distLabel · $hoursLabel",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("map_collapsed_summary_text")
                )
                Text(
                    text = "$hmLabel (${AppStrings.travelModeLabel(travelMode, language)}) · Tap pins to add",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onCycleTravelMode,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("map_collapsed_mode_button")
            ) {
                Icon(
                    imageVector = modeIcon,
                    contentDescription = "Switch travel mode",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onReopenDetails)
                    .testTag("map_reopen_details_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandLess,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Details",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1
                    )
                }
            }

            IconButton(
                onClick = onClearAllMarks,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("map_disconnect_lines_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear connected lines",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OfflineNavigationDrawer(
    route: OfflineRoutePlan,
    stepIndex: Int,
    language: AppLanguage,
    travelMode: TravelMode,
    onTravelModeChange: (TravelMode) -> Unit,
    onStepDelta: (Int) -> Unit,
    onSelectStep: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAllSteps by remember(route.destination.id) { mutableStateOf(false) }
    val currentStep = route.steps.getOrNull(stepIndex)
    val progress = if (route.steps.isNotEmpty()) {
        (stepIndex + 1).toFloat() / route.steps.size.toFloat()
    } else 1f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = route.destination.name.get(language),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val routeMetrics = calculateMetricsForDistance(route.totalDistanceKm, travelMode)
                    Text(
                        text = "${route.originName} → ${formatDistanceDisplay(route.totalDistanceKm)} · ${formatTravelHoursSummary(routeMetrics)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("close_navigation_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "End offline navigation")
                }
            }

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            )

            // Responsive Travel Mode Selector
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TravelMode.entries.forEach { mode ->
                    val selected = mode == travelMode
                    val modeIcon = when (mode) {
                        TravelMode.WALK -> Icons.AutoMirrored.Filled.DirectionsWalk
                        TravelMode.HABAL_HABAL -> Icons.Default.TwoWheeler
                        TravelMode.JEEPNEY_CAR -> Icons.Default.DirectionsCar
                    }
                    Surface(
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable { onTravelModeChange(mode) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = modeIcon,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = AppStrings.travelModeLabel(mode, language),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (currentStep != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Step ${stepIndex + 1}/${route.steps.size} · ${currentStep.bearingLabel.get(language)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${currentStep.distanceKm} km · ${currentStep.roadName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = currentStep.instruction.get(language),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = showAllSteps && route.steps.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 170.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(route.steps, key = { it.index }) { step ->
                            val isCurrent = step.index == stepIndex
                            Surface(
                                color = if (isCurrent) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectStep(step.index) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${step.index + 1}. ${step.fromNodeName} → ${step.toNodeName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${step.distanceKm} km",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(
                    onClick = { onStepDelta(-1) },
                    enabled = stepIndex > 0,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Prev", maxLines = 1)
                }

                TextButton(
                    onClick = { showAllSteps = !showAllSteps },
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .heightIn(min = 48.dp)
                ) {
                    Text(if (showAllSteps) "Hide Steps" else "All Steps (${route.steps.size})", maxLines = 1)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (showAllSteps) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }

                TextButton(
                    onClick = { onStepDelta(1) },
                    enabled = stepIndex < route.steps.lastIndex,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .heightIn(min = 48.dp)
                ) {
                    Text("Next", maxLines = 1)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
