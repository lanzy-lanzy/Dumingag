package com.example

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.ui.components.LiquidGlassFloatingNavBar
import com.example.ui.components.LiquidGlassNavTab
import com.example.ui.components.LiquidGlassSignOutOverlay
import com.example.ui.theme.DumingagTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h860dp")
class LiquidGlassComponentsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun signOutLiquidGlassOverlayDisplaysSpinnerAndHidesWhenDismissed() {
        var visible by mutableStateOf(true)
        compose.mainClock.autoAdvance = false

        compose.setContent {
            DumingagTheme {
                LiquidGlassSignOutOverlay(visible = visible)
            }
        }

        compose.mainClock.advanceTimeBy(300L)
        compose.waitForIdle()

        compose.onNodeWithTag("sign_out_liquid_glass_loader").assertIsDisplayed()
        compose.onNodeWithTag("liquid_glass_spinner", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("sign_out_loader_title", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextContains("Signing Out", substring = true)

        visible = false
        compose.mainClock.advanceTimeBy(400L)
        compose.waitForIdle()

        compose.onNodeWithTag("sign_out_liquid_glass_loader").assertDoesNotExist()
    }

    @Test
    fun floatingLiquidGlassNavBarDisplaysTabsAndSwitchesSelection() {
        var selectedRoute by mutableStateOf("explore")
        val tabs = listOf(
            LiquidGlassNavTab("explore", Icons.Default.Explore, "Explore"),
            LiquidGlassNavTab("map", Icons.Default.Map, "Map"),
            LiquidGlassNavTab("reviews", Icons.Default.RateReview, "Reviews")
        )

        compose.setContent {
            DumingagTheme {
                LiquidGlassFloatingNavBar(
                    tabs = tabs,
                    selectedRoute = selectedRoute,
                    onSelectRoute = { selectedRoute = it }
                )
            }
        }

        compose.onNodeWithTag("nav_explore").assertIsDisplayed()
        compose.onNodeWithTag("nav_map").assertIsDisplayed().performClick()
        assertEquals("map", selectedRoute)

        compose.onNodeWithTag("nav_reviews").assertIsDisplayed().performClick()
        assertEquals("reviews", selectedRoute)
    }
}
