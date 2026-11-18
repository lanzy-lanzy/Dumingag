package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TravelAlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(TravelAlertManager.EXTRA_TITLE)
            ?: "Dumingag Travel Reminder"
        val message = intent.getStringExtra(TravelAlertManager.EXTRA_MESSAGE)
            ?: "Your scheduled Dumingag attraction stop is coming up."
        val id = intent.getIntExtra(TravelAlertManager.EXTRA_ID, 2001)
        TravelAlertManager.showNotification(context, title, message, id)
    }
}
