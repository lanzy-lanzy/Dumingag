package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.data.DumingagSeedData
import com.example.data.SmartTravelEngines
import com.example.data.repository.AiConciergeMessage
import com.example.data.repository.AiConciergeRepository
import com.example.model.AppLanguage
import com.example.model.AttractionCategory
import com.example.model.UiState
import com.example.ui.components.AiConciergeSheet
import com.example.ui.screens.ExploreScreen
import com.example.ui.theme.DumingagTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiConciergeDiagnosticsTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun exploreScreenOpensAiConciergeSendsPromptAndClosesCleanly() {
        val sentPrompts = mutableListOf<String>()
        compose.setContent {
            var showSheet by remember { mutableStateOf(false) }
            var messages by remember { mutableStateOf<List<AiConciergeMessage>>(emptyList()) }
            var loading by remember { mutableStateOf(false) }
            DumingagTheme {
                ExploreScreen(
                    language = AppLanguage.EN,
                    searchQuery = "",
                    selectedCategory = AttractionCategory.ALL,
                    selectedCategoryCode = "ALL",
                    managedCategories = emptyList(),
                    attractions = DumingagSeedData.attractions,
                    allAttractionsForSearch = DumingagSeedData.attractions,
                    savedIds = emptySet(),
                    favoriteIds = emptySet(),
                    showFavoritesOnly = false,
                    weatherState = UiState.Loading,
                    isEffectiveOffline = false,
                    isRefreshing = false,
                    lastSyncedAtMillis = System.currentTimeMillis(),
                    tourismEvents = SmartTravelEngines.starterTourismEvents,
                    transitMethods = DumingagSeedData.defaultTransitMethods,
                    emergencyContacts = DumingagSeedData.emergencyContacts,
                    reviews = emptyList(),
                    onSearchChange = {},
                    onCategorySelected = {},
                    onCategoryCodeSelected = {},
                    onToggleFavoritesOnly = {},
                    onToggleFavorite = {},
                    onRefreshWeather = {},
                    onRefreshAll = {},
                    onAttractionClick = {},
                    onNavigateToAttraction = {},
                    onToggleSaveOffline = {},
                    onOpenMapTab = {},
                    onOpenDirectory = {},
                    onOpenAiConcierge = { showSheet = true },
                    onOpenTransitTab = {},
                    onOpenReviewsTab = {}
                )
                if (showSheet) {
                    AiConciergeSheet(
                        messages = messages,
                        streamingText = if (loading) "Start at Poblacion..." else "",
                        isLoading = loading,
                        errorMessage = null,
                        isOffline = false,
                        onSendPrompt = { p ->
                            sentPrompts.add(p)
                            messages = messages + AiConciergeMessage("u_${sentPrompts.size}", true, p)
                            loading = false
                        },
                        onClearChat = { messages = emptyList() },
                        onDismiss = { showSheet = false }
                    )
                }
            }
        }

        compose.onNodeWithTag("explore_open_ai_concierge").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("ai_concierge_sheet").assertExists()

        compose.onAllNodesWithTag("ai_prompt_chip").onFirst().performClick()
        compose.waitForIdle()
        assertEquals(1, sentPrompts.size)

        compose.onNodeWithTag("ai_concierge_input").performTextInput("Best waterfall?")
        compose.onNodeWithTag("ai_concierge_send_btn").performClick()
        compose.waitForIdle()
        assertEquals("Best waterfall?", sentPrompts.last())

        compose.onNodeWithTag("ai_concierge_close_btn").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("ai_concierge_sheet").assertDoesNotExist()
    }

    @Test
    fun alternatingHistorySkipsOrphanUserMessagesToPreventHttp400() {
        val repo = AiConciergeRepository()
        val historyWithFailedTurn = listOf(
            AiConciergeMessage("u1", isUser = true, text = "First question"),
            AiConciergeMessage("a1", isUser = false, text = "First answer"),
            AiConciergeMessage("u2", isUser = true, text = "Failed question with no reply")
        )
        val contents = repo.buildAlternatingContents(
            userPrompt = "Follow-up question",
            conversationHistory = historyWithFailedTurn,
            liveDumingagContext = "Weather: 28C"
        )
        // Expected roles: user -> model -> user (strictly alternating, never user -> user)
        assertEquals(listOf("user", "model", "user"), contents.map { it.role })
    }

    @Test
    fun sanitizeFormalResponseStripsMarkdownSymbolsAndKeepsCleanProse() {
        val repo = AiConciergeRepository()
        val rawMarkdown = """
            ### **Recommended Itinerary**
            * **Morning:** Visit *Danlugan Falls* (PHP 50 entrance fee).
            * **Afternoon:** Enjoy `organic rice` at Poblacion Market.
            
            
            **Safety Advisory:** Please bring rain gear.
        """.trimIndent()

        val cleaned = repo.sanitizeFormalResponse(rawMarkdown, isStreaming = false)
        assertEquals(false, cleaned.contains("*"))
        assertEquals(false, cleaned.contains("#"))
        assertEquals(false, cleaned.contains("`"))
        assertEquals(true, cleaned.contains("Recommended Itinerary"))
        assertEquals(true, cleaned.contains("Visit Danlugan Falls (PHP 50 entrance fee)."))
        assertEquals(true, cleaned.contains("Safety Advisory: Please bring rain gear."))
    }
}
