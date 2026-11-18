package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.DailyForecast
import com.example.model.UiState
import com.example.model.WeatherSnapshot
import com.example.ui.theme.WeatherBarTrack
import com.example.ui.theme.WeatherCloudyBottom
import com.example.ui.theme.WeatherCloudyMid
import com.example.ui.theme.WeatherCloudyTop
import com.example.ui.theme.WeatherCoolAccent
import com.example.ui.theme.WeatherGlassBorder
import com.example.ui.theme.WeatherGlassStrong
import com.example.ui.theme.WeatherGlassSurface
import com.example.ui.theme.WeatherMistAccent
import com.example.ui.theme.WeatherOnCardMuted
import com.example.ui.theme.WeatherOnCardPrimary
import com.example.ui.theme.WeatherRainAccent
import com.example.ui.theme.WeatherRainBottom
import com.example.ui.theme.WeatherRainMid
import com.example.ui.theme.WeatherRainTop
import com.example.ui.theme.WeatherSunGlow
import com.example.ui.theme.WeatherSunnyBottom
import com.example.ui.theme.WeatherSunnyMid
import com.example.ui.theme.WeatherSunnyTop
import com.example.ui.theme.WeatherWarmAccent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun DumingagWeatherForecastCard(
    language: AppLanguage,
    weatherState: UiState<WeatherSnapshot>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (weatherState) {
        is UiState.Loading -> WeatherLoadingCard(modifier = modifier)
        is UiState.Error -> WeatherErrorCard(
            message = weatherState.message,
            onRefresh = onRefresh,
            modifier = modifier
        )
        is UiState.Success -> WeatherSuccessCard(
            language = language,
            snapshot = weatherState.data,
            onRefresh = onRefresh,
            modifier = modifier
        )
    }
}

