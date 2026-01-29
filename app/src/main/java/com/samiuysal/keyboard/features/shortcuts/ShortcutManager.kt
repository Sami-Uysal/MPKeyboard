package com.samiuysal.keyboard.features.shortcuts

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

class ShortcutManager(context: Context) {

    private val prefs: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    data class Shortcut(val trigger: String, val replacement: String)

    fun getShortcuts(): List<Shortcut> {
        val json = prefs.getString(KEY_SHORTCUTS, "{}") ?: "{}"
        val shortcuts = mutableListOf<Shortcut>()

        try {
            val jsonObject = JSONObject(json)
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val trigger = keys.next()
                val replacement = jsonObject.getString(trigger)
                shortcuts.add(Shortcut(trigger, replacement))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return shortcuts.sortedBy { it.trigger.lowercase() }
    }

    fun addShortcut(trigger: String, replacement: String): Boolean {
        if (trigger.isBlank() || replacement.isBlank()) return false

        val normalizedTrigger = trigger.trim().lowercase()
        if (normalizedTrigger.contains(" ")) return false

        val json = prefs.getString(KEY_SHORTCUTS, "{}") ?: "{}"

        try {
            val jsonObject = JSONObject(json)
            jsonObject.put(normalizedTrigger, replacement.trim())
            prefs.edit().putString(KEY_SHORTCUTS, jsonObject.toString()).apply()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun updateShortcut(oldTrigger: String, newTrigger: String, replacement: String): Boolean {
        if (newTrigger.isBlank() || replacement.isBlank()) return false

        val normalizedOldTrigger = oldTrigger.trim().lowercase()
        val normalizedNewTrigger = newTrigger.trim().lowercase()
        if (normalizedNewTrigger.contains(" ")) return false

        val json = prefs.getString(KEY_SHORTCUTS, "{}") ?: "{}"

        try {
            val jsonObject = JSONObject(json)

            if (normalizedOldTrigger != normalizedNewTrigger) {
                jsonObject.remove(normalizedOldTrigger)
            }

            jsonObject.put(normalizedNewTrigger, replacement.trim())
            prefs.edit().putString(KEY_SHORTCUTS, jsonObject.toString()).apply()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun removeShortcut(trigger: String): Boolean {
        val normalizedTrigger = trigger.trim().lowercase()
        val json = prefs.getString(KEY_SHORTCUTS, "{}") ?: "{}"

        try {
            val jsonObject = JSONObject(json)
            if (jsonObject.has(normalizedTrigger)) {
                jsonObject.remove(normalizedTrigger)
                prefs.edit().putString(KEY_SHORTCUTS, jsonObject.toString()).apply()
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    fun getExpansion(trigger: String): String? {
        val normalizedTrigger = trigger.trim().lowercase()
        val json = prefs.getString(KEY_SHORTCUTS, "{}") ?: "{}"

        try {
            val jsonObject = JSONObject(json)
            if (jsonObject.has(normalizedTrigger)) {
                return jsonObject.getString(normalizedTrigger)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    fun hasShortcut(trigger: String): Boolean {
        return getExpansion(trigger) != null
    }

    companion object {
        private const val PREFS_NAME = "keyboard_shortcuts"
        private const val KEY_SHORTCUTS = "shortcuts_map"
    }
}
