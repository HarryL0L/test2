package com.localguard.gallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.localguard.gallery.ui.GalleryApp
import com.localguard.gallery.ui.theme.GuardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GuardTheme {
                GalleryApp()
            }
        }
    }
}
