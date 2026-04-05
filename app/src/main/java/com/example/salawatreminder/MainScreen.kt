import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import com.example.salawatreminder.SoundService
import androidx.compose.foundation.layout.systemBarsPadding

@Composable
fun MainScreen() {

    var interval by remember { mutableStateOf("") }
    var soundUri by remember { mutableStateOf<Uri?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        soundUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()   // 👈 handles safe area
            .padding(16.dp)
    ) {

        OutlinedTextField(
            value = interval,
            onValueChange = { interval = it },
            label = { Text("Interval (minutes)") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            launcher.launch("audio/*")
        }) {
            Text("Select Sound")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val serviceIntent = Intent(context, SoundService::class.java).apply {
                putExtra("soundUri", soundUri?.toString())
                putExtra("interval", interval.toIntOrNull() ?: 1)
            }
            context.startService(serviceIntent)
        }) {
            Text("Start")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            context.stopService(Intent(context, SoundService::class.java))
        }) {
            Text("Stop")
        }
    }
}