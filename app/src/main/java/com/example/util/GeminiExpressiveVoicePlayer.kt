package com.example.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.util.LinkedHashMap

/**
 * Expressive human voice personas backed by Gemini 3.1 Flash TTS (`gemini-3.1-flash-tts-preview`)
 * via Firebase AI Logic. Each persona pairs a prebuilt Gemini voice with natural emotional acting
 * direction so replies sound warm, caring, and genuinely human rather than robotic system TTS.
 */
enum class ConciergeVoicePersona(
    val displayName: String,
    val voiceName: String,
    val tagline: String,
    val emotionDirection: String
) {
    WARM_GUIDE(
        displayName = "Warm Guide",
        voiceName = "Aoede",
        tagline = "Warm & welcoming",
        emotionDirection = "Speak with genuine Filipino hospitality, a warm smile in your voice, affectionate enthusiasm, and natural conversational flow."
    ),
    CALM_STORYTELLER(
        displayName = "Storyteller",
        voiceName = "Kore",
        tagline = "Gentle & heartfelt",
        emotionDirection = "Speak gently with heartfelt admiration, soothing calm pacing, soft expressive pauses, and tender warmth like sharing a cherished hometown story."
    ),
    UPBEAT_EXPLORER(
        displayName = "Upbeat Explorer",
        voiceName = "Puck",
        tagline = "Lively & cheerful",
        emotionDirection = "Speak with bright, cheerful energy, authentic excitement, lively vocal variety, and encouraging friendliness like an enthusiastic travel companion."
    ),
    FORMAL_HOST(
        displayName = "Formal Host",
        voiceName = "Charon",
        tagline = "Poised & caring",
        emotionDirection = "Speak with gracious dignity, calm confidence, articulate clarity, and courteous human warmth suited to an official tourism host."
    )
}

/**
 * UI state for Gemini expressive voice synthesis and playback inside the AI Concierge chat.
 */
data class VoicePlaybackUiState(
    val activeMessageId: String? = null,
    val isSynthesizing: Boolean = false,
    val isSpeaking: Boolean = false,
    val selectedPersona: ConciergeVoicePersona = ConciergeVoicePersona.WARM_GUIDE,
    val autoSpeakEnabled: Boolean = false,
    val voiceError: String? = null
)

/**
 * Generates and streams lifelike, emotion-rich human speech using Firebase AI Logic's
 * `gemini-3.1-flash-tts-preview` model and plays raw 24 kHz PCM audio directly via [AudioTrack].
 */
class GeminiExpressiveVoicePlayer {

