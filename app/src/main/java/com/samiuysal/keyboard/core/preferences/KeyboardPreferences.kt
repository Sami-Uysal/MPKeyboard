package com.samiuysal.keyboard.core.preferences

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyboardPreferences @Inject constructor(context: Context) {

    private val prefs: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "keyboard_prefs"

        private const val KEY_BG_COLOR = "bg_color"
        private const val KEY_KEY_COLOR = "key_color"

        private const val KEY_LANGUAGE = "selected_language"

        private const val KEY_SOUND_ENABLED = "sound_enabled"

        private const val DEFAULT_BG_COLOR = "#000000"
        private const val DEFAULT_KEY_COLOR = "#3A3A3C"
        private const val DEFAULT_LANGUAGE = "tr"
    }

    var backgroundColor: String
        get() = prefs.getString(KEY_BG_COLOR, DEFAULT_BG_COLOR) ?: DEFAULT_BG_COLOR
        set(value) = prefs.edit().putString(KEY_BG_COLOR, value).apply()

    var keyColor: String
        get() = prefs.getString(KEY_KEY_COLOR, DEFAULT_KEY_COLOR) ?: DEFAULT_KEY_COLOR
        set(value) = prefs.edit().putString(KEY_KEY_COLOR, value).apply()

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()
    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND_ENABLED, value).apply()
}
