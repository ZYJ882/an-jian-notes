package com.example.anjiannotes.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/**
 * 两套主题共用相同的文字层级、圆角与低视觉噪声原则：浅色是微暖浅灰，
 * 深色是低亮度深灰，不采用纯白背景、纯黑背景或纯白文字。
 */
private val ComfortableLightColors = lightColorScheme(
    primary = Color(0xFF765F82),
    onPrimary = Color(0xFFFFFBFF),
    primaryContainer = Color(0xFFEEE6F0),
    onPrimaryContainer = Color(0xFF36243E),
    secondary = Color(0xFF6A626B),
    secondaryContainer = Color(0xFFF0EAEE),
    onSecondaryContainer = Color(0xFF302C31),
    background = Color(0xFFF6F3F1),
    onBackground = Color(0xFF2D2A2E),
    surface = Color(0xFFFCF9F7),
    onSurface = Color(0xFF2D2A2E),
    surfaceVariant = Color(0xFFF1EDEC),
    onSurfaceVariant = Color(0xFF706A71),
    outline = Color(0xFF8B848C),
    outlineVariant = Color(0xFFE1DADC),
    error = Color(0xFFB04544)
)

private val ComfortableDarkColors = darkColorScheme(
    primary = Color(0xFFC7B1CF),
    onPrimary = Color(0xFF3C2943),
    primaryContainer = Color(0xFF55425D),
    onPrimaryContainer = Color(0xFFEFDFF2),
    secondary = Color(0xFFD1C8D0),
    secondaryContainer = Color(0xFF322E34),
    onSecondaryContainer = Color(0xFFE8E1E9),
    background = Color(0xFF1A181B),
    onBackground = Color(0xFFE3DEE3),
    surface = Color(0xFF211F22),
    onSurface = Color(0xFFE3DEE3),
    surfaceVariant = Color(0xFF2A272C),
    onSurfaceVariant = Color(0xFFC9C2CA),
    outline = Color(0xFF968D96),
    outlineVariant = Color(0xFF49454C),
    error = Color(0xFFFFB4AB)
)

private val AnJianShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun AnJianTheme(
    appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    systemDarkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (appearanceMode) {
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
        AppearanceMode.SYSTEM -> systemDarkTheme
    }
    val targetScheme = if (useDarkTheme) ComfortableDarkColors else ComfortableLightColors
    val colorScheme = targetScheme.animatedColorScheme()
    val view = LocalView.current
    val window = (view.context as? Activity)?.window
    SideEffect {
        window?.let {
            it.statusBarColor = colorScheme.background.toArgb()
            it.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(it, view).apply {
                isAppearanceLightStatusBars = !useDarkTheme
                isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AnJianShapes,
        content = content
    )
}

/** 统一用 240ms 的颜色插值完成主题切换，页面结构和导航状态保持不动。 */
@Composable
private fun ColorScheme.animatedColorScheme(): ColorScheme {
    val spec = tween<Color>(durationMillis = 240, easing = FastOutSlowInEasing)
    @Composable
    fun animated(target: Color): Color {
        val value by animateColorAsState(targetValue = target, animationSpec = spec, label = "theme-color")
        return value
    }
    return copy(
        primary = animated(primary),
        onPrimary = animated(onPrimary),
        primaryContainer = animated(primaryContainer),
        onPrimaryContainer = animated(onPrimaryContainer),
        secondary = animated(secondary),
        onSecondary = animated(onSecondary),
        secondaryContainer = animated(secondaryContainer),
        onSecondaryContainer = animated(onSecondaryContainer),
        background = animated(background),
        onBackground = animated(onBackground),
        surface = animated(surface),
        onSurface = animated(onSurface),
        surfaceVariant = animated(surfaceVariant),
        onSurfaceVariant = animated(onSurfaceVariant),
        surfaceTint = animated(surfaceTint),
        inverseSurface = animated(inverseSurface),
        inverseOnSurface = animated(inverseOnSurface),
        inversePrimary = animated(inversePrimary),
        error = animated(error),
        onError = animated(onError),
        errorContainer = animated(errorContainer),
        onErrorContainer = animated(onErrorContainer),
        outline = animated(outline),
        outlineVariant = animated(outlineVariant),
        scrim = animated(scrim)
    )
}