@Composable
private fun WeatherLoadingCard(modifier: Modifier = Modifier) {
    val pulseTransition = rememberInfiniteTransition(label = "weatherPulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "weatherPulseAlpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weather_loading_card"),
        shape = RoundedCornerShape(26.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(WeatherSunnyTop, WeatherSunnyMid, WeatherSunnyBottom)
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = WeatherSunGlow,
                            modifier = Modifier
                                .size(28.dp)
                                .alpha(pulseAlpha)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Dumingag, Zamboanga del Sur",
                                style = MaterialTheme.typography.titleSmall,
                                color = WeatherOnCardPrimary
                            )
                            Text(
                                text = "Syncing OpenWeatherMap & 3-day outlook...",
                                style = MaterialTheme.typography.bodySmall,
                                color = WeatherOnCardMuted
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(3) {
                        Surface(
                            color = WeatherGlassSurface,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(92.dp)
                                .alpha(pulseAlpha)
                        ) {}
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherErrorCard(
    message: String,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weather_error_card"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Unable to refresh Dumingag weather",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("retry_weather_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Retry Forecast", maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeatherSuccessCard(
    language: AppLanguage,
    snapshot: WeatherSnapshot,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = weatherPaletteForCode(snapshot.weatherCode)
    val forecasts = snapshot.dailyForecasts.take(3)
    var selectedDayIndex by remember(snapshot.updatedAtMillis) { mutableIntStateOf(-1) }

    val timeFmt = remember {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("Asia/Manila")
        }
    }
    val conditionText = snapshot.conditionDescription.takeIf { it.isNotBlank() }
        ?: AppStrings.weatherConditionText(snapshot.weatherCode, language)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weather_forecast_card"),
        shape = RoundedCornerShape(26.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(palette.top, palette.mid, palette.bottom)
                    )
                )
                .drawBehind {
                    drawAtmosphericBackdrop(
                        weatherCode = snapshot.weatherCode,
                        accent = palette.accent
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header: Location + Live Source Pill + Refresh Action
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = palette.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Dumingag, Zamboanga del Sur",
                                style = MaterialTheme.typography.titleSmall,
                                color = WeatherOnCardPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val statusText = when {
                            snapshot.isApiKeyMissing -> "${snapshot.sourceLabel} · Set OPENWEATHERMAP_API_KEY"
                            snapshot.isFromCache -> "${snapshot.sourceLabel} · ${timeFmt.format(Date(snapshot.updatedAtMillis))}"
                            else -> "Live OpenWeatherMap · ${timeFmt.format(Date(snapshot.updatedAtMillis))}"
                        }
                        Surface(
                            color = WeatherGlassSurface,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, WeatherGlassBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (!snapshot.isFromCache && !snapshot.isApiKeyMissing) {
                                                WeatherSunGlow
                                            } else {
                                                palette.accent
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WeatherOnCardMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Surface(
                        color = WeatherGlassSurface,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, WeatherGlassBorder),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("refresh_weather_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh weather",
                                tint = WeatherOnCardPrimary
                            )
                        }
                    }
                }

                // Hero Current Conditions Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "${snapshot.temperatureC.roundToInt()}°C",
                            style = MaterialTheme.typography.displayLarge,
                            color = WeatherOnCardPrimary
                        )
                        Text(
                            text = conditionText,
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.accent,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Feels ${snapshot.apparentTempC.roundToInt()}° · H ${snapshot.highTempC.roundToInt()}° / L ${snapshot.lowTempC.roundToInt()}°",
                            style = MaterialTheme.typography.labelSmall,
                            color = WeatherOnCardMuted
                        )
                    }

                    Surface(
                        color = WeatherGlassStrong,
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, WeatherGlassBorder),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = weatherIconForCode(snapshot.weatherCode),
                                contentDescription = conditionText,
                                tint = palette.accent,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                // Telemetry Glass Pills: Humidity, Wind, Pressure
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WeatherTelemetryPill(
                        icon = Icons.Default.WaterDrop,
                        label = "Humidity",
                        value = "${snapshot.humidityPct}%",
                        accent = WeatherRainAccent,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    WeatherTelemetryPill(
                        icon = Icons.Default.Air,
                        label = "Wind",
                        value = "${snapshot.windSpeedKmh.roundToInt()} km/h",
                        accent = WeatherMistAccent,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    WeatherTelemetryPill(
                        icon = Icons.Default.Compress,
                        label = "Pressure",
                        value = "${snapshot.pressureHpa} hPa",
                        accent = WeatherSunGlow,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }

                // Travel & Trail Readiness Banner
                Surface(
                    color = WeatherGlassSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, WeatherGlassBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (snapshot.weatherCode >= 51) Icons.Default.Umbrella else Icons.Default.Eco,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = AppStrings.weatherTravelAdvisory(snapshot.weatherCode, language),
                            style = MaterialTheme.typography.bodySmall,
                            color = WeatherOnCardPrimary
                        )
                    }
                }

                // 3-Day Forecast Section
                if (forecasts.isNotEmpty()) {
                    val spanMin = forecasts.minOf { it.tempMinC }
                    val spanMax = forecasts.maxOf { it.tempMaxC }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = AppStrings.threeDayForecastTitle(language),
                                style = MaterialTheme.typography.titleSmall,
                                color = WeatherOnCardPrimary
                            )
                            Text(
                                text = "Tap day for details",
                                style = MaterialTheme.typography.labelSmall,
                                color = WeatherOnCardMuted
                            )
                        }

                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val useThreeColumnRow = maxWidth >= 300.dp
                            if (useThreeColumnRow) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    forecasts.forEachIndexed { idx, day ->
                                        ForecastDayCard(
                                            day = day,
                                            spanMinC = spanMin,
                                            spanMaxC = spanMax,
                                            isSelected = selectedDayIndex == idx,
                                            onClick = {
                                                selectedDayIndex = if (selectedDayIndex == idx) -1 else idx
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("forecast_day_card_$idx")
                                        )
                                    }
                                }
                            } else {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    itemsIndexed(forecasts) { idx, day ->
                                        ForecastDayCard(
                                            day = day,
                                            spanMinC = spanMin,
                                            spanMaxC = spanMax,
                                            isSelected = selectedDayIndex == idx,
                                            onClick = {
                                                selectedDayIndex = if (selectedDayIndex == idx) -1 else idx
                                            },
                                            modifier = Modifier
                                                .width(124.dp)
                                                .testTag("forecast_day_card_$idx")
                                        )
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = selectedDayIndex in forecasts.indices,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            val activeDay = forecasts.getOrNull(selectedDayIndex)
                            if (activeDay != null) {
                                val (dayName, dateShort) = formatForecastDate(activeDay.dateIso)
                                val dayCondition = activeDay.conditionLabel.ifBlank {
                                    AppStrings.weatherConditionText(activeDay.weatherCode, language)
                                }
                                Surface(
                                    color = WeatherGlassStrong,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, WeatherGlassBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "$dayName ($dateShort) · $dayCondition",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = WeatherOnCardPrimary
                                        )
                                        Text(
                                            text = "High ${activeDay.tempMaxC.roundToInt()}°C · Low ${activeDay.tempMinC.roundToInt()}°C · Rain ${activeDay.precipChancePct}% · Humidity ${activeDay.humidityPct}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = WeatherOnCardMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherTelemetryPill(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = WeatherGlassSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, WeatherGlassBorder),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = WeatherOnCardMuted
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelMedium,
                    color = WeatherOnCardPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ForecastDayCard(
    day: DailyForecast,
    spanMinC: Double,
    spanMaxC: Double,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (dayLabel, dateLabel) = remember(day.dateIso) { formatForecastDate(day.dateIso) }
    val conditionAccent = weatherAccentForCode(day.weatherCode)

    Surface(
        color = if (isSelected) WeatherGlassStrong else WeatherGlassSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) conditionAccent else WeatherGlassBorder
        ),
        modifier = modifier
            .heightIn(min = 128.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = dayLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = WeatherOnCardPrimary,
                    maxLines = 1
                )
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = WeatherOnCardMuted,
                    maxLines = 1
                )
            }

            Icon(
                imageVector = weatherIconForCode(day.weatherCode),
                contentDescription = day.conditionLabel,
                tint = conditionAccent,
                modifier = Modifier.size(26.dp)
            )

            Text(
                text = day.conditionLabel.ifBlank { "Forecast" },
                style = MaterialTheme.typography.bodySmall,
                color = WeatherOnCardMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Normalized temperature range bar
            val totalSpan = max(1.0, spanMaxC - spanMinC)
            val startFraction = ((day.tempMinC - spanMinC) / totalSpan).toFloat().coerceIn(0f, 0.85f)
            val endFraction = ((day.tempMaxC - spanMinC) / totalSpan).toFloat().coerceIn(startFraction + 0.15f, 1f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            ) {
                drawLine(
                    color = WeatherBarTrack,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = size.height,
                    cap = StrokeCap.Round
                )
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(WeatherCoolAccent, WeatherWarmAccent)
                    ),
                    start = Offset(size.width * startFraction, size.height / 2f),
                    end = Offset(size.width * endFraction, size.height / 2f),
                    strokeWidth = size.height,
                    cap = StrokeCap.Round
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${day.tempMinC.roundToInt()}°",
                    style = MaterialTheme.typography.labelSmall,
                    color = WeatherOnCardMuted
                )
                Text(
                    text = "${day.tempMaxC.roundToInt()}°",
                    style = MaterialTheme.typography.labelMedium,
                    color = WeatherOnCardPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = "Rain probability",
                    tint = WeatherRainAccent,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "${day.precipChancePct}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = WeatherRainAccent
                )
            }
        }
    }
}

