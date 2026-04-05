package com.example.salawatreminder

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.work.Worker
import androidx.work.WorkerParameters

class SoundWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {

        val uriString = inputData.getString("soundUri")
        val uri = if (uriString != null) {
            Uri.parse(uriString)
        } else {
            Uri.parse("android.resource://${applicationContext.packageName}/${R.raw.default_sound}")
        }

        val mediaPlayer = MediaPlayer.create(applicationContext, uri)
        mediaPlayer?.start()

        return Result.success()
    }
}