    companion object {
        private const val TAG = "GeminiVoicePlayer"
        private const val TTS_MODEL_NAME = "gemini-3.1-flash-tts-preview"
        private const val DEFAULT_SAMPLE_RATE_HZ = 24_000
        private const val SYNTHESIS_TIMEOUT_MS = 45_000L
        private const val MAX_CACHED_CLIPS = 6
        private const val MAX_SPOKEN_CHARS = 1200

        private val RATE_REGEX = Regex("rate=(\\d+)", RegexOption.IGNORE_CASE)
        private val PHP_AMOUNT_REGEX = Regex(
            "\\bPHP\\s*([0-9]+(?:,[0-9]{3})*(?:\\.[0-9]+)?(?:\\s*[\\-–]\\s*[0-9]+(?:,[0-9]{3})*(?:\\.[0-9]+)?)?)",
            RegexOption.IGNORE_CASE
        )
        private val BRGY_REGEX = Regex("\\bBrgy\\.?", RegexOption.IGNORE_CASE)
        private val KMH_REGEX = Regex("\\bkm/h\\b", RegexOption.IGNORE_CASE)
        private val CELSIUS_REGEX = Regex("(\\d+)\\s*°\\s*C\\b", RegexOption.IGNORE_CASE)
        private val WHITESPACE_REGEX = Regex("\\s+")

        /**
         * Normalizes travel abbreviations and currency codes into natural spoken words so
         * Gemini pronounces Philippine places, fares, and weather fluidly.
         */
        internal fun normalizeForNaturalSpeech(rawText: String): String {
            return rawText
                .replace(PHP_AMOUNT_REGEX, "$1 pesos")
                .replace(BRGY_REGEX, "Barangay")
                .replace(KMH_REGEX, "kilometers per hour")
                .replace(CELSIUS_REGEX, "$1 degrees Celsius")
                .replace("–", " to ")
                .replace("—", ", ")
                .replace(WHITESPACE_REGEX, " ")
                .trim()
                .take(MAX_SPOKEN_CHARS)
        }

        /**
         * Infers subtle contextual emotional nuance from the message content so Gemini adapts
         * its vocal feeling naturally (e.g., reassuring care for rain/advisories, wonder for nature).
         */
        internal fun inferContextualEmotionCue(messageText: String): String {
            val lower = messageText.lowercase()
            return when {
                listOf("rain", "storm", "typhoon", "warning", "advisory", "caution", "slippery", "emergency", "safety")
                    .any { lower.contains(it) } ->
                    "Convey caring, reassuring concern and helpful attentiveness for the traveler's safety."
                listOf("waterfall", "sunrise", "sunset", "peak", "festival", "scenic", "breathtaking", "viewpoint", "falls")
                    .any { lower.contains(it) } ->
                    "Convey genuine wonder, warmth, and inviting excitement about the natural beauty of Dumingag."
                listOf("food", "organic", "dining", "coffee", "market", "culture", "heritage", "family")
                    .any { lower.contains(it) } ->
                    "Convey heartfelt local pride and cozy, welcoming hospitality."
                else ->
                    "Convey friendly, attentive care and natural human presence."
            }
        }

        /**
         * Builds the expressive TTS prompt instructing `gemini-3.1-flash-tts-preview` to speak
         * with human emotion, natural breathing/pauses, and verbatim message delivery.
         */
        internal fun buildExpressiveSpeechPrompt(
            messageText: String,
            persona: ConciergeVoicePersona
        ): String {
            val spokenText = normalizeForNaturalSpeech(messageText)
            val contextCue = inferContextualEmotionCue(spokenText)
            return "Read the following message aloud like a real, caring human tour guide speaking warmly to a traveler in person. " +
                "Voice and feeling: ${persona.emotionDirection} $contextCue " +
                "Use natural human intonation, authentic emotion, subtle emphasis on key places and tips, and relaxed pauses between sentences. " +
                "Speak only the message below without adding introductions or commentary:\n\n$spokenText"
        }

        /**
         * Extracts the PCM sample rate from a Gemini audio MIME type (e.g. `audio/L16;codec=pcm;rate=24000`).
         */
        internal fun parseSampleRateHz(mimeType: String?): Int {
            if (mimeType.isNullOrBlank()) return DEFAULT_SAMPLE_RATE_HZ
            val match = RATE_REGEX.find(mimeType)
            return match?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(8_000, 48_000)
                ?: DEFAULT_SAMPLE_RATE_HZ
        }

        /**
         * Strips a standard 44-byte RIFF/WAVE header if the returned chunk is WAV-wrapped PCM.
         */
        internal fun stripWavHeaderIfPresent(bytes: ByteArray): ByteArray {
            if (bytes.size > 44 &&
                bytes[0] == 'R'.code.toByte() &&
                bytes[1] == 'I'.code.toByte() &&
                bytes[2] == 'F'.code.toByte() &&
                bytes[3] == 'F'.code.toByte() &&
                bytes[8] == 'W'.code.toByte() &&
                bytes[9] == 'A'.code.toByte() &&
                bytes[10] == 'V'.code.toByte() &&
                bytes[11] == 'E'.code.toByte()
            ) {
                return bytes.copyOfRange(44, bytes.size)
            }
            return bytes
        }
    }

    private data class CachedAudioClip(
        val pcmBytes: ByteArray,
        val sampleRateHz: Int
    )

