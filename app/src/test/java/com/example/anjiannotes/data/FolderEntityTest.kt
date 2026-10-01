package com.example.anjiannotes.data

import org.junit.Assert.assertEquals
import org.junit.Test

class FolderEntityTest {
    @Test
    fun newFolder_usesZeroIdForDatabaseAutoGeneration() {
        assertEquals(0L, FolderEntity(name = "工作").id)
    }

    @Test
    fun defaultFolder_keepsDedicatedStableId() {
        assertEquals(DEFAULT_FOLDER_ID, DEFAULT_FOLDER.id)
        assertEquals("默认收藏夹", DEFAULT_FOLDER.name)
        assertEquals(0L, DEFAULT_FOLDER.createdAt)
        assertEquals(0L, DEFAULT_FOLDER.sortOrder)
    }

    @Test
    fun starredFolder_usesVirtualProtectedId() {
        assertEquals(STARRED_FOLDER_ID, STARRED_FOLDER.id)
        assertEquals("星标笔记", STARRED_FOLDER.name)
    }
}
