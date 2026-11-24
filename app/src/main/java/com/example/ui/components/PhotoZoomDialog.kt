package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.util.CloudinaryImageUploader
import java.util.Locale

/**
 * Renders either a Cloudinary HTTPS photo URL (with Coil disk/memory caching) or a legacy Base64
 * photoData string as a tappable thumbnail with subtle zoom indicator.
 */
@Composable
fun EncodedPhotoThumbnail(
    photoData: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    if (photoData.isBlank()) return
    val isRemote = remember(photoData) { CloudinaryImageUploader.isRemoteImageUrl(photoData) }
    val bitmap = remember(photoData, isRemote) {
        if (isRemote) null else decodeBase64ImageBitmap(photoData)
    }
    if (!isRemote && bitmap == null) return

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        if (isRemote) {
            val context = LocalContext.current
            val request = remember(photoData) {
                ImageRequest.Builder(context)
                    .data(photoData.trim())
                    .crossfade(true)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build()
            }
            SubcomposeAsyncImage(
                model = request,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    }
                },
                error = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BrokenImage,
                            contentDescription = contentDescription,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        } else if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (onClick != null) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ZoomIn,
                    contentDescription = "Zoom photo",
                    tint = Color.White,
                    modifier = Modifier
                        .padding(4.dp)
                        .size(14.dp)
                )
            }
        }
    }
}

/**
 * Full-screen pinch-to-zoom viewer for traveler review photos and attraction gallery photos.
 * Supports both Cloudinary HTTPS URLs (cached via Coil) and legacy Base64 photos.
 */
@Composable
fun FullScreenPhotoZoomDialog(
    photoData: String,
    caption: String = "",
    subtitle: String = "",
    onDismiss: () -> Unit
) {
    val isRemote = remember(photoData) { CloudinaryImageUploader.isRemoteImageUrl(photoData) }
    val bitmap = remember(photoData, isRemote) {
        if (isRemote) null else decodeBase64ImageBitmap(photoData)
    }
    var scale by remember(photoData) { mutableFloatStateOf(1f) }
    var offset by remember(photoData) { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .pointerInput(photoData) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, 4f)
                        scale = newScale
                        offset = if (newScale > 1f) {
                            Offset(
                                x = (offset.x + pan.x).coerceIn(-600f, 600f),
                                y = (offset.y + pan.y).coerceIn(-600f, 600f)
                            )
                        } else {
                            Offset.Zero
                        }
                    }
                }
                .testTag("photo_zoom_dialog")
        ) {
            val zoomModifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )

            if (isRemote) {
                val context = LocalContext.current
                val request = remember(photoData) {
                    ImageRequest.Builder(context)
                        .data(photoData.trim())
                        .crossfade(true)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .build()
                }
                SubcomposeAsyncImage(
                    model = request,
                    contentDescription = caption.ifBlank { "Zoomed photo" },
                    contentScale = ContentScale.Fit,
                    modifier = zoomModifier,
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.BrokenImage,
                                    contentDescription = "Unable to load photo",
                                    tint = Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "Unable to load photo right now",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                )
            } else if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = caption.ifBlank { "Zoomed photo" },
                    contentScale = ContentScale.Fit,
                    modifier = zoomModifier
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (caption.isNotBlank()) {
                        Text(
                            text = caption,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.78f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.16f)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("photo_zoom_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close photo viewer",
                            tint = Color.White
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 26.dp)
            ) {
                Text(
                    text = "Pinch to zoom · ${String.format(Locale.US, "%.1fx", scale)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}
