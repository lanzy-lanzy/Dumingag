package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.Attraction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.min

/**
 * Official 4-color Google "G" mark drawn cleanly in vector Canvas inside a white circular badge.
 */
@Composable
fun GoogleBrandIcon(
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        shape = CircleShape,
        modifier = modifier.size(26.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            val strokeWidth = size.minDimension * 0.22f
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)

            // Red upper arc
            drawArc(
                color = Color(0xFFEA4335),
                startAngle = 212f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            // Yellow left arc
            drawArc(
                color = Color(0xFFFBBC05),
                startAngle = 142f,
                sweepAngle = 72f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            // Green bottom arc
            drawArc(
                color = Color(0xFF34A853),
                startAngle = 42f,
                sweepAngle = 102f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            // Blue right arc
            drawArc(
                color = Color(0xFF4285F4),
                startAngle = -5f,
                sweepAngle = 50f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            // Blue horizontal crossbar of "G"
            val midY = size.height / 2f
            drawLine(
                color = Color(0xFF4285F4),
                start = Offset(size.width * 0.50f, midY),
                end = Offset(size.width - inset * 0.35f, midY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Butt
            )
        }
    }
}

fun decodeBase64ImageBitmap(rawBase64: String): ImageBitmap? {
    if (rawBase64.isBlank()) return null
    return try {
        val clean = rawBase64.substringAfter("base64,", rawBase64).trim()
        val bytes = Base64.decode(clean, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}

suspend fun encodeUriToCompressedBase64(
    context: Context,
    uri: Uri,
    maxPx: Int = 160,
    quality: Int = 72
): String? = withContext(Dispatchers.IO) {
    try {
        val resolver = context.contentResolver
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        val origW = boundsOpts.outWidth
        val origH = boundsOpts.outHeight
        if (origW <= 0 || origH <= 0) return@withContext null

        var sampleSize = 1
        while ((origW / sampleSize) > maxPx * 2 || (origH / sampleSize) > maxPx * 2) {
            sampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        } ?: return@withContext null

        val side = min(decoded.width, decoded.height)
        val xOffset = (decoded.width - side) / 2
        val yOffset = (decoded.height - side) / 2
        val square = Bitmap.createBitmap(decoded, xOffset, yOffset, side, side)
        val scaled = if (side != maxPx) {
            Bitmap.createScaledBitmap(square, maxPx, maxPx, true)
        } else {
            square
        }

        var currentQuality = quality
        var encoded: String? = null
        while (currentQuality >= 35) {
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, currentQuality, out)
            val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            if (b64.length <= 45000) {
                encoded = b64
                break
            }
            currentQuality -= 12
        }

        if (scaled != square) scaled.recycle()
        if (square != decoded) square.recycle()
        decoded.recycle()
        encoded
    } catch (_: Exception) {
        null
    }
}

@Composable
fun UserAvatar(
    displayName: String,
    photoData: String,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val decodedBitmap = remember(photoData) { decodeBase64ImageBitmap(photoData) }
    val initial = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "D"

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        modifier = modifier.size(size)
    ) {
        if (decodedBitmap != null) {
            Image(
                bitmap = decodedBitmap,
                contentDescription = "$displayName profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = initial,
                    style = if (size >= 56.dp) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AttractionCoverImage(
    attraction: Attraction,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val customBitmap = remember(attraction.photoData) {
        decodeBase64ImageBitmap(attraction.photoData)
    }
    if (customBitmap != null) {
        Image(
            bitmap = customBitmap,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(id = attraction.imageRes),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditProfileDialog(
    initialName: String,
    initialPhotoData: String,
    email: String,
    isAdmin: Boolean,
    favoriteAttractions: List<Attraction> = emptyList(),
    language: com.example.model.AppLanguage = com.example.model.AppLanguage.EN,
    onOpenAttraction: (Attraction) -> Unit = {},
    onRemoveFavorite: (Attraction) -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (name: String, photoData: String?, clearPhoto: Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember(initialName) { mutableStateOf(initialName) }
    var photoBase64 by remember(initialPhotoData) { mutableStateOf(initialPhotoData) }
    var clearPhoto by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val encoded = encodeUriToCompressedBase64(context, uri, maxPx = 160, quality = 74)
                if (encoded != null) {
                    photoBase64 = encoded
                    clearPhoto = false
                    validationError = null
                } else {
                    validationError = "Could not process selected photo. Try another image."
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Traveler Profile & Favorites",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                UserAvatar(
                    displayName = name.ifBlank { initialName },
                    photoData = if (clearPhoto) "" else photoBase64,
                    size = 82.dp,
                    modifier = Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .heightIn(min = 48.dp)
                            .testTag("pick_profile_photo_button")
                    ) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Choose DP", maxLines = 1)
                    }

                    if (!clearPhoto && photoBase64.isNotBlank()) {
                        TextButton(
                            onClick = {
                                photoBase64 = ""
                                clearPhoto = true
                            },
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .heightIn(min = 48.dp)
                                .testTag("remove_profile_photo_button")
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove", maxLines = 1)
                        }
                    }
                }

                if (email.isNotBlank()) {
                    Surface(
                        color = if (isAdmin) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = if (isAdmin) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isAdmin) "Verified Admin Account" else "Verified Traveler",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(80) },
                    label = { Text("Display Name*") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_profile_name_input")
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Saved Favorites (${favoriteAttractions.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    if (favoriteAttractions.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Tap the heart icon on any Dumingag spot to bookmark it here for quick access.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        favoriteAttractions.forEach { attraction ->
                            FavoriteQuickAccessRow(
                                attraction = attraction,
                                language = language,
                                onOpenAttraction = { onOpenAttraction(attraction) },
                                onRemoveFavorite = { onRemoveFavorite(attraction) }
                            )
                        }
                    }
                }

                validationError?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) {
                        validationError = "Please enter your display name."
                    } else {
                        onSave(
                            trimmed,
                            photoBase64.takeIf { !clearPhoto && it.isNotBlank() },
                            clearPhoto
                        )
                    }
                },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("save_profile_button")
            ) {
                Text("Save Profile", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("cancel_profile_button")
            ) {
                Text("Cancel", maxLines = 1)
            }
        }
    )
}
