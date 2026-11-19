package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.components.UserAvatar
import com.example.ui.theme.MapRatingGold

@Composable
fun CommunityReviewsScreen(
    language: AppLanguage,
    currentUserId: String,
    userDisplayName: String,
    userEmail: String,
    userPhotoData: String,
    isAdmin: Boolean,
    attractions: List<Attraction>,
    favoriteAttractions: List<Attraction> = emptyList(),
    reviewsState: UiState<List<CommunityReview>>,
    onOpenLanguagePicker: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenAdminConsole: () -> Unit,
    onSignOut: () -> Unit,
    onOpenAttraction: (Attraction) -> Unit = {},
    onToggleFavorite: (Attraction) -> Unit = {},
    onSubmitReview: (Attraction, Int, String, String) -> Unit,
    onUpdateReview: (String, Int, String, String) -> Unit,
    onDeleteReview: (String) -> Unit,
    onVerifyEmail: (() -> Unit)? = null
) {
    var filterAttractionId by remember { mutableStateOf<String?>(null) }
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

        // Traveler Profile, Saved Favorites Quick Access, DP Editor, Admin Console & Sign-Out Card
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
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            displayName = userDisplayName,
                            photoData = userPhotoData,
                            size = 52.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(
                                verticalArrangement = Arrangement.Center,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = userDisplayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isAdmin) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = "ADMIN",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            if (userEmail.isNotBlank()) {
                                Text(
                                    text = userEmail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "Language: ${language.nativeLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onEditProfile,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .heightIn(min = 48.dp)
                                .testTag("edit_profile_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit profile", maxLines = 1)
                        }

                        onVerifyEmail?.let { verify ->
                            TextButton(
                                onClick = verify,
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) { Text("Verify email") }
                        }
                        if (isAdmin) {
                            Button(
                                onClick = onOpenAdminConsole,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .heightIn(min = 48.dp)
                                    .testTag("open_admin_console_button")
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Admin Console", maxLines = 1)
                            }
                        }
                        IconButton(
                            onClick = onOpenLanguagePicker,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Default.Translate, contentDescription = "Change language")
                        }
                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // My Favorites Quick Access in Profile Card
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(17.dp)
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
                                            .width(210.dp)
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
        }

        pageSection {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filterAttractionId == null,
                        onClick = { filterAttractionId = null },
                        label = { Text("All Spots") }
                    )
                }
                items(attractions, key = { it.id }) { attraction ->
                    FilterChip(
                        selected = filterAttractionId == attraction.id,
                        onClick = { filterAttractionId = attraction.id },
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
                val filtered = reviewsState.data.filter {
                    filterAttractionId == null || it.attractionId == filterAttractionId
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
                                Text("Be the first to share a tip!", style = MaterialTheme.typography.titleMedium)
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
