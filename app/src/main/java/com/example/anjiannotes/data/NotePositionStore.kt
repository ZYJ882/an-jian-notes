package com.example.anjiannotes.data

import android.content.Context

/**
 * 保存已落库笔记的本地阅读位置与编辑光标范围。
 *
 * 位置数据是界面偏好而非笔记内容，因此不进入 Room、备份或 WebDAV；每条笔记独立保存，
 * 并且只写入轻量整数值，不会影响正文保存队列。
 */
data class NotePosition(
    val readingScrollPx: Int = 0,
    val selectionStart: Int = 0,
    val selectionEnd: Int = 0
)

class NotePositionStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(noteId: Long): NotePosition {
        if (noteId <= 0L) return NotePosition()
        return NotePosition(
            readingScrollPx = preferences.getInt(readingKey(noteId), 0).coerceAtLeast(0),
            selectionStart = preferences.getInt(selectionStartKey(noteId), 0).coerceAtLeast(0),
            selectionEnd = preferences.getInt(selectionEndKey(noteId), 0).coerceAtLeast(0)
        )
    }

    fun saveReadingScroll(noteId: Long, scrollPx: Int) {
        if (noteId <= 0L) return
        preferences.edit().putInt(readingKey(noteId), scrollPx.coerceAtLeast(0)).apply()
    }

    fun saveSelection(noteId: Long, start: Int, end: Int) {
        if (noteId <= 0L) return
        preferences.edit()
            .putInt(selectionStartKey(noteId), start.coerceAtLeast(0))
            .putInt(selectionEndKey(noteId), end.coerceAtLeast(0))
            .apply()
    }

    fun remove(noteId: Long) {
        if (noteId <= 0L) return
        preferences.edit()
            .remove(readingKey(noteId))
            .remove(selectionStartKey(noteId))
            .remove(selectionEndKey(noteId))
            .apply()
    }

    private fun readingKey(noteId: Long): String = "reading.$noteId"
    private fun selectionStartKey(noteId: Long): String = "selection_start.$noteId"
    private fun selectionEndKey(noteId: Long): String = "selection_end.$noteId"

    private companion object {
        const val PREFERENCES_NAME = "note_position_preferences"
    }
}
