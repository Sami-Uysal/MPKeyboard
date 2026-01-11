package com.samiuysal.keyboard

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog

class SettingsFragment : Fragment() {

    private var selectedBgColor = "#000000"
    private var selectedKeyColor = "#3A3A3C"

    private val bgColors = listOf(
        "#000000" to R.id.bgBlack,
        "#0D1B2A" to R.id.bgDarkBlue,
        "#1A0D2E" to R.id.bgDarkPurple,
        "#0D1A12" to R.id.bgDarkGreen,
        "#1A0D0D" to R.id.bgDarkRed
    )

    private val keyColors = listOf(
        "#3A3A3C" to R.id.keyGray,
        "#2563EB" to R.id.keyBlue,
        "#7C3AED" to R.id.keyPurple,
        "#16A34A" to R.id.keyGreen,
        "#DC2626" to R.id.keyRed
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadSettings(view)

        view.findViewById<View>(R.id.btnTheme).setOnClickListener {
            showThemeDialog()
        }

        updateLanguageText(view)
        view.findViewById<View>(R.id.btnLanguage).setOnClickListener {
            showLanguageDialog(view)
        }
    }

    override fun onResume() {
        super.onResume()
        if (requireActivity().intent.getBooleanExtra("ACTION_OPEN_THEME", false)) {
            showThemeDialog()
            requireActivity().intent.removeExtra("ACTION_OPEN_THEME")
        }
    }

    private fun updateLanguageText(view: View) {
        val prefs = requireContext().getSharedPreferences("keyboard_prefs", Context.MODE_PRIVATE)
        val langCode = prefs.getString("selected_language", "tr") ?: "tr"
        val langName = if (langCode == "tr") getString(R.string.lang_tr) else getString(R.string.lang_en)
        view.findViewById<TextView>(R.id.txtCurrentLanguage).text = getString(R.string.btn_languages) + " ($langName)"
    }

    private fun showLanguageDialog(parentView: View) {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.dialog_language, null) // We need to create this layout
        dialog.setContentView(view)

        view.findViewById<View>(R.id.langTurkish).setOnClickListener {
            requireContext().getSharedPreferences("keyboard_prefs", Context.MODE_PRIVATE)
                .edit().putString("selected_language", "tr").apply()
            requireContext().sendBroadcast(Intent(SimpleDarkKeyboard.ACTION_THEME_CHANGED).setPackage(requireContext().packageName)) 
            dialog.dismiss()
            requireActivity().recreate()
        }

        view.findViewById<View>(R.id.langEnglish).setOnClickListener {
             requireContext().getSharedPreferences("keyboard_prefs", Context.MODE_PRIVATE)
                .edit().putString("selected_language", "en").apply()
            requireContext().sendBroadcast(Intent(SimpleDarkKeyboard.ACTION_THEME_CHANGED).setPackage(requireContext().packageName))
            dialog.dismiss()
            requireActivity().recreate()
        }

        dialog.show()
    }

    private fun loadSettings(view: View) {
        val prefs = requireContext().getSharedPreferences("keyboard_prefs", Context.MODE_PRIVATE)
        selectedBgColor = prefs.getString("bg_color", "#000000") ?: "#000000"
        selectedKeyColor = prefs.getString("key_color", "#3A3A3C") ?: "#3A3A3C"
    }

    private fun showThemeDialog() {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.dialog_theme, null)
        dialog.setContentView(view)

        val previewBg = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.keyboardPreview)
        val previewKeys = listOf(
            view.findViewById<TextView>(R.id.previewKey1),
            view.findViewById<TextView>(R.id.previewKey2),
            view.findViewById<TextView>(R.id.previewKey3),
            view.findViewById<TextView>(R.id.previewKey4),
            view.findViewById<TextView>(R.id.previewKey5),
            view.findViewById<TextView>(R.id.previewKey6),
            view.findViewById<TextView>(R.id.previewKey7),
            view.findViewById<TextView>(R.id.previewKey8),
            view.findViewById<TextView>(R.id.previewKey9),
            view.findViewById<TextView>(R.id.previewKey10),
            view.findViewById<TextView>(R.id.previewKey11),
            view.findViewById<TextView>(R.id.previewKey12)
        )

        fun updatePreview() {
            previewBg.setCardBackgroundColor(Color.parseColor(selectedBgColor))
            for (key in previewKeys) {
                val drawable = GradientDrawable().apply {
                    setColor(Color.parseColor(selectedKeyColor))
                    cornerRadius = 16f
                }
                key.background = drawable
            }
        }

        updatePreview()

        for ((color, id) in bgColors) {
            view.findViewById<View>(id).setOnClickListener {
                selectedBgColor = color
                updatePreview()
            }
        }

        for ((color, id) in keyColors) {
            view.findViewById<View>(id).setOnClickListener {
                selectedKeyColor = color
                updatePreview()
            }
        }

        view.findViewById<View>(R.id.btnSaveTheme).setOnClickListener {
            val prefs = requireContext().getSharedPreferences("keyboard_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("bg_color", selectedBgColor)
                .putString("key_color", selectedKeyColor)
                .apply()
            requireContext().sendBroadcast(Intent(SimpleDarkKeyboard.ACTION_THEME_CHANGED).setPackage(requireContext().packageName))
            Toast.makeText(requireContext(), getString(R.string.msg_theme_saved), Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }
}
