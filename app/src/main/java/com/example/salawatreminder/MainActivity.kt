package com.example.salawatreminder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.salawatreminder.ui.theme.SalawatReminderTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SalawatReminderTheme {
                MainScreen()
            }
        }
    }
}