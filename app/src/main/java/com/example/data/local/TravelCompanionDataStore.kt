package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.TravelCompanionEngines
import com.example.model.AppThemeMode
import com.example.model.TrailConditionReport
import com.example.model.TripExpense
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.UUID

private val Context.travelCompanionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "dumingag_travel_companion_store"
)

/**
 * Offline-first Jetpack Preferences DataStore layer for Dumingag traveler companion tools:
 * theme switcher, smart packing checklist state, trip budget expenses, eco-passport check-ins,
 * and community trail condition reports.
 */
class TravelCompanionDataStore(
    private val dataStore: DataStore<Preferences>,
    moshi: Moshi = defaultMoshi
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("app_theme_mode")
        val CHECKED_PACKING_IDS = stringSetPreferencesKey("checked_packing_ids")
        val EXPENSES_JSON = stringPreferencesKey("trip_expenses_json")
        val CHECKED_IN_ATTRACTION_IDS = stringSetPreferencesKey("checked_in_attraction_ids")
        val TRAIL_REPORTS_JSON = stringPreferencesKey("trail_condition_reports_json")
    }

    private val expenseListAdapter = moshi.adapter<List<TripExpense>>(
        Types.newParameterizedType(List::class.java, TripExpense::class.java)
    )
    private val trailReportListAdapter = moshi.adapter<List<TrailConditionReport>>(
        Types.newParameterizedType(List::class.java, TrailConditionReport::class.java)
    )

    private val safePreferences: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    val themeMode: Flow<AppThemeMode> = safePreferences.map { prefs ->
        AppThemeMode.fromCode(prefs[Keys.THEME_MODE])
    }

    val checkedPackingIds: Flow<Set<String>> = safePreferences.map { prefs ->
        prefs[Keys.CHECKED_PACKING_IDS].orEmpty()
    }

    val tripExpenses: Flow<List<TripExpense>> = safePreferences.map { prefs ->
        decodeExpenses(prefs[Keys.EXPENSES_JSON])
    }

    val checkedInAttractionIds: Flow<Set<String>> = safePreferences.map { prefs ->
        prefs[Keys.CHECKED_IN_ATTRACTION_IDS].orEmpty()
    }

    val trailReports: Flow<List<TrailConditionReport>> = safePreferences.map { prefs ->
        val stored = decodeTrailReports(prefs[Keys.TRAIL_REPORTS_JSON])
        if (stored.isEmpty()) TravelCompanionEngines.starterTrailReports else stored
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.code
        }
    }

    suspend fun togglePackingItem(itemId: String) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.CHECKED_PACKING_IDS].orEmpty().toMutableSet()
            if (!current.add(itemId)) {
                current.remove(itemId)
            }
            prefs[Keys.CHECKED_PACKING_IDS] = current
        }
    }

    suspend fun addExpense(
        title: String,
        category: String,
        amountPhp: Double,
        spotName: String = "",
        nowMillis: Long = System.currentTimeMillis()
    ) {
        if (title.isBlank() || amountPhp <= 0.0) return
        val newEntry = TripExpense(
            id = "exp_${UUID.randomUUID().toString().take(8)}",
            title = title.trim(),
            category = category.trim().ifBlank { "Meals" },
            amountPhp = amountPhp,
            spotName = spotName.trim(),
            createdAtMillis = nowMillis
        )
        dataStore.edit { prefs ->
            val updated = listOf(newEntry) + decodeExpenses(prefs[Keys.EXPENSES_JSON])
            prefs[Keys.EXPENSES_JSON] = expenseListAdapter.toJson(updated)
        }
    }

    suspend fun deleteExpense(expenseId: String) {
        dataStore.edit { prefs ->
            val updated = decodeExpenses(prefs[Keys.EXPENSES_JSON]).filterNot { it.id == expenseId }
            prefs[Keys.EXPENSES_JSON] = expenseListAdapter.toJson(updated)
        }
    }

    suspend fun recordCheckIn(attractionId: String) {
        if (attractionId.isBlank()) return
        dataStore.edit { prefs ->
            val current = prefs[Keys.CHECKED_IN_ATTRACTION_IDS].orEmpty()
            prefs[Keys.CHECKED_IN_ATTRACTION_IDS] = current + attractionId
        }
    }

    suspend fun addTrailReport(
        attractionId: String,
        conditionTag: String,
        crowdLevel: String,
        note: String,
        reporterName: String,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        if (attractionId.isBlank() || conditionTag.isBlank()) return
        val report = TrailConditionReport(
            id = "rep_${UUID.randomUUID().toString().take(8)}",
            attractionId = attractionId,
            conditionTag = conditionTag.trim(),
            crowdLevel = crowdLevel.trim().ifBlank { "Quiet" },
            note = note.trim(),
            reporterName = reporterName.trim().ifBlank { "Dumingag Traveler" },
            reportedAtMillis = nowMillis
        )
        dataStore.edit { prefs ->
            val existing = decodeTrailReports(prefs[Keys.TRAIL_REPORTS_JSON])
                .ifEmpty { TravelCompanionEngines.starterTrailReports }
            val updated = (listOf(report) + existing).take(40)
            prefs[Keys.TRAIL_REPORTS_JSON] = trailReportListAdapter.toJson(updated)
        }
    }

    private fun decodeExpenses(raw: String?): List<TripExpense> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            expenseListAdapter.fromJson(raw).orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun decodeTrailReports(raw: String?): List<TrailConditionReport> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            trailReportListAdapter.fromJson(raw).orEmpty()
        }.getOrDefault(emptyList())
    }

    companion object {
        private val defaultMoshi: Moshi by lazy {
            Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
        }

        @Volatile
        private var INSTANCE: TravelCompanionDataStore? = null

        fun getInstance(context: Context): TravelCompanionDataStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TravelCompanionDataStore(
                    dataStore = context.applicationContext.travelCompanionDataStore
                ).also { INSTANCE = it }
            }
        }
    }
}
