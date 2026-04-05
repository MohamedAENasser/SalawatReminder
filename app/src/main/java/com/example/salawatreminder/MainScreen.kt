package com.example.salawatreminder

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("salawat_prefs", Context.MODE_PRIVATE) }

    // Load persisted state
    var interval by remember { mutableStateOf(prefs.getLong("interval_minutes", 15).toString()) }
    var startQuietHour by remember { mutableStateOf(prefs.getInt("start_quiet_hour", 23)) }
    var endQuietHour by remember { mutableStateOf(prefs.getInt("end_quiet_hour", 11)) }
    var soundUriString by remember { mutableStateOf(prefs.getString("sound_uri", null)) }
    var isRunning by remember { mutableStateOf(prefs.getBoolean("is_running", false)) }

    // Sound file picker
    val soundPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            // Take persistable permission so URI survives app death
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            soundUriString = uri.toString()
            prefs.edit().putString("sound_uri", uri.toString()).apply()
        }
    }

    // Notification permission (Android 13+)
    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handled */ }

    fun startReminder() {
        val intervalMinutes = interval.toLongOrNull()?.coerceAtLeast(1) ?: 15

        // Persist all settings
        prefs.edit()
            .putLong("interval_minutes", intervalMinutes)
            .putInt("start_quiet_hour", startQuietHour)
            .putInt("end_quiet_hour", endQuietHour)
            .putString("sound_uri", soundUriString)
            .putBoolean("is_running", true)
            .apply()

        // Request exact alarm permission on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                // Open settings so user grants permission
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                )
                return
            }
        }

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Play sound immediately on Start
        val soundIntent = Intent(context, SoundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(soundIntent)
        } else {
            context.startService(soundIntent)
        }

        // Start the persistent status notification service
        val statusIntent = Intent(context, StatusService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(statusIntent)
        } else {
            context.startService(statusIntent)
        }

        // Schedule the first recurring alarm
        AlarmReceiver.scheduleNextAlarm(context)

        isRunning = true
    }

    fun stopReminder() {
        prefs.edit().putBoolean("is_running", false).apply()
        AlarmReceiver.cancelAlarm(context)
        context.stopService(Intent(context, StatusService::class.java))
        isRunning = false
    }

    val bgColor by animateColorAsState(
        targetValue = if (isRunning) Color(0xFFE8F5E9) else Color(0xFFF5F5F5),
        animationSpec = tween(600), label = "bg"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .systemBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Salawat Reminder",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B5E20)
        )

        if (isRunning) {
            Surface(
                color = Color(0xFFC8E6C9),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "✅ Reminder is active — plays every ${interval} min",
                    modifier = Modifier.padding(12.dp),
                    color = Color(0xFF1B5E20),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        OutlinedTextField(
            value = interval,
            onValueChange = { interval = it },
            label = { Text("Interval (minutes)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRunning
        )

        Text("Quiet Hours (no sound between these hours)", fontWeight = FontWeight.Medium)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = startQuietHour.toString(),
                onValueChange = { startQuietHour = it.toIntOrNull()?.coerceIn(0, 23) ?: startQuietHour },
                label = { Text("From (0-23)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                enabled = !isRunning
            )
            Text("→")
            OutlinedTextField(
                value = endQuietHour.toString(),
                onValueChange = { endQuietHour = it.toIntOrNull()?.coerceIn(0, 23) ?: endQuietHour },
                label = { Text("Until (0-23)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                enabled = !isRunning
            )
        }

        OutlinedButton(
            onClick = { soundPickerLauncher.launch(arrayOf("audio/*")) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRunning
        ) {
            Text(if (soundUriString != null) "✓ Custom sound selected" else "Select Sound File (optional)")
        }

        Spacer(modifier = Modifier.weight(1f))

        if (!isRunning) {
            Button(
                onClick = { startReminder() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text("Start Reminder", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Button(
                onClick = { stopReminder() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
            ) {
                Text("Stop Reminder", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
