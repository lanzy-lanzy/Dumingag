package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.items
import com.example.R
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.DumingagSeedData
import com.example.data.local.TravelGuideCacheStatus
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.Attraction
import com.example.model.EmergencyContact
import com.example.model.TransitMethod
import com.example.model.TransitModeCategory
import com.example.model.UiState
import com.example.notifications.TravelAlertManager
import com.example.ui.components.LiquidGlassSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TransitSafetyScreen(
    language: AppLanguage,
    transitState: UiState<List<TransitMethod>>,
    emergencyContacts: List<EmergencyContact>,
    isAdmin: Boolean,
    isEffectiveOffline: Boolean,
    isRefreshing: Boolean = false,
    cacheStatus: TravelGuideCacheStatus? = null,
    onRefreshTransit: () -> Unit,
    onRefreshAll: () -> Unit = onRefreshTransit,
    onManageEmergencyContacts: () -> Unit,
    onAddTransitMethod: (
        name: String,
        modeCategory: String,
        operatorName: String,
        routeSummary: String,
        scheduleText: String,
        fareRangePhp: String,
        durationText: String,
        terminalLocation: String,
        travelTip: String,
        serviceStatus: String
    ) -> Unit,
    onUpdateTransitMethod: (
        transitId: String,
        name: String,
        modeCategory: String,
        operatorName: String,
        routeSummary: String,
        scheduleText: String,
        fareRangePhp: String,
        durationText: String,
        terminalLocation: String,
        travelTip: String,
        serviceStatus: String
    ) -> Unit,
    onDeleteTransitMethod: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    attractions: List<Attraction> = DumingagSeedData.attractions,
    userLatLon: Pair<Double, Double> = DumingagSeedData.CENTER_LAT to DumingagSeedData.CENTER_LON
) {
    val context = LocalContext.current
    var pendingAlertAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var selectedModeFilter by remember { mutableStateOf<TransitModeCategory?>(null) }
    var showTransitDialog by remember { mutableStateOf(false) }
    var editingMethod by remember { mutableStateOf<TransitMethod?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingAlertAction?.invoke()
        } else {
            onShowMessage("Enable notification permission to receive travel alerts")
        }
        pendingAlertAction = null
    }

    fun triggerAlertWithPermission(title: String, message: String, id: Int) {
        val send = {
            TravelAlertManager.showNotification(context, title, message, id)
            onShowMessage("Travel alert notification dispatched")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                send()
            } else {
                pendingAlertAction = send
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            send()
        }
    }

    val routeCount = (transitState as? UiState.Success)?.data?.size
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefreshAll,
        modifier = Modifier
            .fillMaxSize()
            .testTag("transit_pull_to_refresh")
    ) {
    TravelPageGrid("transit_page") {
        pageSection {
            TravelPageHeader(
                AppStrings.transitTitle(language),
                listOfNotNull(
                    if (isEffectiveOffline) "Offline schedules" else null,
                    routeCount?.let { if (it == 1) "1 route" else "$it routes" }
                ).joinToString(" · ").takeIf { it.isNotEmpty() },
                R.drawable.img_heritage_plaza
            ) {
                TravelHeaderAction("Sync schedules", Icons.Default.Refresh, "transit_refresh_button", onClick = onRefreshTransit)
                if (isAdmin) TravelHeaderAction("Add route", Icons.Default.Add, "transit_add_button") {
                    editingMethod = null
                    showTransitDialog = true
                }
            }
        }

        if (cacheStatus != null && cacheStatus.isReadyOffline) {
            pageSection {
                val syncedTimeLabel = remember(cacheStatus.lastUpdatedAtMillis) {
                    if (cacheStatus.lastUpdatedAtMillis > 0L) {
                        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                            .format(Date(cacheStatus.lastUpdatedAtMillis))
                    } else {
                        null
                    }
                }
                val summaryText = buildString {
                    append(if (isEffectiveOffline) "Offline Cache Active" else "Cached Offline")
                    append(" · ")
                    append("${cacheStatus.transitCount} routes & ${cacheStatus.emergencyContactCount} emergency hotlines")
                    if (syncedTimeLabel != null) {
                        append(" · ")
                        append(syncedTimeLabel)
                    }
                }
                LiquidGlassSurface(
                    cornerRadius = 18.dp,
                    elevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transit_offline_cache_badge")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isEffectiveOffline) Icons.Default.CloudOff else Icons.Default.CloudDone,
                            contentDescription = if (isEffectiveOffline) "Offline cache active" else "Offline cache synced",
                            tint = if (isEffectiveOffline) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = summaryText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        pageSection {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedModeFilter == null,
                        onClick = { selectedModeFilter = null },
                        label = { Text("All Modes", maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("transit_filter_all")
                    )
                }
                items(TransitModeCategory.entries, key = { it.code }) { mode ->
                    val selected = selectedModeFilter == mode
                    FilterChip(
                        selected = selected,
                        onClick = {
                            selectedModeFilter = if (selectedModeFilter == mode) null else mode
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = transitModeIcon(mode),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(AppStrings.transitModeLabel(mode, language), maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("transit_filter_${mode.code}")
                    )
                }
            }
        }

        when (transitState) {
            is UiState.Loading -> {
                items(3) { TransitSkeletonCard() }
            }

            is UiState.Error -> {
                pageSection {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Could not sync Dumingag transit schedules",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = transitState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Button(
                                onClick = onRefreshTransit,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("transit_retry_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry Sync", maxLines = 1)
                            }
                        }
                    }
                }
            }

            is UiState.Success -> {
                val filteredMethods = if (selectedModeFilter == null) {
                    transitState.data
                } else {
                    transitState.data.filter {
                        TransitModeCategory.fromCode(it.modeCategory) == selectedModeFilter
                    }
                }

                if (filteredMethods.isEmpty()) {
                    pageSection {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (selectedModeFilter == null) {
                                        "No transit routes available yet."
                                    } else {
                                        "No routes posted for this transport category yet."
                                    },
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = if (isAdmin) "Sync schedules or add a route." else "Sync to fetch the latest schedules.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filteredMethods, key = { "route-${it.id}" }) { method ->
                        TransitMethodCard(
                            method = method,
                            language = language,
                            canManage = isAdmin,
                            onEdit = {
                                editingMethod = method
                                showTransitDialog = true
                            },
                            onDelete = {
                                onDeleteTransitMethod(method.id)
                            }
                        )
                    }
                }
            }
        }

        pageSection {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = AppStrings.alertsSectionTitle(language),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            TravelAlertsCard(
                onTriggerWeatherAlert = {
                    triggerAlertWithPermission(
                        title = "Dumingag Weather Advisory",
                        message = "Afternoon mountain showers expected near Mt. Timolan & Danlugan Falls. Carry rain gear.",
                        id = 3001
                    )
                },
                onTriggerTransitAlert = {
                    triggerAlertWithPermission(
                        title = "Dumingag Last Bus Reminder",
                        message = "Final Pagadian-bound van departs Poblacion Terminal at 5:30 PM.",
                        id = 3002
                    )
                }
            )
        }

        pageSection {
            HabalHabalFareEstimatorCard(
                language = language,
                attractions = attractions,
                userLatLon = userLatLon
            )
        }

        pageSection {
            OfflinePhrasebookCard(onShowMessage = onShowMessage)
        }

        pageSection {
            EmergencySosSmsCard(
                userLatLon = userLatLon,
                attractions = attractions,
                emergencyContacts = emergencyContacts,
                onShowMessage = onShowMessage
            )
        }

        pageSection {
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = AppStrings.emergencyTitle(language),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Official LGU Dumingag Directory (dumingag.com)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    if (isAdmin) {
                        FilledTonalButton(
                            onClick = onManageEmergencyContacts,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .heightIn(min = 48.dp)
                                .testTag("admin_manage_contacts_button")
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Manage", maxLines = 1)
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://dumingag.com")))
                            } catch (_: Exception) {
                                onShowMessage("No browser available to open dumingag.com")
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .heightIn(min = 48.dp)
                            .testTag("open_dumingag_portal_button")
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("dumingag.com", maxLines = 1)
                    }
                }
            }
        }

        items(emergencyContacts, key = { "contact-${it.id}" }) { contact ->
            EmergencyContactCard(
                contact = contact,
                language = language,
                onDial = {
                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse(
                            "tel:${contact.phoneNumber.replace(" ", "").replace("(", "").replace(")", "")}"
                        )
                    }
                    context.startActivity(dialIntent)
                },
                onEmail = contact.email?.let { addr ->
                    {
                        try {
                            val mailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$addr"))
                            context.startActivity(mailIntent)
                        } catch (_: Exception) {
                            onShowMessage("No email app installed")
                        }
                    }
                },
                onOpenWebsite = contact.websiteUrl?.let { url ->
                    {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        } catch (_: Exception) {
                            onShowMessage("No browser available")
                        }
                    }
                }
            )
        }
    }
    }

    if (isAdmin && showTransitDialog) {
        ComposeTransitDialog(
            language = language,
            initial = editingMethod,
            onDismiss = {
                showTransitDialog = false
                editingMethod = null
            },
            onSave = { name, mode, op, route, sched, fare, dur, term, tip, status ->
                val target = editingMethod
                if (target == null) {
                    onAddTransitMethod(name, mode, op, route, sched, fare, dur, term, tip, status)
                } else {
                    onUpdateTransitMethod(target.id, name, mode, op, route, sched, fare, dur, term, tip, status)
                }
                showTransitDialog = false
                editingMethod = null
            }
        )
    }
}
