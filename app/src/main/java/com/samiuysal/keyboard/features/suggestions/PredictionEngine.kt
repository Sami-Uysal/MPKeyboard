package com.samiuysal.keyboard.features.suggestions

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PredictionEngine(private val context: Context) {

    private val root = TrieNode()
    private var currentLanguage = ""

    data class TrieNode(
            val children: MutableMap<Char, TrieNode> = mutableMapOf(),
            var isEndOfWord: Boolean = false,
            var frequency: Int = 0,
            var word: String = ""
    )

    suspend fun loadLanguage(languageCode: String) {
        if (currentLanguage == languageCode) return
        currentLanguage = languageCode
        root.children.clear()

        val fileName = if (languageCode == "tr") "tr_full.txt" else "en_full.txt"

        withContext(Dispatchers.IO) {
            try {
                val assetManager = context.assets
                val inputStream = assetManager.open(fileName)
                val reader = BufferedReader(InputStreamReader(inputStream))

                var count = 0
                val maxWords = 50000

                var line: String? = reader.readLine()
                while (line != null && count < maxWords) {
                    val parts = line.trim().split(" ")
                    val word = parts[0].lowercase(Locale.getDefault())
                    val frequency = if (parts.size > 1) parts[1].toIntOrNull() ?: 1 else 1

                    if (word.isNotEmpty()) {
                        insert(word, frequency)
                        count++
                    }
                    line = reader.readLine()
                }

                reader.close()
                Log.d(TAG, "Loaded $count words for $languageCode")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading dictionary: $fileName", e)
            }
        }
    }

    private fun insert(word: String, frequency: Int) {
        var current = root
        for (char in word) {
            current = current.children.getOrPut(char) { TrieNode() }
        }
        current.isEndOfWord = true
        current.frequency = frequency
        current.word = word
    }

    fun getPredictions(prefix: String, maxResults: Int = 3): List<String> {
        if (prefix.isEmpty()) return emptyList()

        val lowercasePrefix = prefix.lowercase(Locale.getDefault())
        var current = root

        for (char in lowercasePrefix) {
            current = current.children[char] ?: return emptyList()
        }

        val results = mutableListOf<TrieNode>()
        collectWords(current, results)

        return results.sortedByDescending { it.frequency }.take(maxResults).map { it.word }
    }

    private fun collectWords(node: TrieNode, results: MutableList<TrieNode>) {
        if (node.isEndOfWord) {
            results.add(node)
        }

        for (child in node.children.values) {
            collectWords(child, results)
        }
    }

    fun learn(word: String) {
        if (word.isBlank()) return
        val lowercaseWord = word.lowercase(Locale.getDefault())

        var current = root
        for (char in lowercaseWord) {
            current = current.children.getOrPut(char) { TrieNode() }
        }

        if (!current.isEndOfWord) {
            current.isEndOfWord = true
            current.frequency = 1
            current.word = lowercaseWord
        } else {
            current.frequency++
        }
    }

    companion object {
        private const val TAG = "PredictionEngine"
    }
}
