package com.example.model

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import com.example.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

object AttractionImageMapper {
    val presetKeys: List<Pair<String, String>> = listOf(
        "waterfall" to "Cascades & Caves",
        "heritage" to "Heritage & Plaza",
        "organic" to "Organic Farm & Market",
        "highland" to "Highland & Terraces"
    )

    @DrawableRes
    fun resForKey(key: String?): Int = when (key?.lowercase()) {
        "heritage", "church" -> R.drawable.img_heritage_plaza
        "organic", "market" -> R.drawable.img_organic_village
        "highland", "terrace" -> R.drawable.img_dumingag_hero
        else -> R.drawable.img_waterfall_falls
    }

    fun keyForRes(@DrawableRes resId: Int): String = when (resId) {
        R.drawable.img_heritage_plaza -> "heritage"
        R.drawable.img_organic_village -> "organic"
        R.drawable.img_dumingag_hero -> "highland"
        else -> "waterfall"
    }

    fun defaultKeyForCategory(category: AttractionCategory): String = when (category) {
        AttractionCategory.HISTORICAL, AttractionCategory.CULTURAL -> "heritage"
        AttractionCategory.ORGANIC_FARM -> "organic"
        AttractionCategory.LANDMARK -> "highland"
        AttractionCategory.NATURE, AttractionCategory.ALL -> "waterfall"
    }
}

enum class AppLanguage(val code: String, val displayName: String, val nativeLabel: String) {
    EN("en", "English", "English"),
    CEB("ceb", "Cebuano", "Binisaya"),
    TL("tl", "Tagalog", "Filipino"),
    ES("es", "Español", "Español");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: EN
    }
}

data class LocalizedText(
    val en: String,
    val ceb: String,
    val tl: String,
    val es: String
) {
    fun get(language: AppLanguage): String = when (language) {
        AppLanguage.EN -> en
        AppLanguage.CEB -> ceb
        AppLanguage.TL -> tl
        AppLanguage.ES -> es
    }
}

enum class AttractionCategory {
    ALL,
    HISTORICAL,
    NATURE,
    CULTURAL,
    LANDMARK,
    ORGANIC_FARM
}

enum class TravelMode(val speedKmH: Double) {
    WALK(4.5),
    HABAL_HABAL(28.0),
    JEEPNEY_CAR(38.0)
}

data class Attraction(
    val id: String,
    val mapNodeId: String,
    val name: LocalizedText,
    val subtitle: LocalizedText,
    val description: LocalizedText,
    val historyNote: LocalizedText,
    val barangay: String,
    val category: AttractionCategory,
    val latitude: Double,
    val longitude: Double,
    val mapX: Float,
    val mapY: Float,
    @DrawableRes val imageRes: Int,
    val openHours: String,
    val entryFee: LocalizedText,
    val bestTime: LocalizedText,
    val transitHint: LocalizedText,
    val baseRating: Double,
    val imageKey: String = AttractionImageMapper.keyForRes(imageRes),
    val photoData: String = "",
    val categoryCode: String = category.name,
    val galleryPhotos: List<String> = emptyList()
)

data class OfflineMapNode(
    val id: String,
    val name: LocalizedText,
    val mapX: Float,
    val mapY: Float,
    val latitude: Double,
    val longitude: Double,
    val isLandmark: Boolean = false
)

data class OfflineMapEdge(
    val fromId: String,
    val toId: String,
    val distanceKm: Double,
    val roadName: String,
    val roadType: String
)

data class NavigationStep(
    val index: Int,
    val fromNodeName: String,
    val toNodeName: String,
    val distanceKm: Double,
    val bearingLabel: LocalizedText,
    val instruction: LocalizedText,
    val roadName: String
)

data class OfflineRoutePlan(
    val originName: String,
    val destination: Attraction,
    val travelMode: TravelMode,
    val totalDistanceKm: Double,
    val estimatedMinutes: Int,
    val nodePathIds: List<String>,
    val steps: List<NavigationStep>
)

data class TransitRoute(
    val id: String,
    val title: LocalizedText,
    val operatorName: String,
    val vehicleType: String,
    val scheduleSummary: LocalizedText,
    val fareRangePhp: String,
    val durationText: String,
    val terminalNote: LocalizedText
)

data class EmergencyContact(
    val id: String,
    val title: LocalizedText,
    val department: String,
    val phoneNumber: String,
    val availability: String,
    val priorityLevel: String,
    val email: String? = null,
    val websiteUrl: String? = null
)

