package com.myvillagebus.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Kolory dla jasnego motywu
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),      // Niebieski
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB),
    secondary = Color(0xFF03DAC5),
    onSecondary = Color.Black,
    background = Color(0xFFFAFAFA),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F)
)

// Kolory dla ciemnego motywu
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),      // Jaśniejszy niebieski
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF004A77),
    secondary = Color(0xFF03DAC5),
    onSecondary = Color.Black,
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF2B2930),
    onSurface = Color(0xFFE6E1E5)
)

@Composable
fun BusScheduleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),  // Automatycznie wykrywa motyw systemu
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}