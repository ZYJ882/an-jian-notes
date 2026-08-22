package com.example.anjiannotes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF0A5FFF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEAF2FF),
    onPrimaryContainer = Color(0xFF003A8C),
    secondary = Color(0xFF5E5E66),
    secondaryContainer = Color(0xFFF2F2F7),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF5F5F7),
    onSurface = Color(0xFF111113),
    onSurfaceVariant = Color(0xFF6C6C75),
    outlineVariant = Color(0xFFE1E1E6),
    error = Color(0xFFD92D20)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF75AFFF),
    onPrimary = Color(0xFF002B64),
    primaryContainer = Color(0xFF063B7A),
    onPrimaryContainer = Color(0xFFD9E8FF),
    secondary = Color(0xFFC6C6CC),
    secondaryContainer = Color(0xFF1C1C1E),
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceVariant = Color(0xFF1C1C1E),
    onSurface = Color(0xFFF5F5F7),
    onSurfaceVariant = Color(0xFFAEAEB2),
    outlineVariant = Color(0xFF38383A),
    error = Color(0xFFFF6961)
)

private val AnJianShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AnJianTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = Typography,
        shapes = AnJianShapes,
        content = content
    )
}