data class DailyForecast(
    val dateIso: String,
    val weatherCode: Int,
    val tempMaxC: Double,
    val tempMinC: Double,
    val precipChancePct: Int,
    val conditionLabel: String = "",
    val humidityPct: Int = 75
)

data class WeatherSnapshot(
    val temperatureC: Double,
    val apparentTempC: Double,
    val humidityPct: Int,
    val windSpeedKmh: Double,
    val weatherCode: Int,
    val isFromCache: Boolean,
    val updatedAtMillis: Long,
    val dailyForecasts: List<DailyForecast>,
    val conditionDescription: String = "",
    val sourceLabel: String = "OpenWeatherMap",
    val isApiKeyMissing: Boolean = false,
    val highTempC: Double = temperatureC,
    val lowTempC: Double = temperatureC,
    val pressureHpa: Int = 1012,
    val isSample: Boolean = false
)

data class SavedDocument(
    val id: String,
    val title: String,
    val category: String,
    val summary: String,
    val contentBody: String,
    val fileUri: String? = null,
    val savedAtMillis: Long,
    val isBundled: Boolean = false
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

// --- Firestore Models (Strictly aligned with firestore.rules and firebase-blueprint.json) ---

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val role: String = "regular",
    val displayName: String = "",
    val photoData: String = "",
    val favoriteAttractionIds: List<String> = emptyList(),
    val preferredLanguage: String = "en",
    val offlinePackEnabled: Boolean = false,
    @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
    @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun normalizedRole(): String =
        if (role.trim().equals("admin", ignoreCase = true)) "admin" else "regular"
}

@Keep
@IgnoreExtraProperties
data class AdminRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val grantedBy: String = "",
    @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudAttraction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mapNodeId: String = "node_poblacion",
    val nameEn: String = "",
    val nameCeb: String = "",
    val nameTl: String = "",
    val nameEs: String = "",
    val subtitleEn: String = "",
    val descriptionEn: String = "",
    val historyNoteEn: String = "",
    val barangay: String = "",
    val category: String = AttractionCategory.NATURE.name,
    val latitude: Double = 8.1650,
    val longitude: Double = 123.3465,
    val openHours: String = "06:00 AM – 05:00 PM",
    val entryFeeEn: String = "Free entry",
    val bestTimeEn: String = "Morning",
    val transitHintEn: String = "Accessible from Poblacion Plaza",
    val baseRating: Double = 4.7,
    val imageKey: String = "waterfall",
    val photoData: String = "",
    val galleryPhotos: List<String> = emptyList(),
    val visibility: String = "public",
    @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
    @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toDomain(): Attraction {
        val cat = enumValues<AttractionCategory>()
            .firstOrNull { it.name == category && it != AttractionCategory.ALL }
            ?: AttractionCategory.LANDMARK
        val resolvedEn = nameEn.ifBlank { "Dumingag Spot" }
        val resolvedHistory = historyNoteEn.ifBlank { subtitleEn.ifBlank { descriptionEn } }
        val projX = (((longitude - 123.30) / 0.09).toFloat()).coerceIn(0.08f, 0.92f)
        val projY = (1f - ((latitude - 8.12) / 0.09).toFloat()).coerceIn(0.08f, 0.92f)
        val resolvedKey = imageKey.ifBlank { AttractionImageMapper.defaultKeyForCategory(cat) }

        val normalizedCode = CategoryCatalog.normalizeCode(category).ifBlank { cat.name }
        return Attraction(
            id = id,
            mapNodeId = mapNodeId.ifBlank { "node_poblacion" },
            name = LocalizedText(
                en = resolvedEn,
                ceb = nameCeb.ifBlank { resolvedEn },
                tl = nameTl.ifBlank { resolvedEn },
                es = nameEs.ifBlank { resolvedEn }
            ),
            subtitle = LocalizedText(subtitleEn, subtitleEn, subtitleEn, subtitleEn),
            description = LocalizedText(descriptionEn, descriptionEn, descriptionEn, descriptionEn),
            historyNote = LocalizedText(resolvedHistory, resolvedHistory, resolvedHistory, resolvedHistory),
            barangay = barangay,
            category = cat,
            latitude = latitude,
            longitude = longitude,
            mapX = projX,
            mapY = projY,
            imageRes = AttractionImageMapper.resForKey(resolvedKey),
            openHours = openHours,
            entryFee = LocalizedText(entryFeeEn, entryFeeEn, entryFeeEn, entryFeeEn),
            bestTime = LocalizedText(bestTimeEn, bestTimeEn, bestTimeEn, bestTimeEn),
            transitHint = LocalizedText(transitHintEn, transitHintEn, transitHintEn, transitHintEn),
            baseRating = baseRating.coerceIn(1.0, 5.0),
            imageKey = resolvedKey,
            photoData = photoData,
            categoryCode = normalizedCode,
            galleryPhotos = galleryPhotos.filter { it.isNotBlank() }.take(4)
        )
    }
}

