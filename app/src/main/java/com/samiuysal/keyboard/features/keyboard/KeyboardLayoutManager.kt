package com.samiuysal.keyboard.features.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.samiuysal.keyboard.R
import com.samiuysal.keyboard.core.preferences.KeyboardPreferences

class KeyboardLayoutManager(
        private val context: Context,
        private val layoutInflater: LayoutInflater,
        private val inputHandler: InputHandler,
        private val preferences: KeyboardPreferences,
        private val onStandardKeyClick: (String) -> Unit
) {

    var keyboardView: View? = null
    private val allKeys = mutableListOf<View>()
    private val specialKeys = mutableListOf<View>()
    private val letterKeys = mutableListOf<Button>()

    // Popup state
    var isPopupActive = false
    private var currentPopupWindow: PopupWindow? = null
    private val popupButtons = mutableListOf<TextView>()
    private var currentLongPressRunnable: Runnable? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var currentPopupVariants: List<String> = emptyList()

    private var currentKeyColor = Color.DKGRAY

    fun setupQwertyLayout(
            parent: ViewGroup,
            onOpenEmoji: () -> Unit,
            onShift: () -> Unit,
            onSwitchToNumbers: () -> Unit,
            onSpace: () -> Unit,
            onEnter: () -> Unit,
            onBackspace: () -> Unit
    ) {
        parent.removeAllViews()
        keyboardView = layoutInflater.inflate(R.layout.keyboard_layout, null)
        parent.addView(keyboardView)

        setupQwertyKeys(onOpenEmoji, onShift, onSwitchToNumbers, onSpace, onEnter, onBackspace)
        applyTheme()
    }

    fun setupNumbersLayout(
            parent: ViewGroup,
            onSwitchToQwerty: () -> Unit,
            onSwitchToSymbols: () -> Unit,
            onSpace: () -> Unit,
            onEnter: () -> Unit,
            onBackspace: () -> Unit
    ) {
        parent.removeAllViews()
        keyboardView = layoutInflater.inflate(R.layout.keyboard_numbers, null)
        parent.addView(keyboardView)

        setupNumberKeys(onSwitchToQwerty, onSwitchToSymbols, onSpace, onEnter, onBackspace)
        applyTheme()
    }

    fun setupSymbolsLayout(
            parent: ViewGroup,
            onSwitchToNumbers: () -> Unit,
            onSpace: () -> Unit,
            onEnter: () -> Unit,
            onBackspace: () -> Unit,
            onTab: () -> Unit
    ) {
        parent.removeAllViews()
        keyboardView = layoutInflater.inflate(R.layout.keyboard_symbols, null)
        parent.addView(keyboardView)

        setupSymbolKeys(onSwitchToNumbers, onSpace, onEnter, onBackspace, onTab)
        applyTheme()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupQwertyKeys(
            onOpenEmoji: () -> Unit,
            onShift: () -> Unit,
            onSwitchToNumbers: () -> Unit,
            onSpace: () -> Unit,
            onEnter: () -> Unit,
            onBackspace: () -> Unit
    ) {
        val view = keyboardView ?: return
        allKeys.clear()
        specialKeys.clear()
        letterKeys.clear()

        val letterIds =
                listOf(
                        R.id.key_q,
                        R.id.key_w,
                        R.id.key_e,
                        R.id.key_r,
                        R.id.key_t,
                        R.id.key_y,
                        R.id.key_u,
                        R.id.key_i,
                        R.id.key_o,
                        R.id.key_p,
                        R.id.key_a,
                        R.id.key_s,
                        R.id.key_d,
                        R.id.key_f,
                        R.id.key_g,
                        R.id.key_h,
                        R.id.key_j,
                        R.id.key_k,
                        R.id.key_l,
                        R.id.key_z,
                        R.id.key_x,
                        R.id.key_c,
                        R.id.key_v,
                        R.id.key_b,
                        R.id.key_n,
                        R.id.key_m
                )

        for (id in letterIds) {
            val btn = view.findViewById<Button>(id)
            if (btn != null) {
                letterKeys.add(btn)
                allKeys.add(btn)
                setupKeyWithPopup(btn)
            }
        }

        val space = view.findViewById<Button>(R.id.key_space)
        allKeys.add(space)
        addPressAnimation(space)
        space.setOnClickListener { onSpace() }

        val backspace = view.findViewById<ImageButton>(R.id.key_backspace)
        specialKeys.add(backspace)
        setupBackspaceImageButton(backspace, onBackspace)

        val enter = view.findViewById<ImageButton>(R.id.key_enter)
        specialKeys.add(enter)
        addPressAnimation(enter)
        enter.setOnClickListener { onEnter() }

        val shift = view.findViewById<ImageButton>(R.id.key_shift)
        specialKeys.add(shift)
        addPressAnimation(shift)
        shift.setOnClickListener { onShift() }

        val comma = view.findViewById<Button>(R.id.key_comma)
        allKeys.add(comma)
        addPressAnimation(comma)
        comma.setOnClickListener { inputHandler.commitText(",") }

        val period = view.findViewById<Button>(R.id.key_period)
        allKeys.add(period)
        addPressAnimation(period)
        period.setOnClickListener { inputHandler.commitText(".") }

        val key123 = view.findViewById<Button>(R.id.key_123)
        specialKeys.add(key123)
        addPressAnimation(key123)
        key123.setOnClickListener { onSwitchToNumbers() }

        val emojiKey = view.findViewById<Button>(R.id.key_emoji)
        specialKeys.add(emojiKey)
        addPressAnimation(emojiKey)
        emojiKey.setOnClickListener { onOpenEmoji() }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupNumberKeys(
            onSwitchToQwerty: () -> Unit,
            onSwitchToSymbols: () -> Unit,
            onSpace: () -> Unit,
            onEnter: () -> Unit,
            onBackspace: () -> Unit
    ) {
        val view = keyboardView ?: return
        allKeys.clear()
        specialKeys.clear()

        val numIds =
                listOf(
                        R.id.key_1,
                        R.id.key_2,
                        R.id.key_3,
                        R.id.key_4,
                        R.id.key_5,
                        R.id.key_6,
                        R.id.key_7,
                        R.id.key_8,
                        R.id.key_9,
                        R.id.key_0
                )
        for (id in numIds) {
            val btn = view.findViewById<Button>(id)
            allKeys.add(btn)
            addPressAnimation(btn)
            btn.setOnClickListener { onStandardKeyClick(btn.text.toString()) }
        }

        val row2Ids =
                listOf(
                        R.id.key_at,
                        R.id.key_hash,
                        R.id.key_dollar,
                        R.id.key_percent,
                        R.id.key_amp,
                        R.id.key_minus,
                        R.id.key_plus,
                        R.id.key_lparen,
                        R.id.key_rparen
                )
        for (id in row2Ids) {
            val btn = view.findViewById<Button>(id)
            allKeys.add(btn)
            addPressAnimation(btn)
            btn.setOnClickListener { onStandardKeyClick(btn.text.toString()) }
        }

        val keySymbols = view.findViewById<Button>(R.id.key_symbols)
        if (keySymbols != null) {
            specialKeys.add(keySymbols)
            addPressAnimation(keySymbols)
            keySymbols.setOnClickListener { onSwitchToSymbols() }
        }

        val row3Ids =
                listOf(
                        R.id.key_asterisk,
                        R.id.key_quote,
                        R.id.key_apos,
                        R.id.key_colon,
                        R.id.key_semicolon,
                        R.id.key_excl,
                        R.id.key_quest
                )
        for (id in row3Ids) {
            val btn = view.findViewById<Button>(id)
            allKeys.add(btn)
            addPressAnimation(btn)
            btn.setOnClickListener { onStandardKeyClick(btn.text.toString()) }
        }

        val space = view.findViewById<Button>(R.id.key_num_space)
        allKeys.add(space)
        addPressAnimation(space)
        space.setOnClickListener { onSpace() }

        val backspace = view.findViewById<ImageButton>(R.id.key_num_backspace)
        specialKeys.add(backspace)
        setupBackspaceImageButton(backspace, onBackspace)

        val enter = view.findViewById<ImageButton>(R.id.key_num_enter)
        specialKeys.add(enter)
        addPressAnimation(enter)
        enter.setOnClickListener { onEnter() }

        val keyAbc = view.findViewById<Button>(R.id.key_abc)
        keyAbc.setOnClickListener { onSwitchToQwerty() }
        addPressAnimation(keyAbc)
        specialKeys.add(keyAbc)

        val keyComma = view.findViewById<Button>(R.id.key_num_comma)
        allKeys.add(keyComma)
        keyComma.setOnClickListener { inputHandler.commitText(",") }
        addPressAnimation(keyComma)

        val keyPeriod = view.findViewById<Button>(R.id.key_num_period)
        allKeys.add(keyPeriod)
        keyPeriod.setOnClickListener { inputHandler.commitText(".") }
        addPressAnimation(keyPeriod)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSymbolKeys(
            onSwitchToNumbers: () -> Unit,
            onSpace: () -> Unit,
            onEnter: () -> Unit,
            onBackspace: () -> Unit,
            onTab: () -> Unit
    ) {
        val view = keyboardView ?: return
        allKeys.clear()
        specialKeys.clear()

        val symbolIds =
                listOf(
                        R.id.key_lbracket,
                        R.id.key_rbracket,
                        R.id.key_lbrace,
                        R.id.key_rbrace,
                        R.id.key_hash_sym,
                        R.id.key_percent_sym,
                        R.id.key_caret,
                        R.id.key_asterisk_sym,
                        R.id.key_plus_sym,
                        R.id.key_equals_sym,
                        R.id.key_underscore,
                        R.id.key_backslash,
                        R.id.key_pipe,
                        R.id.key_tilde,
                        R.id.key_lt,
                        R.id.key_gt,
                        R.id.key_dollar_sym,
                        R.id.key_pound,
                        R.id.key_euro,
                        R.id.key_yen,
                        R.id.key_div,
                        R.id.key_mult,
                        R.id.key_plusminus,
                        R.id.key_neq,
                        R.id.key_approx,
                        R.id.key_inf,
                        R.id.key_degree
                )

        for (id in symbolIds) {
            val btn = view.findViewById<Button>(id)
            if (btn != null) {
                allKeys.add(btn)
                addPressAnimation(btn)
                btn.setOnClickListener { onStandardKeyClick(btn.text.toString()) }
            }
        }

        val tab = view.findViewById<Button>(R.id.key_tab)
        if (tab != null) {
            specialKeys.add(tab)
            addPressAnimation(tab)
            tab.setOnClickListener { onTab() }
        }

        val backspace = view.findViewById<ImageButton>(R.id.key_sym_backspace)
        if (backspace != null) {
            specialKeys.add(backspace)
            setupBackspaceImageButton(backspace, onBackspace)
        }

        val key123 = view.findViewById<Button>(R.id.key_sym_123)
        if (key123 != null) {
            specialKeys.add(key123)
            addPressAnimation(key123)
            key123.setOnClickListener { onSwitchToNumbers() }
        }

        val left = view.findViewById<Button>(R.id.key_arrow_left)
        if (left != null) {
            specialKeys.add(left)
            addPressAnimation(left)
            left.setOnClickListener {
                inputHandler.sendKeyEvent(
                        KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT)
                )
                inputHandler.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT))
            }
        }

        val right = view.findViewById<Button>(R.id.key_arrow_right)
        if (right != null) {
            specialKeys.add(right)
            addPressAnimation(right)
            right.setOnClickListener {
                inputHandler.sendKeyEvent(
                        KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT)
                )
                inputHandler.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT))
            }
        }

        val space = view.findViewById<Button>(R.id.key_sym_space)
        if (space != null) {
            allKeys.add(space)
            addPressAnimation(space)
            space.setOnClickListener { onSpace() }
        }

        val enter = view.findViewById<ImageButton>(R.id.key_sym_enter)
        if (enter != null) {
            specialKeys.add(enter)
            addPressAnimation(enter)
            enter.setOnClickListener { onEnter() }
        }
    }

    private var isDeleting = false
    private var currentBackspaceRunnable: Runnable? = null

    private fun createDeleteRunnable(onBackspace: () -> Unit): Runnable {
        return object : Runnable {
            override fun run() {
                if (isDeleting) {
                    onBackspace()
                    handler.postDelayed(this, 50)
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupBackspaceImageButton(backspace: ImageButton, onBackspace: () -> Unit) {
        allKeys.add(backspace)
        backspace.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    backspace.animate().scaleX(0.9f).scaleY(0.9f).setDuration(50).start()
                    isDeleting = true
                    onBackspace()
                    currentBackspaceRunnable = createDeleteRunnable(onBackspace)
                    handler.postDelayed(currentBackspaceRunnable!!, 350)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    backspace.animate().scaleX(1f).scaleY(1f).setDuration(50).start()
                    isDeleting = false
                    currentBackspaceRunnable?.let { handler.removeCallbacks(it) }
                    currentBackspaceRunnable = null
                    true
                }
                else -> false
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addPressAnimation(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(50).start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(50).start()
                }
            }
            false
        }
    }

    fun applyTheme() {
        val bgColorStr = preferences.backgroundColor
        val keyColorStr = preferences.keyColor

        val bgColor =
                try {
                    Color.parseColor(bgColorStr)
                } catch (e: Exception) {
                    Color.BLACK
                }
        val keyColor =
                try {
                    Color.parseColor(keyColorStr)
                } catch (e: Exception) {
                    Color.DKGRAY
                }

        currentKeyColor = keyColor

        keyboardView?.setBackgroundColor(bgColor)

        applyKeyColors()
    }

    private fun applyKeyColors() {
        if (currentKeyColor == 0) return

        for (key in allKeys) {
            val drawable =
                    GradientDrawable().apply {
                        setColor(currentKeyColor)
                        cornerRadius = 24f
                    }
            key.background = drawable

            if (key is TextView) {
                key.setTextColor(Color.WHITE)
            } else if (key is ImageButton) {
                key.setColorFilter(Color.WHITE)
            }
        }

        val specialColor = currentKeyColor
        for (key in specialKeys) {
            val drawable =
                    GradientDrawable().apply {
                        setColor(specialColor)
                        cornerRadius = 24f
                    }
            key.background = drawable

            if (key is TextView) {
                key.setTextColor(Color.WHITE)
            } else if (key is ImageButton) {
                key.setColorFilter(Color.WHITE)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupKeyWithPopup(btn: Button) {
        btn.setOnTouchListener { v, event ->
            val char = btn.text.toString()
            val variants =
                    PopupVariants.getVariants(char, preferences.language, false) // Simple check

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    currentLongPressRunnable = Runnable {
                        if (variants.isNotEmpty()) {
                            showPopup(btn, variants)
                        }
                    }
                    handler.postDelayed(currentLongPressRunnable!!, 250)
                    v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(50).start()
                    v.isPressed = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isPopupActive) {
                        handlePopupTouch(event.rawX, event.rawY)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    currentLongPressRunnable?.let { handler.removeCallbacks(it) }
                    v.animate().scaleX(1f).scaleY(1f).setDuration(50).start()
                    v.isPressed = false

                    if (isPopupActive) {
                        val selected = getSelectedPopupItem()
                        if (selected != null) {
                            onStandardKeyClick(selected)
                        }
                        dismissPopup()
                    } else {
                        onStandardKeyClick(char)
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun showPopup(anchor: View, variants: List<String>) {
        isPopupActive = true
        currentPopupVariants = variants

        val popupView =
                layoutInflater.inflate(R.layout.layout_key_popup, null) as
                        androidx.cardview.widget.CardView

        popupView.setCardBackgroundColor(currentKeyColor)

        val container = popupView.findViewById<LinearLayout>(R.id.popupContainer)
        popupButtons.clear()

        for (variant in variants) {
            val tv = TextView(context)
            tv.text = variant
            tv.textSize = 26f
            tv.setTypeface(null, Typeface.BOLD)
            tv.setTextColor(Color.WHITE)
            tv.setPadding(35, 25, 35, 25)
            tv.gravity = android.view.Gravity.CENTER

            val lp =
                    LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    )
            lp.setMargins(10, 0, 10, 0)
            tv.layoutParams = lp

            container.addView(tv)
            popupButtons.add(tv)
        }

        popupView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        val popupWidth = popupView.measuredWidth
        val popupHeight = popupView.measuredHeight

        val xOffset = (anchor.width - popupWidth) / 2
        val yOffset = -anchor.height - popupHeight - 80

        currentPopupWindow =
                PopupWindow(
                        popupView,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        currentPopupWindow?.isTouchable = false
        currentPopupWindow?.isFocusable = false
        currentPopupWindow?.setBackgroundDrawable(
                android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
        )
        currentPopupWindow?.elevation = 20f

        try {
            currentPopupWindow?.showAsDropDown(anchor, xOffset, yOffset)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Highlight middle item initially
        if (popupButtons.isNotEmpty()) {
            val initialIndex = (popupButtons.size - 1) / 2
            val tv = popupButtons[initialIndex]
            val drawable =
                    GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 20f
                        setColor(Color.WHITE)
                    }
            tv.background = drawable
            tv.setTextColor(Color.BLACK)
        }
    }

    private fun handlePopupTouch(x: Float, y: Float) {
        var closestTv: TextView? = null
        var minUuidDist = Float.MAX_VALUE

        for (tv in popupButtons) {
            val loc = IntArray(2)
            tv.getLocationOnScreen(loc)
            val left = loc[0]
            val right = left + tv.width
            val centerX = (left + right) / 2f

            val dist = kotlin.math.abs(x - centerX)

            if (dist < minUuidDist) {
                if (dist < (tv.width * 1.5)) {
                    minUuidDist = dist
                    closestTv = tv
                }
            }
        }

        for (tv in popupButtons) {
            if (tv == closestTv) {
                val drawable =
                        GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = 20f
                            setColor(Color.WHITE)
                        }
                tv.background = drawable
                tv.setTextColor(Color.BLACK)
            } else {
                tv.background = null
                tv.setTextColor(Color.WHITE)
            }
        }
    }

    private fun getSelectedPopupItem(): String? {
        for (tv in popupButtons) {
            if (tv.currentTextColor == Color.BLACK) {
                return tv.text.toString()
            }
        }
        return null
    }

    private fun dismissPopup() {
        currentPopupWindow?.dismiss()
        currentPopupWindow = null
        isPopupActive = false
        popupButtons.clear()
    }

    fun updateShiftState(isShifted: Boolean) {
        val shiftButton = keyboardView?.findViewById<ImageButton>(R.id.key_shift)
        shiftButton?.setImageResource(
                if (isShifted) R.drawable.ic_shift_active else R.drawable.ic_shift
        )
        for (btn in letterKeys) {
            val text = btn.text.toString()
            btn.text = if (isShifted) text.uppercase() else text.lowercase()
        }
    }
}
