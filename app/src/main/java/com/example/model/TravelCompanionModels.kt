package com.example.model

import androidx.annotation.Keep

enum class AppThemeMode(val code: String, val label: String, val subtitle: String) {
    SYSTEM("system", "System", "Follows device light/dark mode"),
    LIGHT("light", "Light Glass", "Frosted bright emerald & warm cream"),
    DARK("dark", "Deep Emerald Dark", "Nocturnal forest glass & luminous mint");

    companion object {
        fun fromCode(raw: String?): AppThemeMode =
            entries.firstOrNull { it.code.equals(raw, ignoreCase = true) } ?: SYSTEM
    }
}

data class ElevationSample(
    val distanceKm: Double,
    val elevationMeters: Double
)

data class ElevationProfile(
    val samples: List<ElevationSample>,
    val minMeters: Int,
    val maxMeters: Int,
    val gainMeters: Int,
    val lossMeters: Int
)

data class FareEstimateOption(
    val modeName: String,
    val fareRangePhp: String,
    val minFarePhp: Int,
    val maxFarePhp: Int,
    val durationText: String,
    val suitabilityNote: String
)

data class PackingItem(
    val id: String,
    val label: String,
    val category: String,
    val reason: String,
    val isWeatherRecommended: Boolean,
    val checked: Boolean
)

@Keep
data class TripExpense(
    val id: String = "",
    val title: String = "",
    val category: String = "Meals",
    val amountPhp: Double = 0.0,
    val spotName: String = "",
    val createdAtMillis: Long = 0L
)

@Keep
data class TrailConditionReport(
    val id: String = "",
    val attractionId: String = "",
    val conditionTag: String = "Clear trail",
    val crowdLevel: String = "Quiet",
    val note: String = "",
    val reporterName: String = "",
    val reportedAtMillis: Long = 0L
)

data class EcoBadge(
    val id: String,
    val title: String,
    val subtitle: String,
    val unlocked: Boolean,
    val progressText: String,
    val iconKey: String
)

data class PhraseEntry(
    val id: String,
    val category: String,
    val english: String,
    val cebuano: String,
    val tagalog: String,
    val subanen: String,
    val pronunciation: String
)

data class LocalSpecialty(
    val id: String,
    val name: String,
    val category: String,
    val whereToFind: String,
    val priceHint: String,
    val scheduleNote: String,
    val description: String,
    val highlightTag: String
)

data class TileDownloadProgress(
    val isRunning: Boolean = false,
    val downloaded: Int = 0,
    val total: Int = 0,
    val cachedTilesCount: Int = 0
)

data class OptimizedItineraryResult(
    val orderedItems: List<ItineraryItem>,
    val originalDistanceKm: Double,
    val optimizedDistanceKm: Double,
    val savedDistanceKm: Double
)
