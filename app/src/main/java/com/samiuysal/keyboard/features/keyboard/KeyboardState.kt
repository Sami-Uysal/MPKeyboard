package com.samiuysal.keyboard.features.keyboard

data class KeyboardState(
        val currentLayout: LayoutType = LayoutType.QWERTY,
        val isShifted: Boolean = false,
        val isCapsLocked: Boolean = false,
        val language: String = "tr",
        val composingText: String = "",
        val suggestions: List<String> = emptyList(),
        val isEmojiMode: Boolean = false,
        val isGifSearchMode: Boolean = false,
        val isTranslationMode: Boolean = false,
        val isClipboardMode: Boolean = false,
        val isPopupVisible: Boolean = false,
        val popupVariants: List<String> = emptyList(),
        val selectedPopupIndex: Int = -1,
        val shortcutFeedback: ShortcutFeedback? = null,
        val backgroundColor: Int = 0xFF000000.toInt(),
        val keyColor: Int = 0xFF3A3A3C.toInt()
)

enum class LayoutType {
    QWERTY,
    NUMBERS,
    SYMBOLS
}
data class ShortcutFeedback(val trigger: String, val expansion: String)