@Keep
@IgnoreExtraProperties
data class ManagedCategory(
    @DocumentId val id: String = "",
    val userId: String = "",
    val code: String = "",
    val name: String = "",
    val iconKey: String = "forest",
    val sortOrder: Long = 0L,
    val visibility: String = "public",
    @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
    @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
) {
    val effectiveCode: String
        get() = CategoryCatalog.normalizeCode(code.ifBlank { id })
}

object CategoryCatalog {
    val iconOptions: List<Pair<String, String>> = listOf(
        "forest" to "Nature / Forest",
        "water" to "Waterfalls / River",
        "history" to "Historical / Heritage",
        "palette" to "Cultural / Arts",
        "landmark" to "Landmark / Plaza",
        "eco" to "Organic Farm / Eco",
        "mountain" to "Highlands / Peak",
        "camp" to "Camping / Resort",
        "restaurant" to "Food / Cafe",
        "camera" to "Viewpoint / Scenic"
    )

    val defaultCategories: List<ManagedCategory> = listOf(
        ManagedCategory(
            id = AttractionCategory.NATURE.name,
            code = AttractionCategory.NATURE.name,
            name = "Nature",
            iconKey = "forest",
            sortOrder = 0L
        ),
        ManagedCategory(
            id = AttractionCategory.HISTORICAL.name,
            code = AttractionCategory.HISTORICAL.name,
            name = "Historical",
            iconKey = "history",
            sortOrder = 1L
        ),
        ManagedCategory(
            id = AttractionCategory.CULTURAL.name,
            code = AttractionCategory.CULTURAL.name,
            name = "Cultural",
            iconKey = "palette",
            sortOrder = 2L
        ),
        ManagedCategory(
            id = AttractionCategory.LANDMARK.name,
            code = AttractionCategory.LANDMARK.name,
            name = "Landmark",
            iconKey = "landmark",
            sortOrder = 3L
        ),
        ManagedCategory(
            id = AttractionCategory.ORGANIC_FARM.name,
            code = AttractionCategory.ORGANIC_FARM.name,
            name = "Organic Farm",
            iconKey = "eco",
            sortOrder = 4L
        )
    )

    private val defaultNamesByCode: Map<String, String> = defaultCategories.associate { it.code to it.name }

    /**
     * Normalizes a user-typed category name or code into a Firestore-valid uppercase code (`^[A-Z0-9_]{2,40}$`, never `"ALL"`).
     */
    fun normalizeCode(raw: String): String {
        val cleaned = raw.trim()
            .uppercase()
            .replace(Regex("[^A-Z0-9]+"), "_")
            .trim('_')
            .take(40)
        return when {
            cleaned.length < 2 -> ""
            cleaned == "ALL" -> "CUSTOM_ALL"
            else -> cleaned
        }
    }

    fun isValidCode(code: String): Boolean {
        return code.length in 2..40 && code != "ALL" && code.matches(Regex("^[A-Z0-9_]+$"))
    }

    fun defaultImageKeyForCode(code: String, iconKey: String = ""): String {
        val normalized = normalizeCode(code)
        return when {
            normalized == AttractionCategory.HISTORICAL.name ||
                normalized == AttractionCategory.CULTURAL.name ||
                iconKey in listOf("history", "palette") -> "heritage"
            normalized == AttractionCategory.ORGANIC_FARM.name ||
                iconKey in listOf("eco", "restaurant") -> "organic"
            normalized == AttractionCategory.LANDMARK.name ||
                iconKey in listOf("landmark", "mountain", "camera") -> "highland"
            else -> "waterfall"
        }
    }

    fun fallbackEnumForCode(code: String): AttractionCategory {
        val normalized = normalizeCode(code)
        return enumValues<AttractionCategory>()
            .firstOrNull { it.name == normalized && it != AttractionCategory.ALL }
            ?: AttractionCategory.LANDMARK
    }

    fun prettyDisplayNameFromCode(code: String): String {
        return code.trim()
            .split('_')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.lowercase().replaceFirstChar { it.uppercaseChar() }
            }
            .ifBlank { "Attraction" }
    }

    fun isDefaultNameUnmodified(code: String, name: String): Boolean {
        val expected = defaultNamesByCode[code] ?: return false
        return expected.equals(name.trim(), ignoreCase = true)
    }
}

