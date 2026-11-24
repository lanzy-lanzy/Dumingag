package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DumingagPhrasebook
import com.example.data.DumingagSeedData
import com.example.data.TravelCompanionEngines
import com.example.model.AppLanguage
import com.example.model.Attraction
import com.example.model.AttractionCategory
import com.example.model.EmergencyContact
import com.example.ui.components.LiquidGlassSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * One-Tap SOS / Emergency Location SMS Card.
 * Opens the device's default SMS composer via ACTION_SENDTO (zero SMS permission required)
 * pre-filled with exact GPS coordinates and nearest Dumingag barangay/landmark.
 */
@Composable
fun EmergencySosSmsCard(
    userLatLon: Pair<Double, Double>,
    attractions: List<Attraction>,
    emergencyContacts: List<EmergencyContact>,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val primaryHotline = remember(emergencyContacts) {
        emergencyContacts.firstOrNull { it.priorityLevel.equals("CRITICAL", ignoreCase = true) }
            ?: emergencyContacts.firstOrNull()
    }
    val nearestSpot = remember(userLatLon, attractions) {
        attractions.ifEmpty { DumingagSeedData.attractions }.minByOrNull { spot ->
            TravelCompanionEngines.haversineKm(
                userLatLon.first,
                userLatLon.second,
                spot.latitude,
                spot.longitude
            )
        }
    }
    val nearestDistKm = remember(userLatLon, nearestSpot) {
        nearestSpot?.let {
            TravelCompanionEngines.haversineKm(userLatLon.first, userLatLon.second, it.latitude, it.longitude)
        } ?: 0.0
    }

    LiquidGlassSurface(
        cornerRadius = 22.dp,
        elevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("transit_sos_sms_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(9.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "One-Tap Emergency Location SMS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Works offline over basic cellular signal — pre-fills your GPS pin & nearest barangay",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "GPS: ${"%.5f".format(userLatLon.first)}° N, ${"%.5f".format(userLatLon.second)}° E",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    nearestSpot?.let { spot ->
                        Text(
                            text = "Nearest: ${spot.name.en} (Brgy. ${spot.barangay}, ${"%.1f".format(nearestDistKm)} km)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val sanitizedPhone = primaryHotline?.phoneNumber
                        ?.replace(" ", "")
                        ?.replace("(", "")
                        ?.replace(")", "")
                        ?.replace("-", "")
                        .orEmpty()
                    val timeLabel = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date())
                    val smsBody = buildString {
                        append("SOS / EMERGENCY ASSISTANCE NEEDED in Dumingag, Zamboanga del Sur.\n")
                        append("GPS Coordinates: ${"%.5f".format(userLatLon.first)}, ${"%.5f".format(userLatLon.second)}\n")
                        if (nearestSpot != null) {
                            append("Nearest Landmark: ${nearestSpot.name.en} (Brgy. ${nearestSpot.barangay}, ~${"%.1f".format(nearestDistKm)} km away)\n")
                        }
                        append("Time: $timeLabel\n")
                        append("Map link: https://maps.google.com/?q=${"%.5f".format(userLatLon.first)},${"%.5f".format(userLatLon.second)}")
                    }
                    try {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("smsto:$sanitizedPhone")
                            putExtra("sms_body", smsBody)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        onShowMessage("No SMS app available on device")
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("transit_sos_sms_button")
            ) {
                Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (primaryHotline != null) {
                        "Draft SOS SMS to ${primaryHotline.title.en.take(22)}"
                    } else {
                        "Draft Emergency Location SMS"
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Interactive Habal-Habal, Tricycle & Van Fare Estimator for Dumingag point-to-point routes.
 */
@Composable
fun HabalHabalFareEstimatorCard(
    language: AppLanguage,
    attractions: List<Attraction>,
    userLatLon: Pair<Double, Double>,
    modifier: Modifier = Modifier
) {
    val spots = remember(attractions) { attractions.ifEmpty { DumingagSeedData.attractions } }
    var originAttraction by remember { mutableStateOf<Attraction?>(null) } // null = Current GPS / Poblacion
    var destinationAttraction by remember(spots) { mutableStateOf(spots.firstOrNull()) }
    var pickerTarget by remember { mutableStateOf<String?>(null) } // "origin" or "dest"

    val originLatLon = originAttraction?.let { it.latitude to it.longitude } ?: userLatLon
    val originLabel = originAttraction?.name?.get(language) ?: "Current Location / Poblacion"
    val destLatLon = destinationAttraction?.let { it.latitude to it.longitude }
        ?: (DumingagSeedData.CENTER_LAT + 0.02 to DumingagSeedData.CENTER_LON - 0.02)
    val destLabel = destinationAttraction?.name?.get(language) ?: "Select Destination"

    val straightDistKm = remember(originLatLon, destLatLon) {
        TravelCompanionEngines.haversineKm(
            originLatLon.first,
            originLatLon.second,
            destLatLon.first,
            destLatLon.second
        )
    }
    val isMountain = destinationAttraction?.category == AttractionCategory.NATURE ||
        originAttraction?.category == AttractionCategory.NATURE ||
        straightDistKm > 5.0
    val estimates = remember(straightDistKm, isMountain) {
        TravelCompanionEngines.estimateDumingagFares(straightDistKm, isMountain)
    }

    LiquidGlassSurface(
        cornerRadius = 22.dp,
        elevation = 5.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("transit_fare_estimator_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Habal-Habal & Transit Fare Estimator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Estimated ~${"%.1f".format(straightDistKm * 1.22)} km road route · ${if (isMountain) "Highland terrain" else "Town corridor"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { pickerTarget = "origin" },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("fare_pick_origin_button")
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(originLabel, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = { pickerTarget = "dest" },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("fare_pick_dest_button")
                ) {
                    Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(destLabel, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
                }
            }

            estimates.forEach { option ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.64f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TwoWheeler,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.modeName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = option.suitabilityNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = option.fareRangePhp,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = option.durationText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }

    if (pickerTarget != null) {
        val pickingOrigin = pickerTarget == "origin"
        AlertDialog(
            onDismissRequest = { pickerTarget = null },
            title = { Text(if (pickingOrigin) "Select Starting Point" else "Select Destination") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (pickingOrigin) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    originAttraction = null
                                    pickerTarget = null
                                }
                        ) {
                            Text(
                                text = "Current GPS Location / Poblacion",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    spots.forEach { spot ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (pickingOrigin) originAttraction = spot else destinationAttraction = spot
                                    pickerTarget = null
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(spot.name.get(language), style = MaterialTheme.typography.titleSmall)
                                Text("Brgy. ${spot.barangay}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pickerTarget = null }) { Text("Close") }
            }
        )
    }
}

/**
 * Offline Cebuano / Tagalog / Subanen Phrasebook with real Android TextToSpeech playback.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OfflinePhrasebookCard(
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(DumingagPhrasebook.categories.first()) }
    var ttsReady by remember { mutableStateOf(false) }

    val tts = remember(context) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val filLocale = Locale("fil", "PH")
                val res = engine?.setLanguage(filLocale)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine?.setLanguage(Locale.US)
                }
                engine?.setSpeechRate(0.9f)
                ttsReady = true
            }
        }
        engine
    }

    DisposableEffect(tts) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    val filteredPhrases = remember(selectedCategory) {
        DumingagPhrasebook.phrases.filter { it.category == selectedCategory }
    }

    LiquidGlassSurface(
        cornerRadius = 22.dp,
        elevation = 5.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("transit_phrasebook_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Offline Cebuano · Tagalog · Subanen Phrasebook",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap the speaker button for instant offline voice pronunciation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DumingagPhrasebook.categories) { cat ->
                    FilterChip(
                        selected = cat == selectedCategory,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("phrasebook_cat_${cat.lowercase().replace(" ", "_")}")
                    )
                }
            }

            filteredPhrases.forEachIndexed { idx, entry ->
                if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = entry.english,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Ceb: ${entry.cebuano}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tag: ${entry.tagalog}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Subanen: ${entry.subanen}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    IconButton(
                        onClick = {
                            if (ttsReady) {
                                tts.speak(entry.cebuano, TextToSpeech.QUEUE_FLUSH, null, entry.id)
                            } else {
                                onShowMessage("Voice synthesizer is warming up — try tapping again")
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("phrasebook_speak_${entry.id}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Speak ${entry.cebuano}",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
