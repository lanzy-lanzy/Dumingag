package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.Attraction
import com.example.model.AttractionCategory
import com.example.model.EmergencyContact
import com.example.ui.components.AttractionCoverImage

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminManagementScreen(
    language: AppLanguage,
    isAdmin: Boolean,
    adminEmail: String,
    attractions: List<Attraction>,
    emergencyContacts: List<EmergencyContact>,
    initialTab: Int = 0,
    onBackClick: () -> Unit,
    onSaveAttraction: (
        attractionId: String?,
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
    ) -> Unit,
    onDeleteAttraction: (String) -> Unit,
    onSaveEmergencyContact: (
        contactId: String?,
        titleEn: String,
        department: String,
        phoneNumber: String,
        availability: String,
        priorityLevel: String,
        email: String?,
        websiteUrl: String?
    ) -> Unit,
    onDeleteEmergencyContact: (String) -> Unit
) {
    BackHandler { onBackClick() }

    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    var searchQuery by remember { mutableStateOf("") }

    var showAttractionDialog by remember { mutableStateOf(false) }
    var editingAttraction by remember { mutableStateOf<Attraction?>(null) }
    var deletingAttraction by remember { mutableStateOf<Attraction?>(null) }

    var showContactDialog by remember { mutableStateOf(false) }
    var editingContact by remember { mutableStateOf<EmergencyContact?>(null) }
    var deletingContact by remember { mutableStateOf<EmergencyContact?>(null) }

    if (!isAdmin) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Verified Admin Access Required",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = "Only verified LGU administrators can manage attractions and emergency contacts.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Button(
                onClick = onBackClick,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Return to Guide", maxLines = 1)
            }
        }
        return
    }

    val queryTrimmed = searchQuery.trim().lowercase()
    val filteredAttractions = remember(attractions, queryTrimmed, language) {
        if (queryTrimmed.isEmpty()) attractions
        else attractions.filter {
            it.name.get(language).lowercase().contains(queryTrimmed) ||
                it.name.en.lowercase().contains(queryTrimmed) ||
                it.barangay.lowercase().contains(queryTrimmed)
        }
    }
    val filteredContacts = remember(emergencyContacts, queryTrimmed, language) {
        if (queryTrimmed.isEmpty()) emergencyContacts
        else emergencyContacts.filter {
            it.title.get(language).lowercase().contains(queryTrimmed) ||
                it.department.lowercase().contains(queryTrimmed) ||
                it.phoneNumber.lowercase().contains(queryTrimmed)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useTwoColumns = maxWidth >= 600.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("admin_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Admin Console",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = adminEmail.ifBlank { "Verified LGU Admin" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Button(
                                onClick = {
                                    if (selectedTab == 0) {
                                        editingAttraction = null
                                        showAttractionDialog = true
                                    } else {
                                        editingContact = null
                                        showContactDialog = true
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("admin_create_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (selectedTab == 0) "New Spot" else "New Hotline",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                leadingIcon = {
                                    Icon(Icons.Default.Landscape, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text("Attractions (${attractions.size})", maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .heightIn(min = 48.dp)
                                    .testTag("admin_tab_attractions")
                            )
                            FilterChip(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                leadingIcon = {
                                    Icon(Icons.Default.LocalPolice, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text("Emergency (${emergencyContacts.size})", maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .heightIn(min = 48.dp)
                                    .testTag("admin_tab_contacts")
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = if (selectedTab == 0) "Search attractions or barangay..." else "Search hotlines or agencies...",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search admin entries") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_search_input")
                )
            }

            if (selectedTab == 0) {
                if (filteredAttractions.isEmpty()) {
                    item {
                        EmptyAdminStateCard(
                            message = "No matching local attractions found.",
                            actionLabel = "Add Local Attraction",
                            onAction = {
                                editingAttraction = null
                                showAttractionDialog = true
                            }
                        )
                    }
                } else if (useTwoColumns) {
                    val rows = filteredAttractions.chunked(2)
                    items(rows, key = { pair -> pair.joinToString("_") { it.id } }) { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pair.forEach { attraction ->
                                AdminAttractionCard(
                                    attraction = attraction,
                                    language = language,
                                    onEdit = {
                                        editingAttraction = attraction
                                        showAttractionDialog = true
                                    },
                                    onDelete = { deletingAttraction = attraction },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                } else {
                    items(filteredAttractions, key = { it.id }) { attraction ->
                        AdminAttractionCard(
                            attraction = attraction,
                            language = language,
                            onEdit = {
                                editingAttraction = attraction
                                showAttractionDialog = true
                            },
                            onDelete = { deletingAttraction = attraction }
                        )
                    }
                }
            } else {
                if (filteredContacts.isEmpty()) {
                    item {
                        EmptyAdminStateCard(
                            message = "No matching emergency contacts found.",
                            actionLabel = "Add Emergency Hotline",
                            onAction = {
                                editingContact = null
                                showContactDialog = true
                            }
                        )
                    }
                } else if (useTwoColumns) {
                    val rows = filteredContacts.chunked(2)
                    items(rows, key = { pair -> pair.joinToString("_") { it.id } }) { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pair.forEach { contact ->
                                AdminEmergencyContactCard(
                                    contact = contact,
                                    language = language,
                                    onEdit = {
                                        editingContact = contact
                                        showContactDialog = true
                                    },
                                    onDelete = { deletingContact = contact },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                } else {
                    items(filteredContacts, key = { it.id }) { contact ->
                        AdminEmergencyContactCard(
                            contact = contact,
                            language = language,
                            onEdit = {
                                editingContact = contact
                                showContactDialog = true
                            },
                            onDelete = { deletingContact = contact }
                        )
                    }
                }
            }
        }
    }

    if (showAttractionDialog) {
        AdminAttractionEditorDialog(
            initial = editingAttraction,
            language = language,
            onDismiss = {
                showAttractionDialog = false
                editingAttraction = null
            },
            onSave = { nodeId, nameEn, subEn, descEn, histEn, brgy, cat, lat, lon, hours, fee, best, transit, rating, imgKey, photoData, clearPhoto ->
                onSaveAttraction(
                    editingAttraction?.id,
                    nodeId,
                    nameEn,
                    subEn,
                    descEn,
                    histEn,
                    brgy,
                    cat,
                    lat,
                    lon,
                    hours,
                    fee,
                    best,
                    transit,
                    rating,
                    imgKey,
                    photoData,
                    clearPhoto
                )
                showAttractionDialog = false
                editingAttraction = null
            }
        )
    }

    if (showContactDialog) {
        AdminEmergencyContactDialog(
            initial = editingContact,
            onDismiss = {
                showContactDialog = false
                editingContact = null
            },
            onSave = { titleEn, dept, phone, avail, priority, email, url ->
                onSaveEmergencyContact(
                    editingContact?.id,
                    titleEn,
                    dept,
                    phone,
                    avail,
                    priority,
                    email,
                    url
                )
                showContactDialog = false
                editingContact = null
            }
        )
    }

    deletingAttraction?.let { target ->
        AdminDeleteConfirmDialog(
            title = "Delete Local Attraction",
            itemLabel = target.name.get(language),
            onDismiss = { deletingAttraction = null },
            onConfirm = {
                onDeleteAttraction(target.id)
                deletingAttraction = null
            }
        )
    }

    deletingContact?.let { target ->
        AdminDeleteConfirmDialog(
            title = "Delete Emergency Contact",
            itemLabel = target.title.get(language),
            onDismiss = { deletingContact = null },
            onConfirm = {
                onDeleteEmergencyContact(target.id)
                deletingContact = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminAttractionCard(
    attraction: Attraction,
    language: AppLanguage,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AttractionCoverImage(
                    attraction = attraction,
                    contentDescription = attraction.name.get(language),
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = AppStrings.categoryLabel(attraction.category, language),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = attraction.name.get(language),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Brgy. ${attraction.barangay} · ${"%.4f".format(attraction.latitude)}, ${"%.4f".format(attraction.longitude)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("admin_edit_attraction_${attraction.id}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onDelete,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("admin_delete_attraction_${attraction.id}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete", color = MaterialTheme.colorScheme.error, maxLines = 1)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminEmergencyContactCard(
    contact: EmergencyContact,
    language: AppLanguage,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCritical = contact.priorityLevel == "CRITICAL" || contact.priorityLevel == "HIGH"
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (isCritical) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = contact.priorityLevel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCritical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Call, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = contact.phoneNumber,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = contact.title.get(language),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${contact.department} · ${contact.availability}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("admin_edit_contact_${contact.id}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onDelete,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("admin_delete_contact_${contact.id}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete", color = MaterialTheme.colorScheme.error, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun EmptyAdminStateCard(
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Text(text = message, style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = onAction,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(actionLabel, maxLines = 1)
            }
        }
    }
}
