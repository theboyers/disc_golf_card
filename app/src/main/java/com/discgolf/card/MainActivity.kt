package com.discgolf.card

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.discgolf.card.ui.AppNav
import com.discgolf.card.ui.theme.DiscGolfCardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DiscGolfCardTheme {
                AppNav()
            }
        }
    }
}
