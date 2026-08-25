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
    primary = Color(0xFF70558C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE9DDF3),
    onPrimaryContainer = Color(0xFF2E1D3F),
    secondary = Color(0xFF635B68),
    secondaryContainer = Color(0xFFECE6ED),
    onSecondaryContainer = Color(0xFF29242C),
    background = Color(0xFFF7F5F7),
    onBackground = Color(0xFF27242A),
    surface = Color(0xFFFFFCFF),
    onSurface = Color(0xFF27242A),
    surfaceVariant = Color(0xFFF3F0F4),
    onSurfaceVariant = Color(0xFF6E6972),
    outline = Color(0xFF847E87),
    outlineVariant = Color(0xFFDED9E1),
    error = Color(0xFFB3261E)
)

private val ComfortableDarkColors = darkColorScheme(
    primary = Color(0xFFC6A8DE),
    onPrimary = Color(0xFF39264B),
    primaryContainer = Color(0xFF503B62),
    onPrimaryContainer = Color(0xFFEAD9FA),
    secondary = Color(0xFFD0C6D3),
    secondaryContainer = Color(0xFF302B33),
    onSecondaryContainer = Color(0xFFE8E0E9),
    background = Color(0xFF151316),
    onBackground = Color(0xFFE7E1E7),
    surface = Color(0xFF1D1A1F),
    onSurface = Color(0xFFE7E1E7),
    surfaceVariant = Color(0xFF242127),
    onSurfaceVariant = Color(0xFFCAC3CC),
    outline = Color(0xFF958D98),
    outlineVariant = Color(0xFF48434B),
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
