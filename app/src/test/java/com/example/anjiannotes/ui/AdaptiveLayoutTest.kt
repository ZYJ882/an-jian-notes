package com.example.anjiannotes.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 布局分档契约：内边距与内容宽度封顶随窗口宽度分类逐档变化，
 * 判定必须是确定且单调的（分类上移时分档值不放宽倒退）。
 */
class AdaptiveLayoutTest {

    @Test
    fun horizontalPadding_tiersFollowWidthClass() {
        assertEquals(12.dp, horizontalPaddingFor(WindowWidthClass.Watch))
        assertEquals(20.dp, horizontalPaddingFor(WindowWidthClass.Compact))
        assertEquals(24.dp, horizontalPaddingFor(WindowWidthClass.Medium))
        assertEquals(28.dp, horizontalPaddingFor(WindowWidthClass.Expanded))
    }

    @Test
    fun horizontalPadding_isMonotonic() {
        // 12 < 20 < 24 < 28：分类越宽，内边距越宽松，不允许反复。
        val values = WindowWidthClass.entries.map { horizontalPaddingFor(it) }
        values.zipWithNext { a, b -> assertTrue("$a 不得大于 $b", a <= b) }
    }

    @Test
    fun contentMaxWidth_uncappedOnPhonesAndWatches() {
        // 手机竖屏与手表不封顶：内容天然铺满窗口，contentWidthCap 恒等。
        assertEquals(Dp.Infinity, contentMaxWidthFor(WindowWidthClass.Watch))
        assertEquals(Dp.Infinity, contentMaxWidthFor(WindowWidthClass.Compact))
    }

    @Test
    fun contentMaxWidth_cappedOnMediumAndExpanded() {
        // 宽屏封顶居中：阅读行长保持 60–75 个中文字符。
        assertEquals(640.dp, contentMaxWidthFor(WindowWidthClass.Medium))
        assertEquals(720.dp, contentMaxWidthFor(WindowWidthClass.Expanded))
    }

    @Test
    fun contentMaxWidth_capIsMonotonicAndFinite() {
        val values = WindowWidthClass.entries.map { contentMaxWidthFor(it) }
        assertTrue("Watch/Compact 应保持不封顶", values[0] == Dp.Infinity && values[1] == Dp.Infinity)
        assertTrue("Medium/Expanded 封顶值应保持递增", values[2] <= values[3])
        assertTrue("Medium 档封顶值必须有限", contentMaxWidthFor(WindowWidthClass.Medium).value.isFinite())
        assertTrue("Expanded 档封顶值必须有限", contentMaxWidthFor(WindowWidthClass.Expanded).value.isFinite())
    }

    @Test
    fun drawerMaxWidth_uncappedOnPhonesAndWatches() {
        // 手机竖屏（0.76×411≈312dp）与手表（0.76×240≈182dp）比例合理，保持 76% 填充。
        assertEquals(Dp.Infinity, drawerMaxWidthFor(WindowWidthClass.Watch))
        assertEquals(Dp.Infinity, drawerMaxWidthFor(WindowWidthClass.Compact))
    }

    @Test
    fun drawerMaxWidth_cappedOnWideScreens() {
        // 宽屏封顶 360dp（Material3 规范值）：否则 1280dp 平板会把抽屉拉到 973dp。
        assertEquals(360.dp, drawerMaxWidthFor(WindowWidthClass.Medium))
        assertEquals(360.dp, drawerMaxWidthFor(WindowWidthClass.Expanded))
    }

    @Test
    fun drawerMaxWidth_isMonotonic() {
        val values = WindowWidthClass.entries.map { drawerMaxWidthFor(it) }
        assertTrue("Watch/Compact 应保持不封顶", values[0] == Dp.Infinity && values[1] == Dp.Infinity)
        assertEquals("Medium/Expanded 应保持相同的抽屉封顶值", values[2], values[3])
    }

    @Test
    fun dialogAction_defaults() {
        // 对话框动作描述默认值契约：默认可用、非强调、非破坏性。
        val action = DialogAction(label = "确定", onClick = {})
        assertTrue(action.enabled)
        assertTrue(!action.emphasized)
        assertTrue(!action.destructive)
        assertEquals("确定", action.label)
    }
}
