package com.example.salawatreminder

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.work.*
import java.util.concurrent.TimeUnit

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen() {

    val context = LocalContext.current

    var interval by remember { mutableStateOf("15") } // default 15 min
    var soundUri by remember { mutableStateOf<Uri?>(null) }
    var startQuietHour by remember { mutableStateOf(23) } // default 11 PM
    var endQuietHour by remember { mutableStateOf(11) } // default 11 AM

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        soundUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = interval,
            onValueChange = { interval = it },
            label = { Text("Interval (minutes)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Quiet Hours: ", modifier = Modifier.padding(end = 8.dp))
            OutlinedTextField(
                value = startQuietHour.toString(),
                onValueChange = { startQuietHour = it.toIntOrNull() ?: startQuietHour },
                label = { Text("Start") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(80.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = endQuietHour.toString(),
                onValueChange = { endQuietHour = it.toIntOrNull() ?: endQuietHour },
                label = { Text("End") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(80.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { launcher.launch("audio/*") }, modifier = Modifier.fillMaxWidth()) {
            Text("Select Sound")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val intent = Intent(context, ForegroundSoundService::class.java).apply {
                putExtra("soundUri", soundUri)
                putExtra("intervalMinutes", (interval.toLongOrNull() ?: 15).coerceAtLeast(1))
                putExtra("startQuietHour", startQuietHour)
                putExtra("endQuietHour", endQuietHour)
            }
            context.startForegroundService(intent)
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Start")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            val intent = Intent(context, ForegroundSoundService::class.java)
            context.stopService(intent)
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Stop")
        }
    }
}