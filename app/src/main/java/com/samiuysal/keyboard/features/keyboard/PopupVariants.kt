package com.samiuysal.keyboard.features.keyboard

object PopupVariants {

    val TR_MAP =
            mapOf(
                    "c" to listOf("ç"),
                    "s" to listOf("ş"),
                    "g" to listOf("ğ"),
                    "u" to listOf("ü"),
                    "o" to listOf("ö"),
                    "i" to listOf("ı", "İ")
            )

    val EN_MAP =
            mapOf(
                    "e" to listOf("é", "è", "ê", "ë", "ē"),
                    "a" to listOf("à", "á", "â", "ä", "ã", "å", "æ"),
                    "u" to listOf("ù", "ú", "û", "ü", "ū"),
                    "i" to listOf("ì", "í", "î", "ï", "ī"),
                    "o" to listOf("ò", "ó", "ô", "ö", "õ", "ø", "œ"),
                    "c" to listOf("ç", "ć", "č"),
                    "n" to listOf("ñ", "ń"),
                    "s" to listOf("ß", "ś", "š"),
                    "y" to listOf("ý", "ÿ"),
                    "z" to listOf("ž", "ź", "ż")
            )

    val NUMBER_MAP =
            mapOf(
                    "1" to listOf("①", "⅟"),
                    "2" to listOf("②", "½"),
                    "3" to listOf("③", "⅓"),
                    "4" to listOf("④", "¼"),
                    "5" to listOf("⑤", "⅕"),
                    "0" to listOf("⓪", "∅"),
                    "-" to listOf("–", "—", "−"),
                    "." to listOf(",", "…"),
                    "'" to listOf("'", "'", "\"", """, """),
                    "!" to listOf("¡"),
                    "?" to listOf("¿")
            )

    val PUNCTUATION_MAP =
            mapOf(
                    "." to listOf(".", ",", "?", "!", ":", ";", "@"),
                    "," to listOf(",", ".", "?", "!", ":", ";", "'", "\"")
            )

    fun getVariants(char: String, language: String, isShifted: Boolean): List<String> {
        val lowerChar = char.lowercase()

        val baseMap =
                when (language) {
                    "tr" -> TR_MAP
                    else -> EN_MAP
                }

        val variants =
                baseMap[lowerChar]
                        ?: NUMBER_MAP[lowerChar] ?: PUNCTUATION_MAP[lowerChar] ?: return emptyList()

        return if (isShifted) {
            variants.map {
                when {
                    it == "ı" -> "I"
                    it == "i" -> "İ"
                    else -> it.uppercase()
                }
            }
        } else {
            variants
        }
    }

    fun hasVariants(char: String, language: String): Boolean {
        val lowerChar = char.lowercase()
        val baseMap =
                when (language) {
                    "tr" -> TR_MAP
                    else -> EN_MAP
                }
        return baseMap.containsKey(lowerChar) ||
                NUMBER_MAP.containsKey(lowerChar) ||
                PUNCTUATION_MAP.containsKey(lowerChar)
    }
}
