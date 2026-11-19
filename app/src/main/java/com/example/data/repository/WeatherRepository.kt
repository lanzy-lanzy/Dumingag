package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.CachedWeatherEntity
import com.example.data.local.DumingagDatabase
import com.example.data.local.WeatherDao
import com.example.data.remote.OpenWeatherMapApiService
import com.example.data.remote.OwmForecastItemDto
import com.example.data.remote.WeatherApiService
import com.example.model.DailyForecast
import com.example.model.WeatherSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class WeatherRepository(
    private val context: Context,
    private val weatherDao: WeatherDao = DumingagDatabase.getInstance(context).weatherDao(),
    private val openWeatherApi: OpenWeatherMapApiService = OpenWeatherMapApiService.create(),
    private val openMeteoFallbackApi: WeatherApiService = WeatherApiService.create()
) {
    private val appContext = context.applicationContext
    private val manilaTimeZone: TimeZone = TimeZone.getTimeZone("Asia/Manila")

    val cachedWeather: Flow<WeatherSnapshot?> = weatherDao.observeWeather()
        .map { entity -> entity?.toSnapshot(isFromCache = true, isApiKeyMissing = !isOpenWeatherKeyConfigured()) }

    fun isOpenWeatherKeyConfigured(): Boolean {
        val key = BuildConfig.OPENWEATHERMAP_API_KEY.trim()
        return key.isNotEmpty() && key != PLACEHOLDER_KEY
    }

    suspend fun refreshDumingagWeather(isOfflineForced: Boolean): WeatherSnapshot {
        val cached = weatherDao.getWeather()
        val hasValidOwmKey = isOpenWeatherKeyConfigured()

        if (isOfflineForced || !isNetworkConnected()) {
            return cached?.toSnapshot(
                isFromCache = true,
                isApiKeyMissing = !hasValidOwmKey
            ) ?: fallbackWeatherSnapshot(isApiKeyMissing = !hasValidOwmKey)
        }

        if (hasValidOwmKey) {
            return try {
                fetchFromOpenWeatherMap(BuildConfig.OPENWEATHERMAP_API_KEY.trim())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "OpenWeatherMap fetch failed, falling back to cache", e)
                cached?.toSnapshot(isFromCache = true, isApiKeyMissing = false)
                    ?: fallbackWeatherSnapshot(isApiKeyMissing = false)
            }
        }

        // OpenWeatherMap API key has not been configured in Secrets yet; try cache or keyless fallback
        Log.w(TAG, "OPENWEATHERMAP_API_KEY is missing or placeholder. Set it in AI Studio Secrets.")
        return try {
            fetchFromOpenMeteoFallback()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            cached?.toSnapshot(isFromCache = true, isApiKeyMissing = true)
                ?: fallbackWeatherSnapshot(isApiKeyMissing = true)
        }
    }

    private suspend fun fetchFromOpenWeatherMap(apiKey: String): WeatherSnapshot = coroutineScope {
        val currentDeferred = async { openWeatherApi.getCurrentWeather(apiKey = apiKey) }
        val forecastDeferred = async { openWeatherApi.getFiveDayForecast(apiKey = apiKey) }

        val current = currentDeferred.await()
        val forecast = forecastDeferred.await()

        val condition = current.weather?.firstOrNull()
        val mappedWeatherCode = mapOpenWeatherIdToCode(condition?.id)
        val description = condition?.description
            ?.trim()
            ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            .orEmpty()

        val tempC = current.main?.temp ?: 28.5
        val feelsLikeC = current.main?.feelsLike ?: tempC
        val humidity = current.main?.humidity ?: 75
        val pressure = current.main?.pressure ?: 1012
        // OpenWeatherMap metric units return wind speed in m/s; convert to km/h
        val windKmh = ((current.wind?.speedMetersPerSec ?: 2.5) * 3.6)

        val slots = forecast.list.orEmpty()
        val todayIso = formatManilaDate(System.currentTimeMillis())
        val todaySlots = slots.filter { slotDateIso(it) == todayIso }
        val highTempC = max(
            current.main?.tempMax ?: tempC,
            todaySlots.mapNotNull { it.main?.tempMax ?: it.main?.temp }.maxOrNull() ?: tempC
        )
        val lowTempC = min(
            current.main?.tempMin ?: tempC,
            todaySlots.mapNotNull { it.main?.tempMin ?: it.main?.temp }.minOrNull() ?: tempC
        )

        val threeDayForecasts = aggregateThreeDayForecasts(slots, todayIso)
        val now = System.currentTimeMillis()

        val entity = CachedWeatherEntity(
            id = 1,
            temperatureC = tempC,
            apparentTempC = feelsLikeC,
            humidityPct = humidity,
            windSpeedKmh = windKmh,
            weatherCode = mappedWeatherCode,
            dailyJson = serializeForecasts(threeDayForecasts),
            updatedAtMillis = now
        )
        weatherDao.upsertWeather(entity)

        entity.toSnapshot(
            isFromCache = false,
            conditionDescription = description,
            sourceLabel = "OpenWeatherMap",
            isApiKeyMissing = false,
            highTempC = highTempC,
            lowTempC = lowTempC,
            pressureHpa = pressure
        )
    }

    private suspend fun fetchFromOpenMeteoFallback(): WeatherSnapshot {
        val response = openMeteoFallbackApi.getDumingagForecast()
        val cur = response.current
        val daily = response.daily
        val times = daily?.time.orEmpty()
        val codes = daily?.weatherCode.orEmpty()
        val maxs = daily?.temperature2mMax.orEmpty()
        val mins = daily?.temperature2mMin.orEmpty()
        val precips = daily?.precipitationProbabilityMax.orEmpty()

        val todayIso = formatManilaDate(System.currentTimeMillis())
        val allDaily = times.indices.map { i ->
            val code = codes.getOrElse(i) { 2 }
            DailyForecast(
                dateIso = times[i],
                weatherCode = code,
                tempMaxC = maxs.getOrElse(i) { 31.0 },
                tempMinC = mins.getOrElse(i) { 22.0 },
                precipChancePct = precips.getOrElse(i) { 25 },
                conditionLabel = shortConditionForCode(code),
                humidityPct = cur?.relativeHumidity2m ?: 76
            )
        }
        val upcomingThree = allDaily.filter { it.dateIso > todayIso }.take(3)
            .ifEmpty { allDaily.take(3) }

        val todayEntry = allDaily.firstOrNull { it.dateIso == todayIso } ?: allDaily.firstOrNull()
        val tempC = cur?.temperature2m ?: 28.5

        val now = System.currentTimeMillis()
        val entity = CachedWeatherEntity(
            id = 1,
            temperatureC = tempC,
            apparentTempC = cur?.apparentTemperature ?: 31.2,
            humidityPct = cur?.relativeHumidity2m ?: 76,
            windSpeedKmh = cur?.windSpeed10m ?: 9.4,
            weatherCode = cur?.weatherCode ?: 2,
            dailyJson = serializeForecasts(upcomingThree),
            updatedAtMillis = now
        )
        weatherDao.upsertWeather(entity)
        return entity.toSnapshot(
            isFromCache = false,
            sourceLabel = "Open-Meteo",
            isApiKeyMissing = true,
            highTempC = todayEntry?.tempMaxC ?: (tempC + 2.0),
            lowTempC = todayEntry?.tempMinC ?: (tempC - 4.0),
            pressureHpa = 1012
        )
    }

    private fun aggregateThreeDayForecasts(
        items: List<OwmForecastItemDto>,
        todayIso: String
    ): List<DailyForecast> {
        val grouped = items
            .mapNotNull { slot ->
                val dateIso = slotDateIso(slot) ?: return@mapNotNull null
                dateIso to slot
            }
            .groupBy({ it.first }, { it.second })
            .toSortedMap()

        val upcomingEntries = grouped.entries
            .filter { it.key > todayIso }
            .take(3)
            .ifEmpty { grouped.entries.take(3) }

        return upcomingEntries.map { (dateIso, slots) ->
            val maxTemp = slots.mapNotNull { it.main?.tempMax ?: it.main?.temp }.maxOrNull() ?: 31.0
            val minTemp = slots.mapNotNull { it.main?.tempMin ?: it.main?.temp }.minOrNull() ?: 22.0
            val maxPop = ((slots.mapNotNull { it.precipitationProbability }.maxOrNull() ?: 0.2) * 100.0)
                .roundToInt()
                .coerceIn(0, 100)
            val avgHumidity = slots.mapNotNull { it.main?.humidity }
                .takeIf { it.isNotEmpty() }
                ?.average()
                ?.roundToInt()
                ?: 75
            val middaySlot = slots.getOrNull(slots.size / 2) ?: slots.first()
            val owmCondition = middaySlot.weather?.firstOrNull()
            val mappedCode = mapOpenWeatherIdToCode(owmCondition?.id)
            val label = owmCondition?.description
                ?.trim()
                ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                ?.takeIf { it.isNotBlank() }
                ?: owmCondition?.main
                ?: shortConditionForCode(mappedCode)

            DailyForecast(
                dateIso = dateIso,
                weatherCode = mappedCode,
                tempMaxC = maxTemp,
                tempMinC = minTemp,
                precipChancePct = maxPop,
                conditionLabel = label,
                humidityPct = avgHumidity
            )
        }
    }

    private fun slotDateIso(slot: OwmForecastItemDto): String? {
        val sec = slot.timestampSec
        if (sec != null && sec > 0L) {
            return formatManilaDate(sec * 1000L)
        }
        return slot.dtTxt?.substringBefore(" ")?.takeIf { it.isNotBlank() }
    }

    private fun formatManilaDate(millis: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        fmt.timeZone = manilaTimeZone
        return fmt.format(Date(millis))
    }

    private fun mapOpenWeatherIdToCode(owmId: Int?): Int = when (owmId) {
        null -> 2
        in 200..299 -> 95
        in 300..599 -> 61
        in 600..699 -> 61
        in 700..799 -> 45
        800 -> 0
        in 801..804 -> 2
        else -> 2
    }

    private fun shortConditionForCode(code: Int): String = when (code) {
        0 -> "Clear Sky"
        in 1..3 -> "Partly Cloudy"
        in 45..48 -> "Valley Mist"
        in 51..67, in 80..82 -> "Rain Showers"
        in 95..99 -> "Thunderstorm"
        else -> "Mild Breeze"
    }

    private fun isNetworkConnected(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun serializeForecasts(list: List<DailyForecast>): String {
        val arr = JSONArray()
        list.take(3).forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("dateIso", item.dateIso)
                    put("weatherCode", item.weatherCode)
                    put("tempMaxC", item.tempMaxC)
                    put("tempMinC", item.tempMinC)
                    put("precipChancePct", item.precipChancePct)
                    put("conditionLabel", item.conditionLabel)
                    put("humidityPct", item.humidityPct)
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
                val code = obj.optInt("weatherCode", 2)
                DailyForecast(
                    dateIso = obj.optString("dateIso"),
                    weatherCode = code,
                    tempMaxC = obj.optDouble("tempMaxC", 31.0),
                    tempMinC = obj.optDouble("tempMinC", 22.0),
                    precipChancePct = obj.optInt("precipChancePct", 20),
                    conditionLabel = obj.optString("conditionLabel", "").ifBlank { shortConditionForCode(code) },
                    humidityPct = obj.optInt("humidityPct", 75)
                )
            }.take(3)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun CachedWeatherEntity.toSnapshot(
        isFromCache: Boolean,
        conditionDescription: String = "",
        sourceLabel: String = if (isFromCache) "Offline Cache" else "OpenWeatherMap",
        isApiKeyMissing: Boolean = false,
        highTempC: Double = temperatureC + 2.0,
        lowTempC: Double = temperatureC - 4.0,
        pressureHpa: Int = 1012
    ): WeatherSnapshot {
        val forecasts = deserializeForecasts(dailyJson)
        return WeatherSnapshot(
            temperatureC = temperatureC,
            apparentTempC = apparentTempC,
            humidityPct = humidityPct,
            windSpeedKmh = windSpeedKmh,
            weatherCode = weatherCode,
            isFromCache = isFromCache,
            updatedAtMillis = updatedAtMillis,
            dailyForecasts = forecasts,
            conditionDescription = conditionDescription,
            sourceLabel = sourceLabel,
            isApiKeyMissing = isApiKeyMissing,
            highTempC = highTempC,
            lowTempC = lowTempC,
            pressureHpa = pressureHpa
        )
    }

    private fun fallbackWeatherSnapshot(isApiKeyMissing: Boolean): WeatherSnapshot {
        val cal = Calendar.getInstance(manilaTimeZone)
        val upcomingDays = (1..3).map { offset ->
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.DAY_OF_YEAR, offset)
            val dateIso = formatManilaDate(cal.timeInMillis)
            when (offset) {
                1 -> DailyForecast(dateIso, 2, 31.0, 22.5, 20, "Partly Cloudy", 74)
                2 -> DailyForecast(dateIso, 61, 29.0, 21.5, 55, "Scattered Showers", 82)
                else -> DailyForecast(dateIso, 0, 32.0, 22.0, 15, "Clear Sky", 70)
            }
        }
        return WeatherSnapshot(
            temperatureC = 28.4,
            apparentTempC = 31.0,
            humidityPct = 74,
            windSpeedKmh = 8.6,
            weatherCode = 2,
            isFromCache = true,
            updatedAtMillis = System.currentTimeMillis(),
            dailyForecasts = upcomingDays,
            conditionDescription = "Mild highland breeze",
            sourceLabel = "Offline Cache",
            isApiKeyMissing = isApiKeyMissing,
            highTempC = 31.0,
            lowTempC = 22.5,
            pressureHpa = 1012,
            isSample = true
        )
    }

    private companion object {
        const val TAG = "WeatherRepository"
        const val PLACEHOLDER_KEY = "MY_OPENWEATHERMAP_API_KEY"
    }
}
