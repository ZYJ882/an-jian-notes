package com.example.anjiannotes.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NoteFont(val key: String, val label: String) {
    DEFAULT("default", "默认字体"),
    SONG("song", "宋体"),
    CUSTOM("custom", "自定义字体");

    companion object {
        fun fromKey(key: String?): NoteFont = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

class FontPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("font_preferences", Context.MODE_PRIVATE)
    private val mutableFont = MutableStateFlow(NoteFont.fromKey(preferences.getString(KEY_FONT, null)))
    private val mutableCustomPath = MutableStateFlow(preferences.getString(KEY_CUSTOM_PATH, null))

    val font: StateFlow<NoteFont> = mutableFont.asStateFlow()
    val customPath: StateFlow<String?> = mutableCustomPath.asStateFlow()

    fun setFont(value: NoteFont) {
        preferences.edit().putString(KEY_FONT, value.key).apply()
        mutableFont.value = value
    }

    fun setCustomPath(path: String) {
        preferences.edit().putString(KEY_CUSTOM_PATH, path).apply()
        mutableCustomPath.value = path
        setFont(NoteFont.CUSTOM)
    }

    private companion object {
        const val KEY_FONT = "note_font"
        const val KEY_CUSTOM_PATH = "custom_font_path"
    }
}
