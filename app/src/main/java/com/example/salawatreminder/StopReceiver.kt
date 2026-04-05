package com.example.salawatreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives the "Stop" action from the persistent notification.
 */
class StopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Mark as not running
        context.getSharedPreferences("salawat_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("is_running", false).apply()

        // Cancel the scheduled alarm
        AlarmReceiver.cancelAlarm(context)

        // Stop the status foreground service
        context.stopService(Intent(context, StatusService::class.java))
    }
}
