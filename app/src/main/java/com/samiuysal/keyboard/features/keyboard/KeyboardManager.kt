package com.samiuysal.keyboard.features.keyboard

import android.graphics.Color
import android.util.Log
import com.samiuysal.keyboard.core.preferences.KeyboardPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Singleton
class KeyboardManager
@Inject
constructor(private val preferences: KeyboardPreferences, private val appScope: CoroutineScope) {
    companion object {
        private const val TAG = "KeyboardManager"
    }

    private val _state = MutableStateFlow(KeyboardState())
    val state: StateFlow<KeyboardState> = _state.asStateFlow()

    private val composingBuffer = StringBuilder()

    init {
        loadThemeFromPreferences()
        loadLanguageFromPreferences()
    }

    fun currentState(): KeyboardState = _state.value

    fun toggleShift() {
        _state.update { it.copy(isShifted = !it.isShifted, isCapsLocked = false) }
    }

    fun setShift(shifted: Boolean) {
        _state.update { it.copy(isShifted = shifted) }
    }

    fun toggleCapsLock() {
        _state.update { it.copy(isCapsLocked = !it.isCapsLocked, isShifted = !it.isCapsLocked) }
    }

    fun switchToQwerty() {
        _state.update { it.copy(currentLayout = LayoutType.QWERTY) }
    }

    fun switchToNumbers() {
        _state.update { it.copy(currentLayout = LayoutType.NUMBERS) }
    }

    fun switchToSymbols() {
        _state.update { it.copy(currentLayout = LayoutType.SYMBOLS) }
    }

    fun switchLayout(layout: LayoutType) {
        _state.update { it.copy(currentLayout = layout) }
    }

    fun setLanguage(language: String) {
        _state.update { it.copy(language = language) }
        preferences.language = language
    }

    private fun loadLanguageFromPreferences() {
        val savedLanguage = preferences.language
        _state.update { it.copy(language = savedLanguage) }
    }

    fun setEmojiMode(enabled: Boolean) {
        _state.update {
            it.copy(
                    isEmojiMode = enabled,
                    isGifSearchMode = false,
                    isTranslationMode = false,
                    isClipboardMode = false
            )
        }
    }

    fun setGifSearchMode(enabled: Boolean) {
        _state.update {
            it.copy(
                    isGifSearchMode = enabled,
                    isEmojiMode = false,
                    isTranslationMode = false,
                    isClipboardMode = false
            )
        }
    }

    fun setTranslationMode(enabled: Boolean) {
        _state.update {
            it.copy(
                    isTranslationMode = enabled,
                    isEmojiMode = false,
                    isGifSearchMode = false,
                    isClipboardMode = false
            )
        }
    }

    fun setClipboardMode(enabled: Boolean) {
        _state.update {
            it.copy(
                    isClipboardMode = enabled,
                    isEmojiMode = false,
                    isGifSearchMode = false,
                    isTranslationMode = false
            )
        }
    }

    fun closeAllModes() {
        _state.update {
            it.copy(
                    isEmojiMode = false,
                    isGifSearchMode = false,
                    isTranslationMode = false,
                    isClipboardMode = false
            )
        }
    }

    fun updateComposingText(text: String) {
        composingBuffer.clear()
        composingBuffer.append(text)
        _state.update { it.copy(composingText = text) }
    }

    fun appendToComposing(char: String) {
        composingBuffer.append(char)
        _state.update { it.copy(composingText = composingBuffer.toString()) }
    }

    fun deleteFromComposing(): Boolean {
        if (composingBuffer.isNotEmpty()) {
            composingBuffer.deleteCharAt(composingBuffer.length - 1)
            _state.update { it.copy(composingText = composingBuffer.toString()) }
            return true
        }
        return false
    }

    fun clearComposing() {
        composingBuffer.clear()
        _state.update { it.copy(composingText = "") }
    }

    fun getComposingText(): String = composingBuffer.toString()

    fun updateSuggestions(suggestions: List<String>) {
        _state.update { it.copy(suggestions = suggestions) }
    }

    fun clearSuggestions() {
        _state.update { it.copy(suggestions = emptyList()) }
    }

    fun showPopup(key: String) {
        val currentState = _state.value
        val variants = PopupVariants.getVariants(key, currentState.language, currentState.isShifted)

        if (variants.isNotEmpty()) {
            _state.update {
                it.copy(
                        isPopupVisible = true,
                        popupVariants = variants,
                        selectedPopupIndex = variants.size / 2
                )
            }
        }
    }

    fun updatePopupSelection(index: Int) {
        _state.update { it.copy(selectedPopupIndex = index) }
    }

    fun dismissPopup() {
        _state.update {
            it.copy(isPopupVisible = false, popupVariants = emptyList(), selectedPopupIndex = -1)
        }
    }

    fun getSelectedPopupChar(): String? {
        val state = _state.value
        return if (state.selectedPopupIndex in state.popupVariants.indices) {
            state.popupVariants[state.selectedPopupIndex]
        } else null
    }

    fun showShortcutFeedback(trigger: String, expansion: String) {
        _state.update { it.copy(shortcutFeedback = ShortcutFeedback(trigger, expansion)) }

        appScope.launch {
            delay(2000)
            _state.update { it.copy(shortcutFeedback = null) }
        }
    }

    fun updateTheme(bgColor: String, keyColor: String) {
        try {
            val parsedBg = Color.parseColor(bgColor)
            val parsedKey = Color.parseColor(keyColor)
            _state.update { it.copy(backgroundColor = parsedBg, keyColor = parsedKey) }
            preferences.backgroundColor = bgColor
            preferences.keyColor = keyColor
        } catch (e: Exception) {
            Log.e(TAG, "Invalid color format", e)
        }
    }

    private fun loadThemeFromPreferences() {
        try {
            val bgColor = Color.parseColor(preferences.backgroundColor)
            val keyColor = Color.parseColor(preferences.keyColor)
            _state.update { it.copy(backgroundColor = bgColor, keyColor = keyColor) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load theme", e)
        }
    }

    fun handleAutoShift() {
        if (!_state.value.isCapsLocked) {
            val composing = composingBuffer.toString()
            if (composing.isEmpty()) {
                _state.update { it.copy(isShifted = true) }
            }
        }
    }

    fun handlePostKeyShift() {
        if (!_state.value.isCapsLocked && _state.value.isShifted) {
            _state.update { it.copy(isShifted = false) }
        }
    }
}
