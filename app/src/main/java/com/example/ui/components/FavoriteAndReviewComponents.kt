package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.model.AppLanguage
import com.example.model.Attraction
import com.example.model.CommunityReview
import com.example.ui.theme.MapRatingGold
import com.example.util.CloudinaryImageUploader
import com.example.util.ImageUploadPreset
import com.example.util.PhotoGeoTagHelper
import kotlinx.coroutines.launch
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReviewCard(
    review: CommunityReview,
    isOwner: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAttractionClick: (() -> Unit)? = null,
    onViewOnMap: (() -> Unit)? = null,
    showAttractionHeader: Boolean = true,
    currentUserId: String = "",
    isAdmin: Boolean = false,
    onToggleHelpful: (() -> Unit)? = null,
    onAdminReply: ((String) -> Unit)? = null
) {
    val locale = Locale.getDefault()
    val dateFmt = remember(locale) { SimpleDateFormat("MMM d, yyyy", locale) }
    val dateText = review.createdAt?.toDate()?.let { dateFmt.format(it) } ?: "Just now"
    val authorInitial = review.authorName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "T"
    val isHelpfulByMe = currentUserId.isNotBlank() && review.helpfulUserIds.contains(currentUserId)
    val helpfulCount = review.helpfulUserIds.size
    var showZoomPhoto by remember(review.id) { mutableStateOf(false) }
    var showAdminReplyDialog by remember(review.id) { mutableStateOf(false) }

    if (showZoomPhoto && review.photoData.isNotBlank()) {
        FullScreenPhotoZoomDialog(
            photoData = review.photoData,
            caption = review.attractionName,
            subtitle = "Photo by ${review.authorName} · ${review.rating}★",
            onDismiss = { showZoomPhoto = false }
        )
    }

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
        shape = RoundedCornerShape(22.dp),
        border = liquidGlassBorder(),
        colors = liquidGlassCardColors(),
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

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StarRatingBar(
                    rating = review.rating.toInt(),
                    starSize = 18.dp,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                if (review.verifiedVisit) {
                    Surface(
                        color = Color(0xFF0F766E).copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, Color(0xFF14B8A6).copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Verified Visit",
                                tint = Color(0xFF0D9488),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Verified Visit",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF0D9488),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
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

            if (review.photoData.isNotBlank()) {
                EncodedPhotoThumbnail(
                    photoData = review.photoData,
                    contentDescription = "Traveler photo for ${review.attractionName}",
                    onClick = { showZoomPhoto = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(152.dp)
                        .testTag("review_photo_${review.id}")
                )
            }

            if (review.adminReply.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(11.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
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
                                contentDescription = "Mark review helpful",
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        modifier = Modifier.testTag("review_helpful_${review.id}")
                    )
                }
                if (onViewOnMap != null) {
                    TextButton(
                        onClick = onViewOnMap,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("review_view_on_map_${review.id}")
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View on Map", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (isAdmin && onAdminReply != null) {
                    TextButton(
                        onClick = { showAdminReplyDialog = true },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("admin_reply_review_${review.id}")
                    ) {
                        Icon(Icons.Default.Reply, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (review.adminReply.isBlank()) "Official Reply" else "Edit Reply", maxLines = 1)
                    }
                }
                if (isOwner) {
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
                }
                if (isOwner || isAdmin) {
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("delete_review_${review.id}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (!isOwner && isAdmin) "Moderate" else "Delete", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminOfficialReplyDialog(
    attractionName: String,
    authorName: String,
    initialReply: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var replyText by remember(initialReply) { mutableStateOf(initialReply) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Official Dumingag Tourism Reply") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Responding to $authorName's review of $attractionName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it.take(500) },
                    label = { Text("Official response (leave blank to clear)") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_official_reply_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(replyText.trim()) },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("admin_official_reply_save")
            ) {
                Text("Save Reply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ComposeReviewDialog(
    language: AppLanguage,
    attractions: List<Attraction>,
    initialReview: CommunityReview?,
    initialAttraction: Attraction? = null,
    onDismiss: () -> Unit,
    onSave: (Attraction, Int, String, String, String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
    var photoData by remember(initialReview) { mutableStateOf(initialReview?.photoData ?: "") }
    var isUploadingPhoto by remember { mutableStateOf(false) }
    var photoUploadNote by remember { mutableStateOf<String?>(null) }
    var photoUploadError by remember { mutableStateOf<String?>(null) }
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }

    fun handleReviewPhotoUri(uri: Uri) {
        scope.launch {
            isUploadingPhoto = true
            photoUploadError = null
            photoUploadNote = "Compressing & uploading photo to cloud…"
            try {
                val result = CloudinaryImageUploader.uploadImage(
                    context = context,
                    uri = uri,
                    preset = ImageUploadPreset.REVIEW
                )
                if (result != null) {
                    photoData = result.photoData
                    photoUploadNote = result.statusMessage
                } else {
                    photoUploadNote = null
                    photoUploadError = "Unable to process photo. Please try another image."
                }
            } finally {
                isUploadingPhoto = false
            }
        }
    }

    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            handleReviewPhotoUri(uri)
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCaptureUri
        if (success && uri != null) {
            handleReviewPhotoUri(uri)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val target = PhotoGeoTagHelper.createTempCaptureTarget(context)
            pendingCaptureUri = target.uri
            takePictureLauncher.launch(target.uri)
        }
    }

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

                // Photo Review attachment row
                Text("Add a Photo (Optional)", style = MaterialTheme.typography.labelMedium)
                if (photoData.isNotBlank()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        EncodedPhotoThumbnail(
                            photoData = photoData,
                            contentDescription = "Attached review photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(128.dp)
                        )
                        if (!isUploadingPhoto) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        photoData = ""
                                        photoUploadNote = null
                                        photoUploadError = null
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                if (isUploadingPhoto || !photoUploadNote.isNullOrBlank() || !photoUploadError.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isUploadingPhoto) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        Text(
                            text = photoUploadError ?: photoUploadNote.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (photoUploadError != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val hasCam = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasCam) {
                                val target = PhotoGeoTagHelper.createTempCaptureTarget(context)
                                pendingCaptureUri = target.uri
                                takePictureLauncher.launch(target.uri)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        enabled = !isUploadingPhoto,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .testTag("review_capture_photo_btn")
                    ) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Camera", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            galleryPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isUploadingPhoto,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp)
                            .testTag("review_pick_photo_btn")
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Gallery", maxLines = 1)
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
                onClick = { onSave(selectedAttraction, rating, comment, visitTag, photoData.ifBlank { null }) },
                enabled = trimmedLength >= 3 && !isUploadingPhoto,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("review_publish_button")
            ) {
                Text(
                    text = when {
                        isUploadingPhoto -> "Uploading…"
                        initialReview == null -> "Publish"
                        else -> "Update"
                    },
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
    isAdmin: Boolean = false,
    onWriteReview: () -> Unit,
    onEditReview: (CommunityReview) -> Unit,
    onDeleteReview: (String) -> Unit,
    onToggleHelpful: ((CommunityReview) -> Unit)? = null,
    onAdminReply: ((String, String) -> Unit)? = null,
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
                shape = RoundedCornerShape(22.dp),
                colors = liquidGlassCardColors(),
                border = liquidGlassBorder()
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
                        text = "Post a 1–5 star rating, photo, and helpful tips for fellow travelers.",
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
                    currentUserId = currentUserId,
                    isAdmin = isAdmin,
                    onEdit = { onEditReview(review) },
                    onDelete = { onDeleteReview(review.id) },
                    onToggleHelpful = onToggleHelpful?.let { cb -> { cb(review) } },
                    onAdminReply = onAdminReply?.let { cb -> { text -> cb(review.id, text) } },
                    showAttractionHeader = false
                )
            }
        }
    }
}
