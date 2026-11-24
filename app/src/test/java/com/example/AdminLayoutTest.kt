package com.example

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.DumingagSeedData
import com.example.model.AppLanguage
import com.example.model.ManagedUser
import com.example.ui.screens.AdminManagementScreen
import com.example.ui.theme.DumingagTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AdminLayoutTest {
    @get:Rule val compose = createComposeRule()
    private val attractions = DumingagSeedData.attractions.take(2)

    private fun showConsole(fontScale: Float = 1f, admin: Boolean = true) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                DumingagTheme {
                    AdminManagementScreen(
                        language = AppLanguage.EN, isAdmin = admin, adminEmail = "admin@example.org",
                        currentUserId = "admin", loadUsers = {
                            listOf(
                                ManagedUser("admin", "Admin User", "admin@example.org", "admin"),
                                ManagedUser("guest", "Guest", "guest@example.com", "regular")
                            )
                        },
                        attractions = attractions,
                        emergencyContacts = DumingagSeedData.emergencyContacts,
                        onBackClick = {},
                        onSaveAttraction = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
                        onDeleteAttraction = {}, onSaveEmergencyContact = { _, _, _, _, _, _, _, _ -> },
                        onDeleteEmergencyContact = {}
                    )
                }
            }
        }
    }

    @Test
    @Config(qualifiers = "w320dp-h540dp")
    fun largeTextOnCompactScreenKeepsUserManagementReachable() {
        showConsole(1.6f)
        val grid = compose.onNodeWithTag("admin_directory")
        grid.performScrollToNode(hasTestTag("admin_tab_users"))
        compose.onNodeWithTag("admin_tab_users").performClick()
        grid.performScrollToNode(hasText("Guest"))
        compose.onNodeWithText("Guest").assertIsDisplayed()
        compose.onNodeWithText("guest@example.com").assertIsDisplayed()
        compose.onNodeWithText("REGULAR").assertIsDisplayed()
        compose.onNodeWithText("Grant admin access").assertDoesNotExist()
    }

    @Test
    fun regularUserCannotReachManagementEvenOnDirectNavigation() {
        showConsole(admin = false)
        compose.onNodeWithTag("admin_directory").assertDoesNotExist()
        compose.onNodeWithText("Return to Guide").assertExists()
    }

    @Test
    @Config(qualifiers = "w1100dp-h680dp-land")
    fun wideScreenPlacesAttractionsSideBySide() {
        showConsole()
        val grid = compose.onNodeWithTag("admin_directory")
        grid.performScrollToNode(hasTestTag("admin_edit_attraction_${attractions.first().id}"))
        val cards = attractions.map {
            compose.onNodeWithText(it.name.en).fetchSemanticsNode().boundsInRoot
        }
        assertTrue("Both attraction cards share the row", cards[1].left >= cards[0].right)
        assertTrue("Titles align vertically", kotlin.math.abs(cards[0].top - cards[1].top) < 2f)
    }
}
