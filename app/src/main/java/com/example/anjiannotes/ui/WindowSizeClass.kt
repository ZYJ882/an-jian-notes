package com.example.anjiannotes.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

/**
 * 窗口宽度分类断点（dp）。
 *
 * 宽度三档参照 Material3 窗口尺寸类（WindowSizeClass）的规范断点；
 * 另设 [WatchDp] 一档覆盖手表等超窄窗口——标准 Compact 档（<600dp）把手机与
 * 手表一视同仁，但手表需要更激进的紧凑策略（全屏对话框、12dp 内边距、
 * 更小字号），单独分档才能同时满足两者。
 */
object WindowWidthBreakpoints {
    /** 小于该宽度视为手表/超窄窗口（Watch 档）。 */
    const val WatchDp: Int = 320

    /** 小于该宽度为手机竖屏（Compact 档）。 */
    const val MediumDp: Int = 600

    /** 小于该宽度为手机横屏/折叠屏展开/分屏主区（Medium 档），及以上为平板（Expanded 档）。 */
    const val ExpandedDp: Int = 840
}

/** 窗口宽度分类。序数即从小到大顺序，可直接比较大小。 */
enum class WindowWidthClass {
    /** 超窄窗口：手表整机、极端分屏小窗。 */
    Watch,

    /** 常规竖屏手机（含最小宽度机型）。 */
    Compact,

    /** 中等宽度：手机横屏、折叠屏展开、分屏主操作区、小平板。 */
    Medium,

    /** 大宽度：平板、外接显示器、桌面模式。 */
    Expanded;

    companion object {
        /** 分类纯函数（可单元测试）：窗口宽度小于下一档断点即落入当前档。 */
        fun of(widthDp: Int): WindowWidthClass = when {
            widthDp < WindowWidthBreakpoints.WatchDp -> Watch
            widthDp < WindowWidthBreakpoints.MediumDp -> Compact
            widthDp < WindowWidthBreakpoints.ExpandedDp -> Medium
            else -> Expanded
        }
    }
}

/** 窗口高度分类断点（dp），同样参照 Material3 窗口尺寸类。 */
object WindowHeightBreakpoints {
    /** 小于该高度为紧凑高度：手机横屏、手表。 */
    const val CompactDp: Int = 480

    /** 小于该高度为中等高度，及以上为展开高度：平板竖屏、桌面模式。 */
    const val MediumDp: Int = 900
}

/** 窗口高度分类。序数即从小到大顺序，可直接比较大小。 */
enum class WindowHeightClass {
    /** 紧凑高度：手机横屏、手表，纵向空间紧张。 */
    Compact,

    /** 中等高度：常规手机竖屏、小平板横屏。 */
    Medium,

    /** 展开高度：平板竖屏、桌面模式。 */
    Expanded;

    companion object {
        /** 分类纯函数（可单元测试）。 */
        fun of(heightDp: Int): WindowHeightClass = when {
            heightDp < WindowHeightBreakpoints.CompactDp -> Compact
            heightDp < WindowHeightBreakpoints.MediumDp -> Medium
            else -> Expanded
        }
    }
}

/**
 * 当前窗口的尺寸分类。
 *
 * 判定依据是「当前窗口」而非设备型号：手表整机、手机分屏、小窗、折叠屏
 * 展开与旋转都会得到新的分类，布局随窗口尺寸切换而非随设备型号写死。
 */
data class WindowSizeClass(
    val widthClass: WindowWidthClass,
    val heightClass: WindowHeightClass
) {
    /** 手表/超窄窗口：需要最激进的紧凑策略。 */
    val isWatch: Boolean get() = widthClass == WindowWidthClass.Watch

    /** 常规手机竖屏（含手表以外的所有 Compact 宽度）。 */
    val isCompact: Boolean get() = widthClass == WindowWidthClass.Compact

    /** 宽度达到 Medium 及以上：平板/折叠屏/横屏，可启用宽屏布局。 */
    val isAtLeastMedium: Boolean get() = widthClass >= WindowWidthClass.Medium

    /** 宽度达到 Expanded：大屏，内容需要限宽以保证可读性。 */
    val isExpanded: Boolean get() = widthClass == WindowWidthClass.Expanded
}

/**
 * 当前窗口的尺寸分类（响应式）。
 *
 * 监听 [LocalConfiguration]：分屏调整、旋转、折叠屏展开、小窗出入时
 * 分类与依赖它的布局会自动切换，无需手动刷新。
 */
@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp
    val heightDp = configuration.screenHeightDp
    return remember(widthDp, heightDp) {
        WindowSizeClass(
            widthClass = WindowWidthClass.of(widthDp),
            heightClass = WindowHeightClass.of(heightDp)
        )
    }
}
