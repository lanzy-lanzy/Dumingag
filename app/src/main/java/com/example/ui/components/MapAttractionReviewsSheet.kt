package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.Attraction
import com.example.model.CategoryCatalog
import com.example.model.CommunityReview
import com.example.model.ManagedCategory
import com.example.model.TravelMode
import com.example.ui.screens.calculateSpotTravelMetrics
import com.example.ui.screens.formatDistanceDisplay
import com.example.ui.screens.formatHoursDecimal
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.MapRatingGold
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Reply

enum class ReviewSortOption(val label: String) {
    NEWEST("Newest"),
    MOST_HELPFUL("Most Helpful"),
    HIGHEST("Highest Rated"),
    LOWEST("Lowest Rated")
}

data class AttractionReviewSummary(
    val attractionId: String,
    val reviewCount: Int,
    val averageRating: Double,
    val distribution: Map<Int, Int>,
    val topTripTags: List<Pair<String, Int>>,
    val recommendationPercent: Int,
    val latestReview: CommunityReview?,
    val reviews: List<CommunityReview>
) {
    val roundedStars: Int
        get() = averageRating.roundToInt().coerceIn(1, 5)

    val formattedAverage: String
        get() = String.format(Locale.US, "%.1f", averageRating)
}

/**
 * Groups public community reviews by attractionId and computes rich summary analytics.
 * Only attractions with at least 1 review are included so map stars appear exclusively on reviewed spots.
 */
fun buildReviewSummariesByAttraction(
    reviews: List<CommunityReview>
): Map<String, AttractionReviewSummary> {
    if (reviews.isEmpty()) return emptyMap()
    return reviews
        .filter { it.attractionId.isNotBlank() }
        .groupBy { it.attractionId }
        .mapValues { (attractionId, list) ->
            val sortedNewest = list.sortedByDescending { review ->
                review.updatedAt?.toDate()?.time
                    ?: review.createdAt?.toDate()?.time
                    ?: 0L
            }
            val ratings = sortedNewest.map { it.rating.toInt().coerceIn(1, 5) }
            val avg = ratings.average()
            val dist = (5 downTo 1).associateWith { star -> ratings.count { it == star } }
            val positiveCount = ratings.count { it >= 4 }
            val recPct = ((positiveCount.toDouble() / ratings.size.toDouble()) * 100.0).roundToInt()
            val tags = sortedNewest
                .map { it.visitTag.trim() }
                .filter { it.isNotBlank() }
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .map { it.key to it.value }

            AttractionReviewSummary(
                attractionId = attractionId,
                reviewCount = sortedNewest.size,
                averageRating = avg,
                distribution = dist,
                topTripTags = tags,
                recommendationPercent = recPct,
                latestReview = sortedNewest.firstOrNull(),
                reviews = sortedNewest
            )
        }
}

fun filterAndSortReviews(
    reviews: List<CommunityReview>,
    starFilter: Int?,
    tripTagFilter: String?,
    sortOption: ReviewSortOption
): List<CommunityReview> {
    val filtered = reviews.filter { rev ->
        val matchesStar = starFilter == null || rev.rating.toInt().coerceIn(1, 5) == starFilter
        val matchesTag = tripTagFilter == null || rev.visitTag.equals(tripTagFilter, ignoreCase = true)
        matchesStar && matchesTag
    }
    return when (sortOption) {
        ReviewSortOption.NEWEST -> filtered.sortedByDescending {
            it.updatedAt?.toDate()?.time ?: it.createdAt?.toDate()?.time ?: 0L
        }
        ReviewSortOption.MOST_HELPFUL -> filtered.sortedWith(
            compareByDescending<CommunityReview> { it.helpfulUserIds.size }
                .thenByDescending { it.verifiedVisit }
                .thenByDescending { it.rating }
                .thenByDescending { it.updatedAt?.toDate()?.time ?: it.createdAt?.toDate()?.time ?: 0L }
        )
        ReviewSortOption.HIGHEST -> filtered.sortedWith(
            compareByDescending<CommunityReview> { it.rating }
                .thenByDescending { it.updatedAt?.toDate()?.time ?: it.createdAt?.toDate()?.time ?: 0L }
        )
        ReviewSortOption.LOWEST -> filtered.sortedWith(
            compareBy<CommunityReview> { it.rating }
                .thenByDescending { it.updatedAt?.toDate()?.time ?: it.createdAt?.toDate()?.time ?: 0L }
        )
    }
}

