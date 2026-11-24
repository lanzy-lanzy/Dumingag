package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.DumingagSeedData
import com.example.data.local.CachedWeatherEntity
import com.example.data.local.DumingagDatabase
import com.example.data.local.OfflineItineraryEntity
import com.example.data.local.SavedAttractionEntity
import com.example.data.local.SavedDocumentEntity
import com.example.data.local.TravelCompanionDataStore
import com.example.data.local.TravelGuideCacheDataStore
import com.example.data.local.TravelGuideCacheStatus
import com.example.data.remote.OsmTileLoader
import com.example.data.remote.WeatherApiService
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import com.example.model.Attraction
import com.example.model.CommunityReview
import com.example.model.DailyForecast
import com.example.model.EmergencyContact
import com.example.model.ItineraryItem
import com.example.model.SavedDocument
import com.example.model.TourismEvent
import com.example.model.TrailConditionReport
import com.example.model.TransitMethod
import com.example.model.TripExpense
import com.example.model.WeatherSnapshot
import com.example.widget.DumingagWidgetProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dumingag_prefs")

class DumingagOfflineRepository(
    private val context: Context,
    private val db: DumingagDatabase = DumingagDatabase.getInstance(context),
    private val weatherApi: WeatherApiService = WeatherApiService.create(),
    private val attractionRepo: AttractionRepository = AttractionRepository(db.attractionDao()),
    private val travelGuideCache: TravelGuideCacheDataStore = TravelGuideCacheDataStore.getInstance(context),
    private val companionStore: TravelCompanionDataStore = TravelCompanionDataStore.getInstance(context)
) {
    private val appContext = context.applicationContext
    private val osmTileLoader by lazy { OsmTileLoader(appContext) }

    private object PrefKeys {
        val LANGUAGE = stringPreferencesKey("language_code")
        val FORCE_OFFLINE = booleanPreferencesKey("force_offline_mode")
        val OFFLINE_PACK_READY = booleanPreferencesKey("offline_pack_ready")
        val OFFLINE_PACK_UPDATED_AT = longPreferencesKey("offline_pack_updated_at")
    }

    val selectedLanguage: Flow<AppLanguage> = appContext.dataStore.data.map { prefs ->
        AppLanguage.fromCode(prefs[PrefKeys.LANGUAGE])
    }

    val forceOfflineMode: Flow<Boolean> = appContext.dataStore.data.map { prefs ->
        prefs[PrefKeys.FORCE_OFFLINE] ?: false
    }

    val offlinePackReady: Flow<Boolean> = appContext.dataStore.data.map { prefs ->
        prefs[PrefKeys.OFFLINE_PACK_READY] ?: false
    }

    val offlinePackUpdatedAt: Flow<Long> = appContext.dataStore.data.map { prefs ->
        prefs[PrefKeys.OFFLINE_PACK_UPDATED_AT] ?: 0L
    }

    val cachedTransitMethods: Flow<List<TransitMethod>> = travelGuideCache.cachedTransitMethods

    val cachedEmergencyContacts: Flow<List<EmergencyContact>> = travelGuideCache.cachedEmergencyContacts

    val cachedReviews: Flow<List<CommunityReview>> = travelGuideCache.cachedReviews

    val cachedTourismEvents: Flow<List<TourismEvent>> = travelGuideCache.cachedTourismEvents

    val cachedAttractionGalleries: Flow<Map<String, List<String>>> = travelGuideCache.cachedAttractionGalleries

    val travelGuideCacheStatus: Flow<TravelGuideCacheStatus> = travelGuideCache.cacheStatus

    val themeMode: Flow<AppThemeMode> = companionStore.themeMode
    val checkedPackingIds: Flow<Set<String>> = companionStore.checkedPackingIds
    val tripExpenses: Flow<List<TripExpense>> = companionStore.tripExpenses
    val checkedInAttractionIds: Flow<Set<String>> = companionStore.checkedInAttractionIds
    val trailReports: Flow<List<TrailConditionReport>> = companionStore.trailReports

    val attractions: Flow<List<Attraction>> = attractionRepo.allAttractions

    val savedAttractionIds: Flow<Set<String>> = db.savedAttractionDao().observeSavedIds()
        .map { it.toSet() }

    val savedDocuments: Flow<List<SavedDocument>> = db.savedDocumentDao().observeDocuments()
        .map { entities ->
            entities.map {
                SavedDocument(
                    id = it.id,
                    title = it.title,
                    category = it.category,
                    summary = it.summary,
                    contentBody = it.contentBody,
                    fileUri = it.fileUri,
                    savedAtMillis = it.savedAtMillis,
                    isBundled = it.isBundled
                )
            }
        }

    val cachedWeather: Flow<WeatherSnapshot?> = db.weatherDao().observeWeather()
        .map { entity -> entity?.toWeatherSnapshot(isFromCache = true) }

    fun observeOfflineItineraries(userId: String): Flow<List<ItineraryItem>> =
        db.offlineItineraryDao().observeForUser(userId).map { list ->
            list.map {
                ItineraryItem(
                    id = it.id,
                    userId = it.userId,
                    attractionId = it.attractionId,
                    attractionName = it.attractionName,
                    plannedDateMillis = it.plannedDateMillis,
                    notes = it.notes,
                    reminderEnabled = it.reminderEnabled,
                    calendarSynced = it.calendarSynced
                )
            }
        }

    suspend fun mirrorItinerariesOffline(items: List<ItineraryItem>) {
        val entities = items.map {
            OfflineItineraryEntity(
                id = it.id,
                userId = it.userId,
                attractionId = it.attractionId,
                attractionName = it.attractionName,
                plannedDateMillis = it.plannedDateMillis,
                notes = it.notes,
                reminderEnabled = it.reminderEnabled,
                calendarSynced = it.calendarSynced
            )
        }
        db.offlineItineraryDao().replaceForUser(entities)
        DumingagWidgetProvider.syncNextItinerary(appContext, items)
    }

    suspend fun seedStarterDocumentsIfNeeded() {
        val entities = DumingagSeedData.starterDocuments.map { doc ->
            SavedDocumentEntity(
                id = doc.id,
                title = doc.title,
                category = doc.category,
                summary = doc.summary,
                contentBody = doc.contentBody,
                fileUri = doc.fileUri,
                savedAtMillis = doc.savedAtMillis,
                isBundled = doc.isBundled
            )
        }
        db.savedDocumentDao().insertAll(entities)
    }

    suspend fun seedAttractionsIfNeeded() {
        attractionRepo.seedAttractionsIfEmpty()
    }

    suspend fun getAttractionById(id: String): Attraction? =
        attractionRepo.getAttractionById(id)

    suspend fun upsertAttraction(attraction: Attraction, isSavedOffline: Boolean = false) {
        attractionRepo.insertAttraction(attraction, isSavedOffline = isSavedOffline)
    }

    suspend fun setLanguage(language: AppLanguage) {
        appContext.dataStore.edit { it[PrefKeys.LANGUAGE] = language.code }
    }

    suspend fun setForceOfflineMode(enabled: Boolean) {
        appContext.dataStore.edit { it[PrefKeys.FORCE_OFFLINE] = enabled }
    }

    suspend fun toggleAttractionSavedOffline(attractionId: String, currentlySaved: Boolean) {
        if (currentlySaved) {
            db.savedAttractionDao().remove(attractionId)
            attractionRepo.setSavedOffline(attractionId, false)
        } else {
            db.savedAttractionDao().save(SavedAttractionEntity(attractionId, System.currentTimeMillis()))
            attractionRepo.setSavedOffline(attractionId, true)
        }
    }

    suspend fun seedEssentialGuideCacheIfNeeded() {
        travelGuideCache.seedEssentialsIfEmpty(
            defaultTransit = DumingagSeedData.defaultTransitMethods,
            defaultContacts = DumingagSeedData.emergencyContacts
        )
    }

    suspend fun cacheTransitMethods(methods: List<TransitMethod>) {
        travelGuideCache.saveTransitMethods(methods)
    }

    suspend fun cacheEmergencyContacts(contacts: List<EmergencyContact>) {
        travelGuideCache.saveEmergencyContacts(contacts)
    }

    suspend fun cacheReviews(reviews: List<CommunityReview>) {
        travelGuideCache.saveReviews(reviews)
    }

    suspend fun cacheTourismEvents(events: List<TourismEvent>) {
        travelGuideCache.saveTourismEvents(events)
    }

    suspend fun cacheAttractionGalleries(galleries: Map<String, List<String>>) {
        travelGuideCache.saveAttractionGalleries(galleries)
    }

    suspend fun getCachedTransitMethods(): List<TransitMethod> =
        travelGuideCache.getCachedTransitSnapshot()

    suspend fun getCachedEmergencyContacts(): List<EmergencyContact> =
        travelGuideCache.getCachedEmergencyContactsSnapshot()

    suspend fun setThemeMode(mode: AppThemeMode) = companionStore.setThemeMode(mode)

    suspend fun togglePackingItem(itemId: String) = companionStore.togglePackingItem(itemId)

    suspend fun addTripExpense(title: String, category: String, amountPhp: Double, spotName: String = "") =
        companionStore.addExpense(title, category, amountPhp, spotName)

    suspend fun deleteTripExpense(expenseId: String) = companionStore.deleteExpense(expenseId)

    suspend fun recordAttractionCheckIn(attractionId: String) = companionStore.recordCheckIn(attractionId)

    suspend fun addTrailReport(
        attractionId: String,
        conditionTag: String,
        crowdLevel: String,
        note: String,
        reporterName: String
    ) = companionStore.addTrailReport(attractionId, conditionTag, crowdLevel, note, reporterName)

    suspend fun countCachedMapTiles(): Int = osmTileLoader.countCachedDiskTiles()

    suspend fun preDownloadOfflineMapTiles(
        onProgress: (downloaded: Int, total: Int) -> Unit
    ): Int = osmTileLoader.preDownloadDumingagTiles(onProgress)

    fun syncWidgetWeather(snapshot: WeatherSnapshot) {
        DumingagWidgetProvider.syncWeather(appContext, snapshot)
    }

    suspend fun downloadFullOfflinePack(
        latestTransit: List<TransitMethod> = emptyList(),
        latestEmergencyContacts: List<EmergencyContact> = emptyList()
    ) {
        val now = System.currentTimeMillis()
        attractionRepo.insertAttractions(DumingagSeedData.attractions, isSavedOffline = true)
        val allEntities = DumingagSeedData.attractions.map {
            SavedAttractionEntity(it.id, now)
        }
        db.savedAttractionDao().saveAll(allEntities)
        seedStarterDocumentsIfNeeded()
        travelGuideCache.saveTransitMethods(
            methods = latestTransit.ifEmpty { DumingagSeedData.defaultTransitMethods },
            updatedAtMillis = now
        )
        travelGuideCache.saveEmergencyContacts(
            contacts = latestEmergencyContacts.ifEmpty { DumingagSeedData.emergencyContacts },
            updatedAtMillis = now
        )
        appContext.dataStore.edit {
            it[PrefKeys.OFFLINE_PACK_READY] = true
            it[PrefKeys.OFFLINE_PACK_UPDATED_AT] = now
        }
    }

    suspend fun saveCustomDocument(
        title: String,
        category: String,
        contentBody: String,
        fileUri: String? = null
    ) {
        val now = System.currentTimeMillis()
        val cleanTitle = title.trim().ifBlank { "Travel Note" }
        val entity = SavedDocumentEntity(
            id = "doc_${UUID.randomUUID().toString().take(8)}",
            title = cleanTitle,
            category = category.trim().ifBlank { "Personal Note" },
            summary = contentBody.trim().replace('\n', ' ').take(90),
            contentBody = contentBody.trim(),
            fileUri = fileUri,
            savedAtMillis = now,
            isBundled = false
        )
        db.savedDocumentDao().insertDocument(entity)
    }

    suspend fun deleteCustomDocument(id: String) {
        db.savedDocumentDao().deleteCustomDocument(id)
    }

    suspend fun refreshWeather(isOfflineForced: Boolean): WeatherSnapshot {
        val cached = db.weatherDao().getWeather()
        if (isOfflineForced || !isNetworkConnected()) {
            return cached?.toWeatherSnapshot(isFromCache = true) ?: fallbackWeatherSnapshot()
        }

        return try {
            val response = weatherApi.getDumingagForecast()
            val cur = response.current
            val daily = response.daily
            val forecasts = mutableListOf<DailyForecast>()
            val times = daily?.time.orEmpty()
            val codes = daily?.weatherCode.orEmpty()
            val maxs = daily?.temperature2mMax.orEmpty()
            val mins = daily?.temperature2mMin.orEmpty()
            val precips = daily?.precipitationProbabilityMax.orEmpty()

            for (i in times.indices) {
                forecasts.add(
                    DailyForecast(
                        dateIso = times[i],
                        weatherCode = codes.getOrElse(i) { 2 },
                        tempMaxC = maxs.getOrElse(i) { 31.0 },
                        tempMinC = mins.getOrElse(i) { 22.0 },
                        precipChancePct = precips.getOrElse(i) { 25 }
                    )
                )
            }

            val now = System.currentTimeMillis()
            val entity = CachedWeatherEntity(
                id = 1,
                temperatureC = cur?.temperature2m ?: 28.5,
                apparentTempC = cur?.apparentTemperature ?: 31.2,
                humidityPct = cur?.relativeHumidity2m ?: 76,
                windSpeedKmh = cur?.windSpeed10m ?: 9.4,
                weatherCode = cur?.weatherCode ?: 2,
                dailyJson = serializeForecasts(forecasts),
                updatedAtMillis = now
            )
            db.weatherDao().upsertWeather(entity)
            entity.toWeatherSnapshot(isFromCache = false)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            cached?.toWeatherSnapshot(isFromCache = true) ?: fallbackWeatherSnapshot()
        }
    }

    fun observeNetworkConnected(): Flow<Boolean> = callbackFlow {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        trySend(isNetworkConnected())
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }
            override fun onLost(network: Network) {
                trySend(isNetworkConnected())
            }
        }
        try {
            cm.registerDefaultNetworkCallback(callback)
        } catch (_: Exception) {
            trySend(isNetworkConnected())
        }
        awaitClose {
            try { cm.unregisterNetworkCallback(callback) } catch (_: Exception) {}
        }
    }

    private fun isNetworkConnected(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun serializeForecasts(list: List<DailyForecast>): String {
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("dateIso", item.dateIso)
                    put("weatherCode", item.weatherCode)
                    put("tempMaxC", item.tempMaxC)
                    put("tempMinC", item.tempMinC)
                    put("precipChancePct", item.precipChancePct)
                }
            )
        }
        return arr.toString()
    }

    private fun deserializeForecasts(json: String): List<DailyForecast> {
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { idx ->
                val obj = arr.getJSONObject(idx)
                DailyForecast(
                    dateIso = obj.optString("dateIso"),
                    weatherCode = obj.optInt("weatherCode", 2),
                    tempMaxC = obj.optDouble("tempMaxC", 31.0),
                    tempMinC = obj.optDouble("tempMinC", 22.0),
                    precipChancePct = obj.optInt("precipChancePct", 20)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun CachedWeatherEntity.toWeatherSnapshot(isFromCache: Boolean): WeatherSnapshot =
        WeatherSnapshot(
            temperatureC = temperatureC,
            apparentTempC = apparentTempC,
            humidityPct = humidityPct,
            windSpeedKmh = windSpeedKmh,
            weatherCode = weatherCode,
            isFromCache = isFromCache,
            updatedAtMillis = updatedAtMillis,
            dailyForecasts = deserializeForecasts(dailyJson)
        )

    private fun fallbackWeatherSnapshot(): WeatherSnapshot = WeatherSnapshot(
        temperatureC = 28.4,
        apparentTempC = 31.0,
        humidityPct = 74,
        windSpeedKmh = 8.6,
        weatherCode = 2,
        isFromCache = true,
        updatedAtMillis = System.currentTimeMillis(),
        dailyForecasts = listOf(
            DailyForecast("Today", 2, 31.0, 22.0, 20),
            DailyForecast("Tomorrow", 61, 29.0, 21.5, 55),
            DailyForecast("Day 3", 1, 32.0, 22.0, 15)
        )
    )
}
