package com.example.anjiannotes.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * 滚动条滑块的几何契约。
 *
 * 收口前，「滑块高度受视口封顶 → 按滚动比例定位」这套计算在三个 Canvas
 * （原生编辑器滑条、详情预览滑条、笔记列表滑条）里各写了一遍，参数名与
 * 中间变量略有差异但公式完全相同。此处把它固化为唯一实现，三处只负责
 * 提供颜色与滚动比例。
 *
 * 规则（与收口前逐点等价，不改变任何可感知行为）：
 * 1. 高度 = min(thumbHeight, viewportHeight)，滑块永不超出视口；
 * 2. 比例钳制在 [0, 1]，越界的输入不会把滑块画到视口之外；
 * 3. top = (viewportHeight - 高度) × 比例，起点贴顶、终点贴底。
 */
data class ScrollbarThumbGeometry(val top: Float, val height: Float)

internal fun scrollbarThumbGeometry(
    viewportHeight: Float,
    thumbHeight: Float,
    scrollFraction: Float
): ScrollbarThumbGeometry {
    val clampedHeight = thumbHeight.coerceAtMost(viewportHeight)
    val clampedFraction = scrollFraction.coerceIn(0f, 1f)
    return ScrollbarThumbGeometry(
        top = (viewportHeight - clampedHeight) * clampedFraction,
        height = clampedHeight
    )
}

/**
 * 三处滚动条共用的滑块绘制。颜色由调用方决定：原生编辑器拖动态用较高
 * alpha、详情预览用固定 alpha、列表滑条用实色并交给外层 graphicsLayer 淡出。
 */
internal fun DrawScope.drawScrollbarThumb(
    color: Color,
    thumbWidth: Float,
    geometry: ScrollbarThumbGeometry
) {
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width - thumbWidth, geometry.top),
        size = Size(thumbWidth, geometry.height),
        cornerRadius = CornerRadius(thumbWidth, thumbWidth)
    )
}
