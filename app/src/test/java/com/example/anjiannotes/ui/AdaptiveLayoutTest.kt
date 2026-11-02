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

    @Test
    fun twoPane_onlyOnExpanded() {
        // 仅 Expanded（≥840dp）分栏。Medium（600–839dp）刻意不分栏：
        // 400dp 列表 + 剩余 ~200–440dp 详情会互相挤压，维持单栏封顶居中更稳妥。
        assertTrue(twoPaneFor(WindowWidthClass.Expanded))
        assertTrue(!twoPaneFor(WindowWidthClass.Watch))
        assertTrue(!twoPaneFor(WindowWidthClass.Compact))
        assertTrue(!twoPaneFor(WindowWidthClass.Medium))
    }

    @Test
    fun twoPane_breakpointAlignedWithWidthClassOf() {
        // 分栏开关与宽度分类必须逐 dp 对齐：of(widthDp) 为 Expanded 当且仅当分栏。
        // 扫描 0–1600dp（覆盖手表 ~200dp 到超大平板 ~1600dp）。
        for (widthDp in 0..1600) {
            val expected = WindowWidthClass.of(widthDp) == WindowWidthClass.Expanded
            assertEquals("widthDp=$widthDp 分栏判定与分类不一致", expected, twoPaneFor(WindowWidthClass.of(widthDp)))
        }
    }

    @Test
    fun listPaneWidth_fixedOnlyWhenTwoPane() {
        assertEquals(400.dp, listPaneWidthFor(WindowWidthClass.Expanded))
        // 非双栏档无列表 pane，宽度无意义：Unspecified 让调用方按单栏路径忽略它。
        for (widthClass in listOf(WindowWidthClass.Watch, WindowWidthClass.Compact, WindowWidthClass.Medium)) {
            assertEquals(Dp.Unspecified, listPaneWidthFor(widthClass))
        }
    }

    @Test
    fun listPaneWidth_leavesUsableDetailPaneOnNarrowestExpanded() {
        // 最窄 Expanded 窗口 840dp：400dp 列表 + 440dp 详情。不变式：
        // ① 详情 pane ≥ 400dp（可读正文区）；② 列表 + 详情不超出窗口（不裁切）。
        val minExpanded = WindowWidthBreakpoints.ExpandedDp
        val detailPane = minExpanded - listPaneWidthFor(WindowWidthClass.Expanded).value.toInt()
        assertTrue("最窄 Expanded 下详情 pane 仅 ${detailPane}dp，小于 400dp 下限", detailPane >= 400)
        assertTrue(
            "列表与详情总宽超出最窄 Expanded 窗口",
            listPaneWidthFor(WindowWidthClass.Expanded).value.toInt() + 400 <= minExpanded
        )
    }

    @Test
    fun twoPaneDetail_neverUsesContentCapInPractice() {
        // 双栏下详情 pane 宽度（440–1200dp）通常小于 Expanded 档 720dp 行长封顶，
        // 即 widthIn(max=720) 在双栏详情上是恒等变换——封顶只保护单栏宽屏。
        // 此契约记录该事实，防止将来误调 listPaneWidthFor 造成双栏行长失控。
        val detailMin = WindowWidthBreakpoints.ExpandedDp - listPaneWidthFor(WindowWidthClass.Expanded).value.toInt()
        assertTrue(
            "双栏最窄详情 pane 已超过 720dp 封顶值，contentWidthCap 语义需要重新评估",
            detailMin < contentMaxWidthFor(WindowWidthClass.Expanded).value.toInt()
        )
    }
}
