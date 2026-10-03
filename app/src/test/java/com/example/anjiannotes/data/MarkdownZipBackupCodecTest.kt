package com.example.anjiannotes.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MarkdownZipBackupCodecTest {
    @Test(expected = IllegalArgumentException::class)
    fun markdownZip_rejectsPathTraversalEntry() {
        MarkdownZipBackupCodec.decode(zipOf("../evil.md" to "unsafe".toByteArray()))
    }

    @Test(expected = IllegalArgumentException::class)
    fun markdownZip_rejectsOversizedEntry() {
        MarkdownZipBackupCodec.decode(zipOf("notes/1.md" to ByteArray(8 * 1024 * 1024 + 1)))
    }

    @Test
    fun markdownZip_roundTripsFoldersAndNotesWithoutChangingContent() {
        val snapshot = BackupSnapshot(
            folders = listOf(
                FolderEntity(id = 1, name = "默认收藏夹", createdAt = 0, sortOrder = 0),
                FolderEntity(id = 7, name = "项目资料", createdAt = 12, sortOrder = 7)
            ),
            notes = listOf(
                NoteEntity(
                    id = 42,
                    title = "发布计划: 第一阶段",
                    content = "# 计划\n\n- 保留最后换行\n",
                    createdAt = 101,
                    updatedAt = 202,
                    isPinned = true,
                    isTopPinned = true,
                    isMarkdown = true,
                    folderId = 7
                )
            )
        )

        val encoded = MarkdownZipBackupCodec.encode(snapshot, appVersion = "2.3.0")
        val decoded = MarkdownZipBackupCodec.decode(encoded)

        assertEquals(snapshot.folders, decoded.folders)
        assertEquals(snapshot.notes, decoded.notes)
        val markdown = MarkdownZipBackupCodec.encodeNote(snapshot.notes.single())
        assertTrue(markdown.startsWith("---\n"))
        assertTrue(markdown.contains("folderId: 7"))
        assertTrue(markdown.contains("topPinned: true"))
        assertTrue(markdown.endsWith("- 保留最后换行\n"))
    }

    private fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }
}
