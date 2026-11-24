package com.example.data.local

import android.content.Context
import androidx.annotation.Keep
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.CommunityReview
import com.example.model.EmergencyContact
import com.example.model.LocalizedText
import com.example.model.TourismEvent
import com.example.model.TransitMethod
import com.google.firebase.Timestamp
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.travelGuideCacheStore: DataStore<Preferences> by preferencesDataStore(
    name = "dumingag_travel_guide_cache"
)

data class TravelGuideCacheStatus(
    val transitCount: Int = 0,
    val emergencyContactCount: Int = 0,
    val reviewCount: Int = 0,
    val eventCount: Int = 0,
    val transitCachedAtMillis: Long = 0L,
    val emergencyCachedAtMillis: Long = 0L,
    val reviewsCachedAtMillis: Long = 0L,
    val eventsCachedAtMillis: Long = 0L
) {
    val lastUpdatedAtMillis: Long
        get() = maxOf(transitCachedAtMillis, emergencyCachedAtMillis, reviewsCachedAtMillis, eventsCachedAtMillis)

    val isReadyOffline: Boolean
        get() = transitCount > 0 && emergencyContactCount > 0
}

@Keep
internal data class CachedTransitMethodDto(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val modeCategory: String = "TRICYCLE",
    val operatorName: String = "",
    val routeSummary: String = "",
    val scheduleText: String = "",
    val fareRangePhp: String = "",
    val durationText: String = "",
    val terminalLocation: String = "",
    val travelTip: String = "",
    val serviceStatus: String = "OPERATIONAL",
    val visibility: String = "public"
) {
    fun toDomain(): TransitMethod = TransitMethod(
        id = id,
        userId = userId,
        name = name,
        modeCategory = modeCategory,
        operatorName = operatorName,
        routeSummary = routeSummary,
        scheduleText = scheduleText,
        fareRangePhp = fareRangePhp,
        durationText = durationText,
        terminalLocation = terminalLocation,
        travelTip = travelTip,
        serviceStatus = serviceStatus,
        visibility = visibility
    )

    companion object {
        fun fromDomain(method: TransitMethod): CachedTransitMethodDto = CachedTransitMethodDto(
            id = method.id,
            userId = method.userId,
            name = method.name,
            modeCategory = method.modeCategory,
            operatorName = method.operatorName,
            routeSummary = method.routeSummary,
            scheduleText = method.scheduleText,
            fareRangePhp = method.fareRangePhp,
            durationText = method.durationText,
            terminalLocation = method.terminalLocation,
            travelTip = method.travelTip,
            serviceStatus = method.serviceStatus,
            visibility = method.visibility
        )
    }
}

@Keep
internal data class CachedEmergencyContactDto(
    val id: String = "",
    val titleEn: String = "",
    val titleCeb: String = "",
    val titleTl: String = "",
    val titleEs: String = "",
    val department: String = "",
    val phoneNumber: String = "",
    val availability: String = "",
    val priorityLevel: String = "STANDARD",
    val email: String? = null,
    val websiteUrl: String? = null
) {
    fun toDomain(): EmergencyContact = EmergencyContact(
        id = id,
        title = LocalizedText(
            en = titleEn,
            ceb = titleCeb.ifBlank { titleEn },
            tl = titleTl.ifBlank { titleEn },
            es = titleEs.ifBlank { titleEn }
        ),
        department = department,
        phoneNumber = phoneNumber,
        availability = availability,
        priorityLevel = priorityLevel,
        email = email?.takeIf { it.isNotBlank() },
        websiteUrl = websiteUrl?.takeIf { it.isNotBlank() }
    )

    companion object {
        fun fromDomain(contact: EmergencyContact): CachedEmergencyContactDto = CachedEmergencyContactDto(
            id = contact.id,
            titleEn = contact.title.en,
            titleCeb = contact.title.ceb,
            titleTl = contact.title.tl,
            titleEs = contact.title.es,
            department = contact.department,
            phoneNumber = contact.phoneNumber,
            availability = contact.availability,
            priorityLevel = contact.priorityLevel,
            email = contact.email,
            websiteUrl = contact.websiteUrl
        )
    }
}

