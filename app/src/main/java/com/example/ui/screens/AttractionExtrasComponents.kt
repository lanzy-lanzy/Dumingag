package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.TravelCompanionEngines
import com.example.model.AppLanguage
import com.example.model.Attraction
import com.example.model.TrailConditionReport
import com.example.ui.components.LiquidGlassSurface
import com.example.util.CloudinaryImageUploader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TrailConditionTags = listOf(
    "Clear trail",
    "Slippery after rain",
    "Strong waterfall flow",
    "Muddy ascent",
    "Great sunset view"
)

private val CrowdLevels = listOf("Quiet", "Moderate", "Busy")

/**
 * Verified Eco-Passport Check-In + Liquid Glass Souvenir Postcard Generator & Share bar.
 */
@Composable
fun AttractionEcoCheckInAndPostcardRow(
    attraction: Attraction,
    language: AppLanguage,
    isCheckedIn: Boolean,
    userLatLon: Pair<Double, Double>,
    onCheckIn: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSharingPostcard by remember { mutableStateOf(false) }

    val distanceKm = remember(userLatLon, attraction.id) {
        TravelCompanionEngines.haversineKm(
            userLatLon.first,
            userLatLon.second,
            attraction.latitude,
            attraction.longitude
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FilledTonalButton(
            onClick = {
                if (!isCheckedIn) {
                    onCheckIn()
                } else {
                    onShowMessage("Passport stamp already collected for ${attraction.name.get(language)}!")
                }
            },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("detail_eco_checkin_button")
        ) {
            Icon(
                imageVector = if (isCheckedIn) Icons.Default.Verified else Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isCheckedIn) "Stamped" else if (distanceKm <= 1.5) "GPS Check-In" else "Collect Stamp",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        OutlinedButton(
            enabled = !isSharingPostcard,
            onClick = {
                isSharingPostcard = true
                scope.launch {
                    val result = runCatching {
                        val file = withContext(Dispatchers.IO) {
                            renderSouvenirPostcardFile(context, attraction, language)
                        }
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Exploring ${attraction.name.get(language)} in Brgy. ${attraction.barangay}, Dumingag, Zamboanga del Sur!"
                            )
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Dumingag Postcard"))
                    }
                    isSharingPostcard = false
                    result.onFailure {
                        onShowMessage("Could not share souvenir postcard right now")
                    }
                }
            },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .testTag("detail_share_postcard_button")
        ) {
            Icon(
                imageVector = if (isSharingPostcard) Icons.Default.CardGiftcard else Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isSharingPostcard) "Rendering…" else "Share Postcard",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Community Trail Condition & Crowd Reports Card for AttractionDetailScreen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrailConditionReportsCard(
    attractionId: String,
    attractionName: String,
    reports: List<TrailConditionReport>,
    onSubmitReport: (conditionTag: String, crowdLevel: String, note: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val matchingReports = remember(reports, attractionId) {
        reports.filter { it.attractionId == attractionId }.take(4)
    }
    val dateFmt = remember { SimpleDateFormat("MMM d · h:mm a", Locale.getDefault()) }

    LiquidGlassSurface(
        cornerRadius = 22.dp,
        elevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("detail_trail_reports_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Hiking,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Trail Conditions & Crowd Pulse",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Recent field tags from Dumingag travelers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(
                    onClick = { showDialog = true },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("detail_post_trail_report_button")
                ) {
                    Icon(Icons.Default.AddAlert, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Report")
                }
            }

            if (matchingReports.isEmpty()) {
                Text(
                    text = "No recent trail reports yet for $attractionName. Share today's trail condition to help fellow hikers!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                matchingReports.forEachIndexed { idx, rep ->
                    if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = rep.conditionTag,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "Crowd: ${rep.crowdLevel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        if (rep.note.isNotBlank()) {
                            Text(
                                text = rep.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${rep.reporterName} · ${dateFmt.format(Date(rep.reportedAtMillis))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        var chosenTag by remember { mutableStateOf(TrailConditionTags.first()) }
        var chosenCrowd by remember { mutableStateOf(CrowdLevels.first()) }
        var note by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Report Trail Condition") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Trail status", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TrailConditionTags.forEach { tag ->
                            FilterChip(
                                selected = chosenTag == tag,
                                onClick = { chosenTag = tag },
                                label = { Text(tag) }
                            )
                        }
                    }
                    Text("Crowd level", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CrowdLevels.forEach { level ->
                            FilterChip(
                                selected = chosenCrowd == level,
                                onClick = { chosenCrowd = level },
                                label = { Text(level) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Optional field note (e.g., water clarity, path)") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSubmitReport(chosenTag, chosenCrowd, note)
                        showDialog = false
                    }
                ) {
                    Text("Post Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}

/**
 * Generates a high-resolution Liquid Glass Souvenir Postcard PNG (1080x1350) in cacheDir/postcards/
 * stamped with the attraction photo, Dumingag coordinates, eco-badge, and date.
 */
private fun renderSouvenirPostcardFile(
    context: Context,
    attraction: Attraction,
    language: AppLanguage
): File {
    val width = 1080
    val height = 1350
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Deep emerald highland gradient backdrop
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(
                AndroidColor.parseColor("#0B261C"),
                AndroidColor.parseColor("#1B4332"),
                AndroidColor.parseColor("#0F3D3E")
            ),
            null,
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // Attraction cover photo (Cloudinary HTTPS URL, legacy Base64, or bundled drawable)
    val coverBmp: Bitmap? = runCatching {
        when {
            CloudinaryImageUploader.isRemoteImageUrl(attraction.photoData) -> {
                val conn = URL(attraction.photoData.trim()).openConnection().apply {
                    connectTimeout = 8_000
                    readTimeout = 8_000
                }
                conn.getInputStream().use { BitmapFactory.decodeStream(it) }
            }
            attraction.photoData.isNotBlank() -> {
                val clean = attraction.photoData.substringAfter(",")
                val bytes = Base64.decode(clean, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            else -> null
        }
    }.getOrNull() ?: runCatching {
        BitmapFactory.decodeResource(context.resources, attraction.imageRes)
    }.getOrNull()

    val photoRect = RectF(64f, 84f, width - 64f, 860f)
    if (coverBmp != null) {
        val srcAspect = coverBmp.width.toFloat() / coverBmp.height.toFloat().coerceAtLeast(1f)
        val dstAspect = photoRect.width() / photoRect.height()
        val srcRect = if (srcAspect > dstAspect) {
            val cropW = (coverBmp.height * dstAspect).toInt()
            val left = ((coverBmp.width - cropW) / 2).coerceAtLeast(0)
            Rect(left, 0, (left + cropW).coerceAtMost(coverBmp.width), coverBmp.height)
        } else {
            val cropH = (coverBmp.width / dstAspect).toInt()
            val top = ((coverBmp.height - cropH) / 2).coerceAtLeast(0)
            Rect(0, top, coverBmp.width, (top + cropH).coerceAtMost(coverBmp.height))
        }
        canvas.drawBitmap(coverBmp, srcRect, photoRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    }

    // Crisp frosted glass frame around photo
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = AndroidColor.parseColor("#B8F2E6")
    }
    canvas.drawRoundRect(photoRect, 34f, 34f, borderPaint)

    // Bottom Liquid Glass Caption Panel
    val glassPanel = RectF(64f, 900f, width - 64f, height - 74f)
    val glassFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(215, 245, 250, 247)
    }
    canvas.drawRoundRect(glassPanel, 38f, 38f, glassFill)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#102A20")
        textSize = 54f
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    }
    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#2D6A4F")
        textSize = 32f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#475A52")
        textSize = 27f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }

    canvas.drawText("POSTCARD FROM DUMINGAG", 104f, 974f, subPaint)
    val spotTitle = attraction.name.get(language).take(30)
    canvas.drawText(spotTitle, 104f, 1052f, titlePaint)
    canvas.drawText(
        "Brgy. ${attraction.barangay}, Dumingag · Zamboanga del Sur",
        104f,
        1114f,
        metaPaint
    )
    canvas.drawText(
        "GPS: ${"%.5f".format(attraction.latitude)}° N, ${"%.5f".format(attraction.longitude)}° E",
        104f,
        1168f,
        metaPaint
    )
    val dateStr = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())
    canvas.drawText("Eco-Traveler Stamp · $dateStr", 104f, 1226f, subPaint)

    val outDir = File(context.cacheDir, "postcards").apply { mkdirs() }
    val outFile = File(outDir, "dumingag_postcard_${attraction.id}.png")
    FileOutputStream(outFile).use { fos ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 95, fos)
    }
    bitmap.recycle()
    return outFile
}