fun ratingQualitativeLabel(rating: Int): String = when (rating.coerceIn(1, 5)) {
    5 -> "Exceptional"
    4 -> "Great"
    3 -> "Good"
    2 -> "Fair"
    else -> "Needs Improvement"
}

/**
 * Clickable gold star pill rendered beneath map attraction pins when the attraction has community reviews.
 * Tapping this pill directly opens the comprehensive full-review details sheet.
 */
@Composable
fun MapPinStarBadge(
    summary: AttractionReviewSummary,
    attractionName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    darkSurface: Boolean = false
) {
    val isDark = darkSurface || LocalIsDarkTheme.current
    val bgColor = if (isDark) {
        Color(0xFF0A241A).copy(alpha = 0.94f)
    } else {
        Color(0xFFFFFBF0).copy(alpha = 0.97f)
    }
    val textColor = if (isDark) Color(0xFFFFF4D2) else Color(0xFF3E2900)
    val countColor = if (isDark) Color(0xFFD6EADF) else Color(0xFF6B5320)
    val starCount = summary.roundedStars

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.2.dp, MapRatingGold.copy(alpha = if (isDark) 0.9f else 0.85f)),
        shadowElevation = 4.dp,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "View ${summary.reviewCount} reviews, ${summary.formattedAverage} stars for $attractionName"
            }
            .testTag("map_pin_stars_${summary.attractionId}")
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 6.dp else 7.dp,
                vertical = if (compact) 3.dp else 3.5.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 3.dp)
        ) {
            if (compact) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MapRatingGold,
                    modifier = Modifier.size(12.dp)
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(0.5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..5) {
                        Icon(
                            imageVector = if (i <= starCount) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (i <= starCount) MapRatingGold else MapRatingGold.copy(alpha = 0.42f),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
            Text(
                text = summary.formattedAverage,
                style = MaterialTheme.typography.labelSmall,
                fontSize = if (compact) 10.sp else 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = "(${summary.reviewCount})",
                style = MaterialTheme.typography.labelSmall,
                fontSize = if (compact) 9.5.sp else 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = countColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ComprehensiveAttractionReviewsSheet(
    attraction: Attraction,
    summary: AttractionReviewSummary?,
    language: AppLanguage,
    managedCategories: List<ManagedCategory> = CategoryCatalog.defaultCategories,
    userLatLon: Pair<Double, Double>,
    travelMode: TravelMode,
    currentUserId: String,
    isAdmin: Boolean = false,
    onDismiss: () -> Unit,
    onWriteReview: () -> Unit,
    onEditReview: (CommunityReview) -> Unit,
    onDeleteReview: (String) -> Unit,
    onToggleHelpful: ((CommunityReview) -> Unit)? = null,
    onAdminReply: ((String, String) -> Unit)? = null,
    onOpenGuide: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedStarFilter by remember(attraction.id) { mutableStateOf<Int?>(null) }
    var selectedTripTag by remember(attraction.id) { mutableStateOf<String?>(null) }
    var sortOption by remember(attraction.id) { mutableStateOf(ReviewSortOption.NEWEST) }
    var zoomedPhotoReview by remember(attraction.id) { mutableStateOf<CommunityReview?>(null) }

    val reviews = summary?.reviews.orEmpty()
    val photoReviews = remember(reviews) { reviews.filter { it.photoData.isNotBlank() } }
    val filteredReviews = remember(reviews, selectedStarFilter, selectedTripTag, sortOption) {
        filterAndSortReviews(
            reviews = reviews,
            starFilter = selectedStarFilter,
            tripTagFilter = selectedTripTag,
            sortOption = sortOption
        )
    }

    zoomedPhotoReview?.let { target ->
        FullScreenPhotoZoomDialog(
            photoData = target.photoData,
            caption = attraction.name.get(language),
            subtitle = "Photo by ${target.authorName} · ${target.rating}★",
            onDismiss = { zoomedPhotoReview = null }
        )
    }
    val metrics = remember(userLatLon, attraction, travelMode) {
        calculateSpotTravelMetrics(
            fromLat = userLatLon.first,
            fromLon = userLatLon.second,
            toLat = attraction.latitude,
            toLon = attraction.longitude,
            travelMode = travelMode
        )
    }
    val isDark = LocalIsDarkTheme.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("map_reviews_detail_sheet")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Attraction Header Banner
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AttractionCoverImage(
                        attraction = attraction,
                        contentDescription = attraction.name.get(language),
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = AppStrings.categoryLabelForCode(
                                        attraction.categoryCode,
                                        managedCategories,
                                        language
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            if (summary != null) {
                                Surface(
                                    color = MapRatingGold.copy(alpha = if (isDark) 0.22f else 0.16f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "${summary.reviewCount} ${if (summary.reviewCount == 1) "Review" else "Reviews"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isDark) Color(0xFFFFE599) else Color(0xFF7A5300),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = attraction.name.get(language),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Brgy. ${attraction.barangay}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "·",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "${formatDistanceDisplay(metrics.distanceKm)} (${formatHoursDecimal(metrics.estimatedHours)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("close_map_reviews_sheet")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close reviews sheet"
                        )
                    }
                }
            }

            // 2. Comprehensive Rating Overview & Interactive Star Histogram
            item {
                if (summary != null) {
                    ComprehensiveRatingDashboardCard(
                        summary = summary,
                        selectedStarFilter = selectedStarFilter,
                        onToggleStarFilter = { star ->
                            selectedStarFilter = if (selectedStarFilter == star) null else star
                        }
                    )
                } else {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = liquidGlassCardColors(),
                        border = liquidGlassBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StarRatingBar(rating = 5, starSize = 24.dp)
                            Text(
                                text = "No traveler reviews yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Be the first to share trail conditions, entry tips, or highlights for ${attraction.name.get(language)}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 3. Action Buttons Row (Write Review + Full Attraction Guide)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onWriteReview,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("map_sheet_write_review_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddComment,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Write Review", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = onOpenGuide,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("map_sheet_open_guide_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Full Guide", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // 3.5 Traveler Photos Strip (Tap-to-Zoom Gallery)
            if (photoReviews.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Traveler Photos (${photoReviews.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(photoReviews, key = { "photo_${it.id}" }) { rev ->
                                EncodedPhotoThumbnail(
                                    photoData = rev.photoData,
                                    contentDescription = "Traveler photo by ${rev.authorName}",
                                    onClick = { zoomedPhotoReview = rev },
                                    modifier = Modifier
                                        .size(width = 112.dp, height = 84.dp)
                                        .testTag("map_sheet_gallery_photo_${rev.id}")
                                )
                            }
                        }
                    }
                }
            }

            // 4. Trip Style Highlights & Traveler Insights
            if (summary != null && summary.topTripTags.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Traveler Trip Highlights",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            summary.topTripTags.forEach { (tag, count) ->
                                val isSelected = selectedTripTag.equals(tag, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedTripTag = if (isSelected) null else tag
                                    },
                                    label = {
                                        Text("$tag ($count)", maxLines = 1)
                                    },
                                    modifier = Modifier.testTag("map_sheet_tag_filter_$tag")
                                )
                            }
                        }
                    }
                }
            }

            // 5. Sort & Star Filter Bar
            if (reviews.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedStarFilter != null || selectedTripTag != null) {
                                    "Showing ${filteredReviews.size} of ${reviews.size} reviews"
                                } else {
                                    "All Traveler Reviews (${reviews.size})"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (selectedStarFilter != null || selectedTripTag != null) {
                                TextButton(
                                    onClick = {
                                        selectedStarFilter = null
                                        selectedTripTag = null
                                    },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text("Clear Filters")
                                }
                            }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ReviewSortOption.entries) { option ->
                                FilterChip(
                                    selected = sortOption == option,
                                    onClick = { sortOption = option },
                                    label = { Text(option.label, maxLines = 1) },
                                    modifier = Modifier.testTag("map_sheet_sort_${option.name}")
                                )
                            }
                        }
                    }
                }
            }

            // 6. Comprehensive Individual Review Cards
            if (reviews.isNotEmpty() && filteredReviews.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "No reviews match this filter",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            TextButton(
                                onClick = {
                                    selectedStarFilter = null
                                    selectedTripTag = null
                                },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text("Show All ${reviews.size} Reviews")
                            }
                        }
                    }
                }
            } else {
                items(filteredReviews, key = { it.id }) { review ->
                    ComprehensiveReviewDetailCard(
                        review = review,
                        isOwner = review.userId == currentUserId,
                        currentUserId = currentUserId,
                        isAdmin = isAdmin,
                        onEdit = { onEditReview(review) },
                        onDelete = { onDeleteReview(review.id) },
                        onToggleHelpful = onToggleHelpful?.let { cb -> { cb(review) } },
                        onAdminReply = onAdminReply?.let { cb -> { text -> cb(review.id, text) } },
                        onPhotoClick = { zoomedPhotoReview = review }
                    )
                }
            }
        }
    }
}

