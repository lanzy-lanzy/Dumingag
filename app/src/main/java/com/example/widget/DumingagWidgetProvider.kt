package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.model.ItineraryItem
import com.example.model.WeatherSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Home Screen Weather & Next-Stop App Widget for Dumingag Travel Guide.
 * Automatically refreshed whenever live weather or itinerary stops update inside the app.
 */
class DumingagWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        private const val PREFS_NAME = "dumingag_widget_snapshot"
        private const val KEY_TEMP = "temp_label"
        private const val KEY_CONDITION = "condition_label"
        private const val KEY_NEXT_STOP = "next_stop_name"
        private const val KEY_NEXT_DATE = "next_stop_date"

        fun syncWeather(context: Context, snapshot: WeatherSnapshot) {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val tempLabel = "${snapshot.temperatureC.roundToInt()}°C"
            val condition = snapshot.conditionDescription.ifBlank {
                when (snapshot.weatherCode) {
                    0, 1 -> "Clear Highland Skies"
                    2, 3 -> "Partly Cloudy · Dumingag"
                    in 51..67, in 80..82 -> "Mountain Showers"
                    in 95..99 -> "Thunderstorm Watch"
                    else -> "Mild Highland Weather"
                }
            }
            prefs.edit()
                .putString(KEY_TEMP, tempLabel)
                .putString(KEY_CONDITION, condition)
                .apply()
            requestRefresh(context)
        }

        fun syncNextItinerary(context: Context, items: List<ItineraryItem>) {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            val upcoming = items
                .filter { it.plannedDateMillis >= now - 3600_000L }
                .minByOrNull { it.plannedDateMillis }
                ?: items.firstOrNull()
            if (upcoming != null) {
                val fmt = SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault())
                prefs.edit()
                    .putString(KEY_NEXT_STOP, upcoming.attractionName)
                    .putString(KEY_NEXT_DATE, fmt.format(Date(upcoming.plannedDateMillis)))
                    .apply()
            } else {
                prefs.edit()
                    .putString(KEY_NEXT_STOP, "Explore Danlugan Falls & Eco-Trails")
                    .putString(KEY_NEXT_DATE, "Tap to plan your Dumingag trip")
                    .apply()
            }
            requestRefresh(context)
        }

        private fun requestRefresh(context: Context) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val ids = manager.getAppWidgetIds(ComponentName(appContext, DumingagWidgetProvider::class.java))
            if (ids.isEmpty()) return
            for (id in ids) {
                updateWidget(appContext, manager, id)
            }
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val temp = prefs.getString(KEY_TEMP, "28°C") ?: "28°C"
            val condition = prefs.getString(KEY_CONDITION, "Partly Cloudy · Highlands") ?: "Partly Cloudy · Highlands"
            val nextStop = prefs.getString(KEY_NEXT_STOP, "Explore Danlugan Falls & Eco-Trails") ?: "Explore Danlugan Falls"
            val nextDate = prefs.getString(KEY_NEXT_DATE, "Tap to open Dumingag Travel Guide") ?: "Tap to open"

            val views = RemoteViews(context.packageName, R.layout.widget_dumingag_summary).apply {
                setTextViewText(R.id.widget_temp, temp)
                setTextViewText(R.id.widget_weather_condition, condition)
                setTextViewText(R.id.widget_next_stop, nextStop)
                setTextViewText(R.id.widget_next_date, nextDate)
            }

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
