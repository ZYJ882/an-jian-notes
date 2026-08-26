package com.example.anjiannotes.data

import android.content.Context

/** 保存用户上次浏览的收藏夹；首次启动仍使用默认收藏夹。 */
class FolderSelectionPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): Long = preferences.getLong(KEY_FOLDER_ID, DEFAULT_FOLDER_ID)

    fun save(folderId: Long) {
        preferences.edit().putLong(KEY_FOLDER_ID, folderId).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "folder_selection_preferences"
        const val KEY_FOLDER_ID = "last_folder_id"
    }
}
