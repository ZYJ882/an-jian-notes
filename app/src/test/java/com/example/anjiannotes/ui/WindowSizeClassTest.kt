package com.example.anjiannotes.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 窗口尺寸分类契约。
 *
 * 断点取自 Material3 窗口尺寸类（WindowSizeClass）规范值，另设 Watch 档
 * 覆盖手表等超窄窗口。分类必须确定、完备且单调：宽度从小到大只能逐档
 * 上移，不允许跳档或反复。
 */
class WindowSizeClassTest {

    // ---------- 宽度分类 ----------

    @Test
    fun widthClass_watchTier() {
        // 常见手表窗口：圆形约 180–192dp，方形约 204–240dp；分屏小窗可低至 300dp。
        assertEquals(WindowWidthClass.Watch, WindowWidthClass.of(0))
        assertEquals(WindowWidthClass.Watch, WindowWidthClass.of(180))
        assertEquals(WindowWidthClass.Watch, WindowWidthClass.of(240))
        assertEquals(WindowWidthClass.Watch, WindowWidthClass.of(300))
        assertEquals(WindowWidthClass.Watch, WindowWidthClass.of(319))
    }

    @Test
    fun widthClass_compactTier() {
        // 常规手机竖屏：最小宽度机型 320dp，主流 360–412dp。
        assertEquals(WindowWidthClass.Compact, WindowWidthClass.of(320))
        assertEquals(WindowWidthClass.Compact, WindowWidthClass.of(360))
        assertEquals(WindowWidthClass.Compact, WindowWidthClass.of(411))
        assertEquals(WindowWidthClass.Compact, WindowWidthClass.of(599))
    }

    @Test
    fun widthClass_mediumTier() {
        // 手机横屏、折叠屏展开、分屏主区、小平板横屏。
        assertEquals(WindowWidthClass.Medium, WindowWidthClass.of(600))
        assertEquals(WindowWidthClass.Medium, WindowWidthClass.of(720))
        assertEquals(WindowWidthClass.Medium, WindowWidthClass.of(839))
    }

    @Test
    fun widthClass_expandedTier() {
        // 平板竖屏（800dp 级）、外接显示器、桌面模式。
        assertEquals(WindowWidthClass.Expanded, WindowWidthClass.of(840))
        assertEquals(WindowWidthClass.Expanded, WindowWidthClass.of(1280))
        assertEquals(WindowWidthClass.Expanded, WindowWidthClass.of(2560))
    }

    @Test
    fun widthClass_isMonotonicAcrossFullRange() {
        // 0..1600dp 逐点扫描：分类档位序数只能不减，且四档都必须出现。
        var previous = WindowWidthClass.of(0)
        val seen = mutableSetOf(previous)
        for (width in 1..1600) {
            val current = WindowWidthClass.of(width)
            assertTrue(
                "宽度 $width 分类 $current 不得低于 $previous（分类必须单调）",
                current >= previous
            )
            previous = current
            seen += current
        }
        assertEquals(
            "扫描区间内必须覆盖全部四档",
            setOf(
                WindowWidthClass.Watch,
                WindowWidthClass.Compact,
                WindowWidthClass.Medium,
                WindowWidthClass.Expanded
            ),
            seen
        )
    }

    // ---------- 方形小窗（手表整机）修正 ----------

    @Test
    fun squareSmallWindow_detectedOnSquareWatchShapes() {
        // 方形手表：宽高相等或差在容差内（圆屏带下巴）；短边处于紧凑高度档（< 480dp）。
        assertTrue(isSquareSmallWindow(320, 320)) // 480×480px @ hdpi 的方形手表，恰为 320dp
        assertTrue(isSquareSmallWindow(298, 298)) // 实测约 298dp 的方形手表
        assertTrue(isSquareSmallWindow(320, 340)) // 圆屏带下巴：宽高差 20dp 在容差内
        assertTrue(isSquareSmallWindow(240, 260))
        assertTrue(isSquareSmallWindow(204, 204))
    }

    @Test
    fun squareSmallWindow_rejectedOnRectangularWindows() {
        // 手机竖屏/横屏、旧比例机型、折叠屏：长宽差远超容差。
        assertFalse(isSquareSmallWindow(360, 640))
        assertFalse(isSquareSmallWindow(640, 360))
        assertFalse(isSquareSmallWindow(411, 731))
        assertFalse(isSquareSmallWindow(320, 480)) // 2:3 旧机竖屏
        assertFalse(isSquareSmallWindow(480, 320))
    }

