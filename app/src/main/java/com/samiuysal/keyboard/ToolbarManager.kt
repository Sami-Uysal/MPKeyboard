package com.samiuysal.keyboard

import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class ToolbarManager(private val service: SimpleDarkKeyboard, private val rootView: View) {
    // Tools Panel (Collapsible)
    private val toolsPanel: LinearLayout = rootView.findViewById(R.id.tools_panel)

    // Toggle Button (in Suggestions Strip)
    private val btnToggle: ImageView = rootView.findViewById(R.id.btn_toggle_tools)

    // Suggestions
    private val suggestionsRecycler: RecyclerView = rootView.findViewById(R.id.recycler_suggestions)
    private lateinit var suggestionAdapter: SuggestionAdapter
    private var predictionEngine: PredictionEngine? = null
    private var shortcutManager: ShortcutManager? = null

    // Tool Buttons
    private val btnClipboard: ImageView = rootView.findViewById(R.id.tool_clipboard)
    private val btnTranslate: ImageView = rootView.findViewById(R.id.tool_translate)
    private val btnTheme: ImageView = rootView.findViewById(R.id.tool_theme)
    private val btnSettings: ImageView = rootView.findViewById(R.id.tool_settings)

    private var isToolsPanelVisible = false

    init {
        setupInteractions()
    }

    private fun setupInteractions() {
        // Toggle Button: Show/Hide Tools Panel
        btnToggle.setOnClickListener { toggleToolsPanel() }

        // Setup Suggestions Adapter
        suggestionAdapter = SuggestionAdapter { word -> service.commitSuggestion(word) }
        suggestionsRecycler.layoutManager =
                androidx.recyclerview.widget.LinearLayoutManager(
                        service,
                        androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                        false
                )
        suggestionsRecycler.adapter = suggestionAdapter

        // Tool Button Listeners
        btnClipboard.setOnClickListener { service.openClipboardFeature() }

        btnTranslate.setOnClickListener { service.openTranslationFeature() }

        btnTheme.setOnClickListener {
            try {
                val intent = Intent(service, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra("ACTION_OPEN_THEME", true)
                service.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(service, "Could not open settings", Toast.LENGTH_SHORT).show()
            }
        }

        btnSettings.setOnClickListener {
            try {
                val intent = Intent(service, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                service.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(service, "Could not open settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun hideToolsPanel() {
        val parent = toolsPanel.parent as? android.view.ViewGroup
        if (parent != null) {
            val transition = android.transition.Fade()
            transition.duration = 200
            android.transition.TransitionManager.beginDelayedTransition(parent, transition)
        }

        toolsPanel.visibility = View.GONE
        isToolsPanelVisible = false
        // Arrow points UP/LEFT (to open) - Default state
        btnToggle.rotation = -90f
    }

    private fun toggleToolsPanel() {
        val parent = toolsPanel.parent as? android.view.ViewGroup
        if (parent != null) {
            val transition = android.transition.Fade()
            transition.duration = 200
            android.transition.TransitionManager.beginDelayedTransition(parent, transition)
        }

        if (isToolsPanelVisible) {
            hideToolsPanel()
        } else {
            // Show Tools Panel
            toolsPanel.visibility = View.VISIBLE
            isToolsPanelVisible = true

            // Arrow points RIGHT/DOWN (to close)
            btnToggle.rotation = 90f
        }
    }

    fun updateSuggestions(text: String, isNewWord: Boolean) {
        if (predictionEngine == null) return

        // Check if the typed text matches a shortcut
        val shortcutExpansion = shortcutManager?.getExpansion(text)

        service.handler.post {
            if (shortcutExpansion != null && text.isNotEmpty()) {
                // Show shortcut expansion - clicking it will insert it directly
                suggestionAdapter.setSuggestions(listOf(shortcutExpansion))
                suggestionsRecycler.scrollToPosition(0)
            } else {
                // Show normal predictions
                val suggestions = predictionEngine!!.getPredictions(text)
                suggestionAdapter.setSuggestions(suggestions)
                if (suggestions.isNotEmpty()) {
                    suggestionsRecycler.scrollToPosition(0)
                }
            }
        }
    }

    fun showShortcutFeedback(trigger: String, expansion: String) {
        service.handler.post {
            // Show shortcut feedback as a special suggestion
            val feedbackText = "⚡ \"$trigger\" → \"$expansion\""
            suggestionAdapter.setSuggestions(listOf(feedbackText))
            suggestionsRecycler.scrollToPosition(0)

            // After 2 seconds, clear the feedback and restore normal suggestions
            service.handler.postDelayed({ suggestionAdapter.setSuggestions(emptyList()) }, 2000)
        }
    }

    fun setEngine(engine: PredictionEngine) {
        this.predictionEngine = engine
    }

    fun setShortcutManager(manager: ShortcutManager) {
        this.shortcutManager = manager
    }
}
