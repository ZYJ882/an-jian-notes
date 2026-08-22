package com.example.anjiannotes.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 安笺统一采用克制的高级暗色界面：低亮度蓝色作为唯一强调色，
 * 页面、卡片与分割线通过微弱明度差建立层级，而不是依赖重阴影或描边。
 */
private val PremiumDarkColors = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF0D1B2D),
    primaryContainer = Color(0xFF1C3557),
    onPrimaryContainer = Color(0xFFDCE9FF),
    secondary = Color(0xFFB8BEC7),
    secondaryContainer = Color(0xFF1C2027),
    background = Color(0xFF0D0F13),
    surface = Color(0xFF16181D),
    surfaceVariant = Color(0xFF1C2027),
    onSurface = Color(0xFFF5F5F7),
    onSurfaceVariant = Color(0xFF9AA0A6),
    outlineVariant = Color(0xFF242A33),
    error = Color(0xFFFFB4AB)
)

private val AnJianShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun AnJianTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PremiumDarkColors,
        typography = Typography,
        shapes = AnJianShapes,
        content = content
    )
}
