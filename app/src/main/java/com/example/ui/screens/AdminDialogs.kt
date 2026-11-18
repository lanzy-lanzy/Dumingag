package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DumingagSeedData
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.Attraction
import com.example.model.AttractionCategory
import com.example.model.AttractionImageMapper
import com.example.model.EmergencyContact
import com.example.ui.components.decodeBase64ImageBitmap
import com.example.ui.components.encodeUriToCompressedBase64
import kotlinx.coroutines.launch

private val AdminAttractionCategories = listOf(
    AttractionCategory.NATURE,
    AttractionCategory.HISTORICAL,
    AttractionCategory.CULTURAL,
    AttractionCategory.LANDMARK,
    AttractionCategory.ORGANIC_FARM
)

private val AdminEmergencyPriorities = listOf(
    "CRITICAL" to "Critical (911)",
    "HIGH" to "High (LGU)",
    "MEDICAL" to "Medical",
    "STANDARD" to "Standard"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminAttractionEditorDialog(
    initial: Attraction?,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (
        mapNodeId: String,
        nameEn: String,
        subtitleEn: String,
        descriptionEn: String,
        historyNoteEn: String,
        barangay: String,
        category: AttractionCategory,
        latitude: Double,
        longitude: Double,
        openHours: String,
        entryFeeEn: String,
        bestTimeEn: String,
        transitHintEn: String,
        baseRating: Double,
        imageKey: String,
        photoData: String?,
        clearPhoto: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var nameEn by remember(initial) { mutableStateOf(initial?.name?.en ?: "") }
    var subtitleEn by remember(initial) { mutableStateOf(initial?.subtitle?.en ?: "") }
    var barangay by remember(initial) { mutableStateOf(initial?.barangay ?: "Poblacion") }
    var category by remember(initial) {
        mutableStateOf(
            initial?.category?.takeIf { it != AttractionCategory.ALL } ?: AttractionCategory.NATURE
        )
    }
    var mapNodeId by remember(initial) { mutableStateOf(initial?.mapNodeId ?: "node_poblacion") }
    var latText by remember(initial) { mutableStateOf(initial?.latitude?.toString() ?: "8.1650") }
    var lonText by remember(initial) { mutableStateOf(initial?.longitude?.toString() ?: "123.3465") }
    var openHours by remember(initial) { mutableStateOf(initial?.openHours ?: "06:00 AM – 05:00 PM") }
    var entryFeeEn by remember(initial) { mutableStateOf(initial?.entryFee?.en ?: "Free entry") }
    var bestTimeEn by remember(initial) { mutableStateOf(initial?.bestTime?.en ?: "Morning 7:00–10:00 AM") }
    var transitHintEn by remember(initial) { mutableStateOf(initial?.transitHint?.en ?: "Tricycle or habal-habal from Poblacion Market") }
    var ratingText by remember(initial) { mutableStateOf(initial?.baseRating?.toString() ?: "4.8") }
    var descriptionEn by remember(initial) { mutableStateOf(initial?.description?.en ?: "") }
    var historyNoteEn by remember(initial) { mutableStateOf(initial?.historyNote?.en ?: "") }
    var imageKey by remember(initial) {
        mutableStateOf(initial?.imageKey?.ifBlank { AttractionImageMapper.defaultKeyForCategory(category) } ?: "waterfall")
    }
    var photoData by remember(initial) { mutableStateOf(initial?.photoData ?: "") }
    var clearPhoto by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val encoded = encodeUriToCompressedBase64(context, uri, maxPx = 220, quality = 70)
                if (encoded != null) {
                    photoData = encoded
                    clearPhoto = false
                    validationError = null
                } else {
                    validationError = "Unable to process image. Choose a smaller photo."
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "Create Local Attraction" else "Edit Local Attraction",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Visual preview + Photo Picker
                val previewBitmap = remember(photoData, clearPhoto) {
                    if (clearPhoto || photoData.isBlank()) null else decodeBase64ImageBitmap(photoData)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(124.dp)
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap,
                            contentDescription = "Selected attraction photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = AttractionImageMapper.resForKey(imageKey)),
                            contentDescription = "Attraction theme preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .heightIn(min = 48.dp)
                            .testTag("admin_pick_attraction_photo")
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload Photo", maxLines = 1)
                    }
                    if (!clearPhoto && photoData.isNotBlank()) {
                        TextButton(
                            onClick = {
                                photoData = ""
                                clearPhoto = true
                            },
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Use Preset", maxLines = 1)
                        }
                    }
                }

                Text("Category", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AdminAttractionCategories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = {
                                category = cat
                                if (initial == null && photoData.isBlank()) {
                                    imageKey = AttractionImageMapper.defaultKeyForCategory(cat)
                                }
                            },
                            label = { Text(AppStrings.categoryLabel(cat, language), maxLines = 1) },
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .heightIn(min = 48.dp)
                        )
                    }
                }

                Text("Preset Artwork Theme", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AttractionImageMapper.presetKeys.forEach { (key, label) ->
                        FilterChip(
                            selected = imageKey == key,
                            onClick = { imageKey = key },
                            label = { Text(label, maxLines = 1) },
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .heightIn(min = 48.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it.take(120) },
                    label = { Text("Attraction Name*") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_attr_name_input")
                )
                OutlinedTextField(
                    value = subtitleEn,
                    onValueChange = { subtitleEn = it.take(160) },
                    label = { Text("Subtitle / Highlights*") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_attr_subtitle_input")
                )
                OutlinedTextField(
                    value = barangay,
                    onValueChange = { barangay = it.take(80) },
                    label = { Text("Barangay*") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_attr_barangay_input")
                )

                Text("Nearest Offline Map Junction", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DumingagSeedData.mapNodes.take(6).forEach { node ->
                        FilterChip(
                            selected = mapNodeId == node.id,
                            onClick = {
                                mapNodeId = node.id
                                if (initial == null) {
                                    latText = node.latitude.toString()
                                    lonText = node.longitude.toString()
                                }
                            },
                            label = { Text(node.name.en.split(" ").take(2).joinToString(" "), maxLines = 1) },
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .heightIn(min = 48.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { latText = it.take(16) },
                        label = { Text("Latitude*") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lonText,
                        onValueChange = { lonText = it.take(16) },
                        label = { Text("Longitude*") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = openHours,
                        onValueChange = { openHours = it.take(100) },
                        label = { Text("Open Hours*") },
                        singleLine = true,
                        modifier = Modifier.weight(1.4f)
                    )
                    OutlinedTextField(
                        value = ratingText,
                        onValueChange = { ratingText = it.take(4) },
                        label = { Text("Rating (1-5)*") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(0.8f)
                    )
                }

                OutlinedTextField(
                    value = entryFeeEn,
                    onValueChange = { entryFeeEn = it.take(120) },
                    label = { Text("Entry Fee*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bestTimeEn,
                    onValueChange = { bestTimeEn = it.take(120) },
                    label = { Text("Best Time to Visit*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = transitHintEn,
                    onValueChange = { transitHintEn = it.take(240) },
                    label = { Text("How to Get There (Transit Tip)*") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = descriptionEn,
                    onValueChange = { descriptionEn = it.take(1200) },
                    label = { Text("Overview Description*") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_attr_description_input")
                )
                OutlinedTextField(
                    value = historyNoteEn,
                    onValueChange = { historyNoteEn = it.take(1000) },
                    label = { Text("Heritage / Conservation Note (optional)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                validationError?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = latText.trim().toDoubleOrNull()
                    val lon = lonText.trim().toDoubleOrNull()
                    val rating = ratingText.trim().toDoubleOrNull()
                    when {
                        nameEn.trim().length < 2 -> validationError = "Enter an attraction name (min 2 chars)."
                        subtitleEn.trim().length < 2 -> validationError = "Enter a subtitle (min 2 chars)."
                        barangay.trim().length < 2 -> validationError = "Enter a barangay name."
                        descriptionEn.trim().length < 5 -> validationError = "Enter a description (min 5 chars)."
                        lat == null || lat !in -90.0..90.0 -> validationError = "Enter a valid latitude (-90 to 90)."
                        lon == null || lon !in -180.0..180.0 -> validationError = "Enter a valid longitude (-180 to 180)."
                        rating == null || rating !in 1.0..5.0 -> validationError = "Rating must be between 1.0 and 5.0."
                        openHours.trim().length < 2 -> validationError = "Enter opening hours."
                        entryFeeEn.trim().isEmpty() -> validationError = "Enter entry fee details."
                        bestTimeEn.trim().isEmpty() -> validationError = "Enter best time to visit."
                        transitHintEn.trim().isEmpty() -> validationError = "Enter local transit directions."
                        else -> {
                            onSave(
                                mapNodeId,
                                nameEn.trim(),
                                subtitleEn.trim(),
                                descriptionEn.trim(),
                                historyNoteEn.trim(),
                                barangay.trim(),
                                category,
                                lat,
                                lon,
                                openHours.trim(),
                                entryFeeEn.trim(),
                                bestTimeEn.trim(),
                                transitHintEn.trim(),
                                rating,
                                imageKey,
                                photoData.takeIf { !clearPhoto && it.isNotBlank() },
                                clearPhoto
                            )
                        }
                    }
                },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("admin_attr_save_button")
            ) {
                Text(if (initial == null) "Publish" else "Save", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancel", maxLines = 1)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminEmergencyContactDialog(
    initial: EmergencyContact?,
    onDismiss: () -> Unit,
    onSave: (
        titleEn: String,
        department: String,
        phoneNumber: String,
        availability: String,
        priorityLevel: String,
        email: String?,
        websiteUrl: String?
    ) -> Unit
) {
    var titleEn by remember(initial) { mutableStateOf(initial?.title?.en ?: "") }
    var department by remember(initial) { mutableStateOf(initial?.department ?: "") }
    var phoneNumber by remember(initial) { mutableStateOf(initial?.phoneNumber ?: "") }
    var availability by remember(initial) { mutableStateOf(initial?.availability ?: "24/7 Dispatch") }
    var priorityLevel by remember(initial) {
        val raw = initial?.priorityLevel ?: "HIGH"
        mutableStateOf(
            if (raw in listOf("CRITICAL", "HIGH", "MEDICAL", "STANDARD")) raw else "STANDARD"
        )
    }
    var email by remember(initial) { mutableStateOf(initial?.email ?: "") }
    var websiteUrl by remember(initial) { mutableStateOf(initial?.websiteUrl ?: "https://dumingag.com") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "Add Emergency Contact" else "Edit Emergency Contact",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Priority Level", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AdminEmergencyPriorities.forEach { (code, label) ->
                        FilterChip(
                            selected = priorityLevel == code,
                            onClick = { priorityLevel = code },
                            label = { Text(label, maxLines = 1) },
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .heightIn(min = 48.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = titleEn,
                    onValueChange = { titleEn = it.take(140) },
                    label = { Text("Office / Hotline Title*") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_contact_title_input")
                )
                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it.take(160) },
                    label = { Text("Department / Agency*") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_contact_department_input")
                )
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it.take(40) },
                    label = { Text("Phone / Hotline Number*") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_contact_phone_input")
                )
                OutlinedTextField(
                    value = availability,
                    onValueChange = { availability = it.take(100) },
                    label = { Text("Availability (e.g., 24/7 Dispatch)*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.take(120) },
                    label = { Text("Official Email (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = websiteUrl,
                    onValueChange = { websiteUrl = it.take(240) },
                    label = { Text("Website Portal URL (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )

                validationError?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        titleEn.trim().length < 2 -> validationError = "Enter a hotline title (min 2 chars)."
                        department.trim().length < 2 -> validationError = "Enter a department or office name."
                        phoneNumber.trim().length < 2 -> validationError = "Enter a valid phone or hotline number."
                        availability.trim().length < 2 -> validationError = "Enter operating hours or availability."
                        else -> {
                            onSave(
                                titleEn.trim(),
                                department.trim(),
                                phoneNumber.trim(),
                                availability.trim(),
                                priorityLevel,
                                email.trim().ifBlank { null },
                                websiteUrl.trim().ifBlank { null }
                            )
                        }
                    }
                },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("admin_contact_save_button")
            ) {
                Text(if (initial == null) "Publish" else "Save", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancel", maxLines = 1)
            }
        }
    )
}

@Composable
fun AdminDeleteConfirmDialog(
    title: String,
    itemLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text("Remove \"$itemLabel\" from the live Dumingag directory? This updates all travelers immediately.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("admin_confirm_delete_button")
            ) {
                Text("Delete", maxLines = 1)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancel", maxLines = 1)
            }
        }
    )
}
