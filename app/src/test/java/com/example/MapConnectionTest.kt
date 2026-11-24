package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.example.data.DumingagSeedData
import com.example.data.OfflineRouteEngine
import com.example.model.AppLanguage
import com.example.model.Attraction
import com.example.model.AttractionCategory
import com.example.model.TravelMode
import com.example.ui.screens.InteractiveMapScreen
import com.example.ui.screens.buildPointToPointOffsets
import com.example.ui.screens.calculateMetricsForDistance
import com.example.ui.screens.calculateMultiPointRoute
import com.example.ui.screens.calculateSpotTravelMetrics
import com.example.ui.screens.formatDistanceDisplay
import com.example.ui.screens.formatHoursAndMinutes
import com.example.ui.screens.formatHoursDecimal
import com.example.ui.screens.formatTravelHoursSummary
import com.example.ui.screens.pointAlongPolylineAtFraction
import com.example.ui.theme.DumingagTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h860dp")
class MapConnectionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun distanceAndHoursCalculationsMatchTravelModes() {
        val walkMetrics = calculateMetricsForDistance(9.0, TravelMode.WALK)
        assertEquals(2.0, walkMetrics.estimatedHours, 0.001)
        assertEquals(120, walkMetrics.estimatedMinutes)
        assertEquals("9.0 km", formatDistanceDisplay(walkMetrics.distanceKm))
        assertEquals("2.00 hrs", formatHoursDecimal(walkMetrics.estimatedHours))
        assertEquals("2h", formatHoursAndMinutes(walkMetrics.estimatedMinutes))
        assertEquals("2.00 hrs (2h)", formatTravelHoursSummary(walkMetrics))

        val habalMetrics = calculateMetricsForDistance(14.0, TravelMode.HABAL_HABAL)
        assertEquals(0.5, habalMetrics.estimatedHours, 0.001)
        assertEquals(30, habalMetrics.estimatedMinutes)
        assertEquals("0.50 hr (30 min)", formatTravelHoursSummary(habalMetrics))

