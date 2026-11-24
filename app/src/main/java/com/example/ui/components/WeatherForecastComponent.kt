package com.example.ui.components

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.DailyForecast
import com.example.model.UiState
import com.example.model.WeatherSnapshot
import com.example.ui.theme.SplashGlassMintAccent
import com.example.ui.theme.SplashGlassSpecularEdge
import com.example.ui.theme.SplashGlassSpecularMid
import com.example.ui.theme.SplashGlassSpecularTop
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

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
    val cardShape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, cardShape, ambientColor = Color.Black.copy(alpha = 0.24f))
            .clip(cardShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        WeatherSunnyTop.copy(alpha = 0.88f),
                        WeatherSunnyMid.copy(alpha = 0.82f),
                        WeatherSunnyBottom.copy(alpha = 0.86f)
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(SplashGlassSpecularTop, SplashGlassSpecularMid, SplashGlassSpecularEdge)
                ),
                shape = cardShape
            )
            .padding(14.dp)
            .testTag("weather_loading_card")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            .size(22.dp)
                            .alpha(pulseAlpha)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Dumingag Weather",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = WeatherOnCardPrimary
                        )
                        Text(
                            text = "Updating forecast…",
                            style = MaterialTheme.typography.labelSmall,
                            color = WeatherOnCardMuted
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) {
                    Surface(
                        color = WeatherGlassSurface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .alpha(pulseAlpha)
                    ) {}
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
        shape = RoundedCornerShape(24.dp),
        border = liquidGlassBorder(),
        colors = liquidGlassCardColors(
            baseColor = MaterialTheme.colorScheme.errorContainer,
            lightAlpha = 0.86f,
            darkAlpha = 0.80f
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Weather unavailable",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
                Spacer(modifier = Modifier.width(4.dp))
                Text("Retry", maxLines = 1)
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

    // Shared atmospheric motion clock driving clouds, falling rain, wind gusts, and droplet pulse
    val weatherMotion = rememberInfiniteTransition(label = "weather_atmospheric_motion")
    val continuousCycle by weatherMotion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "weather_cycle"
    )
    val gentleWave by weatherMotion.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "weather_gentle_wave"
    )
    val dropBounce by weatherMotion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "weather_drop_bounce"
    )
    val windGust by weatherMotion.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "weather_wind_gust"
    )

    val timeFormat = remember {
        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("Asia/Manila")
        }
    }
    val status = when {
        snapshot.isSample -> "Sample forecast · Offline"
        snapshot.isFromCache -> "Saved · ${timeFormat.format(Date(snapshot.updatedAtMillis))}"
        else -> "${snapshot.sourceLabel} · ${timeFormat.format(Date(snapshot.updatedAtMillis))}"
    }
    val condition = snapshot.conditionDescription.ifBlank {
        AppStrings.weatherConditionText(snapshot.weatherCode, language)
    }

    val cardShape = RoundedCornerShape(26.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = cardShape,
                ambientColor = Color.Black.copy(alpha = 0.26f),
                spotColor = Color.Black.copy(alpha = 0.26f)
            )
            .clip(cardShape)
            .testTag("weather_forecast_card")
    ) {
        // Blurred highland backdrop refracted behind the iOS Weather Liquid Glass shell
        Image(
            painter = painterResource(R.drawable.img_dumingag_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(radius = 16.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    } else {
                        Modifier
                    }
                )
        )

        // Translucent iOS Liquid Glass atmospheric frost + live ambient clouds/rain/caustics
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            palette.top.copy(alpha = 0.86f),
                            palette.mid.copy(alpha = 0.80f),
                            palette.top.copy(alpha = 0.90f)
                        )
                    )
                )
                .drawBehind {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                SplashGlassSpecularTop.copy(alpha = 0.26f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.26f
                        ),
                        cornerRadius = CornerRadius(26.dp.toPx(), 26.dp.toPx())
                    )
                    // Drifting ambient atmospheric cloud/sun glow orb
                    val glowShiftX = size.width * 0.06f * gentleWave
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(palette.accent.copy(alpha = 0.20f), Color.Transparent),
                            center = Offset(size.width * 0.86f + glowShiftX, size.height * 0.16f),
                            radius = size.width * 0.48f
                        )
                    )
                    // Subtle ambient drifting rain streaks when rainy or high precipitation
                    if (snapshot.weatherCode >= 51) {
                        val dropCount = 8
                        for (i in 0 until dropCount) {
                            val progress = (continuousCycle + i * 0.13f) % 1f
                            val startX = size.width * (0.12f + i * 0.11f) - progress * 14.dp.toPx()
                            val startY = progress * (size.height + 24.dp.toPx()) - 18.dp.toPx()
                            drawLine(
                                color = Color.White.copy(alpha = 0.14f),
                                start = Offset(startX, startY),
                                end = Offset(startX - 4.dp.toPx(), startY + 12.dp.toPx()),
                                strokeWidth = 1.2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    } else if (snapshot.weatherCode in 1..48) {
                        // Soft drifting cloud mist lobe
                        val mistX = size.width * (0.28f + 0.10f * gentleWave)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
                                center = Offset(mistX, size.height * 0.22f),
                                radius = size.width * 0.40f
                            ),
                            center = Offset(mistX, size.height * 0.22f),
                            radius = size.width * 0.40f
                        )
                    }
                }
                .border(
                    width = 1.25.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            SplashGlassSpecularTop.copy(alpha = 0.90f),
                            SplashGlassSpecularMid,
                            SplashGlassSpecularEdge
                        )
                    ),
                    shape = cardShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Compact iOS Weather Top Row: Location & Condition on left, Animated Icon + Temp & Refresh on right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Dumingag",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = WeatherOnCardPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = condition,
                        style = MaterialTheme.typography.labelMedium,
                        color = WeatherOnCardPrimary.copy(alpha = 0.94f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AnimatedWeatherConditionIcon(
                        weatherCode = snapshot.weatherCode,
                        tint = palette.accent,
                        size = 28.dp,
                        contentDescription = null,
                        continuousCycle = continuousCycle,
                        gentleWave = gentleWave,
                        dropBounce = dropBounce
                    )
                    Text(
                        text = "${snapshot.temperatureC.roundToInt()}°",
                        fontSize = 34.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WeatherOnCardPrimary
                    )
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("refresh_weather_button")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(WeatherGlassSurface)
                                .border(0.9.dp, SplashGlassSpecularTop.copy(alpha = 0.55f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh weather",
                                tint = WeatherOnCardPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Full-width static High/Low/Feels & Source/Timestamp lines (no marquee, no clipping)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "H:${snapshot.highTempC.roundToInt()}°  L:${snapshot.lowTempC.roundToInt()}° · Feels ${snapshot.apparentTempC.roundToInt()}°",
                    style = MaterialTheme.typography.labelSmall,
                    color = WeatherOnCardPrimary.copy(alpha = 0.90f),
                    maxLines = 2
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    color = SplashGlassMintAccent,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2
                )
            }

            // Compact inline frosted metrics strip with animated drop, wind, and advisory icons + marquee on long text
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CompactGlassMetricPill(
                    icon = Icons.Default.WaterDrop,
                    text = "${snapshot.humidityPct}%",
                    dropBounce = dropBounce,
                    windGust = windGust,
                    gentleWave = gentleWave
                )
                CompactGlassMetricPill(
                    icon = Icons.Default.Air,
                    text = "${snapshot.windSpeedKmh.roundToInt()} km/h",
                    dropBounce = dropBounce,
                    windGust = windGust,
                    gentleWave = gentleWave
                )
                if (!snapshot.isFromCache && !snapshot.isApiKeyMissing) {
                    CompactGlassMetricPill(
                        icon = Icons.Default.Compress,
                        text = "${snapshot.pressureHpa} hPa",
                        dropBounce = dropBounce,
                        windGust = windGust,
                        gentleWave = gentleWave
                    )
                }
                if (!snapshot.isSample) {
                    CompactGlassMetricPill(
                        icon = if (snapshot.weatherCode >= 51) OpenUmbrellaIcon else Icons.Default.Eco,
                        text = AppStrings.weatherTravelAdvisory(snapshot.weatherCode, language),
                        accentTint = SplashGlassMintAccent,
                        enableMarquee = true,
                        dropBounce = dropBounce,
                        windGust = windGust,
                        gentleWave = gentleWave
                    )
                }
            }

            // Compact 3-Day Glass Forecast Section with animated weather icons
            if (forecasts.isNotEmpty()) {
                HorizontalDivider(color = SplashGlassSpecularTop.copy(alpha = 0.22f))
                Text(
                    text = AppStrings.threeDayForecastTitle(language).uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = 0.8.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = WeatherOnCardPrimary.copy(alpha = 0.84f)
                )
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val minTileWidth = 148.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
                    val wide = maxWidth >= minTileWidth * forecasts.size + 6.dp * (forecasts.size - 1)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        forecasts.forEachIndexed { index, day ->
                            ForecastDayCard(
                                day = day,
                                language = language,
                                wide = wide,
                                selected = selectedDayIndex == index,
                                continuousCycle = (continuousCycle + index * 0.27f) % 1f,
                                gentleWave = gentleWave,
                                dropBounce = dropBounce,
                                onClick = { selectedDayIndex = if (selectedDayIndex == index) -1 else index },
                                modifier = (if (wide) Modifier.weight(1f) else Modifier.fillMaxWidth())
                                    .testTag("forecast_day_card_$index")
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Animated weather condition icon that adds smooth, condition-specific motion:
 * - Sun: gentle rotation & warm breathing scale
 * - Clouds / Mist: soft horizontal drift & hover bob
 * - Rain / Showers / Thunderstorm: swaying icon + animated falling raindrops
 */
@Composable
private fun AnimatedWeatherConditionIcon(
    weatherCode: Int,
    tint: Color,
    size: Dp,
    contentDescription: String?,
    continuousCycle: Float,
    gentleWave: Float,
    dropBounce: Float
) {
    val isRainy = weatherCode >= 51
    val isCloudy = weatherCode in 1..48
    val isSunny = weatherCode == 0

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size + 4.dp)
    ) {
        if (isRainy) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val dropColor = SplashGlassMintAccent.copy(alpha = 0.85f)
                for (i in 0..2) {
                    val phase = (continuousCycle * 1.6f + i * 0.33f) % 1f
                    val x = this.size.width * (0.26f + i * 0.24f) - phase * 2.5.dp.toPx()
                    val y = this.size.height * (0.16f + phase * 0.72f)
                    val alpha = sin(phase * PI).toFloat().coerceIn(0f, 1f)
                    drawLine(
                        color = dropColor.copy(alpha = alpha * 0.9f),
                        start = Offset(x, y),
                        end = Offset(x - 1.4.dp.toPx(), y + 4.2.dp.toPx()),
                        strokeWidth = 1.4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        Icon(
            imageVector = weatherIconForCode(weatherCode),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    when {
                        isSunny -> {
                            rotationZ = continuousCycle * 360f
                            val s = 0.95f + 0.09f * dropBounce
                            scaleX = s
                            scaleY = s
                        }
                        isRainy -> {
                            rotationZ = gentleWave * 6f
                            translationY = (dropBounce - 0.5f) * 2.2.dp.toPx()
                        }
                        isCloudy -> {
                            translationX = gentleWave * 2.6.dp.toPx()
                            translationY = cos(continuousCycle * 2f * PI.toFloat()) * 1.4.dp.toPx()
                        }
                        else -> {
                            val s = 0.96f + 0.06f * dropBounce
                            scaleX = s
                            scaleY = s
                        }
                    }
                }
        )
    }
}

@Composable
private fun CompactGlassMetricPill(
    icon: ImageVector,
    text: String,
    accentTint: Color = WeatherOnCardPrimary,
    enableMarquee: Boolean = false,
    dropBounce: Float = 0f,
    windGust: Float = 0f,
    gentleWave: Float = 0f
) {
    Surface(
        color = Color.White.copy(alpha = 0.15f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(0.85.dp, Color.White.copy(alpha = 0.26f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentTint,
                modifier = Modifier
                    .size(13.dp)
                    .graphicsLayer {
                        when (icon) {
                            Icons.Default.WaterDrop -> {
                                translationY = (dropBounce - 0.45f) * 2.8.dp.toPx()
                                scaleY = 0.92f + 0.14f * dropBounce
                                scaleX = 1.05f - 0.08f * dropBounce
                            }
                            Icons.Default.Air -> {
                                translationX = windGust * 2.4.dp.toPx()
                                alpha = 0.80f + 0.20f * ((windGust + 1f) * 0.5f)
                            }
                            OpenUmbrellaIcon, Icons.Default.Eco -> {
                                rotationZ = gentleWave * 7f
                            }
                            Icons.Default.Compress -> {
                                scaleY = 0.92f + 0.12f * dropBounce
                            }
                        }
                    }
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = WeatherOnCardPrimary,
                maxLines = 1,
                overflow = if (enableMarquee) TextOverflow.Clip else TextOverflow.Ellipsis,
                modifier = if (enableMarquee) {
                    Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        initialDelayMillis = 1200,
                        repeatDelayMillis = 1600,
                        velocity = 32.dp
                    )
                } else {
                    Modifier
                }
            )
        }
    }
}

@Composable
private fun ForecastDayCard(
    day: DailyForecast,
    language: AppLanguage,
    wide: Boolean,
    selected: Boolean,
    continuousCycle: Float,
    gentleWave: Float,
    dropBounce: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (weekday, date) = remember(day.dateIso) { formatForecastDate(day.dateIso) }
    val conditionLabel = day.conditionLabel.ifBlank {
        AppStrings.weatherConditionText(day.weatherCode, language)
    }

    Surface(
        onClick = onClick,
        modifier = modifier.semantics { stateDescription = if (selected) "Expanded" else "Collapsed" },
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Color.White.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.12f),
        border = BorderStroke(
            width = if (selected) 1.1.dp else 0.8.dp,
            color = if (selected) SplashGlassSpecularTop.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.20f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (wide) weekday else "$weekday · $date",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = WeatherOnCardPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                AnimatedWeatherConditionIcon(
                    weatherCode = day.weatherCode,
                    tint = WeatherSunGlow,
                    size = 17.dp,
                    contentDescription = conditionLabel,
                    continuousCycle = continuousCycle,
                    gentleWave = gentleWave,
                    dropBounce = dropBounce
                )
                Text(
                    text = "${day.precipChancePct}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = SplashGlassMintAccent,
                    maxLines = 1
                )
                Text(
                    text = "${day.tempMaxC.roundToInt()}°/${day.tempMinC.roundToInt()}°",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = WeatherOnCardPrimary,
                    maxLines = 1
                )
                Icon(
                    imageVector = if (selected) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = WeatherOnCardMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
            if (wide) {
                Text(
                    text = "$date · ${day.precipChancePct}% rain",
                    style = MaterialTheme.typography.labelSmall,
                    color = WeatherOnCardMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            AnimatedVisibility(visible = selected) {
                Text(
                    text = "$conditionLabel · High ${day.tempMaxC.roundToInt()}°C · Low ${day.tempMinC.roundToInt()}°C · Humidity ${day.humidityPct}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = WeatherOnCardPrimary.copy(alpha = 0.94f)
                )
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
    code >= 51 -> OpenUmbrellaIcon
    else -> Icons.Default.WbSunny
}

/**
 * Custom upright open rain umbrella icon (open scalloped canopy + top finial + J-hook handle)
 * replacing Material's default folded umbrella glyph.
 */
private val OpenUmbrellaIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "OpenUmbrella",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // Open umbrella scalloped dome canopy
        path(fill = SolidColor(Color.Black)) {
            moveTo(2f, 13.5f)
            arcTo(
                horizontalEllipseRadius = 10f,
                verticalEllipseRadius = 9.5f,
                theta = 0f,
                isMoreThanHalf = false,
                isPositiveArc = true,
                x1 = 22f,
                y1 = 13.5f
            )
            quadTo(18.67f, 11.2f, 15.33f, 13.5f)
            quadTo(12f, 11.2f, 8.67f, 13.5f)
            quadTo(5.33f, 11.2f, 2f, 13.5f)
            close()
        }
        // Top finial tip
        path(fill = SolidColor(Color.Black)) {
            moveTo(11.1f, 4.4f)
            lineTo(11.1f, 2.7f)
            arcTo(0.9f, 0.9f, 0f, isMoreThanHalf = false, isPositiveArc = true, 12.9f, 2.7f)
            lineTo(12.9f, 4.4f)
            close()
        }
        // Vertical stem & classic curved J-hook handle
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.1f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 12.2f)
            lineTo(12f, 18.8f)
            arcTo(
                horizontalEllipseRadius = 2.4f,
                verticalEllipseRadius = 2.4f,
                theta = 0f,
                isMoreThanHalf = false,
                isPositiveArc = true,
                x1 = 7.2f,
                y1 = 18.8f
            )
        }
    }.build()
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