@Keep
@IgnoreExtraProperties
data class CloudEmergencyContact(
    @DocumentId val id: String = "",
    val userId: String = "",
    val titleEn: String = "",
    val department: String = "",
    val phoneNumber: String = "",
    val availability: String = "",
    val priorityLevel: String = "STANDARD",
    val email: String = "",
    val websiteUrl: String = "",
    val visibility: String = "public",
    @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
    @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toDomain(): EmergencyContact = EmergencyContact(
        id = id,
        title = LocalizedText(titleEn, titleEn, titleEn, titleEn),
        department = department,
        phoneNumber = phoneNumber,
        availability = availability,
        priorityLevel = priorityLevel,
        email = email.takeIf { it.isNotBlank() },
        websiteUrl = websiteUrl.takeIf { it.isNotBlank() }
    )
}

@Keep
@IgnoreExtraProperties
data class ItineraryItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val attractionId: String = "",
    val attractionName: String = "",
    val plannedDateMillis: Long = 0L,
    val notes: String = "",
    val reminderEnabled: Boolean = true,
    val calendarSynced: Boolean = false,
    @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
    @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CommunityReview(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val attractionId: String = "",
    val attractionName: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    val visitTag: String = "Eco-Tour",
    val photoData: String = "",
    val verifiedVisit: Boolean = false,
    val helpfulUserIds: List<String> = emptyList(),
    val adminReply: String = "",
    val adminReplyBy: String = "",
    val visibility: String = "public",
    @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
    @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
)

enum class TourismEventType(val code: String, val label: String) {
    FESTIVAL("FESTIVAL", "Festival"),
    MARKET_DAY("MARKET_DAY", "Market Day"),
    TRAIL_ADVISORY("TRAIL_ADVISORY", "Trail Advisory"),
    WEATHER_ALERT("WEATHER_ALERT", "Weather Alert"),
    COMMUNITY("COMMUNITY", "Community");

    companion object {
        fun fromCode(raw: String?): TourismEventType =
            entries.firstOrNull { it.code.equals(raw, ignoreCase = true) } ?: COMMUNITY
    }
}

enum class TourismEventSeverity(val code: String, val label: String) {
    INFO("INFO", "Info"),
    ADVISORY("ADVISORY", "Advisory"),
    ALERT("ALERT", "Alert");

    companion object {
        fun fromCode(raw: String?): TourismEventSeverity =
            entries.firstOrNull { it.code.equals(raw, ignoreCase = true) } ?: INFO
    }
}

@Keep
@IgnoreExtraProperties
data class TourismEvent(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val summary: String = "",
    val eventType: String = TourismEventType.FESTIVAL.code,
    val severity: String = TourismEventSeverity.INFO.code,
    val dateLabel: String = "",
    val locationName: String = "",
    val attractionId: String = "",
    val active: Boolean = true,
    val visibility: String = "public",
    @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
    @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
)

enum class TransitModeCategory(val code: String) {
    TRICYCLE("TRICYCLE"),
    HABAL_HABAL("HABAL_HABAL"),
    BUS_VAN("BUS_VAN"),
    JEEPNEY_MULTICAB("JEEPNEY_MULTICAB");

    companion object {
        fun fromCode(raw: String?): TransitModeCategory =
            entries.firstOrNull { it.code == raw } ?: TRICYCLE
    }
}

enum class TransitServiceStatus(val code: String) {
    OPERATIONAL("OPERATIONAL"),
    LIMITED("LIMITED"),
    ADVISORY("ADVISORY");

    companion object {
        fun fromCode(raw: String?): TransitServiceStatus =
            entries.firstOrNull { it.code == raw } ?: OPERATIONAL
    }
}

@Keep
@IgnoreExtraProperties
data class TransitMethod(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val modeCategory: String = TransitModeCategory.TRICYCLE.code,
    val operatorName: String = "",
    val routeSummary: String = "",
    val scheduleText: String = "",
    val fareRangePhp: String = "",
    val durationText: String = "",
    val terminalLocation: String = "",
    val travelTip: String = "",
    val serviceStatus: String = TransitServiceStatus.OPERATIONAL.code,
    val visibility: String = "public",
    @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
    @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
)



data class ManagedUser(
    val uid: String,
    val displayName: String,
    val email: String = "",
    val role: String = "regular"
) {
    val isAdminRole: Boolean get() = role.trim().equals("admin", ignoreCase = true)
}