    @Test
    fun squareSmallWindow_rejectedOnLargeSquareWindows() {
        // 短边 ≥ 480dp 的方形窗口（平板方形分屏、桌面方形窗口）不是手表，保持原档位。
        assertFalse(isSquareSmallWindow(480, 480))
        assertFalse(isSquareSmallWindow(600, 600))
        assertFalse(isSquareSmallWindow(1280, 1280))
    }

    @Test
    fun effectiveWidthClass_squareWatchOverridesWidthTier() {
        // 宽度达到 Compact 下沿（320dp）的方形手表修正为 Watch 档。
        assertEquals(WindowWidthClass.Watch, effectiveWidthClass(320, 320))
        assertEquals(WindowWidthClass.Watch, effectiveWidthClass(330, 340))
        assertEquals(WindowWidthClass.Watch, effectiveWidthClass(300, 300))
        // 近方形但宽高差超容差 → 仍按宽度断点分类。
        assertEquals(WindowWidthClass.Compact, effectiveWidthClass(320, 400))
        assertEquals(WindowWidthClass.Compact, effectiveWidthClass(360, 640))
        // 大方形窗口不受形状规则影响。
        assertEquals(WindowWidthClass.Compact, effectiveWidthClass(480, 480))
        assertEquals(WindowWidthClass.Medium, effectiveWidthClass(600, 600))
    }

    // ---------- 高度分类 ----------

    @Test
    fun heightClass_compactTier() {
        // 手机横屏窗口高度（约 360–411dp）与手表。
        assertEquals(WindowHeightClass.Compact, WindowHeightClass.of(0))
        assertEquals(WindowHeightClass.Compact, WindowHeightClass.of(400))
        assertEquals(WindowHeightClass.Compact, WindowHeightClass.of(479))
    }

    @Test
    fun heightClass_mediumTier() {
        // 常规手机竖屏（约 800–915dp）与小平板横屏。
        assertEquals(WindowHeightClass.Medium, WindowHeightClass.of(480))
        assertEquals(WindowHeightClass.Medium, WindowHeightClass.of(800))
        assertEquals(WindowHeightClass.Medium, WindowHeightClass.of(899))
    }

    @Test
    fun heightClass_expandedTier() {
        // 平板竖屏与桌面模式。
        assertEquals(WindowHeightClass.Expanded, WindowHeightClass.of(900))
        assertEquals(WindowHeightClass.Expanded, WindowHeightClass.of(1200))
    }

    @Test
    fun heightClass_isMonotonicAcrossFullRange() {
        var previous = WindowHeightClass.of(0)
        for (height in 1..1600) {
            val current = WindowHeightClass.of(height)
            assertTrue("高度 $height 分类必须单调", current >= previous)
            previous = current
        }
    }

    // ---------- 组合判定 ----------

    @Test
    fun windowSizeClass_derivedFlags() {
        val watch = WindowSizeClass(WindowWidthClass.Watch, WindowHeightClass.Medium)
        assertTrue(watch.isWatch)
        assertFalse(watch.isAtLeastMedium)
        assertFalse(watch.isExpanded)

        val phone = WindowSizeClass(WindowWidthClass.Compact, WindowHeightClass.Medium)
        assertFalse(phone.isWatch)
        assertFalse(phone.isAtLeastMedium)
        assertFalse(phone.isExpanded)

        val foldable = WindowSizeClass(WindowWidthClass.Medium, WindowHeightClass.Medium)
        assertFalse(foldable.isWatch)
        assertTrue(foldable.isAtLeastMedium)
        assertFalse(foldable.isExpanded)

        val tablet = WindowSizeClass(WindowWidthClass.Expanded, WindowHeightClass.Expanded)
        assertFalse(tablet.isWatch)
        assertTrue(tablet.isAtLeastMedium)
        assertTrue(tablet.isExpanded)
    }

    @Test
    fun watchBreakpoints_matchMaterial3PlusWatchTier() {
        // Medium/Expanded 断点必须与 Material3 规范值一致；Watch 档为应用自有的更细档位。
        assertEquals(600, WindowWidthBreakpoints.MediumDp)
        assertEquals(840, WindowWidthBreakpoints.ExpandedDp)
        assertEquals(480, WindowHeightBreakpoints.CompactDp)
        assertEquals(900, WindowHeightBreakpoints.MediumDp)
        assertTrue(WindowWidthBreakpoints.WatchDp < WindowWidthBreakpoints.MediumDp)
    }
}