@Composable
private fun ComprehensiveRatingDashboardCard(
    summary: AttractionReviewSummary,
    selectedStarFilter: Int?,
    onToggleStarFilter: (Int) -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val highlightGradient = Brush.linearGradient(
        colors = if (isDark) {
            listOf(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
            )
        } else {
            listOf(
                Color(0xFFFFF8E5),
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            )
        }
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.2.dp, MapRatingGold.copy(alpha = if (isDark) 0.48f else 0.55f)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("map_reviews_dashboard_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(highlightGradient)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Score Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.width(108.dp)
                ) {
                    Text(
                        text = summary.formattedAverage,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    StarRatingBar(
                        rating = summary.roundedStars,
                        starSize = 18.dp
                    )
                    Text(
                        text = ratingQualitativeLabel(summary.roundedStars),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${summary.reviewCount} ${if (summary.reviewCount == 1) "review" else "reviews"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Interactive 5★ -> 1★ Distribution Bars
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    for (star in 5 downTo 1) {
                        val count = summary.distribution[star] ?: 0
                        val targetFraction = if (summary.reviewCount > 0) {
                            count.toFloat() / summary.reviewCount.toFloat()
                        } else {
                            0f
                        }
                        val animatedFraction by animateFloatAsState(
                            targetValue = targetFraction,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "starBar_$star"
                        )
                        val isFilterSelected = selectedStarFilter == star

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isFilterSelected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    } else {
                                        Color.Transparent
                                    }
                                )
                                .clickable { onToggleStarFilter(star) }
                                .padding(horizontal = 4.dp, vertical = 3.dp)
                                .testTag("map_reviews_bar_$star")
                        ) {
                            Text(
                                text = "$star",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.width(10.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MapRatingGold,
                                modifier = Modifier.size(12.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
                            ) {
                                if (animatedFraction > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(animatedFraction.coerceIn(0.05f, 1f))
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(50))
                                            .background(
                                                if (isFilterSelected) MaterialTheme.colorScheme.primary else MapRatingGold
                                            )
                                    )
                                }
                            }
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(20.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "${summary.recommendationPercent}% recommend (4–5★)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Tap any star bar to filter",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ComprehensiveReviewDetailCard(
    review: CommunityReview,
    isOwner: Boolean,
    currentUserId: String,
    isAdmin: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleHelpful: (() -> Unit)?,
    onAdminReply: ((String) -> Unit)?,
    onPhotoClick: () -> Unit
) {
    val locale = Locale.getDefault()
    val fullDateFmt = remember(locale) { SimpleDateFormat("MMM d, yyyy · h:mm a", locale) }
    val createdDate = review.createdAt?.toDate()
    val updatedDate = review.updatedAt?.toDate()
    val formattedTimestamp = createdDate?.let { fullDateFmt.format(it) } ?: "Just posted"
    val wasEdited = createdDate != null && updatedDate != null &&
        (updatedDate.time - createdDate.time) > 60_000L
    val authorInitial = review.authorName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "T"
    val starInt = review.rating.toInt().coerceIn(1, 5)
    val isDark = LocalIsDarkTheme.current
    val isHelpfulByMe = currentUserId.isNotBlank() && review.helpfulUserIds.contains(currentUserId)
    val helpfulCount = review.helpfulUserIds.size
    var showAdminReplyDialog by remember(review.id) { mutableStateOf(false) }

    if (showAdminReplyDialog && onAdminReply != null) {
        AdminOfficialReplyDialog(
            attractionName = review.attractionName,
            authorName = review.authorName,
            initialReply = review.adminReply,
            onDismiss = { showAdminReplyDialog = false },
            onSave = { replyText ->
                onAdminReply(replyText)
                showAdminReplyDialog = false
            }
        )
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        border = liquidGlassBorder(),
        colors = liquidGlassCardColors(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("map_sheet_review_card_${review.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Author & Score Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = authorInitial,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = review.authorName.ifBlank { "Dumingag Traveler" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified traveler",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        if (isOwner) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "You",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (wasEdited) "$formattedTimestamp · Edited" else formattedTimestamp,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Surface(
                    color = MapRatingGold.copy(alpha = if (isDark) 0.22f else 0.16f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MapRatingGold.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "$starInt out of 5 stars",
                            tint = MapRatingGold,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "$starInt.0",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color(0xFFFFE599) else Color(0xFF6B4700)
                        )
                    }
                }
            }

            // Star Bar + Qualitative Rating + Trip Type Tag
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StarRatingBar(
                    rating = starInt,
                    starSize = 18.dp,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = ratingQualitativeLabel(starInt),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                if (review.verifiedVisit) {
                    Surface(
                        color = Color(0xFF0F766E).copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, Color(0xFF14B8A6).copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified Visit",
                                tint = Color(0xFF0D9488),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Verified Visit",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0D9488)
                            )
                        }
                    }
                }
                if (review.visitTag.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = review.visitTag,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Full Untruncated Review Comment
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 0.42f else 0.48f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = review.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 21.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }

            if (review.photoData.isNotBlank()) {
                EncodedPhotoThumbnail(
                    photoData = review.photoData,
                    contentDescription = "Review photo by ${review.authorName}",
                    onClick = onPhotoClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                        .testTag("map_sheet_review_photo_${review.id}")
                )
            }

            if (review.adminReply.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = review.adminReplyBy.ifBlank { "Official Dumingag Tourism" },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = review.adminReply,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (onToggleHelpful != null) {
                    FilterChip(
                        selected = isHelpfulByMe,
                        onClick = onToggleHelpful,
                        label = {
                            Text(
                                text = if (helpfulCount > 0) "Helpful ($helpfulCount)" else "Helpful",
                                maxLines = 1
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.ThumbUp,
                                contentDescription = "Mark helpful",
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        modifier = Modifier.testTag("map_sheet_helpful_${review.id}")
                    )
                }
                if (isAdmin && onAdminReply != null) {
                    TextButton(
                        onClick = { showAdminReplyDialog = true },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("map_sheet_admin_reply_${review.id}")
                    ) {
                        Icon(Icons.Default.Reply, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (review.adminReply.isBlank()) "Official Reply" else "Edit Reply")
                    }
                }
                if (isOwner) {
                    TextButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("map_sheet_edit_review_${review.id}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit")
                    }
                }
                if (isOwner || isAdmin) {
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("map_sheet_delete_review_${review.id}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (!isOwner && isAdmin) "Moderate" else "Delete")
                    }
                }
            }
        }
    }
}
