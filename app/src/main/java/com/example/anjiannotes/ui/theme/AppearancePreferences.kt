package com.example.anjiannotes.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppearanceMode(val key: String, val label: String) {
    LIGHT("light", "浅色"),
    DARK("dark", "深色"),
    SYSTEM("system", "跟随系统");

    companion object {
        fun fromKey(key: String?): AppearanceMode = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

/** 轻量持久化外观偏好；默认跟随系统，写入后立即驱动 Compose 主题重组。 */
class AppearancePreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("appearance_preferences", Context.MODE_PRIVATE)
    private val mutableMode = MutableStateFlow(AppearanceMode.fromKey(preferences.getString(KEY_MODE, null)))

    val mode: StateFlow<AppearanceMode> = mutableMode.asStateFlow()

    fun setMode(value: AppearanceMode) {
        preferences.edit().putString(KEY_MODE, value.key).apply()
        mutableMode.value = value
    }

    private companion object {
        const val KEY_MODE = "theme_mode"
    }
}