@Keep
internal data class CachedReviewDto(
    val id: String = "",
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
    val createdAtSeconds: Long = 0L
) {
    fun toDomain(): CommunityReview = CommunityReview(
        id = id,
        userId = userId,
        authorName = authorName,
        attractionId = attractionId,
        attractionName = attractionName,
        rating = rating,
        comment = comment,
        visitTag = visitTag,
        photoData = photoData,
        verifiedVisit = verifiedVisit,
        helpfulUserIds = helpfulUserIds,
        adminReply = adminReply,
        adminReplyBy = adminReplyBy,
        visibility = "public",
        createdAt = if (createdAtSeconds > 0L) Timestamp(createdAtSeconds, 0) else null
    )

    companion object {
        fun fromDomain(review: CommunityReview): CachedReviewDto = CachedReviewDto(
            id = review.id,
            userId = review.userId,
            authorName = review.authorName,
            attractionId = review.attractionId,
            attractionName = review.attractionName,
            rating = review.rating,
            comment = review.comment,
            visitTag = review.visitTag,
            photoData = review.photoData,
            verifiedVisit = review.verifiedVisit,
            helpfulUserIds = review.helpfulUserIds,
            adminReply = review.adminReply,
            adminReplyBy = review.adminReplyBy,
            createdAtSeconds = review.createdAt?.seconds ?: 0L
        )
    }
}

@Keep
internal data class CachedTourismEventDto(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val summary: String = "",
    val eventType: String = "FESTIVAL",
    val severity: String = "INFO",
    val dateLabel: String = "",
    val locationName: String = "",
    val attractionId: String = "",
    val active: Boolean = true
) {
    fun toDomain(): TourismEvent = TourismEvent(
        id = id,
        userId = userId,
        title = title,
        summary = summary,
        eventType = eventType,
        severity = severity,
        dateLabel = dateLabel,
        locationName = locationName,
        attractionId = attractionId,
        active = active,
        visibility = "public"
    )

    companion object {
        fun fromDomain(event: TourismEvent): CachedTourismEventDto = CachedTourismEventDto(
            id = event.id,
            userId = event.userId,
            title = event.title,
            summary = event.summary,
            eventType = event.eventType,
            severity = event.severity,
            dateLabel = event.dateLabel,
            locationName = event.locationName,
            attractionId = event.attractionId,
            active = event.active
        )
    }
}

/**
 * Jetpack Preferences DataStore caching layer for essential Dumingag travel guide data:
 * transit schedules/routes, emergency contact directories, community reviews, tourism events,
 * and attraction photo galleries remain immediately accessible offline.
 */
