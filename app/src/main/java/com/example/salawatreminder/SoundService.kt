package com.example.salawatreminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.Calendar

class SoundService : Service() {

    private var mediaPlayer: MediaPlayer? = null

    companion object {
        const val CHANNEL_ID = "salawat_sound_channel"
        const val NOTIF_ID = 42
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Must call startForeground immediately (within 5 seconds on Android 8+)
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())

        val prefs = getSharedPreferences("salawat_prefs", MODE_PRIVATE)
        val startQuietHour = prefs.getInt("start_quiet_hour", 23)
        val endQuietHour = prefs.getInt("end_quiet_hour", 11)

        if (isQuietTime(startQuietHour, endQuietHour)) {
            Log.d("SoundService", "Quiet hours — skipping sound")
            stopSelf()
            return START_NOT_STICKY
        }

        playSound(prefs.getString("sound_uri", null))

        return START_NOT_STICKY // Don't restart — AlarmManager handles timing
    }

    private fun isQuietTime(startHour: Int, endHour: Int): Boolean {
        val nowHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (startHour > endHour) {
            // Spans midnight e.g. 23 → 11
            nowHour >= startHour || nowHour < endHour
        } else {
            nowHour in startHour until endHour
        }
    }

    private fun playSound(uriString: String?) {
        try {
            // MediaPlayer.create() is required for raw resources — it calls prepare() internally.
            // For user-picked external URIs, we must use setDataSource() + prepare() instead.
            mediaPlayer = if (uriString.isNullOrEmpty()) {
                MediaPlayer.create(applicationContext, R.raw.default_sound)
            } else {
                MediaPlayer().apply {
                    setDataSource(applicationContext, Uri.parse(uriString))
                    prepare()
                }
            }

            mediaPlayer?.apply {
                setOnCompletionListener {
                    Log.d("SoundService", "Sound done, releasing")
                    it.release()
                    mediaPlayer = null
                    stopSelf()
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e("SoundService", "MediaPlayer error what=$what extra=$extra")
                    mp.release()
                    mediaPlayer = null
                    stopSelf()
                    true
                }
                start()
            }
            Log.d("SoundService", "Sound started")
        } catch (e: Exception) {
            Log.e("SoundService", "Failed to play sound", e)
            stopSelf()
        }
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Salawat Reminder")
        .setContentText("Playing reminder...")
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setOngoing(false)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Salawat Reminder",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Plays the Salawat reminder sound"
                setSound(null, null) // We handle sound ourselves
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