private data class WeatherCardPalette(
    val top: Color,
    val mid: Color,
    val bottom: Color,
    val accent: Color
)

private fun weatherPaletteForCode(code: Int): WeatherCardPalette = when {
    code == 0 || code in 1..2 -> WeatherCardPalette(
        top = WeatherSunnyTop,
        mid = WeatherSunnyMid,
        bottom = WeatherSunnyBottom,
        accent = WeatherSunGlow
    )
    code in 51..99 -> WeatherCardPalette(
        top = WeatherRainTop,
        mid = WeatherRainMid,
        bottom = WeatherRainBottom,
        accent = WeatherRainAccent
    )
    else -> WeatherCardPalette(
        top = WeatherCloudyTop,
        mid = WeatherCloudyMid,
        bottom = WeatherCloudyBottom,
        accent = WeatherMistAccent
    )
}

private fun weatherAccentForCode(code: Int): Color = when {
    code == 0 -> WeatherSunGlow
    code in 1..3 -> WeatherSunGlow
    code in 45..48 -> WeatherMistAccent
    code >= 51 -> WeatherRainAccent
    else -> WeatherSunGlow
}

private fun weatherIconForCode(code: Int): ImageVector = when {
    code == 0 -> Icons.Default.WbSunny
    code in 1..3 -> Icons.Default.WbCloudy
    code in 45..48 -> Icons.Default.Cloud
    code in 95..99 -> Icons.Default.Thunderstorm
    code >= 51 -> Icons.Default.Umbrella
    else -> Icons.Default.WbSunny
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAtmosphericBackdrop(
    weatherCode: Int,
    accent: Color
) {
    // Glowing celestial / atmospheric halo top-right
    val haloCenter = Offset(size.width * 0.88f, size.height * 0.14f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accent.copy(alpha = 0.24f), Color.Transparent),
            center = haloCenter,
            radius = size.minDimension * 0.55f
        ),
        center = haloCenter,
        radius = size.minDimension * 0.55f
    )

    if (weatherCode >= 51) {
        // Subtle rain streaks
        for (i in 0..5) {
            val startX = size.width * (0.52f + i * 0.08f)
            val startY = size.height * (0.04f + (i % 2) * 0.05f)
            drawLine(
                color = accent.copy(alpha = 0.14f),
                start = Offset(startX, startY),
                end = Offset(startX - 18f, startY + 48f),
                strokeWidth = 2.5f,
                cap = StrokeCap.Round
            )
        }
    }

    // Gentle Dumingag highland silhouette at card base
    val ridgePath = Path().apply {
        moveTo(0f, size.height)
        lineTo(0f, size.height * 0.84f)
        cubicTo(
            size.width * 0.24f, size.height * 0.72f,
            size.width * 0.48f, size.height * 0.90f,
            size.width * 0.72f, size.height * 0.78f
        )
        cubicTo(
            size.width * 0.86f, size.height * 0.71f,
            size.width * 0.95f, size.height * 0.80f,
            size.width, size.height * 0.76f
        )
        lineTo(size.width, size.height)
        close()
    }
    drawPath(
        path = ridgePath,
        color = Color.Black.copy(alpha = 0.12f)
    )
}

private fun formatForecastDate(dateIso: String): Pair<String, String> {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Manila")
        }
        val parsed = parser.parse(dateIso) ?: return dateIso to ""
        val dayFmt = SimpleDateFormat("EEE", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Manila")
        }
        val shortFmt = SimpleDateFormat("MMM d", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Manila")
        }
        dayFmt.format(parsed) to shortFmt.format(parsed)
    } catch (_: Exception) {
        dateIso.take(3) to dateIso.takeLast(5)
    }
}
