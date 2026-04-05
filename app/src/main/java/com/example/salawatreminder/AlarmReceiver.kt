package com.example.salawatreminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.salawatreminder.SoundService

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "Alarm fired: ${intent.action}")

        val prefs = context.getSharedPreferences("salawat_prefs", Context.MODE_PRIVATE)
        val isRunning = prefs.getBoolean("is_running", false)

        when (intent.action) {
            ACTION_PLAY_SOUND -> {
                if (!isRunning) return

                // Start the foreground service to play sound
                val serviceIntent = Intent(context, SoundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }

                // Reschedule the next alarm immediately
                scheduleNextAlarm(context)
            }

            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                // Re-schedule after reboot if it was running before
                if (isRunning) {
                    scheduleNextAlarm(context)
                }
            }
        }
    }

    companion object {
        const val ACTION_PLAY_SOUND = "com.example.salawatreminder.PLAY_SOUND"

        fun scheduleNextAlarm(context: Context) {
            val prefs = context.getSharedPreferences("salawat_prefs", Context.MODE_PRIVATE)
            val intervalMinutes = prefs.getLong("interval_minutes", 15)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_PLAY_SOUND
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = System.currentTimeMillis() + intervalMinutes * 60 * 1000L

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // This fires even in Doze mode / when app is killed
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }

            Log.d("AlarmReceiver", "Next alarm scheduled in $intervalMinutes minutes")
        }

        fun cancelAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_PLAY_SOUND
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Log.d("AlarmReceiver", "Alarm cancelled")
        }
    }
}