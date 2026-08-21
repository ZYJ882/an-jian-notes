package com.example.anjiannotes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF735B35),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFDE7B6),
    onPrimaryContainer = Color(0xFF251A05),
    secondary = Color(0xFF5F5A50),
    background = Color(0xFFFFFBF6),
    surface = Color(0xFFFFFBF6),
    surfaceVariant = Color(0xFFF0EDE5),
    onSurface = Color(0xFF1E1B17),
    onSurfaceVariant = Color(0xFF4C463D),
    error = Color(0xFFB3261E)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE8C785),
    onPrimary = Color(0xFF3D2E0A),
    primaryContainer = Color(0xFF564216),
    onPrimaryContainer = Color(0xFFFFDE99),
    secondary = Color(0xFFCFC5B5),
    background = Color(0xFF15130F),
    surface = Color(0xFF15130F),
    surfaceVariant = Color(0xFF343027),
    onSurface = Color(0xFFE9E2D9),
    onSurfaceVariant = Color(0xFFCEC6BA),
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
