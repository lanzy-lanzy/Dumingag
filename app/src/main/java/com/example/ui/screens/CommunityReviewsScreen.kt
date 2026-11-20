package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.Attraction
import com.example.model.CommunityReview
import com.example.model.UiState
import com.example.ui.components.AttractionCoverImage
import com.example.ui.components.ComposeReviewDialog
import com.example.ui.components.ReviewCard
import com.example.ui.theme.MapRatingGold

@Composable
fun CommunityReviewsScreen(
    language: AppLanguage,
    currentUserId: String,
    attractions: List<Attraction>,
    favoriteAttractions: List<Attraction> = emptyList(),
    reviewsState: UiState<List<CommunityReview>>,
    onOpenAttraction: (Attraction) -> Unit = {},
    onToggleFavorite: (Attraction) -> Unit = {},
    onSubmitReview: (Attraction, Int, String, String) -> Unit,
    onUpdateReview: (String, Int, String, String) -> Unit,
    onDeleteReview: (String) -> Unit
) {
    var filterAttractionId by remember { mutableStateOf<String?>(null) }
    var myReviewsOnly by remember { mutableStateOf(false) }
    var showComposeDialog by remember { mutableStateOf(false) }
    var editingReview by remember { mutableStateOf<CommunityReview?>(null) }

    TravelPageGrid("reviews_page") {
        pageSection {
            val reviews = (reviewsState as? UiState.Success)?.data
            val summary = reviews?.let {
                when (it.size) {
                    0 -> null
                    1 -> "1 review · ${it.first().rating}/5"
                    else -> "%d reviews · %.1f/5".format(it.size, it.map { review -> review.rating }.average())
                }
            }
            TravelPageHeader(AppStrings.communityReviewsTitle(language), summary, R.drawable.img_organic_village) {
                TravelHeaderAction("Write a review", Icons.Default.AddComment, "write_review_button", enabled = attractions.isNotEmpty()) {
                    showComposeDialog = true
                }
            }
        }

        // Dedicated Favorite Attractions quick-access card
        pageSection {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFE11D48),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "My Favorite Attractions (${favoriteAttractions.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (favoriteAttractions.isEmpty()) {
                        Text(
                            text = "Bookmark attractions with the heart icon on Explore or Map for quick access here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(favoriteAttractions, key = { it.id }) { fav ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                    modifier = Modifier
                                        .width(216.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { onOpenAttraction(fav) }
                                        .testTag("reviews_profile_favorite_${fav.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AttractionCoverImage(
                                            attraction = fav,
                                            contentDescription = fav.name.get(language),
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = fav.name.get(language),
                                                style = MaterialTheme.typography.labelLarge,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Brgy. ${fav.barangay}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(
                                            onClick = { onToggleFavorite(fav) },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = "Remove favorite",
                                                tint = Color(0xFFE11D48),
                                                modifier = Modifier.size(18.dp)
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

        pageSection {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filterAttractionId == null && !myReviewsOnly,
                        onClick = {
                            filterAttractionId = null
                            myReviewsOnly = false
                        },
                        label = { Text("All Spots") }
                    )
                }
                item {
                    FilterChip(
                        selected = myReviewsOnly,
                        onClick = { myReviewsOnly = !myReviewsOnly },
                        label = { Text("My Reviews") },
                        modifier = Modifier.testTag("filter_my_reviews_chip")
                    )
                }
                items(attractions, key = { it.id }) { attraction ->
                    FilterChip(
                        selected = filterAttractionId == attraction.id,
                        onClick = {
                            filterAttractionId = if (filterAttractionId == attraction.id) null else attraction.id
                        },
                        label = { Text(attraction.name.get(language)) }
                    )
                }
            }
        }

        when (reviewsState) {
            is UiState.Loading -> {
                pageSection {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                pageSection {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(
                            text = reviewsState.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            is UiState.Success -> {
                val filtered = reviewsState.data.filter { review ->
                    val matchesAttraction = filterAttractionId == null || review.attractionId == filterAttractionId
                    val matchesMine = !myReviewsOnly || review.userId == currentUserId
                    matchesAttraction && matchesMine
                }
                if (filtered.isEmpty()) {
                    pageSection {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = MapRatingGold, modifier = Modifier.size(34.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (myReviewsOnly) "You haven't posted a review yet" else "Be the first to share a tip!",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Post trail conditions, habal-habal tips, or highlights from Dumingag.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filtered, key = { it.id }) { review ->
                        val linkedAttraction = attractions.firstOrNull { it.id == review.attractionId }
                        ReviewCard(
                            review = review,
                            isOwner = review.userId == currentUserId,
                            onEdit = { editingReview = review },
                            onDelete = { onDeleteReview(review.id) },
                            onAttractionClick = linkedAttraction?.let { target -> { onOpenAttraction(target) } }
                        )
                    }
                }
            }
        }
    }

    if (showComposeDialog && attractions.isNotEmpty()) {
        ComposeReviewDialog(
            language = language,
            attractions = attractions,
            initialReview = null,
            onDismiss = { showComposeDialog = false },
            onSave = { attraction, rating, comment, tag ->
                onSubmitReview(attraction, rating, comment, tag)
                showComposeDialog = false
            }
        )
    }

    editingReview?.let { rev ->
        if (attractions.isNotEmpty()) {
            ComposeReviewDialog(
                language = language,
                attractions = attractions,
                initialReview = rev,
                onDismiss = { editingReview = null },
                onSave = { _, rating, comment, tag ->
                    onUpdateReview(rev.id, rating, comment, tag)
                    editingReview = null
                }
            )
        }
    }
}
