package com.example.salawatreminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.util.*

class ForegroundSoundService : Service() {

    private val handler = Handler()
    private var soundUri: Uri? = null // nullable to prevent crash
    private var intervalMinutes: Long = 15
    private var startQuietHour = 23
    private var endQuietHour = 11

    private val runnable = object : Runnable {
        override fun run() {
            playSoundIfAllowed()
            handler.postDelayed(this, intervalMinutes * 60 * 1000)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Get soundUri and settings from Intent extras
        intent?.getStringExtra("soundUri")?.let {
            soundUri = Uri.parse(it)
        }
        intervalMinutes = intent?.getLongExtra("intervalMinutes", 15) ?: 15
        startQuietHour = intent?.getIntExtra("startQuietHour", 23) ?: 23
        endQuietHour = intent?.getIntExtra("endQuietHour", 11) ?: 11

        // 1️⃣ Create notification channel
        createNotificationChannel()

        // 2️⃣ Build notification with system icon
        val notification = NotificationCompat.Builder(this, "sound_channel")
            .setContentTitle("Salawat Reminder Active")
            .setContentText("Service running")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // guaranteed to show
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // 3️⃣ Start foreground immediately
        startForeground(1, notification)

        // 4️⃣ Start recurring handler
        handler.postDelayed(runnable, 1000)

        return START_STICKY
    }

    private fun playSoundIfAllowed() {
        val nowHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val inQuietHours = if (startQuietHour <= endQuietHour) {
            nowHour in startQuietHour until endQuietHour
        } else {
            nowHour >= startQuietHour || nowHour < endQuietHour
        }

        if (!inQuietHours) {
            val uri = soundUri ?: Uri.parse("android.resource://${packageName}/${R.raw.default_sound}")
            val mediaPlayer = MediaPlayer.create(this, uri)
            mediaPlayer?.apply {
                setOnCompletionListener { it.release() }
                start()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "sound_channel",
                "Salawat Reminder",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Plays sound reminders every interval"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}