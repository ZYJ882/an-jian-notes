package com.example.anjiannotes

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 系统返回层级契约：固化「搜索 > 专注 > 离开」的优先级与设置层级回退目标。
 * 与 NoteDetailExitPolicyTest 同源，作为返回操作逻辑的可回归验证。
 */
class BackNavigationTest {

    @Test
    fun searchVisible_takesHighestPriority() {
        assertEquals(DetailBackAction.CLOSE_SEARCH, resolveDetailBackAction(searchVisible = true, focusMode = false))
        // 搜索态残留时即便同时处于专注阅读，也先消化搜索态。
        assertEquals(DetailBackAction.CLOSE_SEARCH, resolveDetailBackAction(searchVisible = true, focusMode = true))
    }

    @Test
    fun focusMode_onlyWhenSearchHidden() {
        assertEquals(DetailBackAction.EXIT_FOCUS, resolveDetailBackAction(searchVisible = false, focusMode = true))
    }

    @Test
    fun noOverlay_leavesDetail() {
        assertEquals(DetailBackAction.LEAVE, resolveDetailBackAction(searchVisible = false, focusMode = false))
    }

    @Test
    fun settingsHierarchy_stepsBackOneLevel() {
        assertEquals(AppPage.Settings, settingsBackTarget(AppPage.Appearance))
        assertEquals(AppPage.Settings, settingsBackTarget(AppPage.Motion))
        assertEquals(AppPage.List, settingsBackTarget(AppPage.Settings))
    }

    @Test
    fun settingsHierarchy_nonSettingsPagesFallBackToList() {
        assertEquals(AppPage.List, settingsBackTarget(AppPage.List))
        assertEquals(AppPage.List, settingsBackTarget(AppPage.Detail(note = null)))
    }
}
