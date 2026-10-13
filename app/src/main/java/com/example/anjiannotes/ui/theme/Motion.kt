package com.example.anjiannotes.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Easing

/**
 * 全局动效规范：全部短时长、无回弹（dampingRatio = 1 的曲线），
 * 只做 alpha / scale 等绘制层变换，不使用触发重新测量的布局动画。
 *
 * 取值原则（刻意保持克制）：
 * - 80ms：微反馈，按压感与指示器退场，几乎不可察觉但不生硬；
 * - 100ms：状态指示（如「✓ 已保存」），跟手、不拖延输入节奏；
 * - 140ms：页面级与主题级过渡，短到不让人觉得「在等动画」。
 */
object MotionTokens {
    const val DurationMicro: Int = 80
    const val DurationFast: Int = 100
    const val DurationMedium: Int = 140

    /** 标准缓动：起步快、收尾缓，感知上比线性更轻快。 */
    val Easing: Easing = FastOutSlowInEasing
}
