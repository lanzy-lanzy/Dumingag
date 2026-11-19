package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.example.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Eco
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.DailyForecast
import com.example.model.UiState
import com.example.model.WeatherSnapshot
import com.example.ui.theme.WeatherCloudyMid
import com.example.ui.theme.WeatherCloudyTop
import com.example.ui.theme.WeatherGlassSurface
import com.example.ui.theme.WeatherMistAccent
import com.example.ui.theme.WeatherOnCardMuted
import com.example.ui.theme.WeatherOnCardPrimary
import com.example.ui.theme.WeatherRainAccent
import com.example.ui.theme.WeatherRainMid
import com.example.ui.theme.WeatherRainTop
import com.example.ui.theme.WeatherSunGlow
import com.example.ui.theme.WeatherSunnyBottom
import com.example.ui.theme.WeatherSunnyMid
import com.example.ui.theme.WeatherSunnyTop
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
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
                                text = "Updating conditions and forecast…",
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

    val timeFormat = remember {
        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("Asia/Manila")
        }
    }
    val status = when {
        snapshot.isSample -> "Sample forecast · Live weather unavailable"
        snapshot.isFromCache -> "Saved forecast · ${timeFormat.format(Date(snapshot.updatedAtMillis))}"
        else -> "${snapshot.sourceLabel} · ${timeFormat.format(Date(snapshot.updatedAtMillis))}"
    }
    val condition = snapshot.conditionDescription.ifBlank {
        AppStrings.weatherConditionText(snapshot.weatherCode, language)
    }
    val colors = MaterialTheme.colorScheme
    Card(
        modifier.fillMaxWidth().testTag("weather_forecast_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Box(Modifier.fillMaxWidth()) {
            Image(
                painterResource(R.drawable.img_dumingag_hero), contentDescription = null,
                modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop
            )
            Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(
                palette.top.copy(alpha = .96f), palette.mid.copy(alpha = .88f), palette.top.copy(alpha = .68f)
            ))))
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Dumingag", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                        Text("Zamboanga del Sur", style = MaterialTheme.typography.bodySmall, color = WeatherOnCardMuted)
                    }
                    Surface(color = WeatherGlassSurface, shape = CircleShape) {
                        IconButton(onClick = onRefresh, modifier = Modifier.testTag("refresh_weather_button")) {
                            Icon(Icons.Default.Refresh, "Refresh weather", tint = Color.White)
                        }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("${snapshot.temperatureC.roundToInt()}°C", fontSize = 54.sp, lineHeight = 60.sp,
                        fontWeight = FontWeight.Medium, color = Color.White, modifier = Modifier.align(Alignment.CenterVertically))
                    Icon(weatherIconForCode(snapshot.weatherCode), null, tint = palette.accent,
                        modifier = Modifier.align(Alignment.CenterVertically).size(42.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(condition, style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text("Feels like ${snapshot.apparentTempC.roundToInt()}° · High ${snapshot.highTempC.roundToInt()}° / Low ${snapshot.lowTempC.roundToInt()}°",
                        style = MaterialTheme.typography.bodySmall, color = WeatherOnCardMuted)
                }
                Text(status, style = MaterialTheme.typography.labelMedium, color = Color.White)
            }
        }
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WeatherMetric(Icons.Default.WaterDrop, "Humidity", "${snapshot.humidityPct}%")
                WeatherMetric(Icons.Default.Air, "Wind", "${snapshot.windSpeedKmh.roundToInt()} km/h")
                if (!snapshot.isFromCache && !snapshot.isApiKeyMissing) {
                    WeatherMetric(Icons.Default.Compress, "Pressure", "${snapshot.pressureHpa} hPa")
                }
            }
            if (!snapshot.isSample) {
                Surface(color = colors.primaryContainer, shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (snapshot.weatherCode >= 51) Icons.Default.Umbrella else Icons.Default.Eco,
                            null, tint = colors.onPrimaryContainer, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(AppStrings.weatherTravelAdvisory(snapshot.weatherCode, language),
                            style = MaterialTheme.typography.bodySmall, color = colors.onPrimaryContainer)
                    }
                }
            }
            if (forecasts.isNotEmpty()) {
                Text(AppStrings.threeDayForecastTitle(language), style = MaterialTheme.typography.titleLarge)
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val minTileWidth = 156.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
                    val wide = maxWidth >= minTileWidth * forecasts.size + 8.dp * (forecasts.size - 1)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        forecasts.forEachIndexed { index, day ->
                            ForecastDayCard(day, language, wide, selectedDayIndex == index,
                                onClick = { selectedDayIndex = if (selectedDayIndex == index) -1 else index },
                                modifier = (if (wide) Modifier.weight(1f) else Modifier.fillMaxWidth())
                                    .testTag("forecast_day_card_$index"))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherMetric(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ForecastDayCard(
    day: DailyForecast,
    language: AppLanguage,
    wide: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (weekday, date) = remember(day.dateIso) { formatForecastDate(day.dateIso) }
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { stateDescription = if (selected) "Expanded" else "Collapsed" },
        shape = RoundedCornerShape(14.dp),
        color = if (selected) colors.primaryContainer else colors.surfaceVariant.copy(alpha = .55f)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(weekday, style = MaterialTheme.typography.titleMedium)
                    Text(date, style = MaterialTheme.typography.labelSmall)
                }
                Icon(weatherIconForCode(day.weatherCode), day.conditionLabel.ifBlank {
                    AppStrings.weatherConditionText(day.weatherCode, language)
                }, tint = colors.primary, modifier = Modifier.size(24.dp))
                if (!wide) {
                    Spacer(Modifier.width(10.dp))
                    Text("${day.tempMaxC.roundToInt()}° / ${day.tempMinC.roundToInt()}°",
                        style = MaterialTheme.typography.titleSmall)
                }
                Icon(if (selected) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, modifier = Modifier.size(18.dp))
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (wide) {
                    Text("${day.tempMaxC.roundToInt()}° / ${day.tempMinC.roundToInt()}°",
                        style = MaterialTheme.typography.titleSmall)
                }
                Text("${day.precipChancePct}% rain", style = MaterialTheme.typography.labelMedium,
                    color = colors.tertiary)
            }
            AnimatedVisibility(visible = selected) {
                Column {
                    Text(day.conditionLabel.ifBlank { AppStrings.weatherConditionText(day.weatherCode, language) },
                        style = MaterialTheme.typography.titleSmall)
                    Text("High ${day.tempMaxC.roundToInt()}°C · Low ${day.tempMinC.roundToInt()}°C · Humidity ${day.humidityPct}%",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private data class WeatherCardPalette(
    val top: Color,
    val mid: Color,
    val accent: Color
)

private fun weatherPaletteForCode(code: Int): WeatherCardPalette = when {
    code == 0 || code in 1..2 -> WeatherCardPalette(
        top = WeatherSunnyTop,
        mid = WeatherSunnyMid,
        accent = WeatherSunGlow
    )
    code in 51..99 -> WeatherCardPalette(
        top = WeatherRainTop,
        mid = WeatherRainMid,
        accent = WeatherRainAccent
    )
    else -> WeatherCardPalette(
        top = WeatherCloudyTop,
        mid = WeatherCloudyMid,
        accent = WeatherMistAccent
    )
}

private fun weatherIconForCode(code: Int): ImageVector = when {
    code == 0 -> Icons.Default.WbSunny
    code in 1..3 -> Icons.Default.WbCloudy
    code in 45..48 -> Icons.Default.Cloud
    code in 95..99 -> Icons.Default.Thunderstorm
    code >= 51 -> Icons.Default.Umbrella
    else -> Icons.Default.WbSunny
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
