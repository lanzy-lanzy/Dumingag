package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.SmartTravelEngines
import com.example.data.WeatherSuitabilityBadge
import com.example.data.WeatherSuitabilityTone
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.Attraction
import com.example.model.AttractionCategory
import com.example.model.CategoryCatalog
import com.example.model.CommunityReview
import com.example.model.EcoBadge
import com.example.model.EmergencyContact
import com.example.model.ManagedCategory
import com.example.model.TourismEvent
import com.example.model.TourismEventSeverity
import com.example.model.TransitMethod
import com.example.model.UiState
import com.example.model.WeatherSnapshot
import com.example.ui.components.AttractionCoverImage
import com.example.ui.components.DumingagWeatherForecastCard
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.categoryIconForKey
import com.example.ui.theme.SplashGlassSpecularEdge
import com.example.ui.theme.SplashGlassSpecularMid
import com.example.ui.theme.SplashGlassSpecularTop
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    language: AppLanguage,
    searchQuery: String,
    selectedCategory: AttractionCategory,
    selectedCategoryCode: String = selectedCategory.name,
    managedCategories: List<ManagedCategory> = CategoryCatalog.defaultCategories,
    attractions: List<Attraction>,
    allAttractionsForSearch: List<Attraction> = attractions,
    savedIds: Set<String>,
    favoriteIds: Set<String> = emptySet(),
    showFavoritesOnly: Boolean = false,
    weatherState: UiState<WeatherSnapshot>,
    isEffectiveOffline: Boolean,
    isRefreshing: Boolean = false,
    lastSyncedAtMillis: Long = 0L,
    tourismEvents: List<TourismEvent> = emptyList(),
    transitMethods: List<TransitMethod> = emptyList(),
    emergencyContacts: List<EmergencyContact> = emptyList(),
    reviews: List<CommunityReview> = emptyList(),
    onSearchChange: (String) -> Unit,
    onCategorySelected: (AttractionCategory) -> Unit,
    onCategoryCodeSelected: (String) -> Unit = { code ->
        val enumMatch = enumValues<AttractionCategory>().firstOrNull { it.name == code } ?: AttractionCategory.ALL
        onCategorySelected(enumMatch)
    },
    onToggleFavoritesOnly: () -> Unit = {},
    onToggleFavorite: (Attraction) -> Unit = {},
    onRefreshWeather: () -> Unit,
    onRefreshAll: () -> Unit = onRefreshWeather,
    onAttractionClick: (Attraction) -> Unit,
    onNavigateToAttraction: (Attraction) -> Unit,
    onToggleSaveOffline: (Attraction) -> Unit,
    onOpenMapTab: () -> Unit,
    onOpenDirectory: () -> Unit,
    onOpenAiConcierge: () -> Unit = {},
    onOpenTransitTab: () -> Unit = {},
    onOpenReviewsTab: () -> Unit = {},
    ecoBadges: List<EcoBadge> = emptyList(),
    checkedInCount: Int = 0,
    totalAttractionsCount: Int = attractions.size
) {
    val activeCategories = managedCategories.ifEmpty { CategoryCatalog.defaultCategories }
    val normalizedSelectedCode = selectedCategoryCode.trim().uppercase().ifBlank { "ALL" }
    val currentWeather = (weatherState as? UiState.Success)?.data
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val activeEvents = remember(tourismEvents) { tourismEvents.filter { it.active } }
    val lastSyncedLabel = remember(lastSyncedAtMillis, isEffectiveOffline) {
        if (lastSyncedAtMillis > 0L) {
            val formatted = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(lastSyncedAtMillis))
            if (isEffectiveOffline) "Offline cache · $formatted" else "Synced · $formatted"
        } else {
            if (isEffectiveOffline) "Offline cache ready" else "Live sync active"
        }
    }
    val unifiedResults = remember(
        searchQuery,
        allAttractionsForSearch,
        transitMethods,
        emergencyContacts,
        activeEvents,
        reviews,
        language
    ) {
        if (searchQuery.trim().length >= 2) {
            SmartTravelEngines.performUnifiedSearch(
                query = searchQuery,
                attractions = allAttractionsForSearch,
                transitMethods = transitMethods,
                emergencyContacts = emergencyContacts,
                tourismEvents = activeEvents,
                reviews = reviews,
                language = language
            )
        } else {
            null
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefreshAll,
        modifier = Modifier
            .fillMaxSize()
            .testTag("explore_pull_to_refresh")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HeroHeaderCard(
                    language = language,
                    isEffectiveOffline = isEffectiveOffline,
                    syncStatusLabel = lastSyncedLabel,
                    onOpenMapTab = onOpenMapTab
                )
            }

            item {
                DumingagWeatherForecastCard(
                    language = language,
                    weatherState = weatherState,
                    onRefresh = onRefreshWeather,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // AI Dumingag Trip Concierge Interactive Entry Card
            item {
                LiquidGlassSurface(
                    cornerRadius = 24.dp,
                    elevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable(onClick = onOpenAiConcierge)
                        .testTag("explore_open_ai_concierge")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Ask Dumingag AI Concierge",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Dumingag AI Trip Concierge",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Ask for 1-day plans, habal-habal fares, rain-safe spots & local food tips.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        FilledTonalButton(
                            onClick = onOpenAiConcierge,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Ask", maxLines = 1)
                        }
                    }
                }
            }

            // Active Dumingag Events & Trail Advisories Strip
            if (activeEvents.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Events & Trail Advisories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.testTag("explore_events_strip")
                        ) {
                            items(activeEvents, key = { it.id }) { evt ->
                                val isWarn = evt.severity == TourismEventSeverity.ALERT.code
                                val isAdvisory = evt.severity == TourismEventSeverity.ADVISORY.code
                                val chipBg = when {
                                    isWarn -> MaterialTheme.colorScheme.errorContainer
                                    isAdvisory -> MaterialTheme.colorScheme.tertiaryContainer
                                    else -> MaterialTheme.colorScheme.primaryContainer
                                }
                                val chipFg = when {
                                    isWarn -> MaterialTheme.colorScheme.onErrorContainer
                                    isAdvisory -> MaterialTheme.colorScheme.onTertiaryContainer
                                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                                }
                                val linkedSpot = allAttractionsForSearch.firstOrNull { it.id == evt.attractionId }
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = chipBg.copy(alpha = 0.88f)),
                                    modifier = Modifier
                                        .width(276.dp)
                                        .clickable(enabled = linkedSpot != null) {
                                            linkedSpot?.let(onAttractionClick)
                                        }
                                        .testTag("explore_event_card_${evt.id}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isWarn || isAdvisory) Icons.Default.Warning else Icons.Default.Event,
                                                contentDescription = null,
                                                tint = chipFg,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = evt.dateLabel.ifBlank { evt.eventType },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = chipFg,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = evt.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = chipFg,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = evt.summary,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = chipFg.copy(alpha = 0.9f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (ecoBadges.isNotEmpty()) {
                item {
                    EcoTravelerPassportCard(
                        badges = ecoBadges,
                        checkedInCount = checkedInCount,
                        totalAttractionsCount = totalAttractionsCount,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            item {
                LocalFoodAndOrganicGuideSection()
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search spots, transit, hotlines, events, reviews...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("explore_search_input")
                )
            }

            // Unified Quick Search Cross-Category Matches
            if (unifiedResults != null &&
                (unifiedResults.transitMethods.isNotEmpty() ||
                    unifiedResults.emergencyContacts.isNotEmpty() ||
                    unifiedResults.tourismEvents.isNotEmpty() ||
                    unifiedResults.reviews.isNotEmpty())
            ) {
                item {
                    LiquidGlassSurface(
                        cornerRadius = 22.dp,
                        elevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("explore_unified_search_results")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Quick Matches Across Guide (${unifiedResults.totalMatches})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            unifiedResults.transitMethods.take(2).forEach { route ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(onClick = onOpenTransitTab)
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(route.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${route.fareRangePhp} · ${route.routeSummary}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                            unifiedResults.emergencyContacts.take(2).forEach { contact ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(onClick = onOpenTransitTab)
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(contact.title.get(language), style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${contact.phoneNumber} · ${contact.department}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                            unifiedResults.tourismEvents.take(2).forEach { evt ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(evt.title, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(evt.dateLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                            unifiedResults.reviews.take(2).forEach { rev ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(onClick = onOpenReviewsTab)
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Forum, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${rev.attractionName} · ${rev.rating}★ by ${rev.authorName}", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(rev.comment, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = showFavoritesOnly,
                            onClick = onToggleFavoritesOnly,
                            leadingIcon = {
                                Icon(
                                    imageVector = if (showFavoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (showFavoritesOnly) Color.White else Color(0xFFE11D48),
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Favorites (${favoriteIds.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE11D48),
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("favorites_filter_chip")
                        )
                    }
                    item(key = "category_all") {
                        val selected = normalizedSelectedCode == "ALL"
                        FilterChip(
                            selected = selected,
                            onClick = { onCategoryCodeSelected("ALL") },
                            label = { Text(AppStrings.categoryLabel(AttractionCategory.ALL, language)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("category_chip_all")
                        )
                    }
                    items(activeCategories, key = { it.effectiveCode }) { cat ->
                        val code = cat.effectiveCode
                        val selected = normalizedSelectedCode == code
                        FilterChip(
                            selected = selected,
                            onClick = { onCategoryCodeSelected(if (selected) "ALL" else code) },
                            leadingIcon = {
                                Icon(
                                    imageVector = categoryIconForKey(cat.iconKey),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(AppStrings.categoryLabelForCode(code, activeCategories, language)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("category_chip_${code.lowercase()}")
                        )
                    }
                }
            }

            item {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Local Attraction Guides",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    TextButton(
                        onClick = onOpenDirectory,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .heightIn(min = 48.dp)
                            .testTag("open_attractions_directory_button")
                    ) {
                        Text(
                            text = "Full Directory",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (attractions.isEmpty()) {
                item {
                    LiquidGlassSurface(
                        cornerRadius = 24.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No matching Dumingag spots found",
                                style = MaterialTheme.typography.titleMedium
                            )
                            TextButton(
                                onClick = {
                                    onSearchChange("")
                                    onCategoryCodeSelected("ALL")
                                    if (showFavoritesOnly) onToggleFavoritesOnly()
                                },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text("Reset filters", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            } else {
                items(attractions, key = { it.id }) { attraction ->
                    val weatherBadge = remember(attraction.id, currentWeather, currentHour) {
                        SmartTravelEngines.evaluateWeatherSuitability(attraction, currentWeather, currentHour)
                    }
                    AttractionGuideCard(
                        attraction = attraction,
                        categoryLabel = AppStrings.categoryLabelForCode(attraction.categoryCode, activeCategories, language),
                        language = language,
                        weatherBadge = weatherBadge,
                        isSavedOffline = savedIds.contains(attraction.id),
                        isFavorite = favoriteIds.contains(attraction.id),
                        onClick = { onAttractionClick(attraction) },
                        onNavigate = { onNavigateToAttraction(attraction) },
                        onToggleSave = { onToggleSaveOffline(attraction) },
                        onToggleFavorite = { onToggleFavorite(attraction) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroHeaderCard(
    language: AppLanguage,
    isEffectiveOffline: Boolean,
    syncStatusLabel: String = "",
    onOpenMapTab: () -> Unit
) {
    val heroShape = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(214.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .shadow(12.dp, heroShape, ambientColor = Color.Black.copy(alpha = 0.24f))
            .clip(heroShape)
            .border(
                width = 1.4.dp,
                brush = Brush.linearGradient(
                    colors = listOf(SplashGlassSpecularTop, SplashGlassSpecularMid, SplashGlassSpecularEdge)
                ),
                shape = heroShape
            )
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_dumingag_hero),
            contentDescription = "Dumingag scenic highlands",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0x28081C15), Color(0xD4061B14))
                    )
                )
                .drawBehind {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(SplashGlassSpecularTop.copy(alpha = 0.22f), Color.Transparent),
                            startY = 0f,
                            endY = size.height * 0.35f
                        ),
                        cornerRadius = CornerRadius(28.dp.toPx(), 28.dp.toPx())
                    )
                }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (isEffectiveOffline) Color(0xD9B55D1E) else Color(0xCC1B4332),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isEffectiveOffline) Icons.Default.CloudOff else Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEffectiveOffline) AppStrings.offlineLabel(language) else AppStrings.onlineLabel(language),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onOpenMapTab() }
                        .testTag("hero_open_map_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Directions,
                            contentDescription = "Open interactive map",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = AppStrings.navMap(language),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Dumingag",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White
                )
                Text(
                    text = AppStrings.welcomeTagline(language),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE9F5EC)
                )
                if (syncStatusLabel.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.testTag("explore_sync_status_label")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = Color(0xFFA7F3D0),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "$syncStatusLabel · Pull down to refresh",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA7F3D0)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttractionGuideCard(
    attraction: Attraction,
    categoryLabel: String,
    language: AppLanguage,
    weatherBadge: WeatherSuitabilityBadge? = null,
    isSavedOffline: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onNavigate: () -> Unit,
    onToggleSave: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassSurface(
        cornerRadius = 26.dp,
        elevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("attraction_card_${attraction.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
            ) {
                AttractionCoverImage(
                    attraction = attraction,
                    contentDescription = attraction.name.get(language),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0x99000000))
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = categoryLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                            contentColor = MaterialTheme.colorScheme.primary,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp)
                        ) {
                            IconButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("favorite_attraction_${attraction.id}")
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                                    tint = if (isFavorite) Color(0xFFE11D48) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                            contentColor = MaterialTheme.colorScheme.primary,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp)
                        ) {
                            IconButton(
                                onClick = onToggleSave,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = if (isSavedOffline) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = AppStrings.saveOffline(language),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Brgy. ${attraction.barangay}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                    Surface(
                        color = Color(0xCC081C15),
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${attraction.baseRating}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = attraction.name.get(language),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = attraction.subtitle.get(language),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                if (weatherBadge != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val pillBg = when (weatherBadge.tone) {
                        WeatherSuitabilityTone.GREAT -> MaterialTheme.colorScheme.primaryContainer
                        WeatherSuitabilityTone.RAIN_FRIENDLY -> MaterialTheme.colorScheme.secondaryContainer
                        WeatherSuitabilityTone.MORNING_PICK -> MaterialTheme.colorScheme.tertiaryContainer
                        WeatherSuitabilityTone.TRAIL_CAUTION -> MaterialTheme.colorScheme.errorContainer
                    }
                    val pillFg = when (weatherBadge.tone) {
                        WeatherSuitabilityTone.GREAT -> MaterialTheme.colorScheme.onPrimaryContainer
                        WeatherSuitabilityTone.RAIN_FRIENDLY -> MaterialTheme.colorScheme.onSecondaryContainer
                        WeatherSuitabilityTone.MORNING_PICK -> MaterialTheme.colorScheme.onTertiaryContainer
                        WeatherSuitabilityTone.TRAIL_CAUTION -> MaterialTheme.colorScheme.onErrorContainer
                    }
                    Surface(
                        color = pillBg,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("weather_badge_${attraction.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = null,
                                tint = pillFg,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${weatherBadge.label} · ${weatherBadge.reason}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = pillFg,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = attraction.description.get(language),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${attraction.entryFee.get(language)} • ${attraction.openHours}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    TextButton(
                        onClick = onNavigate,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .heightIn(min = 48.dp)
                            .testTag("card_navigate_${attraction.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Directions,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = AppStrings.navigateOffline(language),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
