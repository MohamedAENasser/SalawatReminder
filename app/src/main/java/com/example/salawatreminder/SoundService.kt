package com.example.salawatreminder

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import java.util.*
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.annotation.RequiresApi

class SoundService : Service() {

    private val handler = Handler()
    private var mediaPlayer: MediaPlayer? = null
    private var interval: Int = 1
    private var startHour: Int = 0
    private var endHour: Int = 0
    private var soundUri: Uri? = null

    private val playSoundTask = object : Runnable {
        override fun run() {
            val now = Calendar.getInstance()
            val currentHour = now.get(Calendar.HOUR_OF_DAY)

            if (!(currentHour in startHour until endHour)) {
                mediaPlayer?.start()
            }
            handler.postDelayed(this, interval * 60 * 1000L)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "salawat_channel",
                "Salawat Reminder",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        val notification = Notification.Builder(this, "salawat_channel")
            .setContentTitle("Salawat Reminder")
            .setContentText("Running...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        startForeground(1, notification)

        val uriString = intent?.getStringExtra("soundUri")

        soundUri = if (uriString != null) {
            Uri.parse(uriString)
        } else {
            Uri.parse("android.resource://$packageName/${R.raw.default_sound}")
        }
        interval = intent?.getIntExtra("interval", 1) ?: 1
        startHour = intent?.getIntExtra("startHour", 0) ?: 0
        endHour = intent?.getIntExtra("endHour", 0) ?: 0

        soundUri?.let {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, it)
        }

        handler.post(playSoundTask)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(playSoundTask)
        mediaPlayer?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
