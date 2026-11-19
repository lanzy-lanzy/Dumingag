package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.model.Attraction
import com.example.model.CommunityReview
import com.example.ui.theme.MapRatingGold
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun StarRatingBar(
    rating: Int,
    modifier: Modifier = Modifier,
    maxStars: Int = 5,
    starSize: Dp = 18.dp
) {
    val clamped = rating.coerceIn(0, maxStars)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..maxStars) {
            val filled = i <= clamped
            Icon(
                imageVector = if (filled) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = if (i == 1) "$clamped out of $maxStars stars" else null,
                tint = if (filled) MapRatingGold else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(starSize)
            )
        }
    }
}

@Composable
fun FavoriteQuickAccessRow(
    attraction: Attraction,
    language: AppLanguage,
    onOpenAttraction: () -> Unit,
    onRemoveFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onOpenAttraction)
            .testTag("profile_favorite_row_${attraction.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AttractionCoverImage(
                attraction = attraction,
                contentDescription = attraction.name.get(language),
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = attraction.name.get(language),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Brgy. ${attraction.barangay} · ★ ${attraction.baseRating}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onRemoveFavorite,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("profile_remove_favorite_${attraction.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Remove ${attraction.name.get(language)} from favorites",
                    tint = Color(0xFFE11D48),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ReviewCard(
    review: CommunityReview,
    isOwner: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAttractionClick: (() -> Unit)? = null,
    showAttractionHeader: Boolean = true
) {
    val locale = Locale.getDefault()
    val dateFmt = remember(locale) { SimpleDateFormat("MMM d, yyyy", locale) }
    val dateText = review.createdAt?.toDate()?.let { dateFmt.format(it) } ?: "Just now"
    val authorInitial = review.authorName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "T"

    Card(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("review_card_${review.id}")
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = authorInitial,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (onAttractionClick != null && showAttractionHeader) {
                                Modifier.clickable(onClick = onAttractionClick)
                            } else {
                                Modifier
                            }
                        )
                ) {
                    if (showAttractionHeader) {
                        Text(
                            text = review.attractionName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "${review.authorName} · $dateText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating ${review.rating} out of 5",
                            tint = MapRatingGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${review.rating}/5",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StarRatingBar(rating = review.rating.toInt(), starSize = 18.dp)
                if (review.visitTag.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = review.visitTag,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Text(
                text = review.comment,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (isOwner) {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("edit_review_${review.id}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("delete_review_${review.id}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun ComposeReviewDialog(
    language: AppLanguage,
    attractions: List<Attraction>,
    initialReview: CommunityReview?,
    initialAttraction: Attraction? = null,
    onDismiss: () -> Unit,
    onSave: (Attraction, Int, String, String) -> Unit
) {
    var selectedAttraction by remember(attractions, initialReview, initialAttraction) {
        mutableStateOf(
            initialAttraction
                ?: attractions.firstOrNull { it.id == initialReview?.attractionId }
                ?: attractions.first()
        )
    }
    var rating by remember(initialReview) {
        mutableIntStateOf((initialReview?.rating ?: 5L).toInt().coerceIn(1, 5))
    }
    var comment by remember(initialReview) { mutableStateOf(initialReview?.comment ?: "") }
    var visitTag by remember(initialReview) { mutableStateOf(initialReview?.visitTag ?: "Eco-Tour") }
    val tags = listOf("Eco-Tour", "Family", "Solo", "Hiker", "Foodie")
    val trimmedLength = comment.trim().length

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialReview == null) "Write Community Review" else "Edit Review"
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (initialReview == null && initialAttraction == null) {
                    Text("Attraction", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(attractions, key = { it.id }) { attr ->
                            FilterChip(
                                selected = attr.id == selectedAttraction.id,
                                onClick = { selectedAttraction = attr },
                                label = {
                                    Text(
                                        text = attr.name.get(language).split(" ").take(2).joinToString(" "),
                                        maxLines = 1
                                    )
                                }
                            )
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = initialReview?.attractionName ?: selectedAttraction.name.get(language),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                Text("Star Rating ($rating / 5)", style = MaterialTheme.typography.labelMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (s in 1..5) {
                        IconButton(
                            onClick = { rating = s },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("review_star_$s")
                        ) {
                            Icon(
                                imageVector = if (s <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Rate $s stars",
                                tint = if (s <= rating) MapRatingGold else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                Text("Trip Type", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(tags) { tag ->
                        FilterChip(
                            selected = visitTag == tag,
                            onClick = { visitTag = tag },
                            label = { Text(tag) }
                        )
                    }
                }

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it.take(800) },
                    label = { Text("Share tips, trail status, or highlights") },
                    supportingText = {
                        Text(
                            text = if (trimmedLength < 3) {
                                "Minimum 3 characters ($trimmedLength/800)"
                            } else {
                                "$trimmedLength/800 characters"
                            }
                        )
                    },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("review_comment_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedAttraction, rating, comment, visitTag) },
                enabled = trimmedLength >= 3,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("review_publish_button")
            ) {
                Text(
                    text = if (initialReview == null) "Publish" else "Update",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancel", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttractionReviewsSection(
    attractionName: String,
    reviews: List<CommunityReview>,
    currentUserId: String,
    onWriteReview: () -> Unit,
    onEditReview: (CommunityReview) -> Unit,
    onDeleteReview: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.CenterVertically)) {
                Text(
                    text = "Traveler Reviews & Ratings",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                val summary = if (reviews.isEmpty()) {
                    "No community reviews yet"
                } else {
                    val avg = reviews.map { it.rating }.average()
                    "%.1f / 5.0 · %d %s".format(avg, reviews.size, if (reviews.size == 1) "review" else "reviews")
                }
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Button(
                onClick = onWriteReview,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .heightIn(min = 48.dp)
                    .testTag("detail_write_review_button")
            ) {
                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Rate & Review", maxLines = 1)
            }
        }

        if (reviews.isEmpty()) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StarRatingBar(rating = 5, starSize = 22.dp)
                    Text(
                        text = "Share your visit to $attractionName",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Post a 1–5 star rating and helpful tips for fellow travelers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            reviews.forEach { review ->
                ReviewCard(
                    review = review,
                    isOwner = review.userId == currentUserId,
                    onEdit = { onEditReview(review) },
                    onDelete = { onDeleteReview(review.id) },
                    showAttractionHeader = false
                )
            }
        }
    }
}
