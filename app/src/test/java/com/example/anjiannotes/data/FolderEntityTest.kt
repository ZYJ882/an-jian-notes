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
        assertEquals(DEFAULT_FOLDER_ID, FolderEntity(id = DEFAULT_FOLDER_ID, name = "默认收藏夹").id)
    }

    @Test
    fun starredFolder_usesVirtualProtectedId() {
        assertEquals(STARRED_FOLDER_ID, STARRED_FOLDER.id)
        assertEquals("星标笔记", STARRED_FOLDER.name)
    }
}
