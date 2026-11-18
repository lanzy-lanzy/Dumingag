package com.example.ui.screens

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.EmergencyContact
import com.example.model.TransitMethod
import com.example.model.TransitModeCategory
import com.example.model.TransitServiceStatus

@Composable
fun TravelAlertsCard(
    onTriggerWeatherAlert: () -> Unit,
    onTriggerTransitAlert: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dumingag Travel & Weather Push Alerts",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Dispatch instant local notifications for upland weather or last-trip advisories.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                    )
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val stackButtons = maxWidth < 340.dp
                if (stackButtons) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onTriggerWeatherAlert,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("send_weather_alert_button")
                        ) {
                            Text("Weather Alert", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        FilledTonalButton(
                            onClick = onTriggerTransitAlert,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("send_transit_alert_button")
                        ) {
                            Text("Transit Alert", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onTriggerWeatherAlert,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("send_weather_alert_button")
                        ) {
                            Text("Weather Alert", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        FilledTonalButton(
                            onClick = onTriggerTransitAlert,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("send_transit_alert_button")
                        ) {
                            Text("Transit Alert", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmergencyContactCard(
    contact: EmergencyContact,
    language: AppLanguage,
    onDial: () -> Unit,
    onEmail: (() -> Unit)?,
    onOpenWebsite: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val isCritical = contact.priorityLevel == "CRITICAL" || contact.priorityLevel == "HIGH"
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (contact.priorityLevel == "CRITICAL") {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isCritical) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
            }
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    color = if (isCritical) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = if (contact.priorityLevel == "MEDICAL") Icons.Default.LocalHospital else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isCritical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = contact.title.get(language),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${contact.department} · ${contact.availability}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = contact.phoneNumber,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isCritical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    contact.email?.let { mail ->
                        Text(
                            text = mail,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onDial,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCritical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .heightIn(min = 48.dp)
                        .testTag("dial_emergency_${contact.id}")
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Call ${contact.phoneNumber}", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Call ${contact.phoneNumber}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                if (onEmail != null) {
                    OutlinedButton(
                        onClick = onEmail,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = "Email ${contact.email}", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Email", maxLines = 1)
                    }
                }

                if (onOpenWebsite != null) {
                    TextButton(
                        onClick = onOpenWebsite,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Portal", maxLines = 1)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComposeTransitDialog(
    language: AppLanguage,
    initial: TransitMethod?,
    onDismiss: () -> Unit,
    onSave: (
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
    ) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var modeCategory by remember(initial) {
        mutableStateOf(
            initial?.let { TransitModeCategory.fromCode(it.modeCategory) }
                ?: TransitModeCategory.TRICYCLE
        )
    }
    var serviceStatus by remember(initial) {
        mutableStateOf(
            initial?.let { TransitServiceStatus.fromCode(it.serviceStatus) }
                ?: TransitServiceStatus.OPERATIONAL
        )
    }
    var operatorName by remember(initial) { mutableStateOf(initial?.operatorName ?: "") }
    var routeSummary by remember(initial) { mutableStateOf(initial?.routeSummary ?: "") }
    var scheduleText by remember(initial) { mutableStateOf(initial?.scheduleText ?: "") }
    var fareRangePhp by remember(initial) { mutableStateOf(initial?.fareRangePhp ?: "") }
    var durationText by remember(initial) { mutableStateOf(initial?.durationText ?: "") }
    var terminalLocation by remember(initial) { mutableStateOf(initial?.terminalLocation ?: "") }
    var travelTip by remember(initial) { mutableStateOf(initial?.travelTip ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "Add Dumingag Transit Route" else "Update Transit Route",
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
                Text(
                    text = "Transport Type",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TransitModeCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = modeCategory == cat,
                            onClick = { modeCategory = cat },
                            label = { Text(AppStrings.transitModeLabel(cat, language), maxLines = 1) },
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .heightIn(min = 48.dp)
                        )
                    }
                }

                Text(
                    text = "Service Status",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TransitServiceStatus.entries.forEach { status ->
                        FilterChip(
                            selected = serviceStatus == status,
                            onClick = { serviceStatus = status },
                            label = { Text(AppStrings.transitStatusLabel(status, language), maxLines = 1) },
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .heightIn(min = 48.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(120) },
                    label = { Text("Transport / Route Name*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = operatorName,
                    onValueChange = { operatorName = it.take(100) },
                    label = { Text("Operator / Association*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = routeSummary,
                    onValueChange = { routeSummary = it.take(240) },
                    label = { Text("Route Coverage*") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = scheduleText,
                    onValueChange = { scheduleText = it.take(160) },
                    label = { Text("Operating Schedule*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fareRangePhp,
                    onValueChange = { fareRangePhp = it.take(60) },
                    label = { Text("Fare Range (e.g., ₱15 - ₱40)*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it.take(60) },
                    label = { Text("Estimated Travel Time*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = terminalLocation,
                    onValueChange = { terminalLocation = it.take(160) },
                    label = { Text("Terminal / Pickup Point*") },
                    minLines = 2,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = travelTip,
                    onValueChange = { travelTip = it.take(400) },
                    label = { Text("Traveler Tip (optional)") },
                    minLines = 2,
                    maxLines = 3,
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
                    val cleanName = name.trim()
                    val cleanOp = operatorName.trim()
                    val cleanRoute = routeSummary.trim()
                    val cleanSched = scheduleText.trim()
                    val cleanFare = fareRangePhp.trim()
                    val cleanDur = durationText.trim()
                    val cleanTerm = terminalLocation.trim()
                    when {
                        cleanName.length < 2 -> validationError = "Enter a transport name (min 2 chars)."
                        cleanOp.length < 2 -> validationError = "Enter an operator or TODA name."
                        cleanRoute.length < 3 -> validationError = "Describe the route coverage."
                        cleanSched.length < 3 -> validationError = "Enter operating hours or intervals."
                        cleanFare.isEmpty() -> validationError = "Enter the fare range in PHP."
                        cleanDur.isEmpty() -> validationError = "Enter estimated travel duration."
                        cleanTerm.length < 2 -> validationError = "Enter the terminal or pickup location."
                        else -> {
                            onSave(
                                cleanName,
                                modeCategory.code,
                                cleanOp,
                                cleanRoute,
                                cleanSched,
                                cleanFare,
                                cleanDur,
                                cleanTerm,
                                travelTip.trim(),
                                serviceStatus.code
                            )
                        }
                    }
                },
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("transit_dialog_save")
            ) {
                Text(if (initial == null) "Publish" else "Save", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("transit_dialog_cancel")
            ) {
                Text("Cancel", maxLines = 1)
            }
        }
    )
}
