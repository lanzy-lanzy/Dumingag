package com.example.ui.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DumingagSeedData
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.Attraction
import com.example.model.AttractionImageMapper
import com.example.model.CategoryCatalog
import com.example.model.EmergencyContact
import com.example.model.ManagedCategory
import com.example.model.TourismEvent
import com.example.model.TourismEventSeverity
import com.example.model.TourismEventType
import com.example.ui.components.TravelPhotoImage
import com.example.ui.components.categoryIconForKey
import com.example.util.CapturedPhotoTarget
import com.example.util.CloudinaryImageUploader
import com.example.util.GeoTagSource
import com.example.util.ImageUploadPreset
import com.example.util.PhotoGeoTagHelper
import com.example.util.PhotoGeoTagResult
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

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
    managedCategories: List<ManagedCategory> = CategoryCatalog.defaultCategories,
    existingAttractions: List<Attraction> = DumingagSeedData.attractions,
    fallbackLatLon: Pair<Double, Double> = DumingagSeedData.CENTER_LAT to DumingagSeedData.CENTER_LON,
    onQuickCreateCategory: (code: String, name: String, iconKey: String, sortOrder: Long) -> Unit = { _, _, _, _ -> },
    onDismiss: () -> Unit,
    onSave: (
        mapNodeId: String,
        nameEn: String,
        subtitleEn: String,
        descriptionEn: String,
        historyNoteEn: String,
        barangay: String,
        categoryCode: String,
        latitude: Double,
        longitude: Double,
        openHours: String,
        entryFeeEn: String,
        bestTimeEn: String,
        transitHintEn: String,
        baseRating: Double,
        imageKey: String,
        photoData: String?,
        galleryPhotos: List<String>,
        clearPhoto: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activeCategories = managedCategories.ifEmpty { CategoryCatalog.defaultCategories }

    var nameEn by remember(initial) { mutableStateOf(initial?.name?.en ?: "") }
    var subtitleEn by remember(initial) { mutableStateOf(initial?.subtitle?.en ?: "") }
    var barangay by remember(initial) { mutableStateOf(initial?.barangay ?: "Poblacion") }
    var selectedCategoryCode by remember(initial) {
        mutableStateOf(
            initial?.categoryCode?.ifBlank { initial.category.name }
                ?: activeCategories.firstOrNull()?.effectiveCode
                ?: "NATURE"
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
        mutableStateOf(
            initial?.imageKey?.ifBlank { CategoryCatalog.defaultImageKeyForCode(selectedCategoryCode) }
                ?: CategoryCatalog.defaultImageKeyForCode(selectedCategoryCode)
        )
    }
    var photoData by remember(initial) { mutableStateOf(initial?.photoData ?: "") }
    var galleryPhotos by remember(initial) { mutableStateOf(initial?.galleryPhotos.orEmpty().take(4)) }
    var clearPhoto by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Camera capture + auto geo-tag + Cloudinary upload state
    var pendingCaptureTarget by remember { mutableStateOf<CapturedPhotoTarget?>(null) }
    var pendingGeoTagRequest by remember { mutableStateOf<Pair<Uri?, File?>?>(null) }
    var isGeoTagging by remember { mutableStateOf(false) }
    var isUploadingPhoto by remember { mutableStateOf(false) }
    var uploadStatusNote by remember { mutableStateOf<String?>(null) }
    var geoTagResult by remember { mutableStateOf<PhotoGeoTagResult?>(null) }

    // Quick inline category creator state
    var showQuickCategoryRow by remember { mutableStateOf(false) }
    var quickCategoryName by remember { mutableStateOf("") }

    fun applyGeoTagResult(result: PhotoGeoTagResult) {
        geoTagResult = result
        latText = String.format(Locale.US, "%.5f", result.latitude)
        lonText = String.format(Locale.US, "%.5f", result.longitude)
        mapNodeId = result.nearestNodeId
        if (initial == null && !result.suggestedBarangay.isNullOrBlank() &&
            (barangay.isBlank() || barangay.equals("Poblacion", ignoreCase = true))
        ) {
            barangay = result.suggestedBarangay
        }
    }

    fun executeAutoGeoTag(uri: Uri?, capturedFile: File?) {
        scope.launch {
            isGeoTagging = true
            try {
                val result = if (uri != null) {
                    PhotoGeoTagHelper.autoGeoTagPhoto(
                        context = context,
                        uri = uri,
                        capturedFile = capturedFile,
                        fallbackLatLon = fallbackLatLon,
                        existingAttractions = existingAttractions
                    )
                } else {
                    val live = PhotoGeoTagHelper.resolveDeviceLocation(context)
                    val (coords, src) = live ?: (fallbackLatLon to GeoTagSource.FALLBACK_LOCATION)
                    val (nodeId, nodeName, suggestedBrgy) = PhotoGeoTagHelper.resolveNearestNodeAndBarangay(
                        latitude = coords.first,
                        longitude = coords.second,
                        existingAttractions = existingAttractions
                    )
                    PhotoGeoTagResult(
                        latitude = coords.first,
                        longitude = coords.second,
                        source = src,
                        nearestNodeId = nodeId,
                        nearestNodeName = nodeName,
                        suggestedBarangay = suggestedBrgy,
                        exifWrittenToFile = false
                    )
                }
                applyGeoTagResult(result)
            } finally {
                isGeoTagging = false
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        val pending = pendingGeoTagRequest
        pendingGeoTagRequest = null
        executeAutoGeoTag(pending?.first, pending?.second)
    }

    fun requestOrRunGeoTag(uri: Uri?, capturedFile: File?) {
        if (PhotoGeoTagHelper.hasLocationPermission(context)) {
            executeAutoGeoTag(uri, capturedFile)
        } else {
            pendingGeoTagRequest = uri to capturedFile
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val target = pendingCaptureTarget
        pendingCaptureTarget = null
        if (success && target != null) {
            // Preserve GPS EXIF geo-tagging immediately from the original captured file
            requestOrRunGeoTag(target.uri, target.file)
            scope.launch {
                isUploadingPhoto = true
                uploadStatusNote = "Optimizing & uploading cover photo to cloud…"
                try {
                    val result = CloudinaryImageUploader.uploadImage(
                        context = context,
                        uri = target.uri,
                        preset = ImageUploadPreset.ATTRACTION
                    )
                    if (result != null) {
                        photoData = result.photoData
                        clearPhoto = false
                        validationError = null
                        uploadStatusNote = result.statusMessage
                    } else {
                        uploadStatusNote = null
                        validationError = "Unable to process captured photo. Please try again."
                    }
                } finally {
                    isUploadingPhoto = false
                }
            }
        }
    }

    fun launchCameraCapture() {
        val target = runCatching { PhotoGeoTagHelper.createTempCaptureTarget(context) }.getOrNull()
        if (target == null) {
            validationError = "Could not prepare camera capture file."
            return
        }
        pendingCaptureTarget = target
        takePictureLauncher.launch(target.uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCameraCapture()
        } else {
            validationError = "Camera permission is required to capture attraction photos."
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            // Extract EXIF GPS coordinates from the original photo URI before compression
            requestOrRunGeoTag(uri, null)
            scope.launch {
                isUploadingPhoto = true
                uploadStatusNote = "Optimizing & uploading cover photo to cloud…"
                try {
                    val result = CloudinaryImageUploader.uploadImage(
                        context = context,
                        uri = uri,
                        preset = ImageUploadPreset.ATTRACTION
                    )
                    if (result != null) {
                        photoData = result.photoData
                        clearPhoto = false
                        validationError = null
                        uploadStatusNote = result.statusMessage
                    } else {
                        uploadStatusNote = null
                        validationError = "Unable to process image. Choose a smaller photo."
                    }
                } finally {
                    isUploadingPhoto = false
                }
            }
        }
    }

    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && galleryPhotos.size < 4) {
            scope.launch {
                isUploadingPhoto = true
                uploadStatusNote = "Optimizing & uploading gallery photo to cloud…"
                try {
                    val result = CloudinaryImageUploader.uploadImage(
                        context = context,
                        uri = uri,
                        preset = ImageUploadPreset.ATTRACTION
                    )
                    if (result != null) {
                        galleryPhotos = (galleryPhotos + result.photoData).distinct().take(4)
                        validationError = null
                        uploadStatusNote = "Gallery · ${result.statusMessage}"
                    } else {
                        uploadStatusNote = null
                        validationError = "Unable to process gallery image."
                    }
                } finally {
                    isUploadingPhoto = false
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
                // Visual preview + Geo-Tag & Cloud Upload Overlay
                val activeCoverPhoto = if (clearPhoto) "" else photoData
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(138.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    TravelPhotoImage(
                        photoData = activeCoverPhoto,
                        contentDescription = if (activeCoverPhoto.isNotBlank()) "Selected attraction photo" else "Attraction theme preview",
                        contentScale = ContentScale.Crop,
                        fallbackDrawableRes = AttractionImageMapper.resForKey(imageKey),
                        modifier = Modifier.matchParentSize()
                    )

                    if (isUploadingPhoto) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    if (geoTagResult != null || isGeoTagging) {
                        Surface(
                            color = Color(0xCC0A2823),
                            contentColor = Color.White,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .testTag("admin_photo_geotag_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isGeoTagging) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(13.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF34D399)
                                    )
                                    Text(
                                        text = "Geo-tagging location…",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                } else {
                                    val tag = geoTagResult!!
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF34D399),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = tag.formattedCoordinates,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                uploadStatusNote?.let { note ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isUploadingPhoto) {
                            CircularProgressIndicator(modifier = Modifier.size(13.dp), strokeWidth = 2.dp)
                        }
                        Text(
                            text = note,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Capture Photo + Upload Photo + Use Preset actions
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            if (PhotoGeoTagHelper.hasCameraPermission(context)) {
                                launchCameraCapture()
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        enabled = !isUploadingPhoto,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .heightIn(min = 48.dp)
                            .testTag("admin_capture_attraction_photo")
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Capture Photo", maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isUploadingPhoto,
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

                    if (!clearPhoto && photoData.isNotBlank() && !isUploadingPhoto) {
                        TextButton(
                            onClick = {
                                photoData = ""
                                clearPhoto = true
                                geoTagResult = null
                                uploadStatusNote = null
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

                // Auto geo-tag summary strip
                geoTagResult?.let { tag ->
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_geotag_summary_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Auto Geo-Tagged · ${tag.source.label}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = buildString {
                                    append("Nearest hub: ${tag.nearestNodeName}")
                                    if (tag.exifWrittenToFile) append(" · GPS EXIF embedded")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                // Multi-Photo Gallery (up to 4 extra photos shown in Attraction Detail carousel)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gallery Photos (${galleryPhotos.size}/4)",
                        style = MaterialTheme.typography.labelLarge
                    )
                    if (galleryPhotos.size < 4) {
                        AssistChip(
                            onClick = {
                                galleryPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !isUploadingPhoto,
                            leadingIcon = {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text("Add Photo", maxLines = 1) },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("admin_add_gallery_photo_button")
                        )
                    }
                }
                if (galleryPhotos.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(galleryPhotos) { index, photoItem ->
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                TravelPhotoImage(
                                    photoData = photoItem,
                                    contentDescription = "Gallery photo ${index + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize()
                                )
                                IconButton(
                                    onClick = {
                                        galleryPhotos = galleryPhotos.filterIndexed { i, _ -> i != index }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.65f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove gallery photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Dynamic Manageable Categories
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Category", style = MaterialTheme.typography.labelLarge)
                    AssistChip(
                        onClick = { showQuickCategoryRow = !showQuickCategoryRow },
                        leadingIcon = {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(if (showQuickCategoryRow) "Close" else "New Category", maxLines = 1) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("admin_inline_new_category_chip")
                    )
                }

                if (showQuickCategoryRow) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = quickCategoryName,
                                onValueChange = { quickCategoryName = it.take(50) },
                                label = { Text("Category Name") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("admin_quick_category_input")
                            )
                            Button(
                                onClick = {
                                    val trimmed = quickCategoryName.trim()
                                    val code = CategoryCatalog.normalizeCode(trimmed)
                                    if (trimmed.length >= 2 && CategoryCatalog.isValidCode(code)) {
                                        onQuickCreateCategory(
                                            code,
                                            trimmed,
                                            "forest",
                                            activeCategories.size.toLong()
                                        )
                                        selectedCategoryCode = code
                                        quickCategoryName = ""
                                        showQuickCategoryRow = false
                                        validationError = null
                                    } else {
                                        validationError = "Category name must be at least 2 letters."
                                    }
                                },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("admin_quick_category_save")
                            ) {
                                Text("Add", maxLines = 1)
                            }
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    activeCategories.forEach { cat ->
                        val code = cat.effectiveCode
                        val isSelected = selectedCategoryCode.equals(code, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategoryCode = code
                                if (initial == null && photoData.isBlank()) {
                                    imageKey = CategoryCatalog.defaultImageKeyForCode(code, cat.iconKey)
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = categoryIconForKey(cat.iconKey),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = AppStrings.categoryLabelForCode(code, activeCategories, language),
                                    maxLines = 1
                                )
                            },
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .heightIn(min = 48.dp)
                                .testTag("admin_attr_cat_${code.lowercase()}")
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
                                if (initial == null && geoTagResult == null) {
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("GPS Coordinates", style = MaterialTheme.typography.labelLarge)
                    TextButton(
                        onClick = { requestOrRunGeoTag(null, null) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("admin_geotag_gps_button")
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tag Current GPS", maxLines = 1)
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
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_attr_lat_input")
                    )
                    OutlinedTextField(
                        value = lonText,
                        onValueChange = { lonText = it.take(16) },
                        label = { Text("Longitude*") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_attr_lon_input")
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
                    val normalizedCategory = CategoryCatalog.normalizeCode(selectedCategoryCode)
                    when {
                        !CategoryCatalog.isValidCode(normalizedCategory) -> validationError = "Select a valid category."
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
                                normalizedCategory,
                                lat,
                                lon,
                                openHours.trim(),
                                entryFeeEn.trim(),
                                bestTimeEn.trim(),
                                transitHintEn.trim(),
                                rating,
                                imageKey,
                                photoData.takeIf { !clearPhoto && it.isNotBlank() },
                                galleryPhotos,
                                clearPhoto
                            )
                        }
                    }
                },
                enabled = !isUploadingPhoto,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("admin_attr_save_button")
            ) {
                Text(
                    text = when {
                        isUploadingPhoto -> "Uploading…"
                        initial == null -> "Publish"
                        else -> "Save"
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
                Text("Cancel", maxLines = 1)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminCategoryEditorDialog(
    initial: ManagedCategory?,
    defaultSortOrder: Long = 0L,
    onDismiss: () -> Unit,
    onSave: (
        code: String,
        name: String,
        iconKey: String,
        sortOrder: Long
    ) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var iconKey by remember(initial) { mutableStateOf(initial?.iconKey?.ifBlank { "forest" } ?: "forest") }
    var sortOrderText by remember(initial) {
        mutableStateOf((initial?.sortOrder ?: defaultSortOrder).toString())
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    val resolvedCode = remember(initial, name) {
        initial?.effectiveCode?.takeIf { it.isNotBlank() } ?: CategoryCatalog.normalizeCode(name)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "Add Attraction Category" else "Edit Attraction Category",
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
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(50) },
                    label = { Text("Category Display Name*") },
                    placeholder = { Text("e.g., Waterfalls & Springs") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_category_name_input")
                )

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Category Code",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = resolvedCode.ifBlank { "—" },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text("Category Icon", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CategoryCatalog.iconOptions.forEach { (key, label) ->
                        FilterChip(
                            selected = iconKey == key,
                            onClick = { iconKey = key },
                            leadingIcon = {
                                Icon(
                                    imageVector = categoryIconForKey(key),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(label.substringBefore(" /"), maxLines = 1) },
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .heightIn(min = 48.dp)
                                .testTag("admin_category_icon_$key")
                        )
                    }
                }

                OutlinedTextField(
                    value = sortOrderText,
                    onValueChange = { sortOrderText = it.filter(Char::isDigit).take(3) },
                    label = { Text("Display Sort Order (0–999)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_category_order_input")
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
                    val cleanName = name.trim()
                    val order = sortOrderText.trim().toLongOrNull() ?: defaultSortOrder
                    when {
                        cleanName.length < 2 -> validationError = "Enter a category name (min 2 chars)."
                        !CategoryCatalog.isValidCode(resolvedCode) -> validationError = "Name must contain at least 2 letters or digits."
                        order !in 0L..999L -> validationError = "Sort order must be between 0 and 999."
                        else -> onSave(resolvedCode, cleanName, iconKey, order)
                    }
                },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("admin_category_save_button")
            ) {
                Text(if (initial == null) "Create" else "Save", maxLines = 1)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminEventEditorDialog(
    initial: TourismEvent?,
    attractions: List<Attraction>,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        summary: String,
        eventType: String,
        severity: String,
        dateLabel: String,
        locationName: String,
        attractionId: String,
        active: Boolean
    ) -> Unit
) {
    var title by remember(initial) { mutableStateOf(initial?.title ?: "") }
    var summary by remember(initial) { mutableStateOf(initial?.summary ?: "") }
    var eventType by remember(initial) { mutableStateOf(initial?.eventType ?: TourismEventType.FESTIVAL.code) }
    var severity by remember(initial) { mutableStateOf(initial?.severity ?: TourismEventSeverity.INFO.code) }
    var dateLabel by remember(initial) { mutableStateOf(initial?.dateLabel ?: "This Weekend · 08:00 AM") }
    var locationName by remember(initial) { mutableStateOf(initial?.locationName ?: "Poblacion, Dumingag") }
    var attractionId by remember(initial) { mutableStateOf(initial?.attractionId ?: "") }
    var active by remember(initial) { mutableStateOf(initial?.active ?: true) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "Publish Event or Advisory" else "Edit Event / Advisory",
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
                Text("Type", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TourismEventType.entries.forEach { type ->
                        FilterChip(
                            selected = eventType == type.code,
                            onClick = { eventType = type.code },
                            label = { Text(type.label, maxLines = 1) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }

                Text("Severity", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TourismEventSeverity.entries.forEach { sev ->
                        FilterChip(
                            selected = severity == sev.code,
                            onClick = { severity = sev.code },
                            label = { Text(sev.label, maxLines = 1) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(120) },
                    label = { Text("Headline*") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_event_title_input")
                )
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it.take(600) },
                    label = { Text("Advisory / Event Details*") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_event_summary_input")
                )
                OutlinedTextField(
                    value = dateLabel,
                    onValueChange = { dateLabel = it.take(100) },
                    label = { Text("Schedule / Date*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it.take(120) },
                    label = { Text("Location / Barangay") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Link to Attraction (optional)", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = attractionId.isBlank(),
                        onClick = { attractionId = "" },
                        label = { Text("None", maxLines = 1) }
                    )
                    attractions.take(8).forEach { spot ->
                        FilterChip(
                            selected = attractionId == spot.id,
                            onClick = { attractionId = spot.id },
                            label = { Text(spot.name.get(language).split(" ").take(2).joinToString(" "), maxLines = 1) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Visible to Travelers", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = active, onCheckedChange = { active = it })
                }

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
                        title.trim().length < 2 -> validationError = "Enter a headline (min 2 chars)."
                        summary.trim().length < 5 -> validationError = "Enter details (min 5 chars)."
                        dateLabel.trim().length < 2 -> validationError = "Enter a schedule or date."
                        else -> onSave(
                            title.trim(),
                            summary.trim(),
                            eventType,
                            severity,
                            dateLabel.trim(),
                            locationName.trim(),
                            attractionId.trim(),
                            active
                        )
                    }
                },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("admin_event_save_button")
            ) {
                Text(if (initial == null) "Publish" else "Save", maxLines = 1)
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
