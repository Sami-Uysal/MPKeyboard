package com.samiuysal.keyboard.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.samiuysal.keyboard.R

class HomeFragment : Fragment() {

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnEnable = view.findViewById<View>(R.id.btnEnableKeyboard)
        val btnSelect = view.findViewById<View>(R.id.btnSelectKeyboard)
        val txtStatus = view.findViewById<TextView>(R.id.statusText)

        btnEnable.setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        btnSelect.setOnClickListener {
            val imm =
                    requireContext().getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as
                            InputMethodManager
            imm.showInputMethodPicker()
        }

        updateStatus(txtStatus)
    }

    override fun onResume() {
        super.onResume()
        view?.findViewById<TextView>(R.id.statusText)?.let { updateStatus(it) }
    }

    private fun updateStatus(txtStatus: TextView) {
        val imm =
                requireContext().getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as
                        InputMethodManager
        val enabledInputMethods = imm.enabledInputMethodList
        val isEnabled = enabledInputMethods.any { it.packageName == requireContext().packageName }

        if (isEnabled) {
            txtStatus.text = getString(R.string.status_enabled)
            txtStatus.setTextColor(requireContext().getColor(R.color.status_active))
            view?.findViewById<View>(R.id.statusIndicator)
                    ?.background
                    ?.setTint(requireContext().getColor(R.color.status_active))
        } else {
            txtStatus.text = getString(R.string.status_disabled)
            txtStatus.setTextColor(requireContext().getColor(R.color.status_inactive))
            view?.findViewById<View>(R.id.statusIndicator)
                    ?.background
                    ?.setTint(requireContext().getColor(R.color.status_inactive))
        }
    }
}
