package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.model.AppLanguage
import com.example.model.DailyForecast
import com.example.model.UiState
import com.example.model.WeatherSnapshot
import com.example.ui.components.DumingagWeatherForecastCard
import com.example.ui.theme.DumingagTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WeatherCardTest {
    @get:Rule val compose = createComposeRule()
    private var refreshes = 0
    private val weather = WeatherSnapshot(
        temperatureC = 27.0, apparentTempC = 29.0, humidityPct = 78, windSpeedKmh = 10.0,
        weatherCode = 2, isFromCache = false, updatedAtMillis = 1_795_068_000_000,
        dailyForecasts = (20..22).map {
            DailyForecast("2026-11-$it", 2, 30.0, 22.0, 40, "Partly cloudy", 76)
        }, sourceLabel = "Open-Meteo", isApiKeyMissing = true
    )

    private fun show(snapshot: WeatherSnapshot) {
        compose.setContent {
            DumingagTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    DumingagWeatherForecastCard(AppLanguage.EN, UiState.Success(snapshot), { refreshes++ })
                }
            }
        }
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp")
    fun phoneShowsStackedDaysAndOpensForecastDetails() {
        show(weather)
        compose.onNodeWithText("Open-Meteo", substring = true).assertExists()
        compose.onNodeWithTag("refresh_weather_button").performClick()
        assertEquals(1, refreshes)
        val first = compose.onNodeWithTag("forecast_day_card_0").performScrollTo()
        val second = compose.onNodeWithTag("forecast_day_card_1")
        assertTrue(second.getUnclippedBoundsInRoot().top > first.getUnclippedBoundsInRoot().top)
        first.performClick()
        compose.onNodeWithText("Humidity 76%", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w960dp-h720dp-land")
    fun tabletPlacesSampleDaysSideBySideAndLabelsThem() {
        show(weather.copy(isSample = true, isFromCache = true))
        compose.onNodeWithText("Sample forecast", substring = true).assertExists()
        val first = compose.onNodeWithTag("forecast_day_card_0").fetchSemanticsNode()
        val last = compose.onNodeWithTag("forecast_day_card_2").fetchSemanticsNode()
        assertEquals(first.boundsInRoot.top, last.boundsInRoot.top)
        assertTrue(last.boundsInRoot.left > first.boundsInRoot.left)
    }
}
