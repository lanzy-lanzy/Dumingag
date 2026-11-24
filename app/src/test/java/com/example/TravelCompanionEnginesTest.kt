package com.example

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.example.data.DumingagSeedData
import com.example.data.TravelCompanionEngines
import com.example.data.local.TravelCompanionDataStore
import com.example.model.AppThemeMode
import com.example.model.DailyForecast
import com.example.model.ItineraryItem
import com.example.model.WeatherSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TravelCompanionEnginesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun buildElevationProfileComputesPositiveClimbTowardMtTimolan() {
        val poblacion = 8.1650 to 123.3465
        val mtTimolan = 8.1360 to 123.3110

        val profile = TravelCompanionEngines.buildElevationProfile(
            waypoints = listOf(poblacion, mtTimolan),
            samplesPerSegment = 10
        )

        assertEquals(11, profile.samples.size)
        assertTrue("Expected Mt. Timolan climb max > min", profile.maxMeters > profile.minMeters)
        assertTrue("Expected positive elevation gain", profile.gainMeters > 300)
        assertTrue("Expected monotonically increasing distance", profile.samples.last().distanceKm > 3.0)
    }

    @Test
    fun estimateDumingagFaresAdaptsBetweenMountainAndTownRoutes() {
        val townOptions = TravelCompanionEngines.estimateDumingagFares(
            distanceKm = 2.0,
            isMountainRoute = false
        )
        assertEquals(3, townOptions.size)
        assertTrue(townOptions.any { it.modeName.contains("Tricycle", ignoreCase = true) })
        assertTrue(townOptions.all { it.minFarePhp > 0 && it.maxFarePhp >= it.minFarePhp })

        val mountainOptions = TravelCompanionEngines.estimateDumingagFares(
            distanceKm = 8.5,
            isMountainRoute = true
        )
        assertEquals(2, mountainOptions.size)
        assertFalse(mountainOptions.any { it.modeName.contains("Tricycle", ignoreCase = true) })
        val habal = mountainOptions.first { it.modeName.contains("Habal-Habal", ignoreCase = true) }
        assertTrue(habal.suitabilityNote.contains("Recommended", ignoreCase = true))
        assertTrue(habal.minFarePhp >= 100)
    }

    @Test
    fun generateSmartPackingChecklistRespondsToRainyWeatherAndCheckedState() {
        val rainyWeather = WeatherSnapshot(
            temperatureC = 26.0,
            apparentTempC = 28.0,
            humidityPct = 88,
            windSpeedKmh = 14.0,
            weatherCode = 63,
            isFromCache = false,
            updatedAtMillis = 1_700_000_000_000L,
            dailyForecasts = listOf(
                DailyForecast(
                    dateIso = "2026-11-23",
                    weatherCode = 63,
                    tempMaxC = 28.0,
                    tempMinC = 23.0,
                    precipChancePct = 80
                )
            )
        )

        val checklist = TravelCompanionEngines.generateSmartPackingChecklist(
            weather = rainyWeather,
            itineraryNames = listOf("Danlugan Falls", "Mt. Timolan Peak"),
            checkedIds = setOf("pack_rain_poncho", "pack_powerbank")
        )

        val poncho = checklist.first { it.id == "pack_rain_poncho" }
        assertTrue(poncho.isWeatherRecommended)
        assertTrue(poncho.checked)
        assertTrue(checklist.any { it.id == "pack_quickdry" })
        assertTrue(checklist.any { it.id == "pack_headlamp" })
        assertFalse(checklist.first { it.id == "pack_cash_small" }.checked)
    }

    @Test
    fun optimizeItineraryOrderReducesOrMatchesTotalDistance() {
        val attractions = DumingagSeedData.attractions
        assertTrue(attractions.size >= 3)

        // Deliberately pick far -> near -> far order from Poblacion center
        val poblacion = 8.1650 to 123.3465
        val sortedByDist = attractions.sortedBy {
            TravelCompanionEngines.haversineKm(poblacion.first, poblacion.second, it.latitude, it.longitude)
        }
        val nearest = sortedByDist.first()
        val farthest = sortedByDist.last()
        val middle = sortedByDist[sortedByDist.size / 2]

        val outOfOrder = listOf(farthest, nearest, middle).mapIndexed { index, attr ->
            ItineraryItem(
                id = "it_$index",
                userId = "tester",
                attractionId = attr.id,
                attractionName = attr.name.en,
                plannedDateMillis = 1_700_000_000_000L + index * 3600_000L
            )
        }

        val result = TravelCompanionEngines.optimizeItineraryOrder(
            userLatLon = poblacion,
            items = outOfOrder,
            attractions = attractions
        )

        assertEquals(3, result.orderedItems.size)
        assertEquals(nearest.id, result.orderedItems.first().attractionId)
        assertTrue(result.optimizedDistanceKm <= result.originalDistanceKm + 0.0001)
        assertTrue(result.savedDistanceKm >= 0.0)
    }

    @Test
    fun computeEcoBadgesUnlocksMatchingMilestones() {
        val attractions = DumingagSeedData.attractions
        val natureIds = attractions.filter { it.category == com.example.model.AttractionCategory.NATURE }
            .take(2).map { it.id }
        val farmId = attractions.firstOrNull { it.category == com.example.model.AttractionCategory.ORGANIC_FARM }?.id
        val checkedIn = (natureIds + listOfNotNull(farmId)).toSet()

        val badges = TravelCompanionEngines.computeEcoBadges(
            checkedInIds = checkedIn,
            userReviewCount = 1,
            trailReportCount = 0,
            offlinePackReady = true,
            attractions = attractions
        )

        assertTrue(badges.first { it.id == "badge_first_stamp" }.unlocked)
        assertTrue(badges.first { it.id == "badge_nature_chaser" }.unlocked)
        assertTrue(badges.first { it.id == "badge_trail_guardian" }.unlocked)
        assertTrue(badges.first { it.id == "badge_offline_ready" }.unlocked)
        if (farmId != null) {
            assertTrue(badges.first { it.id == "badge_organic_steward" }.unlocked)
        }
    }

    @Test
    fun computeBearingDegreesAndCardinalDirectionMatchCompassPoints() {
        val originLat = 8.1650
        val originLon = 123.3465

        val north = TravelCompanionEngines.computeBearingDegrees(originLat, originLon, originLat + 0.05, originLon)
        val east = TravelCompanionEngines.computeBearingDegrees(originLat, originLon, originLat, originLon + 0.05)
        val south = TravelCompanionEngines.computeBearingDegrees(originLat, originLon, originLat - 0.05, originLon)
        val west = TravelCompanionEngines.computeBearingDegrees(originLat, originLon, originLat, originLon - 0.05)

        assertEquals("N", TravelCompanionEngines.cardinalDirection(north))
        assertEquals("E", TravelCompanionEngines.cardinalDirection(east))
        assertEquals("S", TravelCompanionEngines.cardinalDirection(south))
        assertEquals("W", TravelCompanionEngines.cardinalDirection(west))
        assertEquals(90f, east, 2f)
        assertEquals(180f, south, 2f)
        assertEquals(270f, west, 2f)
    }

    @Test
    fun travelCompanionDataStorePersistsAllCompanionStateOffline() = runTest {
        val prefsFile = File(tempFolder.newFolder("companion_store"), "companion.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val store = TravelCompanionDataStore(
                PreferenceDataStoreFactory.create(
                    scope = scope,
                    produceFile = { prefsFile }
                )
            )

            // Theme Mode
            assertEquals(AppThemeMode.SYSTEM, store.themeMode.first())
            store.setThemeMode(AppThemeMode.DARK)
            assertEquals(AppThemeMode.DARK, store.themeMode.first())

            // Packing Checklist toggle
            store.togglePackingItem("pack_rain_poncho")
            assertTrue("pack_rain_poncho" in store.checkedPackingIds.first())
            store.togglePackingItem("pack_rain_poncho")
            assertFalse("pack_rain_poncho" in store.checkedPackingIds.first())

            // Trip Expenses add & delete
            store.addExpense(
                title = "Habal-Habal to Danlugan Falls",
                category = "Transit",
                amountPhp = 160.0,
                spotName = "Danlugan Falls",
                nowMillis = 1_700_000_000_000L
            )
            val savedExpenses = store.tripExpenses.first()
            assertEquals(1, savedExpenses.size)
            assertEquals(160.0, savedExpenses.first().amountPhp, 0.001)
            store.deleteExpense(savedExpenses.first().id)
            assertTrue(store.tripExpenses.first().isEmpty())

            // Eco-Passport Check-in
            store.recordCheckIn("attr_danlugan_falls")
            assertTrue("attr_danlugan_falls" in store.checkedInAttractionIds.first())

            // Trail Condition Report
            store.addTrailReport(
                attractionId = "attr_danlugan_falls",
                conditionTag = "Slippery stones",
                crowdLevel = "Moderate",
                note = "Bring grip sandals after noon shower.",
                reporterName = "Eco Hiker",
                nowMillis = 1_700_000_123_000L
            )
            val reports = store.trailReports.first()
            assertEquals("Slippery stones", reports.first().conditionTag)
            assertEquals("Eco Hiker", reports.first().reporterName)
        } finally {
            scope.cancel()
        }
    }
}
