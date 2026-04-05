package com.example.salawatreminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Persistent foreground service that shows the "Reminder Active" notification.
 * This keeps a visible indicator that the reminder is scheduled, and also
 * survives battery optimization better than no foreground service at all.
 *
 * NOTE: The actual sound scheduling is done by AlarmManager (survives kill),
 * so this service is optional but improves reliability on some OEMs (Samsung,
 * Xiaomi, Huawei) that aggressively kill apps.
 */
class StatusService : Service() {

    companion object {
        const val CHANNEL_ID = "salawat_status_channel"
        const val NOTIF_ID = 1
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        val prefs = getSharedPreferences("salawat_prefs", MODE_PRIVATE)
        val interval = prefs.getLong("interval_minutes", 15)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Salawat Reminder Active")
            .setContentText("Playing every $interval min · Tap to open")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(
                android.R.drawable.ic_delete,
                "Stop",
                PendingIntent.getBroadcast(
                    this, 1,
                    Intent(this, StopReceiver::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

        startForeground(NOTIF_ID, notification)
        return START_STICKY // We want this one to restart if killed
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Salawat Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows that the reminder is active"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
