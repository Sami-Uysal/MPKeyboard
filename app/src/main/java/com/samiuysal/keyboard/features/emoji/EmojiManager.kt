package com.samiuysal.keyboard.features.emoji

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.samiuysal.keyboard.R
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class EmojiData(
        val codes: String,
        val char: String,
        val name: String,
        val category: String,
        val group: String
)

object EmojiManager {

    private var emojiMap: Map<String, List<EmojiData>>? = null

    val CATEGORY_ORDER =
            listOf(
                    "Smileys & Emotion",
                    "People & Body",
                    "Animals & Nature",
                    "Food & Drink",
                    "Travel & Places",
                    "Activities",
                    "Objects",
                    "Symbols",
                    "Flags"
            )

    suspend fun getEmojis(context: Context): Map<String, List<EmojiData>> {
        if (emojiMap != null) return emojiMap!!

        return withContext(Dispatchers.IO) {
            try {
                val assetManager = context.assets
                val inputStream = assetManager.open("emoji.json")
                val reader = InputStreamReader(inputStream)

                val type = object : TypeToken<List<EmojiData>>() {}.type
                val allEmojis: List<EmojiData> = Gson().fromJson(reader, type)
                reader.close()

                val grouped = allEmojis.groupBy { it.group }
                emojiMap = grouped
                grouped
            } catch (e: Exception) {
                e.printStackTrace()
                emptyMap()
            }
        }
    }

    fun getCategoryResId(categoryKey: String): Int {
        return when (categoryKey) {
            "Smileys & Emotion" -> R.string.category_smileys
            "People & Body" -> R.string.category_people
            "Animals & Nature" -> R.string.category_animals
            "Food & Drink" -> R.string.category_food
            "Travel & Places" -> R.string.category_travel
            "Activities" -> R.string.category_activities
            "Objects" -> R.string.category_objects
            "Symbols" -> R.string.category_symbols
            "Flags" -> R.string.category_flags
            else -> 0
        }
    }

    fun getCategoryDisplayName(context: Context, categoryKey: String): String {
        return when (categoryKey) {
            "Smileys & Emotion" -> context.getString(R.string.category_smileys)
            "People & Body" -> context.getString(R.string.category_people)
            "Animals & Nature" -> context.getString(R.string.category_animals)
            "Food & Drink" -> context.getString(R.string.category_food)
            "Travel & Places" -> context.getString(R.string.category_travel)
            "Activities" -> context.getString(R.string.category_activities)
            "Objects" -> context.getString(R.string.category_objects)
            "Symbols" -> context.getString(R.string.category_symbols)
            "Flags" -> context.getString(R.string.category_flags)
            else -> categoryKey
        }
    }
}
