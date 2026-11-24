package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.ui.screens.LiquidGlassSplashScreen
import com.example.ui.screens.PreliminarySplashScreen
import com.example.ui.theme.DumingagTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h860dp")
class SplashScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun preliminarySplashDisplaysCrestTitleAndAdvancesOnTap() {
        var finishedCount = 0
        compose.mainClock.autoAdvance = false

        compose.setContent {
            DumingagTheme {
                PreliminarySplashScreen(
                    durationMillis = 5000,
                    onFinished = { finishedCount++ }
                )
            }
        }

        compose.mainClock.advanceTimeBy(700L)
        compose.waitForIdle()

        compose.onNodeWithTag("preliminary_splash_root").assertIsDisplayed()
        compose.onNodeWithTag("preliminary_splash_crest", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("preliminary_splash_title", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextContains("Dumingag", substring = true)

        compose.onNodeWithTag("preliminary_splash_root").performClick()
        compose.mainClock.advanceTimeBy(100L)
        assertEquals(1, finishedCount)
    }

    @Test
    fun interactiveLiquidGlassScenesAndExploreActionWorkSmoothly() {
        var enteredCount = 0

        compose.setContent {
            DumingagTheme {
                LiquidGlassSplashScreen(
                    onEnterApp = { enteredCount++ }
                )
            }
        }

        // Initial Highlands scene is displayed along with crest medallion and explore control
        compose.onNodeWithTag("liquid_glass_splash_root").assertIsDisplayed()
        compose.onNodeWithTag("splash_crest_medallion").assertIsDisplayed()
        compose.onNodeWithTag("splash_scene_title")
            .assertIsDisplayed()
            .assertTextContains("Misty Dumingag Highlands", substring = true)

        // Tapping Waterfalls glass pill crossfades to Waterfalls highlight
        compose.onNodeWithTag("splash_scene_pill_waterfalls").performClick()
        compose.onNodeWithTag("splash_scene_title")
            .assertTextContains("Crystal Cascades", substring = true)

        // Tapping the refractive crest medallion advances to the next scene (Eco-Farms)
        compose.onNodeWithTag("splash_crest_medallion").performClick()
        compose.onNodeWithTag("splash_scene_title")
            .assertTextContains("Organic Capital", substring = true)

        // Tapping Heritage glass pill switches to Heritage scene
        compose.onNodeWithTag("splash_scene_pill_heritage").performClick()
        compose.onNodeWithTag("splash_scene_title")
            .assertTextContains("Subanen Roots", substring = true)

        // Tapping the Slide-or-Tap Explore pill enters the app once
        compose.onNodeWithTag("splash_explore_button").performClick()
        assertEquals(1, enteredCount)
    }

    @Test
    fun skipGlassPillEntersAppImmediately() {
        var enteredCount = 0

        compose.setContent {
            DumingagTheme {
                LiquidGlassSplashScreen(
                    onEnterApp = { enteredCount++ }
                )
            }
        }

        compose.onNodeWithTag("splash_skip_button")
            .assertIsDisplayed()
            .performClick()
        assertEquals(1, enteredCount)
    }
}