class TravelGuideCacheDataStore(
    private val dataStore: DataStore<Preferences>,
    moshi: Moshi = defaultMoshi
) {
    private object Keys {
        val SCHEMA_VERSION = intPreferencesKey("cache_schema_version")
        val TRANSIT_METHODS_JSON = stringPreferencesKey("cached_transit_methods_json")
        val TRANSIT_CACHED_AT = longPreferencesKey("transit_cached_at_millis")
        val EMERGENCY_CONTACTS_JSON = stringPreferencesKey("cached_emergency_contacts_json")
        val EMERGENCY_CACHED_AT = longPreferencesKey("emergency_cached_at_millis")
        val REVIEWS_JSON = stringPreferencesKey("cached_reviews_json")
        val REVIEWS_CACHED_AT = longPreferencesKey("reviews_cached_at_millis")
        val EVENTS_JSON = stringPreferencesKey("cached_events_json")
        val EVENTS_CACHED_AT = longPreferencesKey("events_cached_at_millis")
        val GALLERIES_JSON = stringPreferencesKey("cached_attraction_galleries_json")
    }

    private val transitListAdapter = moshi.adapter<List<CachedTransitMethodDto>>(
        Types.newParameterizedType(List::class.java, CachedTransitMethodDto::class.java)
    )
    private val emergencyListAdapter = moshi.adapter<List<CachedEmergencyContactDto>>(
        Types.newParameterizedType(List::class.java, CachedEmergencyContactDto::class.java)
    )
    private val reviewListAdapter = moshi.adapter<List<CachedReviewDto>>(
        Types.newParameterizedType(List::class.java, CachedReviewDto::class.java)
    )
    private val eventListAdapter = moshi.adapter<List<CachedTourismEventDto>>(
        Types.newParameterizedType(List::class.java, CachedTourismEventDto::class.java)
    )
    private val galleriesMapAdapter = moshi.adapter<Map<String, List<String>>>(
        Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            Types.newParameterizedType(List::class.java, String::class.java)
        )
    )

    private val safePreferences: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) {
            emit(emptyPreferences())
        } else {
            throw error
        }
    }

    val cachedTransitMethods: Flow<List<TransitMethod>> = safePreferences.map { prefs ->
        decodeTransitMethods(prefs[Keys.TRANSIT_METHODS_JSON])
    }

    val cachedEmergencyContacts: Flow<List<EmergencyContact>> = safePreferences.map { prefs ->
        decodeEmergencyContacts(prefs[Keys.EMERGENCY_CONTACTS_JSON])
    }

    val cachedReviews: Flow<List<CommunityReview>> = safePreferences.map { prefs ->
        decodeReviews(prefs[Keys.REVIEWS_JSON])
    }

    val cachedTourismEvents: Flow<List<TourismEvent>> = safePreferences.map { prefs ->
        decodeEvents(prefs[Keys.EVENTS_JSON])
    }

    val cachedAttractionGalleries: Flow<Map<String, List<String>>> = safePreferences.map { prefs ->
        decodeGalleries(prefs[Keys.GALLERIES_JSON])
    }

    val cacheStatus: Flow<TravelGuideCacheStatus> = safePreferences.map { prefs ->
        val transit = decodeTransitMethods(prefs[Keys.TRANSIT_METHODS_JSON])
        val contacts = decodeEmergencyContacts(prefs[Keys.EMERGENCY_CONTACTS_JSON])
        val reviews = decodeReviews(prefs[Keys.REVIEWS_JSON])
        val events = decodeEvents(prefs[Keys.EVENTS_JSON])
        TravelGuideCacheStatus(
            transitCount = transit.size,
            emergencyContactCount = contacts.size,
            reviewCount = reviews.size,
            eventCount = events.size,
            transitCachedAtMillis = prefs[Keys.TRANSIT_CACHED_AT] ?: 0L,
            emergencyCachedAtMillis = prefs[Keys.EMERGENCY_CACHED_AT] ?: 0L,
            reviewsCachedAtMillis = prefs[Keys.REVIEWS_CACHED_AT] ?: 0L,
            eventsCachedAtMillis = prefs[Keys.EVENTS_CACHED_AT] ?: 0L
        )
    }

    suspend fun saveTransitMethods(
        methods: List<TransitMethod>,
        updatedAtMillis: Long = System.currentTimeMillis()
    ) {
        if (methods.isEmpty()) return
        val encoded = encodeTransitMethods(methods)
        dataStore.edit { prefs ->
            prefs[Keys.SCHEMA_VERSION] = CURRENT_SCHEMA_VERSION
            prefs[Keys.TRANSIT_METHODS_JSON] = encoded
            prefs[Keys.TRANSIT_CACHED_AT] = updatedAtMillis
        }
    }

    suspend fun saveEmergencyContacts(
        contacts: List<EmergencyContact>,
        updatedAtMillis: Long = System.currentTimeMillis()
    ) {
        if (contacts.isEmpty()) return
        val encoded = encodeEmergencyContacts(contacts)
        dataStore.edit { prefs ->
            prefs[Keys.SCHEMA_VERSION] = CURRENT_SCHEMA_VERSION
            prefs[Keys.EMERGENCY_CONTACTS_JSON] = encoded
            prefs[Keys.EMERGENCY_CACHED_AT] = updatedAtMillis
        }
    }

    /**
     * Seeds essential Dumingag transit schedules and emergency contact numbers into DataStore
     * if they have not yet been cached, without overwriting previously synced live records.
     */
    suspend fun seedEssentialsIfEmpty(
        defaultTransit: List<TransitMethod>,
        defaultContacts: List<EmergencyContact>,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        dataStore.edit { prefs ->
            val existingTransit = decodeTransitMethods(prefs[Keys.TRANSIT_METHODS_JSON])
            if (existingTransit.isEmpty() && defaultTransit.isNotEmpty()) {
                prefs[Keys.TRANSIT_METHODS_JSON] = encodeTransitMethods(defaultTransit)
                prefs[Keys.TRANSIT_CACHED_AT] = nowMillis
            }
            val existingContacts = decodeEmergencyContacts(prefs[Keys.EMERGENCY_CONTACTS_JSON])
            if (existingContacts.isEmpty() && defaultContacts.isNotEmpty()) {
                prefs[Keys.EMERGENCY_CONTACTS_JSON] = encodeEmergencyContacts(defaultContacts)
                prefs[Keys.EMERGENCY_CACHED_AT] = nowMillis
            }
            prefs[Keys.SCHEMA_VERSION] = CURRENT_SCHEMA_VERSION
        }
    }

    suspend fun getCachedTransitSnapshot(): List<TransitMethod> =
        cachedTransitMethods.first()

    suspend fun getCachedEmergencyContactsSnapshot(): List<EmergencyContact> =
        cachedEmergencyContacts.first()

    suspend fun saveReviews(
        reviews: List<CommunityReview>,
        updatedAtMillis: Long = System.currentTimeMillis()
    ) {
        val encoded = reviewListAdapter.toJson(reviews.take(120).map(CachedReviewDto::fromDomain))
        dataStore.edit { prefs ->
            prefs[Keys.SCHEMA_VERSION] = CURRENT_SCHEMA_VERSION
            prefs[Keys.REVIEWS_JSON] = encoded
            prefs[Keys.REVIEWS_CACHED_AT] = updatedAtMillis
        }
    }

    suspend fun saveTourismEvents(
        events: List<TourismEvent>,
        updatedAtMillis: Long = System.currentTimeMillis()
    ) {
        val encoded = eventListAdapter.toJson(events.take(40).map(CachedTourismEventDto::fromDomain))
        dataStore.edit { prefs ->
            prefs[Keys.SCHEMA_VERSION] = CURRENT_SCHEMA_VERSION
            prefs[Keys.EVENTS_JSON] = encoded
            prefs[Keys.EVENTS_CACHED_AT] = updatedAtMillis
        }
    }

    suspend fun saveAttractionGalleries(galleries: Map<String, List<String>>) {
        val cleaned = galleries.mapValues { (_, photos) -> photos.filter { it.isNotBlank() }.take(4) }
            .filterValues { it.isNotEmpty() }
        val encoded = galleriesMapAdapter.toJson(cleaned)
        dataStore.edit { prefs ->
            prefs[Keys.GALLERIES_JSON] = encoded
        }
    }

    suspend fun clearCache() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.TRANSIT_METHODS_JSON)
            prefs.remove(Keys.TRANSIT_CACHED_AT)
            prefs.remove(Keys.EMERGENCY_CONTACTS_JSON)
            prefs.remove(Keys.EMERGENCY_CACHED_AT)
            prefs.remove(Keys.REVIEWS_JSON)
            prefs.remove(Keys.REVIEWS_CACHED_AT)
            prefs.remove(Keys.EVENTS_JSON)
            prefs.remove(Keys.EVENTS_CACHED_AT)
            prefs.remove(Keys.GALLERIES_JSON)
        }
    }

    private fun encodeTransitMethods(methods: List<TransitMethod>): String =
        transitListAdapter.toJson(methods.map(CachedTransitMethodDto::fromDomain))

    private fun decodeTransitMethods(rawJson: String?): List<TransitMethod> {
        if (rawJson.isNullOrBlank()) return emptyList()
        return runCatching {
            transitListAdapter.fromJson(rawJson).orEmpty().map(CachedTransitMethodDto::toDomain)
        }.getOrDefault(emptyList())
    }

    private fun encodeEmergencyContacts(contacts: List<EmergencyContact>): String =
        emergencyListAdapter.toJson(contacts.map(CachedEmergencyContactDto::fromDomain))

    private fun decodeEmergencyContacts(rawJson: String?): List<EmergencyContact> {
        if (rawJson.isNullOrBlank()) return emptyList()
        return runCatching {
            emergencyListAdapter.fromJson(rawJson).orEmpty().map(CachedEmergencyContactDto::toDomain)
        }.getOrDefault(emptyList())
    }

    private fun decodeReviews(rawJson: String?): List<CommunityReview> {
        if (rawJson.isNullOrBlank()) return emptyList()
        return runCatching {
            reviewListAdapter.fromJson(rawJson).orEmpty().map(CachedReviewDto::toDomain)
        }.getOrDefault(emptyList())
    }

    private fun decodeEvents(rawJson: String?): List<TourismEvent> {
        if (rawJson.isNullOrBlank()) return emptyList()
        return runCatching {
            eventListAdapter.fromJson(rawJson).orEmpty().map(CachedTourismEventDto::toDomain)
        }.getOrDefault(emptyList())
    }

    private fun decodeGalleries(rawJson: String?): Map<String, List<String>> {
        if (rawJson.isNullOrBlank()) return emptyMap()
        return runCatching {
            galleriesMapAdapter.fromJson(rawJson).orEmpty()
        }.getOrDefault(emptyMap())
    }

    companion object {
        private const val CURRENT_SCHEMA_VERSION = 2

        private val defaultMoshi: Moshi by lazy {
            Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
        }

        @Volatile
        private var INSTANCE: TravelGuideCacheDataStore? = null

        fun getInstance(context: Context): TravelGuideCacheDataStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TravelGuideCacheDataStore(
                    dataStore = context.applicationContext.travelGuideCacheStore
                ).also { INSTANCE = it }
            }
        }
    }
}
