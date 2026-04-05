package com.example.salawatreminder

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import java.util.*

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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