    private val modelCache = mutableMapOf<ConciergeVoicePersona, GenerativeModel>()
    private val audioCache = object : LinkedHashMap<String, CachedAudioClip>(MAX_CACHED_CLIPS + 1, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedAudioClip>?): Boolean {
            return size > MAX_CACHED_CLIPS
        }
    }

    private val trackLock = Any()
    @Volatile
    private var activeTrack: AudioTrack? = null

    @OptIn(PublicPreviewAPI::class)
    private fun getOrCreateTtsModel(persona: ConciergeVoicePersona): GenerativeModel {
        return synchronized(modelCache) {
            modelCache.getOrPut(persona) {
                Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                    modelName = TTS_MODEL_NAME,
                    generationConfig = generationConfig {
                        responseModalities = listOf(ResponseModality.AUDIO)
                        speechConfig = SpeechConfig(voice = Voice(persona.voiceName))
                    }
                )
            }
        }
    }

    /**
     * Synthesizes (or retrieves from cache) and plays an AI Concierge message with expressive
     * human Gemini voice. Calls [onPlaybackStarted] as soon as the first audio chunk begins playing.
     */
    suspend fun speakMessage(
        messageId: String,
        messageText: String,
        persona: ConciergeVoicePersona,
        onPlaybackStarted: suspend () -> Unit
    ) = withContext(Dispatchers.IO) {
        stop()

        val cacheKey = "${messageId}_${persona.name}"
        val cached = synchronized(audioCache) { audioCache[cacheKey] }
        if (cached != null && cached.pcmBytes.isNotEmpty()) {
            onPlaybackStarted()
            playPcmBuffer(cached.pcmBytes, cached.sampleRateHz)
            return@withContext
        }

        val ttsPrompt = buildExpressiveSpeechPrompt(messageText, persona)
        val model = getOrCreateTtsModel(persona)
        val collectedPcm = ByteArrayOutputStream()
        var track: AudioTrack? = null
        var detectedSampleRate = DEFAULT_SAMPLE_RATE_HZ
        var hasStartedPlayback = false
        var isFirstAudioChunk = true

        try {
            withTimeout(SYNTHESIS_TIMEOUT_MS) {
                model.generateContentStream(ttsPrompt).collect { chunk ->
                    currentCoroutineContext().ensureActive()
                    val audioParts = extractAudioParts(chunk.inlineDataParts, chunk.candidates.firstOrNull()?.content?.parts)
                    for (part in audioParts) {
                        val rawBytes = part.inlineData
                        if (rawBytes.isEmpty()) continue

                        val pcmBytes = if (isFirstAudioChunk) {
                            isFirstAudioChunk = false
                            detectedSampleRate = parseSampleRateHz(part.mimeType)
                            stripWavHeaderIfPresent(rawBytes)
                        } else {
                            rawBytes
                        }
                        if (pcmBytes.isEmpty()) continue

                        collectedPcm.write(pcmBytes)

                        if (track == null) {
                            track = createStreamingAudioTrack(detectedSampleRate)
                            synchronized(trackLock) { activeTrack = track }
                            track?.play()
                        }
                        if (!hasStartedPlayback) {
                            hasStartedPlayback = true
                            onPlaybackStarted()
                        }

                        writePcmChunk(track, pcmBytes)
                    }
                }
            }

            if (collectedPcm.size() == 0) {
                throw IllegalStateException("Gemini Voice returned no audio data. Please try again.")
            }

            synchronized(audioCache) {
                audioCache[cacheKey] = CachedAudioClip(
                    pcmBytes = collectedPcm.toByteArray(),
                    sampleRateHz = detectedSampleRate
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini TTS timed out", e)
            throw IllegalStateException("Expressive voice generation timed out. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.e(TAG, "Gemini TTS failed", t)
            if (t is Exception) throw t
            throw IllegalStateException(t.localizedMessage ?: "Unable to generate Gemini Voice right now.", t)
        } finally {
            releaseTrackSafely(track)
        }
    }

    /**
     * Immediately halts any in-progress Gemini voice playback and releases hardware audio resources.
     */
    fun stop() {
        val current = synchronized(trackLock) {
            val t = activeTrack
            activeTrack = null
            t
        }
        if (current != null) {
            runCatching {
                if (current.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    current.pause()
                    current.flush()
                    current.stop()
                }
            }
            runCatching { current.release() }
        }
    }

    fun release() {
        stop()
        synchronized(audioCache) { audioCache.clear() }
    }

    private fun extractAudioParts(
        directParts: List<InlineDataPart>,
        candidateParts: List<com.google.firebase.ai.type.Part>?
    ): List<InlineDataPart> {
        val source = if (directParts.isNotEmpty()) {
            directParts
        } else {
            candidateParts.orEmpty().filterIsInstance<InlineDataPart>()
        }
        return source.filter { part ->
            part.mimeType.startsWith("audio/", ignoreCase = true) || part.inlineData.isNotEmpty()
        }
    }

    private fun createStreamingAudioTrack(sampleRateHz: Int): AudioTrack {
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRateHz,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRateHz)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    private suspend fun playPcmBuffer(pcmBytes: ByteArray, sampleRateHz: Int) {
        var track: AudioTrack? = null
        try {
            track = createStreamingAudioTrack(sampleRateHz)
            synchronized(trackLock) { activeTrack = track }
            track.play()
            writePcmChunk(track, pcmBytes)
        } finally {
            releaseTrackSafely(track)
        }
    }

    private suspend fun writePcmChunk(track: AudioTrack?, pcmBytes: ByteArray) {
        if (track == null || pcmBytes.isEmpty()) return
        var offset = 0
        val chunkSize = 4096
        while (offset < pcmBytes.size) {
            currentCoroutineContext().ensureActive()
            val toWrite = minOf(chunkSize, pcmBytes.size - offset)
            val written = track.write(pcmBytes, offset, toWrite, AudioTrack.WRITE_BLOCKING)
            if (written <= 0) break
            offset += written
        }
    }

    private fun releaseTrackSafely(track: AudioTrack?) {
        if (track == null) return
        val shouldRelease = synchronized(trackLock) {
            if (activeTrack === track) {
                activeTrack = null
                true
            } else {
                false
            }
        }
        if (shouldRelease) {
            runCatching {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop()
                }
            }
            runCatching { track.release() }
        }
    }
}
