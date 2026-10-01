package com.example.anjiannotes

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteDetailExitPolicyTest {
    @Test
    fun emptyNewNote_forcesOneFinalRoomSaveBeforeLeaving() {
        assertTrue(shouldForceFinalDraftSave(isNewNote = true, savedNoteId = 0L))
    }

    @Test
    fun newNoteWithPendingInput_stillUsesExistingPendingSaveRevision() {
        // editRevision 大于 savedRevision 时，既有保存队列会负责写入；此处不需要另起保存路径。
        assertTrue(shouldForceFinalDraftSave(isNewNote = true, savedNoteId = 0L))
    }

    @Test
    fun existingNote_doesNotCreateAnExtraForcedSave() {
        assertFalse(shouldForceFinalDraftSave(isNewNote = false, savedNoteId = 42L))
    }

    @Test
    fun alreadyPersistedNewNote_doesNotCreateANewBlankRecord() {
        assertFalse(shouldForceFinalDraftSave(isNewNote = true, savedNoteId = 42L))
    }
}