        val shortMetrics = calculateMetricsForDistance(0.45, TravelMode.WALK)
        assertEquals("450 m", formatDistanceDisplay(shortMetrics.distanceKm))
        assertEquals(6, shortMetrics.estimatedMinutes)
        assertEquals("0.10 hr (6 min)", formatTravelHoursSummary(shortMetrics))
    }

    @Test
    fun clickingMapSpotShowsConnectionPillAndUpdatesHoursByTravelMode() {
        val spots = DumingagSeedData.attractions
        val targetSpot = spots.first { spot ->
            calculateSpotTravelMetrics(
                DumingagSeedData.CENTER_LAT,
                DumingagSeedData.CENTER_LON,
                spot.latitude,
                spot.longitude,
                TravelMode.HABAL_HABAL
            ).distanceKm >= 2.0
        }

        val expectedHabal = calculateSpotTravelMetrics(
            DumingagSeedData.CENTER_LAT,
            DumingagSeedData.CENTER_LON,
            targetSpot.latitude,
            targetSpot.longitude,
            TravelMode.HABAL_HABAL
        )
        val expectedWalk = calculateSpotTravelMetrics(
            DumingagSeedData.CENTER_LAT,
            DumingagSeedData.CENTER_LON,
            targetSpot.latitude,
            targetSpot.longitude,
            TravelMode.WALK
        )

        compose.setContent {
            var selected by remember { mutableStateOf<Attraction?>(null) }
            var mode by remember { mutableStateOf(TravelMode.HABAL_HABAL) }

            DumingagTheme {
                InteractiveMapScreen(
                    language = AppLanguage.EN,
                    attractions = spots,
                    selectedCategory = AttractionCategory.ALL,
                    selectedAttraction = selected,
                    activeRoute = null,
                    activeStepIndex = 0,
                    travelMode = mode,
                    userLatLon = DumingagSeedData.CENTER_LAT to DumingagSeedData.CENTER_LON,
                    offlinePackReady = true,
                    isEffectiveOffline = true,
                    onCategorySelected = {},
                    onSelectAttraction = { selected = it },
                    onStartNavigation = {},
                    onClearNavigation = {},
                    onStepNavigation = {},
                    onTravelModeChange = { mode = it },
                    onUpdateLocation = { _, _ -> },
                    onOpenDetail = {}
                )
            }
        }

        // Before selecting any spot, connection pill is hidden
        compose.onNodeWithTag("map_connection_pill").assertDoesNotExist()

        // Tap the spot from the quick strip (or pin) to connect from current location
        compose.onNodeWithTag("map_strip_card_${targetSpot.id}").performClick()

        // Connection line midpoint pill, origin dot, exact centered pin, and distance/hours panel appear
        compose.onNodeWithTag("map_user_origin_dot").assertIsDisplayed()
        compose.onNodeWithTag("map_pin_${targetSpot.id}")
            .assertIsDisplayed()
            .assertWidthIsEqualTo(54.dp)
        compose.onNodeWithTag("map_connection_pill")
            .assertIsDisplayed()
            .assertTextContains(formatDistanceDisplay(expectedHabal.distanceKm), substring = true)
            .assertTextContains(formatHoursDecimal(expectedHabal.estimatedHours), substring = true)
        compose.onNodeWithTag("map_distance_duration_card").assertIsDisplayed()
        compose.onNodeWithTag("map_pin_distance_value")
            .assertTextContains(formatDistanceDisplay(expectedHabal.distanceKm), substring = true)
        compose.onNodeWithTag("map_pin_hours_value")
            .assertTextContains(formatHoursDecimal(expectedHabal.estimatedHours), substring = true)

        // Switch travel mode to Walk and verify the hour estimate recalculates live
        compose.onNodeWithTag("pin_travel_mode_WALK").performClick()
        assertTrue(expectedWalk.estimatedHours > expectedHabal.estimatedHours)
        compose.onNodeWithTag("map_pin_hours_value")
            .assertTextContains(formatHoursDecimal(expectedWalk.estimatedHours), substring = true)
        compose.onNodeWithTag("map_connection_pill")
            .assertTextContains(formatHoursDecimal(expectedWalk.estimatedHours), substring = true)
    }

    @Test
    fun pointToPointPolylineAndMidpointMatchExactAnchors() {
        val nodesById = DumingagSeedData.mapNodes.associateBy { it.id }
        val paglinawan = DumingagSeedData.attractions.first { it.id == "paglinawan_eco_farm" }
        val danlugan = DumingagSeedData.attractions.first { it.id == "danlugan_falls" }
        val project: (Double, Double) -> Offset = { lat, lon ->
            Offset(((lon - 123.30) * 10000.0).toFloat(), ((8.20 - lat) * 10000.0).toFloat())
        }
        val userPt = project(DumingagSeedData.CENTER_LAT, DumingagSeedData.CENTER_LON)
        val paglinawanPt = project(paglinawan.latitude, paglinawan.longitude)
        val spotPt = project(danlugan.latitude, danlugan.longitude)

        // Direct selection connects exact origin point -> exact destination pin center
        val directPts = buildPointToPointOffsets(
            userPt = userPt,
            activeRoute = null,
            markedAttractions = listOf(danlugan),
            nodesById = nodesById,
            latLonToScreen = project
        )
        assertEquals(2, directPts.size)
        assertEquals(userPt, directPts.first())
        assertEquals(spotPt, directPts.last())

        // Multi-landmark chain connects exact origin -> #1 -> #2 point-to-point
        val multiPts = buildPointToPointOffsets(
            userPt = userPt,
            activeRoute = null,
            markedAttractions = listOf(paglinawan, danlugan),
            nodesById = nodesById,
            latLonToScreen = project
        )
        assertEquals(3, multiPts.size)
        assertEquals(userPt, multiPts[0])
        assertEquals(paglinawanPt, multiPts[1])
        assertEquals(spotPt, multiPts[2])

        val (midPt, tangent) = pointAlongPolylineAtFraction(directPts, 0.5f)
        assertEquals((userPt.x + spotPt.x) / 2f, midPt.x, 0.5f)
        assertEquals((userPt.y + spotPt.y) / 2f, midPt.y, 0.5f)
        assertTrue(kotlin.math.hypot(tangent.x, tangent.y) in 0.99f..1.01f)

        // Guided route connects userPt -> intermediate junctions -> exact destination without duplicate start
        val route = OfflineRouteEngine.computeRoute("node_poblacion", danlugan, TravelMode.HABAL_HABAL)
        val routePts = buildPointToPointOffsets(
            userPt = userPt,
            activeRoute = route,
            markedAttractions = listOf(danlugan),
            nodesById = nodesById,
            latLonToScreen = project
        )
        assertTrue(routePts.size >= 3)
        assertEquals(userPt, routePts.first())
        assertEquals(spotPt, routePts.last())
    }

    @Test
    fun tappingMultipleLandmarksMarksAndConnectsLinesWithPerLegAndTotalKmAndHours() {
        val spots = DumingagSeedData.attractions
        val firstSpot = spots.first { it.id == "paglinawan_eco_farm" }
        val secondSpot = spots.first { it.id == "danlugan_falls" }
        val userOrigin = DumingagSeedData.CENTER_LAT to DumingagSeedData.CENTER_LON

        val expectedChainHabal = calculateMultiPointRoute(
            userLatLon = userOrigin,
            markedAttractions = listOf(firstSpot, secondSpot),
            travelMode = TravelMode.HABAL_HABAL
        )
        val expectedChainWalk = calculateMultiPointRoute(
            userLatLon = userOrigin,
            markedAttractions = listOf(firstSpot, secondSpot),
            travelMode = TravelMode.WALK
        )

        compose.setContent {
            var marked by remember { mutableStateOf<List<Attraction>>(emptyList()) }
            var mode by remember { mutableStateOf(TravelMode.HABAL_HABAL) }

            DumingagTheme {
                InteractiveMapScreen(
                    language = AppLanguage.EN,
                    attractions = spots,
                    selectedCategory = AttractionCategory.ALL,
                    selectedAttraction = marked.lastOrNull(),
                    markedAttractions = marked,
                    activeRoute = null,
                    activeStepIndex = 0,
                    travelMode = mode,
                    userLatLon = userOrigin,
                    offlinePackReady = true,
                    isEffectiveOffline = true,
                    onCategorySelected = {},
                    onSelectAttraction = { spot ->
                        marked = if (spot == null) emptyList() else listOf(spot)
                    },
                    onToggleMarkAttraction = { spot ->
                        marked = if (marked.any { it.id == spot.id }) {
                            marked.filterNot { it.id == spot.id }
                        } else {
                            marked + spot
                        }
                    },
                    onRemoveMarkedAttraction = { spot ->
                        marked = marked.filterNot { it.id == spot.id }
                    },
                    onClearAllMarks = { marked = emptyList() },
                    onStartNavigation = {},
                    onClearNavigation = {},
                    onStepNavigation = {},
                    onTravelModeChange = { mode = it },
                    onUpdateLocation = { _, _ -> },
                    onOpenDetail = {}
                )
            }
        }

        // Tap first landmark -> marks #1 and connects from current location
        compose.onNodeWithTag("map_pin_${firstSpot.id}").performClick()
        compose.onNodeWithTag("map_pin_badge_${firstSpot.id}", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("map_pin_${firstSpot.id}").assertTextContains("1", substring = true)
        compose.onNodeWithTag("map_connection_pill").assertIsDisplayed()

        // Tap second landmark -> marks #2 and chains current location -> #1 -> #2
        compose.onNodeWithTag("map_pin_${secondSpot.id}").performClick()
        compose.onNodeWithTag("map_pin_badge_${firstSpot.id}", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("map_pin_badge_${secondSpot.id}", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("map_pin_${firstSpot.id}").assertTextContains("1", substring = true)
        compose.onNodeWithTag("map_pin_${secondSpot.id}").assertTextContains("2", substring = true)
        compose.onNodeWithTag("map_marked_stops_chain").assertIsDisplayed()

        // Both per-leg km/hr pills appear on the map
        compose.onNodeWithTag("map_connection_pill")
            .assertIsDisplayed()
            .assertTextContains(formatDistanceDisplay(expectedChainHabal.legs[0].metrics.distanceKm), substring = true)
            .assertTextContains(formatHoursDecimal(expectedChainHabal.legs[0].metrics.estimatedHours), substring = true)
        compose.onNodeWithTag("map_connection_leg_1")
            .assertIsDisplayed()
            .assertTextContains(formatDistanceDisplay(expectedChainHabal.legs[1].metrics.distanceKm), substring = true)
            .assertTextContains(formatHoursDecimal(expectedChainHabal.legs[1].metrics.estimatedHours), substring = true)

        // Bottom summary card displays combined total distance (km) and total hours
        compose.onNodeWithTag("map_pin_distance_value")
            .assertTextContains(formatDistanceDisplay(expectedChainHabal.totalDistanceKm), substring = true)
        compose.onNodeWithTag("map_pin_hours_value")
            .assertTextContains(formatHoursDecimal(expectedChainHabal.totalHours), substring = true)

        // Switching travel mode updates total hours and leg badges live
        compose.onNodeWithTag("pin_travel_mode_WALK").performClick()
        compose.onNodeWithTag("map_pin_hours_value")
            .assertTextContains(formatHoursDecimal(expectedChainWalk.totalHours), substring = true)
        compose.onNodeWithTag("map_connection_leg_1")
            .assertTextContains(formatHoursDecimal(expectedChainWalk.legs[1].metrics.estimatedHours), substring = true)

        // Clear all removes marks and connection pills
        compose.onNodeWithTag("map_clear_all_marks").performClick()
        compose.onNodeWithTag("map_connection_pill").assertDoesNotExist()
        compose.onNodeWithTag("map_connection_leg_1").assertDoesNotExist()
    }

    @Test
    fun closingDetailsModalKeepsLinesDistanceAndHoursVisibleOnMap() {
        val spots = DumingagSeedData.attractions
        val firstSpot = spots.first { it.id == "paglinawan_eco_farm" }
        val secondSpot = spots.first { it.id == "danlugan_falls" }
        val userOrigin = DumingagSeedData.CENTER_LAT to DumingagSeedData.CENTER_LON

        val expectedChain = calculateMultiPointRoute(
            userLatLon = userOrigin,
            markedAttractions = listOf(firstSpot, secondSpot),
            travelMode = TravelMode.HABAL_HABAL
        )

        compose.setContent {
            var marked by remember { mutableStateOf<List<Attraction>>(emptyList()) }
            var mode by remember { mutableStateOf(TravelMode.HABAL_HABAL) }

            DumingagTheme {
                InteractiveMapScreen(
                    language = AppLanguage.EN,
                    attractions = spots,
                    selectedCategory = AttractionCategory.ALL,
                    selectedAttraction = marked.lastOrNull(),
                    markedAttractions = marked,
                    activeRoute = null,
                    activeStepIndex = 0,
                    travelMode = mode,
                    userLatLon = userOrigin,
                    offlinePackReady = true,
                    isEffectiveOffline = true,
                    onCategorySelected = {},
                    onSelectAttraction = { spot ->
                        marked = if (spot == null) emptyList() else listOf(spot)
                    },
                    onToggleMarkAttraction = { spot ->
                        marked = if (marked.any { it.id == spot.id }) {
                            marked.filterNot { it.id == spot.id }
                        } else {
                            marked + spot
                        }
                    },
                    onRemoveMarkedAttraction = { spot ->
                        marked = marked.filterNot { it.id == spot.id }
                    },
                    onClearAllMarks = { marked = emptyList() },
                    onStartNavigation = {},
                    onClearNavigation = {},
                    onStepNavigation = {},
                    onTravelModeChange = { mode = it },
                    onUpdateLocation = { _, _ -> },
                    onOpenDetail = {}
                )
            }
        }

        // 1. Tap first landmark -> connects line & opens details card
        compose.onNodeWithTag("map_pin_${firstSpot.id}").performClick()
        compose.onNodeWithTag("map_distance_duration_card").assertIsDisplayed()
        compose.onNodeWithTag("map_connection_pill").assertIsDisplayed()

        // 2. Close details modal card -> card hides, BUT line, badge, km & hr pill stay connected!
        compose.onNodeWithTag("map_close_details_card").performClick()
        compose.onNodeWithTag("map_distance_duration_card").assertDoesNotExist()
        compose.onNodeWithTag("map_collapsed_route_bar").assertIsDisplayed()
        compose.onNodeWithTag("map_pin_badge_${firstSpot.id}", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("map_connection_pill")
            .assertIsDisplayed()
            .assertTextContains(formatDistanceDisplay(expectedChain.legs[0].metrics.distanceKm), substring = true)
            .assertTextContains(formatHoursDecimal(expectedChain.legs[0].metrics.estimatedHours), substring = true)

        // 3. Tap second landmark while details card is closed -> stays closed so map remains unobstructed!
        compose.onNodeWithTag("map_pin_${secondSpot.id}").performClick()
        compose.onNodeWithTag("map_distance_duration_card").assertDoesNotExist()
        compose.onNodeWithTag("map_pin_badge_${secondSpot.id}", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("map_connection_pill").assertIsDisplayed()
        compose.onNodeWithTag("map_connection_leg_1")
            .assertIsDisplayed()
            .assertTextContains(formatDistanceDisplay(expectedChain.legs[1].metrics.distanceKm), substring = true)
            .assertTextContains(formatHoursDecimal(expectedChain.legs[1].metrics.estimatedHours), substring = true)
        compose.onNodeWithTag("map_collapsed_summary_text", useUnmergedTree = true)
            .assertTextContains("2 stops", substring = true)
            .assertTextContains(formatDistanceDisplay(expectedChain.totalDistanceKm), substring = true)
            .assertTextContains(formatHoursDecimal(expectedChain.totalHours), substring = true)

        // 4. Reopen details card on demand
        compose.onNodeWithTag("map_reopen_details_button").performClick()
        compose.onNodeWithTag("map_distance_duration_card").assertIsDisplayed()

        // 5. Close again, then explicitly disconnect lines using the clear button
        compose.onNodeWithTag("map_close_details_card").performClick()
        compose.onNodeWithTag("map_disconnect_lines_button").performClick()
        compose.onNodeWithTag("map_connection_pill").assertDoesNotExist()
        compose.onNodeWithTag("map_connection_leg_1").assertDoesNotExist()
        compose.onNodeWithTag("map_collapsed_route_bar").assertDoesNotExist()
    }
}
