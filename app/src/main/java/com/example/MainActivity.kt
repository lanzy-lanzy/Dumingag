package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.notifications.TravelAlertManager
import com.example.ui.AppNavigation
import com.example.ui.theme.DumingagTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        TravelAlertManager.ensureChannel(this)
        setContent {
            DumingagTheme {
                AppNavigation()
            }
        }
    }
}
