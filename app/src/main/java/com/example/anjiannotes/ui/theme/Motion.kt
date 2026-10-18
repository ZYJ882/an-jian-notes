package com.example.anjiannotes.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.IntOffset

/**
 * 动效档位。
 *
 * 四档按速度与幅度递增：极速（默认）→ 标准 → 舒展 → 沉浸。
 * 所有档位都只做 alpha / scale 等绘制层变换，不使用触发重新测量的布局动画；
 * 区别仅在时长、缓动与列表项位置弹簧的刚度，动画种类与数量不随档位变化。
 *
 * 扩展方式：新增档位只需在枚举中追加一项并给出 [MotionSpec]；
 * 所有动画调用点都经 [motionSpec] 读取，无需逐个修改。
 */
enum class MotionLevel(
    /** 持久化键，同时是设置项的稳定标识。 */
    val key: String,
    /** 设置页显示名。 */
    val label: String,
    /** 设置页一句话说明。 */
    val description: String,
    /** 本档的完整动效规格。 */
    val spec: MotionSpec
) {
    /** 极速：默认档，最快、最短，几乎无等待感。 */
    FASTEST(
        key = "fastest",
        label = "极速",
        description = "最快、最短的过渡，几乎无等待感（默认）",
        spec = MotionSpec(
            micro = 80,
            fast = 100,
            medium = 140,
            easing = FastOutSlowInEasing,
            placementSpec = spring<IntOffset>(
                stiffness = Spring.StiffnessHigh,
                dampingRatio = Spring.DampingRatioNoBouncy
            )
        )
    ),

    /** 标准：稍长的过渡，节奏更从容。 */
    BALANCED(
        key = "balanced",
        label = "标准",
        description = "稍长的过渡，节奏更从容",
        spec = MotionSpec(
            micro = 100,
            fast = 130,
            medium = 180,
            easing = FastOutSlowInEasing,
            placementSpec = spring<IntOffset>(
                stiffness = Spring.StiffnessMedium,
                dampingRatio = Spring.DampingRatioNoBouncy
            )
        )
    ),

    /** 舒展：更完整的过渡，动作更明显。 */
    EXPRESSIVE(
        key = "expressive",
        label = "舒展",
        description = "更完整的过渡，动作更明显",
        spec = MotionSpec(
            micro = 130,
            fast = 170,
            medium = 230,
            easing = FastOutSlowInEasing,
            placementSpec = spring<IntOffset>(
                stiffness = 800f,
                dampingRatio = Spring.DampingRatioNoBouncy
            )
        )
    ),

    /** 沉浸：最完整的过渡，接近系统动效节奏。 */
    CINEMATIC(
        key = "cinematic",
        label = "沉浸",
        description = "最完整的过渡，接近系统动效节奏",
        spec = MotionSpec(
            micro = 160,
            fast = 210,
            medium = 300,
            easing = FastOutSlowInEasing,
            placementSpec = spring<IntOffset>(
                stiffness = 400f,
                dampingRatio = Spring.DampingRatioNoBouncy
            )
        )
    );

    companion object {
        /** 持久化值解析；未知或缺失一律回落到默认的极速档（冷启动默认值）。 */
        fun fromKey(key: String?): MotionLevel = entries.firstOrNull { it.key == key } ?: FASTEST
    }
}

/** 一档动效的完整规格：时长、缓动与列表项位置弹簧。 */
data class MotionSpec(
    /** 微反馈与退场。 */
    val micro: Int,
    /** 状态反馈（如「✓ 已保存」）。 */
    val fast: Int,
    /** 页面级与主题级过渡。 */
    val medium: Int,
    /** 标准缓动：起步快、收尾缓，感知上比线性更轻快。 */
    val easing: Easing,
    /** 列表项位置变化（LazyColumn animateItem）：无回弹。 */
    val placementSpec: FiniteAnimationSpec<IntOffset>
)

/**
 * 当前档位。冷启动默认极速档（[MotionLevel.FASTEST]），首帧零额外计算；
 * 用户在设置中切换后，由主题入口的 Provider 注入新值，所有动画调用点自动跟随。
 */
val LocalMotionLevel = staticCompositionLocalOf { MotionLevel.FASTEST }

/** 读取当前档位的动效规格。 */
@Composable
fun motionSpec(): MotionSpec = LocalMotionLevel.current.spec

/**
 * 兼容层：极速档的时长与缓动常量，供既有调用点与单元测试使用。
 * 数值与 [MotionLevel.FASTEST] 的规格同源（getter 读取），不维护第二套事实来源；
 * 新代码请直接使用 [motionSpec]。
 */
object MotionTokens {
    private val spec: MotionSpec get() = MotionLevel.FASTEST.spec

    val DurationMicro: Int get() = spec.micro
    val DurationFast: Int get() = spec.fast
    val DurationMedium: Int get() = spec.medium
    val Easing: Easing get() = spec.easing
}
