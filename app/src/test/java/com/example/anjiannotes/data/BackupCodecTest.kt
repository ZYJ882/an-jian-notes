package com.example.anjiannotes.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {
    @Test
    fun jsonBackup_roundTripsFoldersAndNotesWithoutChangingContent() {
        val snapshot = BackupSnapshot(
            folders = listOf(
                FolderEntity(id = DEFAULT_FOLDER_ID, name = "默认收藏夹", createdAt = 0, sortOrder = 0),
                FolderEntity(id = 6, name = "项目", createdAt = 1_700_000_000_000, sortOrder = 1)
            ),
            notes = listOf(
                NoteEntity(
                    id = 8,
                    title = "会议记录",
                    content = "# 待办\n\n- 确认时间",
                    color = 0xFFF5F0E8,
                    createdAt = 1_700_000_000_000,
                    updatedAt = 1_700_000_001_000,
                    isPinned = true,
                    isMarkdown = true,
                    folderId = 6
                )
            )
        )

        val restored = BackupCodec.decode(BackupCodec.encode(snapshot))

        assertEquals(snapshot.folders, restored.folders)
        assertEquals(snapshot.notes, restored.notes)
        assertTrue(restored.notes.single().isPinned)
        assertFalse(restored.notes.single().content.isBlank())
    }
}
