package com.example.anjiannotes.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PlainTextBackupCodecTest {
    @Test
    fun plainTextBackup_roundTripsFoldersAndNotes() {
        val snapshot = BackupSnapshot(
            folders = listOf(
                FolderEntity(id = 1, name = "默认收藏夹", createdAt = 0, sortOrder = 0),
                FolderEntity(id = 2, name = "工作", createdAt = 1, sortOrder = 1)
            ),
            notes = listOf(
                NoteEntity(
                    id = 7,
                    title = "会议记录",
                    content = "第一行\n第二行",
                    isTopPinned = true,
                    isMarkdown = true,
                    folderId = 2
                )
            )
        )

        val restored = PlainTextBackupCodec.decode(PlainTextBackupCodec.encode(snapshot))

        assertEquals(snapshot.folders.map { it.name }, restored.folders.map { it.name })
        assertEquals(snapshot.notes.single().title, restored.notes.single().title)
        assertEquals(snapshot.notes.single().content, restored.notes.single().content)
        assertEquals(snapshot.notes.single().folderId, restored.notes.single().folderId)
        assertEquals(snapshot.notes.single().isTopPinned, restored.notes.single().isTopPinned)
    }
}
