package com.example.anjiannotes.data

import android.content.Context

/**
 * 列表排序偏好：
 * - 笔记列表按“最近打开”置顶（关闭时按最近修改排序，保持原有默认）。
 * - 收藏夹列表不设开关，始终按手动基准顺序（sortOrder）排序。
 */
class AppSortingPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun loadNoteListByRecentOpen(): Boolean = preferences.getBoolean(KEY_NOTE_BY_OPEN, false)

    fun saveNoteListByRecentOpen(value: Boolean) {
        preferences.edit().putBoolean(KEY_NOTE_BY_OPEN, value).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "app_sorting_preferences"
        const val KEY_NOTE_BY_OPEN = "note_list_sort_by_recent_open"
    }
}
