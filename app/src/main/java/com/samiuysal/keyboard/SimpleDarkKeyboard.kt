package com.samiuysal.keyboard

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat
import androidx.core.view.inputmethod.InputContentInfoCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import java.io.File
import java.util.concurrent.Executors
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SimpleDarkKeyboard : InputMethodService() {

    companion object {
        const val ACTION_THEME_CHANGED = "com.samiuysal.keyboard.THEME_CHANGED"
        val GIPHY_API_KEY: String = BuildConfig.GIPHY_API_KEY
        private const val TAG = "SimpleDarkKeyboard"
    }

    private var composingText = StringBuilder()
    private val predictionEngine by lazy { PredictionEngine(this) }

    private var isShifted = false
    private var isCapsLocked = false
    private var currentLayout = "qwerty"
    private lateinit var keyboardView: View
    private val letterKeys = mutableListOf<Button>()
    private val allKeys = mutableListOf<View>()
    private var shiftButton: ImageButton? = null

    private var soundEnabled = true

    private var emojiAdapter: EmojiAdapter? = null
    private var gifAdapter: GifAdapter? = null

    private lateinit var clipboardManager: KeyboardClipboardManager

    private var clipboardView: View? = null
    private var translationView: View? = null
    private var translationInputField: TextView? = null
    private var isTranslationMode = false

    private lateinit var mainKeyboardFrame: FrameLayout
    private lateinit var translationBarContainer: FrameLayout
    private lateinit var suggestionsStrip: LinearLayout
    private lateinit var toolsPanel: View
    private lateinit var toolbarManager: ToolbarManager

    private var currentLanguage = "tr"
    private var currentKeyColor = Color.parseColor("#3A3A3C")
    private var isPopupActive = false
    private var isGifSearchMode = false
    private val gifSearchQuery = StringBuilder()
    private var gifSearchInput: TextView? = null
    private var currentPopupWindow: android.widget.PopupWindow? = null
    private var currentLongPressRunnable: Runnable? = null
    private var currentPopupVariants: List<String> = emptyList()
    private val popupButtons = mutableListOf<TextView>()
    private val specialKeys = mutableListOf<View>()
    private var lastPopupX = 0f
    private var lastPopupY = 0f

    private val popupMapTr =
            mapOf(
                    "c" to listOf("ç"),
                    "s" to listOf("ş"),
                    "g" to listOf("ğ"),
                    "u" to listOf("ü"),
                    "o" to listOf("ö"),
                    "i" to listOf("ı", "İ")
            )

    private val popupMapEn =
            mapOf(
                    "e" to listOf("é", "è", "ê", "ë"),
                    "a" to listOf("à", "á", "â", "ä"),
                    "u" to listOf("ù", "ú", "û", "ü"),
                    "i" to listOf("ì", "í", "î", "ï"),
                    "o" to listOf("ò", "ó", "ô", "ö"),
                    "c" to listOf("ç"),
                    "n" to listOf("ñ")
            )
    private val giphyApi: GiphyApi by lazy {
        Retrofit.Builder()
                .baseUrl("https://api.giphy.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GiphyApi::class.java)
    }

    private val executor = Executors.newSingleThreadExecutor()
    val handler = Handler(Looper.getMainLooper())
    private var isDeleting = false
    private val deleteRunnable =
            object : Runnable {
                override fun run() {
                    if (isDeleting) {
                        handleBackspace()
                        handler.postDelayed(this, 50)
                    }
                }
            }

    private val themeReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == ACTION_THEME_CHANGED) {
                        applyTheme()
                    }
                }
            }

    private val gifCacheDir: File by lazy {
        File(cacheDir, "images").apply { if (!exists()) mkdirs() }
    }

    fun commitSuggestion(word: String) {
        val inputConnection = currentInputConnection ?: return
        inputConnection.commitText("$word ", 1)

        val prevWord = composingText.toString()
        predictionEngine.learn(word)

        composingText.clear()

        toolbarManager.updateSuggestions(word, isNewWord = true)
    }

    override fun onCreate() {
        super.onCreate()
        clipboardManager = KeyboardClipboardManager(this)
        ContextCompat.registerReceiver(
                this,
                themeReceiver,
                IntentFilter(ACTION_THEME_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onCreateCandidatesView(): View? = null

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)

        val mimeTypes = arrayOf("image/gif")
        EditorInfoCompat.setContentMimeTypes(
                info ?: android.view.inputmethod.EditorInfo(),
                mimeTypes
        )
    }

    fun openClipboardFeature() {
        closeGifSearch()
        val parent = mainKeyboardFrame.parent as? android.view.ViewGroup
        if (parent != null) {
            val transition = android.transition.Fade()
            transition.duration = 200
            android.transition.TransitionManager.beginDelayedTransition(parent, transition)
        }
        val keyboardHeight = keyboardView.height
        val suggestionsHeight = suggestionsStrip.height

        clipboardView = layoutInflater.inflate(R.layout.keyboard_clipboard, null)
        clipboardView!!.layoutParams =
                FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        if (keyboardHeight > 0) keyboardHeight + suggestionsHeight else 600
                )

        val recycler = clipboardView!!.findViewById<RecyclerView>(R.id.clipboardList)
        recycler.layoutManager = LinearLayoutManager(this)

        val adapter =
                ClipboardAdapter(clipboardManager.getHistory()) { text ->
                    currentInputConnection?.commitText(text, 1)
                }
        recycler.adapter = adapter

        clipboardView!!.findViewById<View>(R.id.btnCloseClipboard).setOnClickListener {
            suggestionsStrip.visibility = View.VISIBLE
            switchToQwerty()
        }

        mainKeyboardFrame.removeAllViews()
        mainKeyboardFrame.addView(clipboardView)

        suggestionsStrip.visibility = View.GONE
        toolbarManager.hideToolsPanel()
        applyTheme()
    }

    fun openTranslationFeature() {
        closeGifSearch()
        if (translationView == null) {
            translationView = layoutInflater.inflate(R.layout.keyboard_translation, null)

            translationView!!.findViewById<View>(R.id.btnCloseTranslate).setOnClickListener {
                closeTranslation()
            }

            val languages = listOf("TR", "EN", "ES", "DE", "FR", "AR", "RU", "ZH", "JA", "KO")
            val langCodes =
                    listOf(
                            TranslateLanguage.TURKISH,
                            TranslateLanguage.ENGLISH,
                            TranslateLanguage.SPANISH,
                            TranslateLanguage.GERMAN,
                            TranslateLanguage.FRENCH,
                            TranslateLanguage.ARABIC,
                            TranslateLanguage.RUSSIAN,
                            TranslateLanguage.CHINESE,
                            TranslateLanguage.JAPANESE,
                            TranslateLanguage.KOREAN
                    )

            val adapter = ArrayAdapter(this, R.layout.spinner_item_white, languages)
            adapter.setDropDownViewResource(R.layout.spinner_item_white)

            val spinnerSource = translationView!!.findViewById<Spinner>(R.id.spinnerSourceLang)
            val spinnerTarget = translationView!!.findViewById<Spinner>(R.id.spinnerTargetLang)

            spinnerSource.adapter = adapter
            spinnerTarget.adapter = adapter

            spinnerSource.setSelection(0)
            spinnerTarget.setSelection(1)

            translationView!!.findViewById<View>(R.id.btnSwapLangs).setOnClickListener {
                val sourcePos = spinnerSource.selectedItemPosition
                val targetPos = spinnerTarget.selectedItemPosition
                spinnerSource.setSelection(targetPos)
                spinnerTarget.setSelection(sourcePos)
            }

            val inputDisplay = translationView!!.findViewById<TextView>(R.id.txtTranslateInput)
            val translateBtn = translationView!!.findViewById<Button>(R.id.btnTranslate)
            val clearBtn = translationView!!.findViewById<View>(R.id.btnClearInput)

            // Set blue border to indicate translation is active
            inputDisplay.setBackgroundResource(R.drawable.bg_translation_active)

            clearBtn.setOnClickListener { inputDisplay.text = "" }

            translateBtn.setOnClickListener {
                val input = inputDisplay.text.toString().trim()
                if (input.isNotEmpty()) {
                    translateBtn.isEnabled = false
                    translateBtn.text = "..."

                    val sourceLang = langCodes[spinnerSource.selectedItemPosition]
                    val targetLang = langCodes[spinnerTarget.selectedItemPosition]

                    translateText(input, sourceLang, targetLang) { result ->
                        currentInputConnection?.commitText(result, 1)

                        inputDisplay.text = ""
                        translateBtn.isEnabled = true
                        translateBtn.text = "Çevir"
                    }
                } else {
                    Toast.makeText(this, "Metin girin", Toast.LENGTH_SHORT).show()
                }
            }
        }

        translationInputField = translationView!!.findViewById(R.id.txtTranslateInput)
        isTranslationMode = true

        val parent = translationBarContainer.parent as? android.view.ViewGroup
        if (parent != null) {
            val transition = android.transition.Fade()
            transition.duration = 200
            android.transition.TransitionManager.beginDelayedTransition(parent, transition)
        }

        translationBarContainer.removeAllViews()
        translationBarContainer.addView(translationView)
        translationBarContainer.visibility = View.VISIBLE

        suggestionsStrip.visibility = View.GONE
        toolbarManager.hideToolsPanel()
        applyTheme()
    }

    private fun closeTranslation() {
        val parent = translationBarContainer.parent as? android.view.ViewGroup
        if (parent != null) {
            val transition = android.transition.Fade()
            transition.duration = 200
            android.transition.TransitionManager.beginDelayedTransition(parent, transition)
        }

        isTranslationMode = false
        translationInputField = null
        translationBarContainer.visibility = View.GONE
        translationBarContainer.removeAllViews()

        suggestionsStrip.visibility = View.VISIBLE
    }

    private fun translateText(
            text: String,
            sourceLang: String,
            targetLang: String,
            callback: (String) -> Unit
    ) {
        val options =
                TranslatorOptions.Builder()
                        .setSourceLanguage(sourceLang)
                        .setTargetLanguage(targetLang)
                        .build()

        val translator = Translation.getClient(options)

        val conditions = DownloadConditions.Builder().build()

        translator
                .downloadModelIfNeeded(conditions)
                .addOnSuccessListener {
                    translator
                            .translate(text)
                            .addOnSuccessListener { translatedText ->
                                handler.post { callback(translatedText) }
                                translator.close()
                            }
                            .addOnFailureListener { exception ->
                                Log.e(TAG, "Translation failed", exception)
                                handler.post {
                                    Toast.makeText(
                                                    this,
                                                    "Çeviri hatası: ${exception.message}",
                                                    Toast.LENGTH_SHORT
                                            )
                                            .show()
                                    callback(text)
                                }
                                translator.close()
                            }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Model download failed", exception)
                    handler.post {
                        Toast.makeText(
                                        this,
                                        "Model indirilemedi. İnternet bağlantınızı kontrol edin.",
                                        Toast.LENGTH_SHORT
                                )
                                .show()
                        callback(text)
                    }
                    translator.close()
                }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(themeReceiver)
        executor.shutdown()
    }

    private fun applyTheme() {
        val prefs = getSharedPreferences("keyboard_prefs", Context.MODE_PRIVATE)

        currentLanguage = prefs.getString("selected_language", "tr") ?: "tr"

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            predictionEngine.loadLanguage(currentLanguage)
        }

        val bgColor = prefs.getString("bg_color", "#000000") ?: "#000000"
        val keyColor = prefs.getString("key_color", "#3A3A3C") ?: "#3A3A3C"

        val parsedBgColor = Color.parseColor(bgColor)
        val parsedKeyColor = Color.parseColor(keyColor)
        currentKeyColor = parsedKeyColor

        window.window?.let { win ->
            win.decorView.setBackgroundColor(parsedBgColor)
            @Suppress("DEPRECATION") win.navigationBarColor = parsedBgColor
        }

        keyboardView.setBackgroundColor(parsedBgColor)

        toolsPanel.setBackgroundColor(parsedBgColor)
        suggestionsStrip.setBackgroundColor(parsedBgColor)

        translationBarContainer?.setBackgroundColor(parsedBgColor)
        clipboardView?.setBackgroundColor(parsedBgColor)

        applyKeyColors()

        clipboardView?.findViewById<RecyclerView>(R.id.clipboardList)?.adapter?.let {
            if (it is ClipboardAdapter) {
                it.setTheme(parsedKeyColor, Color.WHITE)
            }
        }
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

    private fun switchToNumbers() {
        currentLayout = "numbers"
        keyboardView = layoutInflater.inflate(R.layout.keyboard_numbers, null)

        mainKeyboardFrame.removeAllViews()
        mainKeyboardFrame.addView(keyboardView)

        setupNumberKeys()
        applyTheme()
    }

    private fun switchToQwerty() {
        currentLayout = "qwerty"
        keyboardView = layoutInflater.inflate(R.layout.keyboard_layout, null)

        mainKeyboardFrame.removeAllViews()
        mainKeyboardFrame.addView(keyboardView)

        setupQwertyKeys()
        applyTheme()
        suggestionsStrip.visibility = View.VISIBLE
        toolbarManager.updateSuggestions(composingText.toString(), false)
    }

    private fun getStringByLocale(context: Context, resId: Int, localeCode: String): String {
        val resources = context.resources
        val config = android.content.res.Configuration(resources.configuration)
        config.setLocale(java.util.Locale(localeCode))
        return context.createConfigurationContext(config).getText(resId).toString()
    }

    private fun setupKeyWithPopup(btn: Button) {
        btn.setOnTouchListener { v, event ->
            val char = btn.text.toString()
            val lowerChar = char.lowercase(java.util.Locale.ENGLISH)

            val map = if (currentLanguage == "tr") popupMapTr else popupMapEn
            val variants = map[lowerChar]

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    currentLongPressRunnable = Runnable {
                        if (variants != null && variants.isNotEmpty()) {
                            val isUpper = char != lowerChar
                            val displayVariants =
                                    if (isUpper) {
                                        variants.map {
                                            if (it == "ı") "I"
                                            else if (it == "i") "İ" else it.uppercase()
                                        }
                                    } else {
                                        variants
                                    }
                            showPopup(btn, displayVariants)
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
                            sendKeyChar(selected)
                        }
                        dismissPopup()
                    } else {
                        sendKeyChar(char)
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
            val tv = TextView(this)
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
                android.widget.PopupWindow(
                        popupView,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
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

    private fun sendKeyChar(text: String) {
        if (isGifSearchMode) {
            handleGifSearchInput(text)
        } else if (isTranslationMode && translationInputField != null) {
            translationInputField!!.append(text)
        } else {
            composingText.append(text)
            currentInputConnection?.setComposingText(composingText, 1)
            toolbarManager.updateSuggestions(composingText.toString(), isNewWord = false)
        }
    }

    private fun handleSpace() {
        if (isGifSearchMode) {
            handleGifSearchInput(" ")
            return
        }
        if (isTranslationMode && translationInputField != null) {
            translationInputField!!.append(" ")
            return
        }

        if (composingText.isNotEmpty()) {
            val text = composingText.toString()
            currentInputConnection?.commitText("$text ", 1)
            predictionEngine.learn(text)
            composingText.clear()
            toolbarManager.updateSuggestions("", isNewWord = true)
        } else {
            currentInputConnection?.commitText(" ", 1)
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

    @SuppressLint("ClickableViewAccessibility")
    private fun setupQwertyKeys() {
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
            val btn = keyboardView.findViewById<Button>(id)
            letterKeys.add(btn)
            allKeys.add(btn)
            setupKeyWithPopup(btn)
        }

        val space = keyboardView.findViewById<Button>(R.id.key_space)
        allKeys.add(space)
        addPressAnimation(space)
        space.setOnClickListener { handleSpace() }

        val backspace = keyboardView.findViewById<ImageButton>(R.id.key_backspace)
        specialKeys.add(backspace)
        setupBackspaceImageButton(backspace)

        val enter = keyboardView.findViewById<ImageButton>(R.id.key_enter)
        specialKeys.add(enter)
        addPressAnimation(enter)
        enter.setOnClickListener { handleEnter() }

        val shift = keyboardView.findViewById<ImageButton>(R.id.key_shift)
        shiftButton = shift
        specialKeys.add(shift)
        addPressAnimation(shift)
        shift.setOnClickListener {
            isShifted = !isShifted
            updateShiftState()
        }

        val comma = keyboardView.findViewById<Button>(R.id.key_comma)
        allKeys.add(comma)
        addPressAnimation(comma)
        comma.setOnClickListener { sendKeyChar(",") }

        val period = keyboardView.findViewById<Button>(R.id.key_period)
        allKeys.add(period)
        addPressAnimation(period)
        period.setOnClickListener { sendKeyChar(".") }

        val key123 = keyboardView.findViewById<Button>(R.id.key_123)
        specialKeys.add(key123)
        addPressAnimation(key123)
        key123.setOnClickListener { switchToNumbers() }

        val emojiKey = keyboardView.findViewById<Button>(R.id.key_emoji)
        specialKeys.add(emojiKey)
        addPressAnimation(emojiKey)
        emojiKey.setOnClickListener { openEmojiBoard() }
        applyKeyColors()
    }

    private fun switchToSymbols() {
        currentLayout = "symbols"
        keyboardView = layoutInflater.inflate(R.layout.keyboard_symbols, null)
        mainKeyboardFrame.removeAllViews()
        mainKeyboardFrame.addView(keyboardView)

        setupSymbolKeys()
        applyTheme()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupNumberKeys() {
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
            val btn = keyboardView.findViewById<Button>(id)
            allKeys.add(btn)
            addPressAnimation(btn)
            btn.setOnClickListener { sendKeyChar(btn.text.toString()) }
        }

        // Row 2
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
            val btn = keyboardView.findViewById<Button>(id)
            allKeys.add(btn)
            addPressAnimation(btn)
            btn.setOnClickListener { sendKeyChar(btn.text.toString()) }
        }

        // Row 3
        val keySymbols = keyboardView.findViewById<Button>(R.id.key_symbols)
        if (keySymbols != null) {
            specialKeys.add(keySymbols)
            addPressAnimation(keySymbols)
            keySymbols.setOnClickListener { switchToSymbols() }
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
            val btn = keyboardView.findViewById<Button>(id)
            allKeys.add(btn)
            addPressAnimation(btn)
            btn.setOnClickListener { sendKeyChar(btn.text.toString()) }
        }

        val space = keyboardView.findViewById<Button>(R.id.key_num_space)
        allKeys.add(space)
        addPressAnimation(space)
        space.setOnClickListener { handleSpace() }

        val backspace = keyboardView.findViewById<ImageButton>(R.id.key_num_backspace)
        specialKeys.add(backspace)
        setupBackspaceImageButton(backspace)

        val enter = keyboardView.findViewById<ImageButton>(R.id.key_num_enter)
        specialKeys.add(enter)
        addPressAnimation(enter)
        enter.setOnClickListener { handleEnter() }

        val keyAbc = keyboardView.findViewById<Button>(R.id.key_abc)
        keyAbc.setOnClickListener { switchToQwerty() }
        addPressAnimation(keyAbc)
        specialKeys.add(keyAbc)

        val keyComma = keyboardView.findViewById<Button>(R.id.key_num_comma)
        allKeys.add(keyComma)
        keyComma.setOnClickListener { sendKeyChar(",") }
        addPressAnimation(keyComma)

        val keyPeriod = keyboardView.findViewById<Button>(R.id.key_num_period)
        allKeys.add(keyPeriod)
        keyPeriod.setOnClickListener { sendKeyChar(".") }
        addPressAnimation(keyPeriod)
        applyKeyColors()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSymbolKeys() {
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
            val btn = keyboardView.findViewById<Button>(id)
            if (btn != null) {
                allKeys.add(btn)
                addPressAnimation(btn)
                btn.setOnClickListener { sendKeyChar(btn.text.toString()) }
            }
        }

        val tab = keyboardView.findViewById<Button>(R.id.key_tab)
        if (tab != null) {
            specialKeys.add(tab)
            addPressAnimation(tab)
            tab.setOnClickListener { currentInputConnection?.commitText("\t", 1) }
        }

        val backspace = keyboardView.findViewById<ImageButton>(R.id.key_sym_backspace)
        if (backspace != null) {
            specialKeys.add(backspace)
            setupBackspaceImageButton(backspace)
        }

        val key123 = keyboardView.findViewById<Button>(R.id.key_sym_123)
        if (key123 != null) {
            specialKeys.add(key123)
            addPressAnimation(key123)
            key123.setOnClickListener { switchToNumbers() }
        }

        val left = keyboardView.findViewById<Button>(R.id.key_arrow_left)
        if (left != null) {
            specialKeys.add(left)
            addPressAnimation(left)
            left.setOnClickListener {
                currentInputConnection?.sendKeyEvent(
                        KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT)
                )
                currentInputConnection?.sendKeyEvent(
                        KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT)
                )
            }
        }

        val right = keyboardView.findViewById<Button>(R.id.key_arrow_right)
        if (right != null) {
            specialKeys.add(right)
            addPressAnimation(right)
            right.setOnClickListener {
                currentInputConnection?.sendKeyEvent(
                        KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT)
                )
                currentInputConnection?.sendKeyEvent(
                        KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT)
                )
            }
        }

        val space = keyboardView.findViewById<Button>(R.id.key_sym_space)
        if (space != null) {
            allKeys.add(space)
            addPressAnimation(space)
            space.setOnClickListener { currentInputConnection?.commitText(" ", 1) }
        }

        val enter = keyboardView.findViewById<ImageButton>(R.id.key_sym_enter)
        if (enter != null) {
            specialKeys.add(enter)
            addPressAnimation(enter)
            enter.setOnClickListener { handleEnter() }
        }

        applyKeyColors()
    }

    private fun updateShiftState() {
        shiftButton?.setImageResource(
                if (isShifted) R.drawable.ic_shift_active else R.drawable.ic_shift
        )
        for (btn in letterKeys) {
            val text = btn.text.toString()
            btn.text = if (isShifted) text.uppercase() else text.lowercase()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupBackspaceButton(backspace: Button) {
        allKeys.add(backspace)
        backspace.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDeleting = true
                    handleBackspace()
                    handler.postDelayed(deleteRunnable, 350)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isDeleting = false
                    handler.removeCallbacks(deleteRunnable)
                }
            }
            false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupBackspaceImageButton(backspace: ImageButton) {
        allKeys.add(backspace)
        backspace.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    backspace.animate().scaleX(0.9f).scaleY(0.9f).setDuration(50).start()
                    isDeleting = true
                    handleBackspace()
                    handler.postDelayed(deleteRunnable, 350)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    backspace.animate().scaleX(1f).scaleY(1f).setDuration(50).start()
                    isDeleting = false
                    handler.removeCallbacks(deleteRunnable)
                }
            }
            false
        }
    }

    private fun onKeyPress(text: String) {
        val char = if (isShifted) text.uppercase() else text.lowercase()
        sendText(char)
        if (isShifted && !isCapsLocked) {
            isShifted = false
            updateShiftState()
        }
    }

    private fun sendText(text: String) {
        if (isGifSearchMode) {
            handleGifSearchInput(text)
        } else if (isTranslationMode && translationInputField != null) {
            translationInputField!!.append(text)
        } else {
            composingText.append(text)
            currentInputConnection?.setComposingText(composingText, 1)

            toolbarManager.updateSuggestions(composingText.toString(), isNewWord = false)
        }
    }

    private fun handleGifSearchInput(text: String) {
        gifSearchQuery.append(text)
        updateGifSearchUI()
    }

    private fun handleGifSearchBackspace() {
        if (gifSearchQuery.isNotEmpty()) {
            gifSearchQuery.deleteCharAt(gifSearchQuery.length - 1)
            updateGifSearchUI()
        }
    }

    private fun updateGifSearchUI() {
        val query = gifSearchQuery.toString()
        gifSearchInput?.text = query
        if (query.isNotEmpty()) {
            onGifSearchUpdate?.invoke(query)
        } else {
            val countryCode = currentLanguage.uppercase()
            GiphyManager.getTrendingGifs(countryCode = countryCode) { gifs ->
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    val recyclerGifResults =
                            gifStripContainer.findViewById<RecyclerView>(R.id.recycler_gif_results)
                    recyclerGifResults.adapter =
                            GifAdapter(gifs) { url, progressBar ->
                                downloadAndShareGif(url, progressBar)
                            }
                }
            }
        }
    }

    private fun handleBackspace() {
        if (isGifSearchMode) {
            handleGifSearchBackspace()
            return
        }

        if (isTranslationMode && translationInputField != null) {
            val currentText = translationInputField!!.text.toString()
            if (currentText.isNotEmpty()) {
                translationInputField!!.text = currentText.dropLast(1)
            }
        } else {
            if (composingText.isNotEmpty()) {
                composingText.deleteCharAt(composingText.length - 1)

                if (composingText.isEmpty()) {
                    currentInputConnection?.commitText("", 1) // Clear composing
                    toolbarManager.updateSuggestions("", isNewWord = true)
                } else {
                    currentInputConnection?.setComposingText(composingText, 1)
                    toolbarManager.updateSuggestions(composingText.toString(), isNewWord = false)
                }
            } else {
                currentInputConnection?.deleteSurroundingText(1, 0)
                toolbarManager.updateSuggestions("", isNewWord = true)
            }
        }
    }

    private fun handleEnter() {
        if (isGifSearchMode) {
            closeGifSearch()
            return
        }

        if (isTranslationMode && translationInputField != null) {
            translationView?.findViewById<View>(R.id.btnTranslate)?.performClick()
        } else {
            if (composingText.isNotEmpty()) {
                val text = composingText.toString()
                currentInputConnection?.commitText(text, 1)

                predictionEngine.learn(text)

                composingText.clear()
                toolbarManager.updateSuggestions("", isNewWord = true)
            }

            currentInputConnection?.sendKeyEvent(
                    android.view.KeyEvent(
                            android.view.KeyEvent.ACTION_DOWN,
                            android.view.KeyEvent.KEYCODE_ENTER
                    )
            )
            currentInputConnection?.sendKeyEvent(
                    android.view.KeyEvent(
                            android.view.KeyEvent.ACTION_UP,
                            android.view.KeyEvent.KEYCODE_ENTER
                    )
            )
        }
    }

    fun openEmojiBoard() {
        closeGifSearch()
        val parent = mainKeyboardFrame.parent as? android.view.ViewGroup
        if (parent != null) {
            val transition = android.transition.Fade()
            transition.duration = 200
            android.transition.TransitionManager.beginDelayedTransition(parent, transition)
        }

        val emojiView = layoutInflater.inflate(R.layout.layout_emoji_board, null)
        val keyboardHeight = keyboardView.height
        val suggestionsHeight = suggestionsStrip.height

        val prefs = getSharedPreferences("keyboard_prefs", Context.MODE_PRIVATE)
        val bgColor =
                android.graphics.Color.parseColor(
                        prefs.getString("bg_color", "#1C1C1E") ?: "#1C1C1E"
                )
        emojiView.setBackgroundColor(bgColor)

        emojiView.layoutParams =
                FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        if (keyboardHeight > 0) keyboardHeight + suggestionsHeight else 600
                )

        val recyclerCategories =
                emojiView.findViewById<RecyclerView>(R.id.recycler_emoji_categories)
        val recyclerEmojis = emojiView.findViewById<RecyclerView>(R.id.recycler_emojis)

        val iconEmojiActive =
                emojiView.findViewById<android.widget.ImageView>(R.id.icon_emoji_active)
        val btnOpenGif = emojiView.findViewById<View>(R.id.btn_open_gif)
        val btnCloseEmoji = emojiView.findViewById<View>(R.id.btn_close_emoji)

        recyclerCategories.layoutManager =
                LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val emojiLayoutManager = GridLayoutManager(this, 8)
        recyclerEmojis.layoutManager = emojiLayoutManager

        fun setupEmojiGrid() {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                val emojis = EmojiManager.getEmojis(this@SimpleDarkKeyboard)
                val localizedCategories =
                        EmojiManager.CATEGORY_ORDER
                                .map { key ->
                                    CategoryItem(
                                            key,
                                            run {
                                                val resId = EmojiManager.getCategoryResId(key)
                                                if (resId != 0) {
                                                    getStringByLocale(
                                                            this@SimpleDarkKeyboard,
                                                            resId,
                                                            currentLanguage
                                                    )
                                                } else {
                                                    key
                                                }
                                            }
                                    )
                                }
                                .filter { emojis.containsKey(it.key) }

                if (localizedCategories.isNotEmpty()) {
                    val initialCategory = localizedCategories[0]

                    val emojiAdapter =
                            EmojiAdapter(emojis[initialCategory.key] ?: emptyList()) { emojiData ->
                                sendText(emojiData.char)
                            }
                    recyclerEmojis.adapter = emojiAdapter

                    val categoryAdapter =
                            CategoryAdapter(localizedCategories) { categoryKey ->
                                val newEmojis = emojis[categoryKey] ?: emptyList()
                                emojiAdapter.updateEmojis(newEmojis)
                            }
                    recyclerCategories.adapter = categoryAdapter
                }
            }
        }

        setupEmojiGrid()

        btnOpenGif.setOnClickListener {
            switchToQwerty()
            openGifSearchStrip()
        }

        btnCloseEmoji.setOnClickListener { switchToQwerty() }

        mainKeyboardFrame.removeAllViews()
        mainKeyboardFrame.addView(emojiView)
        suggestionsStrip.visibility = View.GONE
        toolbarManager.hideToolsPanel()
    }

    private lateinit var gifStripContainer: FrameLayout

    override fun onCreateInputView(): View {
        val mainContainer =
                layoutInflater.inflate(R.layout.layout_main_container, null) as LinearLayout
        toolbarManager = ToolbarManager(this, mainContainer)
        toolbarManager.setEngine(predictionEngine)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            predictionEngine.loadLanguage(currentLanguage)
        }

        mainKeyboardFrame = mainContainer.findViewById(R.id.main_keyboard_view)
        translationBarContainer = mainContainer.findViewById(R.id.translation_bar_container)
        suggestionsStrip = mainContainer.findViewById(R.id.suggestions_strip)
        gifStripContainer = mainContainer.findViewById(R.id.gif_strip_container)
        toolsPanel = mainContainer.findViewById(R.id.tools_panel)
        switchToQwerty()
        return mainContainer
    }

    private fun openGifSearchStrip() {
        suggestionsStrip.visibility = View.GONE
        gifStripContainer.removeAllViews()
        gifStripContainer.visibility = View.VISIBLE

        val params = gifStripContainer.layoutParams
        params.height =
                (resources.displayMetrics.density * 160).toInt()
        gifStripContainer.layoutParams = params

        val gifStripView = layoutInflater.inflate(R.layout.layout_gif_strip, null)
        gifStripContainer.addView(gifStripView)

        val inputGifSearch =
                gifStripView.findViewById<TextView>(
                        R.id.input_gif_search_strip
                )
        val btnCloseGif = gifStripView.findViewById<View>(R.id.btn_close_gif_strip)
        val recyclerGifResults = gifStripView.findViewById<RecyclerView>(R.id.recycler_gif_results)

        val prefs = getSharedPreferences("keyboard_prefs", Context.MODE_PRIVATE)
        val bgColor =
                android.graphics.Color.parseColor(
                        prefs.getString("bg_color", "#1C1C1E") ?: "#1C1C1E"
                )
        gifStripView.setBackgroundColor(bgColor)

        recyclerGifResults.layoutManager = GridLayoutManager(this, 3)

        val countryCode = currentLanguage.uppercase()
        GiphyManager.getTrendingGifs(countryCode = countryCode) { gifs ->
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                recyclerGifResults.adapter =
                        GifAdapter(gifs) { url, progressBar ->
                            downloadAndShareGif(url, progressBar)
                        }
            }
        }

        fun performGifSearch(query: String) {
            val country = currentLanguage.uppercase()
            GiphyManager.searchGifs(query, currentLanguage, country) { gifs ->
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                    recyclerGifResults.adapter =
                            GifAdapter(gifs) { url, progressBar ->
                                downloadAndShareGif(url, progressBar)
                            }
                }
            }
        }

        isGifSearchMode = true
        gifSearchInput = inputGifSearch
        gifSearchQuery.clear()
        inputGifSearch.text = ""

        this.onGifSearchUpdate = { query -> performGifSearch(query) }

        btnCloseGif.setOnClickListener { closeGifSearch() }
    }

    private var onGifSearchUpdate: ((String) -> Unit)? = null

    private fun closeGifSearch() {
        if (!isGifSearchMode) return

        gifStripContainer.visibility = View.GONE
        gifStripContainer.removeAllViews()

        suggestionsStrip.visibility = View.VISIBLE
        isGifSearchMode = false
        gifSearchInput = null
        onGifSearchUpdate = null
        gifSearchQuery.clear()

        val currentText = composingText.toString()
        if (currentText.isNotEmpty()) {
            toolbarManager.updateSuggestions(currentText, false)
        } else {
            toolbarManager.updateSuggestions("", true)
        }
    }

    private fun downloadAndShareGif(url: String, progressBar: ProgressBar) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val cachePath = File(cacheDir, "images")
                if (!cachePath.exists()) cachePath.mkdirs()

                val fileName = "share_${System.currentTimeMillis()}.gif"
                val file = File(cachePath, fileName)

                val fileBytes =
                        Glide.with(this@SimpleDarkKeyboard).asFile().load(url).submit().get()

                fileBytes.copyTo(file, overwrite = true)

                val contentUri: Uri =
                        FileProvider.getUriForFile(
                                this@SimpleDarkKeyboard,
                                "${packageName}.fileprovider",
                                file
                        )

                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    shareContent(contentUri, "image/gif")
                    progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@SimpleDarkKeyboard, "GIF gönderilemedi", Toast.LENGTH_SHORT)
                            .show()
                }
            }
        }
    }

    private fun shareContent(uri: Uri, mimeType: String) {
        val inputConnection = currentInputConnection ?: return
        val editorInfo = currentInputEditorInfo ?: return

        val description = ClipDescription("GIF Content", arrayOf(mimeType))
        val inputContentInfo = InputContentInfoCompat(uri, description, null)

        val flags = InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION

        InputConnectionCompat.commitContent(
                inputConnection,
                editorInfo,
                inputContentInfo,
                flags,
                null
        )
    }
}
