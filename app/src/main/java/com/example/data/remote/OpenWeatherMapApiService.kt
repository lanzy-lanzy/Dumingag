package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OpenWeatherCurrentResponse(
    @Json(name = "coord") val coord: OwmCoordDto? = null,
    @Json(name = "weather") val weather: List<OwmWeatherConditionDto>? = null,
    @Json(name = "main") val main: OwmMainDto? = null,
    @Json(name = "wind") val wind: OwmWindDto? = null,
    @Json(name = "dt") val timestampSec: Long? = null,
    @Json(name = "name") val cityName: String? = null
)

@JsonClass(generateAdapter = true)
data class OpenWeatherForecastResponse(
    @Json(name = "list") val list: List<OwmForecastItemDto>? = null
)

@JsonClass(generateAdapter = true)
data class OwmCoordDto(
    @Json(name = "lat") val lat: Double? = null,
    @Json(name = "lon") val lon: Double? = null
)

@JsonClass(generateAdapter = true)
data class OwmWeatherConditionDto(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "main") val main: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "icon") val icon: String? = null
)

@JsonClass(generateAdapter = true)
data class OwmMainDto(
    @Json(name = "temp") val temp: Double? = null,
    @Json(name = "feels_like") val feelsLike: Double? = null,
    @Json(name = "temp_min") val tempMin: Double? = null,
    @Json(name = "temp_max") val tempMax: Double? = null,
    @Json(name = "humidity") val humidity: Int? = null,
    @Json(name = "pressure") val pressure: Int? = null
)

@JsonClass(generateAdapter = true)
data class OwmWindDto(
    @Json(name = "speed") val speedMetersPerSec: Double? = null,
    @Json(name = "deg") val degrees: Int? = null
)

@JsonClass(generateAdapter = true)
data class OwmForecastItemDto(
    @Json(name = "dt") val timestampSec: Long? = null,
    @Json(name = "dt_txt") val dtTxt: String? = null,
    @Json(name = "main") val main: OwmMainDto? = null,
    @Json(name = "weather") val weather: List<OwmWeatherConditionDto>? = null,
    @Json(name = "pop") val precipitationProbability: Double? = null
)

interface OpenWeatherMapApiService {

    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("lat") latitude: Double = DUMINGAG_LAT,
        @Query("lon") longitude: Double = DUMINGAG_LON,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): OpenWeatherCurrentResponse

    @GET("data/2.5/forecast")
    suspend fun getFiveDayForecast(
        @Query("lat") latitude: Double = DUMINGAG_LAT,
        @Query("lon") longitude: Double = DUMINGAG_LON,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): OpenWeatherForecastResponse

    companion object {
        const val BASE_URL = "https://api.openweathermap.org/"
        const val DUMINGAG_LAT = 8.1650
        const val DUMINGAG_LON = 123.3465

        fun create(): OpenWeatherMapApiService {
            val client = OkHttpClient.Builder()
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .build()
            val moshi = Moshi.Builder().build()
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(OpenWeatherMapApiService::class.java)
        }
    }
}
