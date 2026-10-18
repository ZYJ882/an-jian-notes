package com.example.anjiannotes.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 轻量持久化动效档位偏好；默认极速档，写入后立即驱动主题注入点重组。 */
class MotionPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("motion_preferences", Context.MODE_PRIVATE)
    private val mutableLevel = MutableStateFlow(MotionLevel.fromKey(preferences.getString(KEY_LEVEL, null)))

    val level: StateFlow<MotionLevel> = mutableLevel.asStateFlow()

    fun setLevel(value: MotionLevel) {
        preferences.edit().putString(KEY_LEVEL, value.key).apply()
        mutableLevel.value = value
    }

    private companion object {
        const val KEY_LEVEL = "motion_level"
    }
}
