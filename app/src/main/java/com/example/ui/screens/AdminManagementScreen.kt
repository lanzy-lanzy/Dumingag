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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import com.example.R
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
import com.example.model.ManagedUser
import com.example.model.UiState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.CancellationException
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
    currentUserId: String,
    loadUsers: suspend () -> List<ManagedUser>,
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

    if (!isAdmin) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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
                text = "Only verified LGU administrators can manage users, attractions, and emergency contacts.",
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

    var selectedTab by rememberSaveable(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    var searchQuery by rememberSaveable(selectedTab) { mutableStateOf("") }

    var showAttractionDialog by remember { mutableStateOf(false) }
    var editingAttraction by remember { mutableStateOf<Attraction?>(null) }
    var deletingAttraction by remember { mutableStateOf<Attraction?>(null) }

    var showContactDialog by remember { mutableStateOf(false) }
    var editingContact by remember { mutableStateOf<EmergencyContact?>(null) }
    var deletingContact by remember { mutableStateOf<EmergencyContact?>(null) }

    var refresh by remember { mutableIntStateOf(0) }
    var users by remember(selectedTab, refresh) { mutableStateOf<UiState<List<ManagedUser>>>(UiState.Loading) }
    LaunchedEffect(selectedTab, refresh) {
        if (selectedTab == 2) {
            users = try {
                UiState.Success(loadUsers())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UiState.Error(e.localizedMessage ?: "Unable to load users.")
            }
        }
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

    val sections = listOf("Attractions", "Hotlines", "Users")
    val section = sections[selectedTab]
    val onCreate = {
        if (selectedTab == 0) {
            editingAttraction = null
            showAttractionDialog = true
        } else {
            editingContact = null
            showContactDialog = true
        }
    }
    val matchingUsers = (users as? UiState.Success)?.data.orEmpty().filter {
        it.displayName.contains(queryTrimmed, ignoreCase = true) ||
            it.email.contains(queryTrimmed, ignoreCase = true) ||
            it.role.contains(queryTrimmed, ignoreCase = true) ||
            it.uid.contains(queryTrimmed, ignoreCase = true)
    }
    val count = when (selectedTab) {
        0 -> filteredAttractions.size
        1 -> filteredContacts.size
        else -> matchingUsers.size
    }
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val gutter = if (maxWidth >= 600.dp) 24.dp else 12.dp
        LazyVerticalGrid(
            columns = GridCells.Adaptive(292.dp * fontScale),
            modifier = Modifier.widthIn(max = 1280.dp).fillMaxSize().testTag("admin_directory"),
            contentPadding = PaddingValues(gutter),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))) {
                    Image(
                        painterResource(R.drawable.img_dumingag_hero), contentDescription = null,
                        contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize()
                    )
                    Box(Modifier.matchParentSize().background(
                        Brush.horizontalGradient(listOf(Color(0xF20C302B), Color(0xA0163930)))
                    ))
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onBackClick, modifier = Modifier.testTag("admin_back_button")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to guide", tint = Color.White)
                        }
                        Text("Admin Console", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                        Text(adminEmail.ifBlank { "Dumingag LGU" }, style = MaterialTheme.typography.bodyMedium,
                            color = Color.White, modifier = Modifier.padding(bottom = 6.dp))
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val icons = listOf(Icons.Default.Landscape, Icons.Default.LocalPolice, Icons.Default.Group)
                    val tags = listOf("attractions", "contacts", "users")
                    sections.forEachIndexed { index, label ->
                        FilterChip(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            label = { Text(label) },
                            leadingIcon = { Icon(icons[index], null, Modifier.size(18.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.heightIn(min = 48.dp).testTag("admin_tab_${tags[index]}")
                        )
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Column(Modifier.align(Alignment.CenterVertically).padding(end = 12.dp)) {
                            Text(section, style = MaterialTheme.typography.headlineMedium)
                            if (selectedTab != 2 || users is UiState.Success) {
                                Text(
                                    if (count == 1) "1 entry" else "$count entries",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (selectedTab < 2) {
                            Button(onClick = onCreate, modifier = Modifier.align(Alignment.CenterVertically)
                                .heightIn(min = 48.dp).testTag("admin_create_button")) {
                                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (selectedTab == 0) "Add attraction" else "Add hotline")
                            }
                        } else {
                            OutlinedButton(onClick = { refresh++ }, modifier = Modifier.align(Alignment.CenterVertically)) {
                                Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Refresh users")
                            }
                        }
                    }
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text(when (selectedTab) {
                            0 -> "Search attractions or barangay"
                            1 -> "Search hotlines or agencies"
                            else -> "Search names, emails, roles, or UIDs"
                        }) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, "Clear search")
                            }
                        },
                        singleLine = true, shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("admin_search_input")
                    )
                }
            }
            if (selectedTab == 2) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Assign admins in Firestore using a verified email: set role = \"admin\" on users/{uid} or in admin_emails/{email}. Accounts without role = \"admin\" stay regular.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
            val userState = users
            when {
                selectedTab == 2 && userState is UiState.Loading -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Text("Loading users…")
                }
                selectedTab == 2 && userState is UiState.Error -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(userState.message, color = MaterialTheme.colorScheme.error)
                }
                count == 0 -> item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyAdminStateCard(
                        message = if (queryTrimmed.isEmpty()) "No ${section.lowercase()} yet." else "No matching ${section.lowercase()}.",
                        actionLabel = when {
                            queryTrimmed.isNotEmpty() -> "Clear search"
                            selectedTab == 0 -> "Add attraction"
                            selectedTab == 1 -> "Add hotline"
                            else -> "Refresh users"
                        },
                        onAction = {
                            when {
                                queryTrimmed.isNotEmpty() -> searchQuery = ""
                                selectedTab == 2 -> refresh++
                                else -> onCreate()
                            }
                        }
                    )
                }
                selectedTab == 0 -> items(filteredAttractions, key = { "attraction-${it.id}" }) { attraction ->
                    AdminAttractionCard(attraction, language,
                        onEdit = {
                            editingAttraction = attraction
                            showAttractionDialog = true
                        },
                        onDelete = { deletingAttraction = attraction })
                }
                selectedTab == 1 -> items(filteredContacts, key = { "contact-${it.id}" }) { contact ->
                    AdminEmergencyContactCard(contact, language,
                        onEdit = {
                            editingContact = contact
                            showContactDialog = true
                        },
                        onDelete = { deletingContact = contact })
                }
                else -> items(matchingUsers, key = { "user-${it.uid}" }) { user ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    color = if (user.isAdminRole) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = if (user.isAdminRole) "ADMIN" else "REGULAR",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (user.isAdminRole) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                if (user.uid == currentUserId) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = "YOU",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                            Text(user.displayName.ifBlank { "Traveler" }, style = MaterialTheme.typography.titleLarge)
                            if (user.email.isNotBlank()) {
                                Text(
                                    text = user.email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(user.uid, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
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
        Column {
            AttractionCoverImage(
                attraction = attraction, contentDescription = null,
                modifier = Modifier.fillMaxWidth().aspectRatio(3f)
            )
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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

            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
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
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp)
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
