package com.example

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.example.data.DumingagSeedData
import com.example.model.*
import com.example.ui.screens.CommunityReviewsScreen
import com.example.ui.screens.ItineraryVaultScreen
import com.example.ui.screens.TransitSafetyScreen
import com.example.ui.theme.DumingagTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TravelPagesLayoutTest {
    @get:Rule val compose = createComposeRule()
    private val page = mutableStateOf("transit")
    private val attractions = DumingagSeedData.attractions.take(2)

    private fun showPages(fontScale: Float, admin: Boolean = false) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                DumingagTheme {
                    key(page.value) {
                        when (page.value) {
                            "transit" -> TransitSafetyScreen(
                                language = AppLanguage.EN,
                                transitState = UiState.Success(listOf(
                                    TransitMethod(id = "a", name = "Route A"),
                                    TransitMethod(id = "b", name = "Route B")
                                )),
                                emergencyContacts = emptyList(),
                                isAdmin = admin, isEffectiveOffline = false, onRefreshTransit = {},
                                onManageEmergencyContacts = {}, onAddTransitMethod = { _, _, _, _, _, _, _, _, _, _ -> },
                                onUpdateTransitMethod = { _, _, _, _, _, _, _, _, _, _, _ -> },
                                onDeleteTransitMethod = {}, onShowMessage = {}
                            )
                            "plan" -> ItineraryVaultScreen(
                                language = AppLanguage.EN, attractions = attractions,
                                itinerariesState = UiState.Success(listOf(
                                    ItineraryItem(id = "a", attractionName = "Stop A"),
                                    ItineraryItem(id = "b", attractionName = "Stop B")
                                )),
                                savedDocuments = emptyList(), savedAttractionCount = 0, offlinePackReady = false,
                                forceOfflineMode = false, onToggleForceOffline = {}, onDownloadOfflinePack = {},
                                onAddItinerary = { _, _, _, _ -> }, onSyncCalendar = {}, onToggleReminder = {},
                                onDeleteItinerary = {}, onSaveCustomDocument = { _, _, _, _ -> },
                                onDeleteCustomDocument = {}, onShowMessage = {}
                            )
                            else -> CommunityReviewsScreen(
                                language = AppLanguage.EN, currentUserId = "viewer", userDisplayName = "Traveler",
                                userEmail = "", userPhotoData = "", isAdmin = false, attractions = attractions,
                                reviewsState = UiState.Success(listOf(
                                    CommunityReview(id = "a", attractionName = "Review A"),
                                    CommunityReview(id = "b", attractionName = "Review B")
                                )),
                                onOpenLanguagePicker = {}, onEditProfile = {}, onOpenAdminConsole = {}, onSignOut = {},
                                onSubmitReview = { _, _, _, _ -> }, onUpdateReview = { _, _, _, _ -> }, onDeleteReview = {}
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    @Config(qualifiers = "w320dp-h600dp")
    fun compactScreensKeepPrimaryActionsReachableWithLargeText() {
        showPages(1.5f, admin = true)
        compose.onNodeWithTag("transit_add_button").performClick()
        compose.onNodeWithText("Add Dumingag Transit Route").assertIsDisplayed()
        compose.runOnIdle { page.value = "plan" }
        compose.onNodeWithTag("add_itinerary_stop_button").performClick()
        compose.onNodeWithText("Select Destination").assertIsDisplayed()
        compose.runOnIdle { page.value = "reviews" }
        compose.onNodeWithTag("write_review_button").performClick()
        compose.onNodeWithText("Write Community Review").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1000dp-h660dp-land")
    fun wideScreensPlaceRelatedItemsSideBySide() {
        showPages(1f)
        compose.onNodeWithTag("transit_add_button").assertDoesNotExist()
        for ((screen, label) in listOf("transit" to "Route", "plan" to "Stop", "reviews" to "Review")) {
            compose.runOnIdle { page.value = screen }
            compose.onNodeWithTag("${screen}_page").performScrollToNode(hasText("$label A"))
            val left = compose.onNodeWithText("$label A").getUnclippedBoundsInRoot()
            val right = compose.onNodeWithText("$label B").getUnclippedBoundsInRoot()
            assertTrue("$screen cards should share a row", right.left >= left.right && right.top == left.top)
        }
    }
}
