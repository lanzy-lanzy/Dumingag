package com.example.ui.screens

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
import androidx.compose.foundation.lazy.grid.items
import com.example.R
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.Attraction
import com.example.model.CommunityReview
import com.example.model.UiState
import com.example.ui.components.UserAvatar
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CommunityReviewsScreen(
    language: AppLanguage,
    currentUserId: String,
    userDisplayName: String,
    userEmail: String,
    userPhotoData: String,
    isAdmin: Boolean,
    attractions: List<Attraction>,
    reviewsState: UiState<List<CommunityReview>>,
    onOpenLanguagePicker: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenAdminConsole: () -> Unit,
    onSignOut: () -> Unit,
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
        // Traveler Profile, DP Editor, Admin Console & Sign-Out Card
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
                            TextButton(onClick = verify) { Text("Verify email") }
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
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(34.dp))
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
                        ReviewCard(
                            review = review,
                            isOwner = review.userId == currentUserId,
                            onEdit = { editingReview = review },
                            onDelete = { onDeleteReview(review.id) }
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

@Composable
private fun ReviewCard(
    review: CommunityReview,
    isOwner: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val locale = Locale.getDefault()
    val dateFmt = remember(locale) { SimpleDateFormat("MMM d, yyyy", locale) }
    val dateText = review.createdAt?.toDate()?.let { dateFmt.format(it) } ?: "Just now"

    Card(
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(review.attractionName, style = MaterialTheme.typography.titleLarge)
                    Text("${review.authorName} · $dateText", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(10.dp)) {
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = "Rating out of five", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${review.rating}/5", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            if (review.visitTag.isNotBlank()) {
                Text(review.visitTag, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Text(review.comment, style = MaterialTheme.typography.bodyMedium)

            if (isOwner) {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(
                        onClick = onEdit,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.heightIn(min = 48.dp)
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
private fun ComposeReviewDialog(
    language: AppLanguage,
    attractions: List<Attraction>,
    initialReview: CommunityReview?,
    onDismiss: () -> Unit,
    onSave: (Attraction, Int, String, String) -> Unit
) {
    var selectedAttraction by remember(attractions, initialReview) {
        mutableStateOf(
            attractions.firstOrNull { it.id == initialReview?.attractionId } ?: attractions.first()
        )
    }
    var rating by remember { mutableIntStateOf((initialReview?.rating ?: 5L).toInt().coerceIn(1, 5)) }
    var comment by remember { mutableStateOf(initialReview?.comment ?: "") }
    var visitTag by remember { mutableStateOf(initialReview?.visitTag ?: "Eco-Tour") }
    val tags = listOf("Eco-Tour", "Family", "Solo", "Hiker", "Foodie")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialReview == null) "Write Community Review" else "Edit Review") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (initialReview == null) {
                    Text("Attraction", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(attractions) { attr ->
                            FilterChip(
                                selected = attr.id == selectedAttraction.id,
                                onClick = { selectedAttraction = attr },
                                label = { Text(attr.name.get(language).split(" ").take(2).joinToString(" ")) }
                            )
                        }
                    }
                }

                Text("Rating", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (s in 1..5) {
                        Icon(
                            imageVector = if (s <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Rate $s stars",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { rating = s }
                        )
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
                    label = { Text("Share tips, trail status, or highlights (min 3 chars)") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedAttraction, rating, comment, visitTag) },
                enabled = comment.trim().length >= 3,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Publish", maxLines = 1, overflow = TextOverflow.Ellipsis)
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
