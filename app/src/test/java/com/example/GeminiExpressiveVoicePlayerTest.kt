package com.example

import com.example.util.ConciergeVoicePersona
import com.example.util.GeminiExpressiveVoicePlayer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiExpressiveVoicePlayerTest {

    @Test
    fun normalizeForNaturalSpeech_expandsPhilippineTravelAbbreviations() {
        val normalized = GeminiExpressiveVoicePlayer.normalizeForNaturalSpeech(
            "Visit Brgy. Poblacion with PHP 150–250 fare, 28°C weather, and 12 km/h wind."
        )
        assertTrue(normalized.contains("Barangay Poblacion"))
        assertTrue(normalized.contains("150 to 250 pesos"))
        assertTrue(normalized.contains("28 degrees Celsius"))
        assertTrue(normalized.contains("kilometers per hour"))
    }

    @Test
    fun inferContextualEmotionCue_adaptsToneToTravelContext() {
        val safetyCue = GeminiExpressiveVoicePlayer.inferContextualEmotionCue(
            "Heavy rain advisory: trails may be slippery today."
        )
        assertTrue(safetyCue.contains("reassuring", ignoreCase = true))

        val natureCue = GeminiExpressiveVoicePlayer.inferContextualEmotionCue(
            "Enjoy a breathtaking sunrise viewpoint and waterfall hike."
        )
        assertTrue(natureCue.contains("wonder", ignoreCase = true))
    }

    @Test
    fun buildExpressiveSpeechPrompt_includesPersonaEmotionAndMessage() {
        val prompt = GeminiExpressiveVoicePlayer.buildExpressiveSpeechPrompt(
            messageText = "Welcome to Brgy. Poblacion! Budget PHP 300.",
            persona = ConciergeVoicePersona.WARM_GUIDE
        )
        assertTrue(prompt.contains("Filipino hospitality"))
        assertTrue(prompt.contains("Barangay Poblacion"))
        assertTrue(prompt.contains("300 pesos"))
    }

    @Test
    fun parseSampleRateHz_extractsRateFromMimeTypeOrFallsBackTo24kHz() {
        assertEquals(24_000, GeminiExpressiveVoicePlayer.parseSampleRateHz("audio/L16;codec=pcm;rate=24000"))
        assertEquals(16_000, GeminiExpressiveVoicePlayer.parseSampleRateHz("audio/pcm; rate=16000"))
        assertEquals(24_000, GeminiExpressiveVoicePlayer.parseSampleRateHz(null))
        assertEquals(24_000, GeminiExpressiveVoicePlayer.parseSampleRateHz("audio/pcm"))
    }

    @Test
    fun stripWavHeaderIfPresent_strips44ByteRiffWaveHeaderOnly() {
        val rawPcm = byteArrayOf(1, 2, 3, 4, 5, 6)
        assertArrayEquals(rawPcm, GeminiExpressiveVoicePlayer.stripWavHeaderIfPresent(rawPcm))

        val wavBytes = ByteArray(50)
        "RIFF".toByteArray().copyInto(wavBytes, 0)
        "WAVE".toByteArray().copyInto(wavBytes, 8)
        rawPcm.copyInto(wavBytes, 44)
        assertArrayEquals(rawPcm, GeminiExpressiveVoicePlayer.stripWavHeaderIfPresent(wavBytes))
    }
}
