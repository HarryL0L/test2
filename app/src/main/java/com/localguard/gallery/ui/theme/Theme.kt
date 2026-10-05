package com.localguard.gallery.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val FallbackLight = lightColorScheme(
    primary = Color(0xFF4355B9),
    secondary = Color(0xFF5B5D72),
    tertiary = Color(0xFF77536D),
)

private val FallbackDark = darkColorScheme(
    primary = Color(0xFFBAC3FF),
    secondary = Color(0xFFC4C5DD),
    tertiary = Color(0xFFE6BAD7),
)

/** Material You: wallpaper-based dynamic colour on Android 12+, a static M3 palette below that. */
@Composable
fun GuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> FallbackDark
        else -> FallbackLight
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
