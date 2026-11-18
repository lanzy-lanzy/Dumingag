package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.CalendarContract
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.model.ItineraryItem

object TravelAlertManager {
    const val CHANNEL_ID = "dumingag_travel_alerts"
    const val EXTRA_TITLE = "extra_alert_title"
    const val EXTRA_MESSAGE = "extra_alert_message"
    const val EXTRA_ID = "extra_alert_id"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Dumingag Travel Alerts & Itinerary",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Travel weather advisories, upland alerts, and itinerary reminders in Dumingag"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showNotification(context: Context, title: String, message: String, notificationId: Int = 1001) {
        ensureChannel(context)
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted on Android 13+; handled gracefully in UI
        }
    }

    fun scheduleItineraryReminder(context: Context, item: ItineraryItem) {
        ensureChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TravelAlertReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, "Upcoming Dumingag Visit: ${item.attractionName}")
            putExtra(
                EXTRA_MESSAGE,
                item.notes.ifBlank { "Check offline map route and local transit times before departure." }
            )
            putExtra(EXTRA_ID, item.id.hashCode())
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = (item.plannedDateMillis - 30 * 60 * 1000L).coerceAtLeast(System.currentTimeMillis() + 5000L)
        try {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } catch (_: Exception) {
            // Fallback safe if alarm scheduling restricted
        }
    }

    fun createCalendarSyncIntent(item: ItineraryItem): Intent {
        val startMillis = item.plannedDateMillis.coerceAtLeast(System.currentTimeMillis())
        val endMillis = startMillis + 2 * 60 * 60 * 1000L
        return Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "Dumingag Visit: ${item.attractionName}")
            putExtra(CalendarContract.Events.EVENT_LOCATION, "${item.attractionName}, Dumingag, Zamboanga del Sur")
            putExtra(
                CalendarContract.Events.DESCRIPTION,
                "Planned via Dumingag Tourism Guide.\nNotes: ${item.notes.ifBlank { "Bring offline trail map & water." }}"
            )
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
        }
    }
}
