package com.example.anjiannotes.ui.theme

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.lerp
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
    val colorScheme = animatedColorScheme(useDarkTheme)
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

/** 统一用一个 240ms 进度动画完成主题切换，避免为每种颜色分别调度动画。 */
@Composable
private fun animatedColorScheme(useDarkTheme: Boolean): ColorScheme {
    val progress by animateFloatAsState(
        targetValue = if (useDarkTheme) 1f else 0f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "theme-color-progress"
    )
    return ComfortableLightColors.blendTo(ComfortableDarkColors, progress)
}

private fun ColorScheme.blendTo(target: ColorScheme, progress: Float): ColorScheme = copy(
    primary = lerp(primary, target.primary, progress),
    onPrimary = lerp(onPrimary, target.onPrimary, progress),
    primaryContainer = lerp(primaryContainer, target.primaryContainer, progress),
    onPrimaryContainer = lerp(onPrimaryContainer, target.onPrimaryContainer, progress),
    secondary = lerp(secondary, target.secondary, progress),
    onSecondary = lerp(onSecondary, target.onSecondary, progress),
    secondaryContainer = lerp(secondaryContainer, target.secondaryContainer, progress),
    onSecondaryContainer = lerp(onSecondaryContainer, target.onSecondaryContainer, progress),
    tertiary = lerp(tertiary, target.tertiary, progress),
    onTertiary = lerp(onTertiary, target.onTertiary, progress),
    tertiaryContainer = lerp(tertiaryContainer, target.tertiaryContainer, progress),
    onTertiaryContainer = lerp(onTertiaryContainer, target.onTertiaryContainer, progress),
    background = lerp(background, target.background, progress),
    onBackground = lerp(onBackground, target.onBackground, progress),
    surface = lerp(surface, target.surface, progress),
    onSurface = lerp(onSurface, target.onSurface, progress),
    surfaceVariant = lerp(surfaceVariant, target.surfaceVariant, progress),
    onSurfaceVariant = lerp(onSurfaceVariant, target.onSurfaceVariant, progress),
    surfaceTint = lerp(surfaceTint, target.surfaceTint, progress),
    inverseSurface = lerp(inverseSurface, target.inverseSurface, progress),
    inverseOnSurface = lerp(inverseOnSurface, target.inverseOnSurface, progress),
    inversePrimary = lerp(inversePrimary, target.inversePrimary, progress),
    error = lerp(error, target.error, progress),
    onError = lerp(onError, target.onError, progress),
    errorContainer = lerp(errorContainer, target.errorContainer, progress),
    onErrorContainer = lerp(onErrorContainer, target.onErrorContainer, progress),
    outline = lerp(outline, target.outline, progress),
    outlineVariant = lerp(outlineVariant, target.outlineVariant, progress),
    scrim = lerp(scrim, target.scrim, progress),
    surfaceBright = lerp(surfaceBright, target.surfaceBright, progress),
    surfaceDim = lerp(surfaceDim, target.surfaceDim, progress),
    surfaceContainer = lerp(surfaceContainer, target.surfaceContainer, progress),
    surfaceContainerHigh = lerp(surfaceContainerHigh, target.surfaceContainerHigh, progress),
    surfaceContainerHighest = lerp(surfaceContainerHighest, target.surfaceContainerHighest, progress),
    surfaceContainerLow = lerp(surfaceContainerLow, target.surfaceContainerLow, progress),
    surfaceContainerLowest = lerp(surfaceContainerLowest, target.surfaceContainerLowest, progress)
)
