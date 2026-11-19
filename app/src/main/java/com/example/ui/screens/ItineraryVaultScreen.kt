package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.DumingagSeedData
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.Attraction
import com.example.model.ItineraryItem
import com.example.model.SavedDocument
import com.example.model.UiState
import com.example.notifications.TravelAlertManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItineraryVaultScreen(
    language: AppLanguage,
    attractions: List<Attraction>,
    itinerariesState: UiState<List<ItineraryItem>>,
    savedDocuments: List<SavedDocument>,
    savedAttractionCount: Int,
    offlinePackReady: Boolean,
    forceOfflineMode: Boolean,
    onToggleForceOffline: (Boolean) -> Unit,
    onDownloadOfflinePack: () -> Unit,
    onAddItinerary: (Attraction, Long, String, Boolean) -> Unit,
    onSyncCalendar: (ItineraryItem) -> Unit,
    onToggleReminder: (ItineraryItem) -> Unit,
    onDeleteItinerary: (String) -> Unit,
    onSaveCustomDocument: (String, String, String, String?) -> Unit,
    onDeleteCustomDocument: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var showAddItineraryPicker by remember { mutableStateOf(false) }
    var attractionForDialog by remember { mutableStateOf<Attraction?>(null) }
    var showNewDocDialog by remember { mutableStateOf(false) }
    var viewingDocument by remember { mutableStateOf<SavedDocument?>(null) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            onShowMessage("Notification permission needed for itinerary reminders")
        }
    }

    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            var fileName = "Imported Travel Document"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) {
                    fileName = cursor.getString(idx) ?: fileName
                }
            }
            onSaveCustomDocument(
                fileName,
                "Imported File",
                "Attached local offline travel document ($fileName). Open anytime from your Offline Vault.",
                uri.toString()
            )
        }
    }

    TravelPageGrid("plan_page") {
        pageSection {
            val stops = (itinerariesState as? UiState.Success)?.data?.size
            TravelPageHeader(
                AppStrings.navPlan(language),
                stops?.let { if (it == 1) "1 planned stop" else "$it planned stops" },
                R.drawable.img_waterfall_falls
            ) {
                TravelHeaderAction("Add stop", Icons.Default.Add, "add_itinerary_stop_button") {
                    showAddItineraryPicker = true
                }
            }
        }
        when (itinerariesState) {
            is UiState.Loading -> {
                pageSection {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                            text = itinerariesState.message,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            is UiState.Success -> {
                val items = itinerariesState.data
                if (items.isEmpty()) {
                    pageSection {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("No stops planned yet", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Add Dumingag landmarks to sync with your device calendar and get offline reminders.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(items, key = { "stop-${it.id}" }) { stop ->
                        ItineraryStopCard(
                            item = stop,
                            language = language,
                            onSyncCalendar = {
                                try {
                                    val calIntent = TravelAlertManager.createCalendarSyncIntent(stop)
                                    context.startActivity(calIntent)
                                    onSyncCalendar(stop)
                                } catch (_: Exception) {
                                    onShowMessage("No calendar app available on device")
                                }
                            },
                            onToggleReminder = {
                                if (!stop.reminderEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!granted) {
                                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                                if (!stop.reminderEnabled) {
                                    TravelAlertManager.scheduleItineraryReminder(context, stop)
                                    onShowMessage("Reminder scheduled for ${stop.attractionName}")
                                }
                                onToggleReminder(stop)
                            },
                            onDelete = { onDeleteItinerary(stop.id) }
                        )
                    }
                }
            }
        }

        // Offline Mode & Map Pack Control Card
        pageSection {
            Text(
                text = AppStrings.offlineVaultTitle(language),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dumingag Offline Map & Trail Pack",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (offlinePackReady) {
                                    "All ${attractions.size} landmarks, trail graph & guides cached ($savedAttractionCount saved)"
                                } else {
                                    "Cache all maps, attraction guides & trail routes for zero-signal areas"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Icon(
                            imageVector = if (offlinePackReady) Icons.Default.CheckCircle else Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Force Offline Mode", style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(
                            checked = forceOfflineMode,
                            onCheckedChange = onToggleForceOffline,
                            modifier = Modifier.testTag("force_offline_switch")
                        )
                    }

                    Button(
                        onClick = onDownloadOfflinePack,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("download_offline_pack_button")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (offlinePackReady) "Refresh Offline Pack" else "Download Full Offline Pack",
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Saved Offline Documents Section
        pageSection {
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Saved Documents",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(end = 8.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    OutlinedButton(
                        onClick = {
                            openDocLauncher.launch(arrayOf("application/pdf", "text/*", "image/*"))
                        },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("import_document_button")
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    FilledTonalButton(
                        onClick = { showNewDocDialog = true },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("add_offline_note_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Note", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        items(savedDocuments, key = { "doc-${it.id}" }) { doc ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewingDocument = doc }
                    .testTag("saved_doc_${doc.id}")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(doc.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "${doc.category} • ${if (doc.isBundled) "Offline Ready" else "Personal"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = doc.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                    if (!doc.isBundled) {
                        IconButton(onClick = { onDeleteCustomDocument(doc.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete document")
                        }
                    }
                }
            }
        }
    }

    if (showAddItineraryPicker) {
        AlertDialog(
            onDismissRequest = { showAddItineraryPicker = false },
            title = { Text("Select Destination") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val selectableAttractions = attractions.ifEmpty { DumingagSeedData.attractions }
                    selectableAttractions.forEach { attraction ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showAddItineraryPicker = false
                                    attractionForDialog = attraction
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(attraction.name.get(language), style = MaterialTheme.typography.titleSmall)
                                Text("Brgy. ${attraction.barangay}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddItineraryPicker = false }) { Text("Close") }
            }
        )
    }

    attractionForDialog?.let { selected ->
        AddToItineraryDialog(
            attractionName = selected.name.get(language),
            onDismiss = { attractionForDialog = null },
            onConfirm = { days, notes, reminder ->
                val millis = System.currentTimeMillis() + days * 24L * 60L * 60L * 1000L
                onAddItinerary(selected, millis, notes, reminder)
                attractionForDialog = null
            }
        )
    }

    if (showNewDocDialog) {
        var docTitle by remember { mutableStateOf("") }
        var docBody by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewDocDialog = false },
            title = { Text("New Offline Travel Document") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = docTitle,
                        onValueChange = { docTitle = it },
                        label = { Text("Title (e.g., Permit / Booking)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = docBody,
                        onValueChange = { docBody = it },
                        label = { Text("Details stored offline") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (docTitle.isNotBlank() && docBody.isNotBlank()) {
                            onSaveCustomDocument(docTitle, "Travel Note", docBody, null)
                            showNewDocDialog = false
                        }
                    }
                ) {
                    Text("Save Offline")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewDocDialog = false }) { Text("Cancel") }
            }
        )
    }

    viewingDocument?.let { doc ->
        AlertDialog(
            onDismissRequest = { viewingDocument = null },
            title = { Text(doc.title) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = doc.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = doc.contentBody,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                if (doc.fileUri != null) {
                    Button(onClick = {
                        try {
                            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse(doc.fileUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(viewIntent)
                        } catch (_: Exception) {
                            onShowMessage("No viewer app installed for this file")
                        }
                    }) {
                        Text("Open File")
                    }
                } else {
                    Button(onClick = { viewingDocument = null }) { Text("Done") }
                }
            },
            dismissButton = {
                if (doc.fileUri != null) {
                    TextButton(onClick = { viewingDocument = null }) { Text("Close") }
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItineraryStopCard(
    item: ItineraryItem,
    language: AppLanguage,
    onSyncCalendar: () -> Unit,
    onToggleReminder: () -> Unit,
    onDelete: () -> Unit
) {
    val date = Date(item.plannedDateMillis)
    val locale = Locale.getDefault()
    val month = remember(locale) { SimpleDateFormat("MMM", locale) }
    val day = remember(locale) { SimpleDateFormat("d", locale) }
    val schedule = remember(locale) { SimpleDateFormat("EEE, MMM d, yyyy · h:mm a", locale) }
    Card(
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(month.format(date), style = MaterialTheme.typography.labelMedium)
                        Text(day.format(date), style = MaterialTheme.typography.titleLarge)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(item.attractionName, style = MaterialTheme.typography.titleLarge)
                    Text(schedule.format(date), style = MaterialTheme.typography.bodySmall)
                }
            }
            if (item.notes.isNotBlank()) {
                Text(
                    text = item.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onToggleReminder) {
                    Icon(if (item.reminderEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                        contentDescription = if (item.reminderEnabled) "Disable reminder" else "Enable reminder")
                }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Remove stop") }
                Surface(
                    color = if (item.calendarSynced) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Text(
                        text = if (item.calendarSynced) "Calendar Synced" else "Not yet in calendar",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                FilledTonalButton(
                    onClick = onSyncCalendar,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .heightIn(min = 48.dp)
                        .testTag("sync_calendar_${item.id}")
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = AppStrings.syncToCalendar(language),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
