package com.samiuysal.keyboard.features.clipboard

import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class KeyboardClipboardManager(private val context: Context) {

    private val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val prefs: SharedPreferences =
            context.getSharedPreferences("clipboard_history", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val MAX_HISTORY = 20

    private var history: MutableList<String> = loadHistory()

    private val clipListener =
            ClipboardManager.OnPrimaryClipChangedListener {
                val clip = clipboard.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val text = clip.getItemAt(0).text?.toString()
                    if (!text.isNullOrEmpty()) {
                        addClip(text)
                    }
                }
            }

    init {
        try {
            clipboard.addPrimaryClipChangedListener(clipListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getHistory(): List<String> {
        return history
    }

    fun addClip(text: String) {
        if (history.contains(text)) {
            history.remove(text)
        }
        history.add(0, text)
        if (history.size > MAX_HISTORY) {
            history.removeAt(history.size - 1)
        }
        saveHistory()
    }

    fun clearHistory() {
        history.clear()
        saveHistory()
    }

    private fun loadHistory(): MutableList<String> {
        val json = prefs.getString("history", "[]") ?: "[]"
        val type = object : TypeToken<MutableList<String>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    private fun saveHistory() {
        val json = gson.toJson(history)
        prefs.edit().putString("history", json).apply()
    }

    fun copyToClipboard(text: String) {}
}
