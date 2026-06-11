package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ZisprColorScheme = darkColorScheme(
    primary = Color(0xFFD2E750), // Elegant Lime
    onPrimary = Color.Black,
    secondary = Color(0xFF2D2F20), // Selected background pill
    onSecondary = Color(0xFFD2E750),
    tertiary = Color(0xFFFF5E7E),
    onTertiary = Color.White,
    background = Color(0xFF121212), // Dark Background
    onBackground = Color(0xFFF1F5F9), // slate-100
    surface = Color(0xFF242424), // Elegant surface
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF282828), // Elegant surface variant
    onSurfaceVariant = Color(0xFF94A3B8) // slate-400
)

@Composable
fun ZisprTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ZisprColorScheme,
        typography = Typography,
        content = content
    )
}
