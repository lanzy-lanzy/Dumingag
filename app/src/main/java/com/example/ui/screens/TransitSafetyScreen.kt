package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.EmergencyContact
import com.example.model.TransitMethod
import com.example.model.TransitModeCategory
import com.example.model.UiState
import com.example.notifications.TravelAlertManager
import com.example.ui.theme.WeatherCloudyBottom
import com.example.ui.theme.WeatherGlassBorder
import com.example.ui.theme.WeatherGlassStrong
import com.example.ui.theme.WeatherGlassSurface
import com.example.ui.theme.WeatherOnCardMuted
import com.example.ui.theme.WeatherOnCardPrimary
import com.example.ui.theme.WeatherSunGlow
import com.example.ui.theme.WeatherSunnyMid
import com.example.ui.theme.WeatherSunnyTop

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransitSafetyScreen(
    language: AppLanguage,
    transitState: UiState<List<TransitMethod>>,
    emergencyContacts: List<EmergencyContact>,
    currentUserId: String,
    isAdmin: Boolean,
    isEffectiveOffline: Boolean,
    onRefreshTransit: () -> Unit,
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
    onShowMessage: (String) -> Unit
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

    val routeCount = (transitState as? UiState.Success)?.data?.size ?: 0

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useTwoColumnGrid = maxWidth >= 600.dp
        val isCompactNarrow = maxWidth < 340.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                TransitHeroBanner(
                    title = AppStrings.transitTitle(language),
                    routeCount = routeCount,
                    isEffectiveOffline = isEffectiveOffline,
                    stackActions = isCompactNarrow,
                    onRefreshTransit = onRefreshTransit,
                    onAddRoute = {
                        editingMethod = null
                        showTransitDialog = true
                    }
                )
            }

            item {
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
                    if (useTwoColumnGrid) {
                        items(2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TransitSkeletonCard(modifier = Modifier.weight(1f))
                                TransitSkeletonCard(modifier = Modifier.weight(1f))
                            }
                        }
                    } else {
                        items(3) {
                            TransitSkeletonCard()
                        }
                    }
                }

                is UiState.Error -> {
                    item {
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
                        item {
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
                                            "Preparing live Dumingag transit schedules..."
                                        } else {
                                            "No routes posted for this transport category yet."
                                        },
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Tap Sync to fetch the latest schedules or Add Route to share an updated terminal timetable.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else if (useTwoColumnGrid) {
                        val rows = filteredMethods.chunked(2)
                        items(rows, key = { pair -> pair.joinToString("_") { it.id } }) { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                pair.forEach { method ->
                                    TransitMethodCard(
                                        method = method,
                                        language = language,
                                        isOwner = method.userId == currentUserId,
                                        onEdit = {
                                            editingMethod = method
                                            showTransitDialog = true
                                        },
                                        onDelete = {
                                            onDeleteTransitMethod(method.id)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        items(filteredMethods, key = { it.id }) { method ->
                            TransitMethodCard(
                                method = method,
                                language = language,
                                isOwner = method.userId == currentUserId,
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

            item {
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

            item {
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

            if (useTwoColumnGrid) {
                val contactRows = emergencyContacts.chunked(2)
                items(contactRows, key = { pair -> pair.joinToString("_") { it.id } }) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { contact ->
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
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                items(emergencyContacts, key = { it.id }) { contact ->
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
    }

    if (showTransitDialog) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TransitHeroBanner(
    title: String,
    routeCount: Int,
    isEffectiveOffline: Boolean,
    stackActions: Boolean,
    onRefreshTransit: () -> Unit,
    onAddRoute: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(26.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(WeatherSunnyTop, WeatherSunnyMid, WeatherCloudyBottom)
                    )
                )
                .drawBehind {
                    drawCircle(
                        color = WeatherSunGlow.copy(alpha = 0.14f),
                        radius = size.minDimension * 0.45f,
                        center = Offset(size.width * 0.9f, size.height * 0.2f)
                    )
                    drawLine(
                        color = WeatherGlassBorder,
                        start = Offset(size.width * 0.08f, size.height * 0.88f),
                        end = Offset(size.width * 0.92f, size.height * 0.88f),
                        strokeWidth = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                    )
                }
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Surface(
                            color = WeatherGlassStrong,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, WeatherGlassBorder),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsBus,
                                    contentDescription = null,
                                    tint = WeatherSunGlow,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = WeatherOnCardPrimary
                            )
                            Text(
                                text = "Dumingag Terminal & Upland Mobility",
                                style = MaterialTheme.typography.bodySmall,
                                color = WeatherOnCardMuted
                            )
                        }
                    }

                    Surface(
                        color = WeatherGlassSurface,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, WeatherGlassBorder),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isEffectiveOffline) WeatherOnCardMuted else WeatherSunGlow)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (isEffectiveOffline) Icons.Default.CloudOff else Icons.Default.CloudDone,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = WeatherOnCardPrimary
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isEffectiveOffline) {
                                    "Offline Cache"
                                } else if (routeCount > 0) {
                                    "Live · $routeCount routes"
                                } else {
                                    "Live Firestore"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = WeatherOnCardPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }

                if (stackActions) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onRefreshTransit,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WeatherOnCardPrimary),
                            border = BorderStroke(1.dp, WeatherGlassBorder),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("transit_refresh_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh transit schedules", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync Schedules", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        FilledTonalButton(
                            onClick = onAddRoute,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = WeatherGlassStrong,
                                contentColor = WeatherOnCardPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("transit_add_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add transit route", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Route", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRefreshTransit,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WeatherOnCardPrimary),
                            border = BorderStroke(1.dp, WeatherGlassBorder),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("transit_refresh_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh transit schedules", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        FilledTonalButton(
                            onClick = onAddRoute,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = WeatherGlassStrong,
                                contentColor = WeatherOnCardPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("transit_add_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add transit route", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Route", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
