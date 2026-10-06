package com.example.anjiannotes

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 新建草稿离开详情页的策略：
 * - 有有效内容的新建页必须在离开前强制写入一次（沿用单一保存队列）；
 * - 空草稿（标题与正文均无有效字符）一律不落库，避免列表出现“未命名空笔记”。
 */
class NoteDetailExitPolicyTest {
    @Test
    fun emptyNewNote_skipsFinalSaveAndLeavesNoRecord() {
        assertFalse(shouldForceFinalDraftSave(isNewNote = true, savedNoteId = 0L, draftTitle = "", draftContent = ""))
        assertTrue(isNewDraftEmpty(draftTitle = "", draftContent = ""))
    }

    @Test
    fun whitespaceOnlyNewNote_countsAsEmpty() {
        assertTrue(isNewDraftEmpty(draftTitle = "  ", draftContent = " \n\t"))
        assertFalse(
            shouldForceFinalDraftSave(isNewNote = true, savedNoteId = 0L, draftTitle = " ", draftContent = " \n")
        )
    }

    @Test
    fun newNoteWithPendingInput_stillForcesFinalSave() {
        assertTrue(shouldForceFinalDraftSave(isNewNote = true, savedNoteId = 0L, draftTitle = "", draftContent = "大家"))
        assertTrue(shouldForceFinalDraftSave(isNewNote = true, savedNoteId = 0L, draftTitle = "标题", draftContent = ""))
    }

    @Test
    fun existingNote_doesNotCreateAnExtraForcedSave() {
        assertFalse(shouldForceFinalDraftSave(isNewNote = false, savedNoteId = 42L, draftTitle = "a", draftContent = "b"))
    }

    @Test
    fun alreadyPersistedNewNote_doesNotCreateANewBlankRecord() {
        assertFalse(shouldForceFinalDraftSave(isNewNote = true, savedNoteId = 42L, draftTitle = "", draftContent = ""))
    }

    @Test
    fun nonBlankContentDetectionIsCharacterAware() {
        assertFalse(isNewDraftEmpty(draftTitle = "", draftContent = "a"))
        assertFalse(isNewDraftEmpty(draftTitle = "记", draftContent = ""))
    }
}
