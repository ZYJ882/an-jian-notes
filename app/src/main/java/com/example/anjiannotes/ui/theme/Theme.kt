package com.example.anjiannotes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF6E6250),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEAE1D2),
    onPrimaryContainer = Color(0xFF2A251D),
    secondary = Color(0xFF625E56),
    secondaryContainer = Color(0xFFEAE6DF),
    background = Color(0xFFFAF7F1),
    surface = Color(0xFFFAF7F1),
    surfaceVariant = Color(0xFFEDE9E1),
    onSurface = Color(0xFF25231F),
    onSurfaceVariant = Color(0xFF605C54),
    outlineVariant = Color(0xFFD1CCC2),
    error = Color(0xFF9D433B)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD9C6A6),
    onPrimary = Color(0xFF3A3022),
    primaryContainer = Color(0xFF504536),
    onPrimaryContainer = Color(0xFFF2DFC0),
    secondary = Color(0xFFCBC3B7),
    secondaryContainer = Color(0xFF393630),
    background = Color(0xFF171613),
    surface = Color(0xFF1C1B18),
    surfaceVariant = Color(0xFF2C2A25),
    onSurface = Color(0xFFE7E1D7),
    onSurfaceVariant = Color(0xFFC8C2B8),
    outlineVariant = Color(0xFF4B4740),
    error = Color(0xFFFFB4AB)
)

@Composable
fun AnJianTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
