package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.data.DumingagSeedData
import com.example.model.AppLanguage
import com.example.model.Attraction
import com.example.model.AttractionCategory
import com.example.model.TravelMode
import com.example.ui.screens.HOLO_DEFAULT_PITCH_DEG
import com.example.ui.screens.HOLO_DEFAULT_YAW_DEG
import com.example.ui.screens.InteractiveMapScreen
import com.example.ui.screens.Vec3
import com.example.ui.screens.buildElevated3DPolyline
import com.example.ui.screens.computeHoloGeoBounds
import com.example.ui.screens.formatCoordLabel
import com.example.ui.screens.formatDistanceDisplay
import com.example.ui.screens.geoToSlabLocal
import com.example.ui.screens.haversineDistanceKmExact
import com.example.ui.screens.isInsideSlab
import com.example.ui.screens.project3DPoint
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
class Holographic3DMapTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun projectionMathMapsEndpointsInsideSlabAndElevatesPolylineArc() {
        val start = DumingagSeedData.CENTER_LAT to DumingagSeedData.CENTER_LON
        val spot = DumingagSeedData.attractions.first { it.id == "danlugan_falls" }
        val end = spot.latitude to spot.longitude

        val bounds = computeHoloGeoBounds(listOf(start, end))
        val startLocal = geoToSlabLocal(start.first, start.second, bounds, z = 0f)
        val endLocal = geoToSlabLocal(end.first, end.second, bounds, z = 0f)

        assertTrue(isInsideSlab(startLocal))
        assertTrue(isInsideSlab(endLocal))

        // Higher z (vertical beacon / elevated arc) projects upward (smaller screen Y)
        val groundProj = project3DPoint(
            point = Vec3(0f, 0f, 0f),
            yawDeg = HOLO_DEFAULT_YAW_DEG,
            pitchDeg = HOLO_DEFAULT_PITCH_DEG,
            zoom = 1f,
            floatOffsetY = 0f,
            viewportWidth = 800f,
            viewportHeight = 800f
        )
        val apexProj = project3DPoint(
            point = Vec3(0f, 0f, 0.25f),
            yawDeg = HOLO_DEFAULT_YAW_DEG,
            pitchDeg = HOLO_DEFAULT_PITCH_DEG,
            zoom = 1f,
            floatOffsetY = 0f,
            viewportWidth = 800f,
            viewportHeight = 800f
        )
        assertTrue(apexProj.screen.x.isFinite() && apexProj.screen.y.isFinite())
        assertTrue(apexProj.screen.y < groundProj.screen.y)

        // Elevated 3D polyline anchors cleanly at start & end and peaks in Z at midpoint
        val arc = buildElevated3DPolyline(listOf(startLocal, endLocal), subdivisionsPerSegment = 20)
        assertEquals(21, arc.size)
        assertEquals(startLocal.x, arc.first().x, 0.0001f)
        assertEquals(startLocal.y, arc.first().y, 0.0001f)
        assertEquals(endLocal.x, arc.last().x, 0.0001f)
        assertEquals(endLocal.y, arc.last().y, 0.0001f)
        assertTrue(arc[10].z > 0.14f)
    }

    @Test
    fun toggling3DHolographicSlabDisplaysStartEndPinsCoordinatesAndReturnsTo2D() {
        val spots = DumingagSeedData.attractions
        val targetSpot = spots.first { it.id == "danlugan_falls" }
        val userOrigin = DumingagSeedData.CENTER_LAT to DumingagSeedData.CENTER_LON
        val expectedDist = haversineDistanceKmExact(
            userOrigin.first,
            userOrigin.second,
            targetSpot.latitude,
            targetSpot.longitude
        )

        compose.setContent {
            var marked by remember { mutableStateOf(listOf(targetSpot)) }
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
                        marked = listOfNotNull(spot)
                    },
                    onToggleMarkAttraction = { spot ->
                        marked = listOf(spot)
                    },
                    onRemoveMarkedAttraction = { marked = emptyList() },
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

        // Default is 2D map
        compose.waitForIdle()
        compose.onNodeWithTag("map_3d_holographic_slab").assertDoesNotExist()
        compose.onNodeWithTag("map_3d_toggle_button").assertIsDisplayed().performClick()
        compose.waitForIdle()

        // 3D floating holographic slab is now active with Start & End pins and coordinate telemetry
        compose.onNodeWithTag("map_3d_holographic_slab").assertIsDisplayed()
        compose.onNodeWithTag("map_3d_start_pin")
            .assertIsDisplayed()
            .assertTextContains("START", substring = true)
        compose.onNodeWithTag("map_3d_end_pin")
            .assertIsDisplayed()
            .assertTextContains("END", substring = true)
            .assertTextContains(targetSpot.name.en, substring = true)

        compose.onNodeWithTag("map_3d_coords_hud")
            .assertIsDisplayed()
            .assertTextContains(formatCoordLabel(userOrigin.first, userOrigin.second), substring = true)
            .assertTextContains(formatCoordLabel(targetSpot.latitude, targetSpot.longitude), substring = true)
            .assertTextContains(formatDistanceDisplay(expectedDist), substring = true)

        // Toggle pin assignment mode from END to START
        compose.onNodeWithTag("map_3d_pin_mode_toggle")
            .assertIsDisplayed()
            .assertTextContains("Set END", substring = true)
            .performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("map_3d_pin_mode_toggle")
            .assertTextContains("Set START", substring = true)

        // Switch cleanly back to 2D map
        compose.onNodeWithTag("map_3d_toggle_button").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("map_3d_holographic_slab").assertDoesNotExist()
        compose.onNodeWithTag("map_user_origin_dot").assertIsDisplayed()
    }
}
