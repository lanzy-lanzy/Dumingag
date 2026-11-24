package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirportShuttle
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.TransitMethod
import com.example.model.TransitModeCategory
import com.example.model.TransitServiceStatus
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.theme.LocalIsDarkTheme

fun transitModeIcon(category: TransitModeCategory): ImageVector = when (category) {
    TransitModeCategory.TRICYCLE -> Icons.Default.Commute
    TransitModeCategory.HABAL_HABAL -> Icons.Default.TwoWheeler
    TransitModeCategory.BUS_VAN -> Icons.Default.DirectionsBus
    TransitModeCategory.JEEPNEY_MULTICAB -> Icons.Default.AirportShuttle
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransitMethodCard(
    method: TransitMethod,
    language: AppLanguage,
    canManage: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val darkMode = LocalIsDarkTheme.current
    val category = TransitModeCategory.fromCode(method.modeCategory)
    val status = TransitServiceStatus.fromCode(method.serviceStatus)

    val modeContainer = when (category) {
        TransitModeCategory.TRICYCLE -> MaterialTheme.colorScheme.primaryContainer
        TransitModeCategory.HABAL_HABAL -> MaterialTheme.colorScheme.secondaryContainer
        TransitModeCategory.BUS_VAN -> MaterialTheme.colorScheme.tertiaryContainer
        TransitModeCategory.JEEPNEY_MULTICAB -> MaterialTheme.colorScheme.primaryContainer
    }
    val modeTint = when (category) {
        TransitModeCategory.TRICYCLE -> MaterialTheme.colorScheme.primary
        TransitModeCategory.HABAL_HABAL -> MaterialTheme.colorScheme.secondary
        TransitModeCategory.BUS_VAN -> MaterialTheme.colorScheme.tertiary
        TransitModeCategory.JEEPNEY_MULTICAB -> MaterialTheme.colorScheme.tertiary
    }

    val statusContainerColor = when (status) {
        TransitServiceStatus.OPERATIONAL -> MaterialTheme.colorScheme.primaryContainer
        TransitServiceStatus.LIMITED -> MaterialTheme.colorScheme.secondaryContainer
        TransitServiceStatus.ADVISORY -> MaterialTheme.colorScheme.errorContainer
    }
    val statusContentColor = when (status) {
        TransitServiceStatus.OPERATIONAL -> MaterialTheme.colorScheme.onPrimaryContainer
        TransitServiceStatus.LIMITED -> MaterialTheme.colorScheme.onSecondaryContainer
        TransitServiceStatus.ADVISORY -> MaterialTheme.colorScheme.onErrorContainer
    }
    val statusDotColor = when (status) {
        TransitServiceStatus.OPERATIONAL -> if (darkMode) Color(0xFF34D399) else MaterialTheme.colorScheme.primary
        TransitServiceStatus.LIMITED -> MaterialTheme.colorScheme.secondary
        TransitServiceStatus.ADVISORY -> MaterialTheme.colorScheme.error
    }

    LiquidGlassSurface(
        cornerRadius = 24.dp,
        elevation = 5.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("transit_card_${method.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Mode icon tile + Route name & operator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    modeContainer,
                                    modeContainer.copy(alpha = 0.65f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = modeTint.copy(alpha = 0.30f),
                            shape = RoundedCornerShape(15.dp)
                        )
                ) {
                    Icon(
                        imageVector = transitModeIcon(category),
                        contentDescription = AppStrings.transitModeLabel(category, language),
                        tint = modeTint,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = method.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${AppStrings.transitModeLabel(category, language)} · ${method.operatorName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Clean Fare, Status & Duration Ribbon
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FareRangePill(fareRange = method.fareRangePhp)

                Surface(
                    color = statusContainerColor.copy(alpha = if (darkMode) 0.78f else 0.90f),
                    contentColor = statusContentColor,
                    border = BorderStroke(0.9.dp, statusDotColor.copy(alpha = 0.30f)),
                    shape = RoundedCornerShape(50),
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
                                .background(statusDotColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = AppStrings.transitStatusLabel(status, language),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusContentColor,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (darkMode) 0.72f else 0.80f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = method.durationText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Sleek Frosted Route, Schedule & Terminal Panel
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (darkMode) 0.44f else 0.46f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(0.9.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (darkMode) 0.24f else 0.18f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(13.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Text(
                        text = method.routeSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))

                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(15.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = method.scheduleText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(15.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = method.terminalLocation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Refined Traveler Tip Callout
            if (method.travelTip.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (darkMode) 0.42f else 0.52f),
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(0.9.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.24f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(top = 1.dp)
                                .size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = method.travelTip,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            if (canManage) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onEdit,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("transit_edit_${method.id}")
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            "Edit",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(
                        onClick = onDelete,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("transit_delete_${method.id}")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            "Delete",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FareRangePill(fareRange: String) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = fareRange,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun TransitSkeletonCard(modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "transitSkeleton")
    val alpha by pulse.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "transitSkeletonAlpha"
    )

    LiquidGlassSurface(
        cornerRadius = 24.dp,
        elevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .alpha(alpha),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.size(46.dp)
                ) {}
                Spacer(modifier = Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(18.dp)
                    ) {}
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.35f)
                            .height(12.dp)
                    ) {}
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
            ) {}
        }
    }
}
