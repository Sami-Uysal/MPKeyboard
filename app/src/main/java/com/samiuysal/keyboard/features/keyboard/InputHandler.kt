package com.samiuysal.keyboard.features.keyboard

import android.util.Log
import android.view.inputmethod.InputConnection
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InputHandler @Inject constructor() {

    companion object {
        private const val TAG = "InputHandler"
    }

    private var currentConnection: InputConnection? = null

    fun updateConnection(connection: InputConnection?) {
        this.currentConnection = connection
    }
    fun isConnected(): Boolean = currentConnection != null

    fun commitText(text: String, cursorPosition: Int = 1): Boolean {
        return safeExecute { currentConnection?.commitText(text, cursorPosition) ?: false }
    }
    fun setComposingText(text: CharSequence, cursorPosition: Int = 1): Boolean {
        return safeExecute { currentConnection?.setComposingText(text, cursorPosition) ?: false }
    }

    fun finishComposingText(): Boolean {
        return safeExecute { currentConnection?.finishComposingText() ?: false }
    }
    fun deleteBackward(count: Int = 1): Boolean {
        return safeExecute { currentConnection?.deleteSurroundingText(count, 0) ?: false }
    }

    fun deleteSurroundingText(before: Int, after: Int): Boolean {
        return safeExecute { currentConnection?.deleteSurroundingText(before, after) ?: false }
    }
    fun sendEnterKey(): Boolean {
        return safeExecute {
            currentConnection?.performEditorAction(
                    android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            )
                    ?: false
        }
    }

    fun sendKeyEvent(event: android.view.KeyEvent): Boolean {
        return safeExecute { currentConnection?.sendKeyEvent(event) ?: false }
    }

    fun getTextBeforeCursor(length: Int): String {
        return try {
            currentConnection?.getTextBeforeCursor(length, 0)?.toString() ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "getTextBeforeCursor failed", e)
            ""
        }
    }

    fun getTextAfterCursor(length: Int): String {
        return try {
            currentConnection?.getTextAfterCursor(length, 0)?.toString() ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "getTextAfterCursor failed", e)
            ""
        }
    }

    private inline fun safeExecute(block: () -> Boolean): Boolean {
        return try {
            block()
        } catch (e: Exception) {
            Log.e(TAG, "InputConnection operation failed", e)
            false
        }
    }
}
