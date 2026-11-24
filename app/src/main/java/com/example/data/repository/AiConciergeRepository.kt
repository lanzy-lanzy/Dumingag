package com.example.data.repository

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.ResponseStoppedException
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

data class AiConciergeMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

class AiConciergeRepository {
    companion object {
        private const val TAG = "AiConciergeRepository"
        private const val MODEL_NAME = "gemini-3.5-flash-lite"
        private const val REQUEST_TIMEOUT_MS = 45_000L

        private val HEADING_REGEX = Regex("(?m)^\\s*#{1,6}\\s+")
        private val BULLET_REGEX = Regex("(?m)^\\s*[-*•]+\\s+")
        private val HR_REGEX = Regex("(?m)^\\s*---+$")
        private val UNDERSCORE_REGEX = Regex("(?<!\\w)_+|_+(?!\\w)")
        private val MULTI_SPACE_REGEX = Regex("[ \\t]{2,}")
        private val LEADING_DASH_REGEX = Regex("(?m)^ - ")
        private val MULTI_NEWLINE_REGEX = Regex("\n{3,}")
    }

    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.4f
                maxOutputTokens = 1024
            },
            systemInstruction = content {
                text(
                    "You are the official Dumingag Travel Concierge for the Municipal Tourism Guide of Dumingag, Zamboanga del Sur, Philippines. " +
                        "Respond promptly in a formal, courteous, and professional tone using clear, complete sentences. " +
                        "Keep replies concise and direct (about 100 to 220 words). " +
                        "Ground all recommendations strictly in the provided live Dumingag attractions, transit fares, weather, and active events; never invent places or phone numbers. " +
                        "Strict formatting: plain text only. Never use asterisks (* or **), hash symbols (#), underscores (_), backticks (`), or emojis. " +
                        "Use short paragraphs or numbered items (1., 2., 3.) with plain section labels ending in a colon (e.g., Recommended Itinerary:, Estimated Expenses:). " +
                        "State prices in PHP."
                )
            }
        )
    }

    /**
     * Streams a fast Gemini reply directly on [Dispatchers.IO] within a 45s timeout window,
     * invoking [onChunk] on each received text piece and returning the polished formal reply.
     */
    suspend fun streamConciergeReply(
        userPrompt: String,
        conversationHistory: List<AiConciergeMessage>,
        liveDumingagContext: String,
        onChunk: suspend (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val contents = buildAlternatingContents(
            userPrompt = userPrompt,
            conversationHistory = conversationHistory,
            liveDumingagContext = liveDumingagContext
        )

        val accumulated = StringBuilder()
        try {
            withTimeout(REQUEST_TIMEOUT_MS) {
                model.generateContentStream(contents).collect { chunk ->
                    val piece = runCatching { chunk.text }.getOrNull()
                    if (!piece.isNullOrEmpty()) {
                        accumulated.append(piece)
                        onChunk(sanitizeFormalResponse(accumulated.toString(), isStreaming = true))
                    }
                }
            }
            sanitizeFormalResponse(accumulated.toString(), isStreaming = false)
        } catch (e: ResponseStoppedException) {
            Log.w(TAG, "AI concierge stopped early: ${e.localizedMessage}", e)
            val partial = sanitizeFormalResponse(accumulated.toString(), isStreaming = false)
            if (partial.isNotEmpty()) {
                partial
            } else {
                throw IllegalStateException("The response could not be completed (${e.localizedMessage ?: "generation limit reached"}). Please try a shorter question.", e)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI concierge timed out", e)
            throw IllegalStateException("The AI concierge timed out after 45 seconds. Please check your connection and try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.e(TAG, "AI concierge request failed", t)
            if (t is Exception) throw t
            throw IllegalStateException(t.localizedMessage ?: "AI Concierge encountered an unexpected error (${t.javaClass.simpleName}).", t)
        }
    }

    /**
     * Strips stray markdown control characters (*, **, #, `, _) so responses always render as
     * crisp, formal, publication-quality plain text.
     */
    internal fun sanitizeFormalResponse(raw: String, isStreaming: Boolean = false): String {
        if (raw.isEmpty()) return ""
        val normalized = raw
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace(HEADING_REGEX, "")
            .replace(BULLET_REGEX, "  - ")
            .replace(HR_REGEX, "")
            .replace("**", "")
            .replace("__", "")
            .replace("*", "")
            .replace("`", "")
            .replace(UNDERSCORE_REGEX, "")
            .replace(MULTI_SPACE_REGEX, " ")
            .replace(LEADING_DASH_REGEX, "  - ")
            .replace(MULTI_NEWLINE_REGEX, "\n\n")
        return if (isStreaming) normalized.trimStart() else normalized.trim()
    }

    internal fun buildAlternatingContents(
        userPrompt: String,
        conversationHistory: List<AiConciergeMessage>,
        liveDumingagContext: String
    ): List<Content> = buildList {
        // Keep only valid alternating (user -> model) pairs from prior turns so retries
        // or failed turns never produce back-to-back "user" messages (which cause HTTP 400).
        val validPairs = mutableListOf<Pair<AiConciergeMessage, AiConciergeMessage>>()
        var i = 0
        while (i + 1 < conversationHistory.size) {
            val first = conversationHistory[i]
            val second = conversationHistory[i + 1]
            if (first.isUser && !second.isUser && first.text.isNotBlank() && second.text.isNotBlank()) {
                validPairs.add(first to second)
                i += 2
            } else {
                i += 1
            }
        }

        val recentPairs = validPairs.takeLast(2)
        if (recentPairs.isEmpty()) {
            add(
                content(role = "user") {
                    text("Dumingag Context:\n$liveDumingagContext\n\nQuestion:\n$userPrompt")
                }
            )
        } else {
            recentPairs.forEachIndexed { idx, (uMsg, mMsg) ->
                val userText = if (idx == 0) {
                    "Dumingag Context:\n$liveDumingagContext\n\nQuestion:\n${uMsg.text}"
                } else {
                    uMsg.text
                }
                add(content(role = "user") { text(userText) })
                add(content(role = "model") { text(mMsg.text) })
            }
            add(
                content(role = "user") {
                    text(userPrompt)
                }
            )
        }
    }
}
