package com.samiuysal.keyboard.features.toolbar

import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.samiuysal.keyboard.R
import com.samiuysal.keyboard.features.shortcuts.ShortcutManager
import com.samiuysal.keyboard.features.suggestions.PredictionEngine
import com.samiuysal.keyboard.features.suggestions.SuggestionAdapter
import com.samiuysal.keyboard.service.MPKeyboardService
import com.samiuysal.keyboard.settings.MainActivity

class ToolbarManager(private val service: MPKeyboardService, private val rootView: View) {
    private val toolsPanel: LinearLayout = rootView.findViewById(R.id.tools_panel)
    private val btnToggle: ImageView = rootView.findViewById(R.id.btn_toggle_tools)
    private val suggestionsRecycler: RecyclerView = rootView.findViewById(R.id.recycler_suggestions)
    private lateinit var suggestionAdapter: SuggestionAdapter
    private var predictionEngine: PredictionEngine? = null
    private var shortcutManager: ShortcutManager? = null
    private val btnClipboard: ImageView = rootView.findViewById(R.id.tool_clipboard)
    private val btnTranslate: ImageView = rootView.findViewById(R.id.tool_translate)
    private val btnPassword: ImageView? = rootView.findViewById(R.id.tool_password)
    private val btnTheme: ImageView = rootView.findViewById(R.id.tool_theme)
    private val btnSettings: ImageView = rootView.findViewById(R.id.tool_settings)

    private var isToolsPanelVisible = false

    init {
        setupInteractions()
    }

    private fun setupInteractions() {
        btnToggle.setOnClickListener { toggleToolsPanel() }

        suggestionAdapter = SuggestionAdapter { word -> service.commitSuggestion(word) }
        suggestionsRecycler.layoutManager =
                androidx.recyclerview.widget.LinearLayoutManager(
                        service,
                        androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,
                        false
                )
        suggestionsRecycler.adapter = suggestionAdapter

        btnClipboard.setOnClickListener { service.openClipboardFeature() }

        btnTranslate.setOnClickListener { service.openTranslationFeature() }

        btnPassword?.setOnClickListener { service.openPasswordFeature() }

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

    fun hideToolsPanel(animate: Boolean = true) {
        val parent = toolsPanel.parent as? android.view.ViewGroup
        if (animate && parent != null) {
            val transition = android.transition.Fade()
            transition.duration = 200
            android.transition.TransitionManager.beginDelayedTransition(parent, transition)
        }

        toolsPanel.visibility = View.GONE
        isToolsPanelVisible = false
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
            toolsPanel.visibility = View.VISIBLE
            isToolsPanelVisible = true
            btnToggle.rotation = 90f
        }
    }

    fun updateSuggestions(text: String, isNewWord: Boolean) {
        if (predictionEngine == null) return

        val shortcutExpansion = shortcutManager?.getExpansion(text)

        service.handler.post {
            if (shortcutExpansion != null && text.isNotEmpty()) {
                suggestionAdapter.setSuggestions(listOf(shortcutExpansion))
                suggestionsRecycler.scrollToPosition(0)
            } else {
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
            val feedbackText = "⚡ \"$trigger\" → \"$expansion\""
            suggestionAdapter.setSuggestions(listOf(feedbackText))
            suggestionsRecycler.scrollToPosition(0)

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
