package com.example.salawatreminder

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.*

class SoundWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {

        val nowHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val startQuiet = inputData.getInt("startQuietHour", 22)
        val endQuiet = inputData.getInt("endQuietHour", 6)

        val inQuietHours = if (startQuiet <= endQuiet) {
            nowHour in startQuiet until endQuiet
        } else {
            nowHour >= startQuiet || nowHour < endQuiet
        }

        if (!inQuietHours) {
            val uriString = inputData.getString("soundUri")
            val uri = if (uriString != null) {
                Uri.parse(uriString)
            } else {
                Uri.parse("android.resource://${applicationContext.packageName}/${R.raw.default_sound}")
            }

            val mediaPlayer = MediaPlayer.create(applicationContext, uri)
            mediaPlayer?.start()
        }

        return Result.success()
    }
}