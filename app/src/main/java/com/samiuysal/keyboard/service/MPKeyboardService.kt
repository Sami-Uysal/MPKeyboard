package com.samiuysal.keyboard.service

import android.content.BroadcastReceiver
import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
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
import com.samiuysal.keyboard.BuildConfig
import com.samiuysal.keyboard.R
import com.samiuysal.keyboard.features.clipboard.ClipboardAdapter
import com.samiuysal.keyboard.features.clipboard.KeyboardClipboardManager
import com.samiuysal.keyboard.features.emoji.CategoryAdapter
import com.samiuysal.keyboard.features.emoji.CategoryItem
import com.samiuysal.keyboard.features.emoji.EmojiAdapter
import com.samiuysal.keyboard.features.emoji.EmojiManager
import com.samiuysal.keyboard.features.gif.GifAdapter
import com.samiuysal.keyboard.features.gif.GiphyApi
import com.samiuysal.keyboard.features.gif.GiphyManager
import com.samiuysal.keyboard.features.shortcuts.ShortcutManager
import com.samiuysal.keyboard.features.suggestions.PredictionEngine
import com.samiuysal.keyboard.features.toolbar.ToolbarManager
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@AndroidEntryPoint
class MPKeyboardService : InputMethodService() {

    private val serviceScope =
            kotlinx.coroutines.CoroutineScope(
                    kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main
            )

    @Inject lateinit var inputHandler: com.samiuysal.keyboard.features.keyboard.InputHandler
    @Inject lateinit var keyboardManager: com.samiuysal.keyboard.features.keyboard.KeyboardManager
    @Inject
    lateinit var keyboardPreferences: com.samiuysal.keyboard.core.preferences.KeyboardPreferences
    @Inject lateinit var passwordManager: com.samiuysal.keyboard.features.password.PasswordManager

    private lateinit var layoutManager:
            com.samiuysal.keyboard.features.keyboard.KeyboardLayoutManager

    companion object {
        const val ACTION_THEME_CHANGED = "com.samiuysal.keyboard.THEME_CHANGED"
        val GIPHY_API_KEY: String = BuildConfig.GIPHY_API_KEY
        private const val TAG = "MPKeyboardService"
    }

    private var composingText = StringBuilder()
    private val predictionEngine by lazy { PredictionEngine(this) }
    private val shortcutManager by lazy { ShortcutManager(this) }

    private var isShifted = false
    private var isCapsLocked = false
    private var currentLayout = "qwerty"
    private lateinit var keyboardView: View

    private var soundEnabled = true

    private var emojiAdapter: EmojiAdapter? = null
    private var gifAdapter: GifAdapter? = null

    private lateinit var clipboardManager: KeyboardClipboardManager

    private var clipboardView: View? = null
    private var passwordView: View? = null
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

    private var isGifSearchMode = false
    private val gifSearchQuery = StringBuilder()
    private var gifSearchInput: TextView? = null

    private var onGifSearchUpdate: ((String) -> Unit)? = null
    private lateinit var gifStripContainer: FrameLayout

    private val giphyApi: GiphyApi by lazy {
        Retrofit.Builder()
                .baseUrl("https://api.giphy.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GiphyApi::class.java)
    }

    private val executor = Executors.newSingleThreadExecutor()
    val handler = Handler(Looper.getMainLooper())

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

    override fun onStartInput(
            attribute: android.view.inputmethod.EditorInfo?,
            restarting: Boolean
    ) {
        super.onStartInput(attribute, restarting)
        inputHandler.updateConnection(currentInputConnection)
    }

    fun commitSuggestion(word: String) {
        inputHandler.commitText("$word ")

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

        layoutManager =
                com.samiuysal.keyboard.features.keyboard.KeyboardLayoutManager(
                        this,
                        layoutInflater,
                        inputHandler,
                        keyboardPreferences
                ) { text -> sendKeyChar(text) }
    }

    override fun onCreateCandidatesView(): View? = null

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)

        composingText.clear()

        val mimeTypes = arrayOf("image/gif")
        EditorInfoCompat.setContentMimeTypes(
                info ?: android.view.inputmethod.EditorInfo(),
                mimeTypes
        )
        applyTheme()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)

        composingText.clear()

        if (isGifSearchMode) {
            closeGifSearch()
        }

        if (::toolbarManager.isInitialized) {
            toolbarManager.updateSuggestions("", isNewWord = true)
        }
    }

    fun openClipboardFeature() {
        closeGifSearch()

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
                    inputHandler.commitText(text)
                }
        recycler.adapter = adapter

        clipboardView!!.findViewById<View>(R.id.btnCloseClipboard).setOnClickListener {
            suggestionsStrip.visibility = View.VISIBLE
            switchToQwerty()
        }

        mainKeyboardFrame.removeAllViews()
        mainKeyboardFrame.addView(clipboardView)

        suggestionsStrip.visibility = View.GONE
        toolbarManager.hideToolsPanel(animate = false)
        applyTheme()
    }

    fun openPasswordFeature() {
        closeGifSearch()

        val keyboardHeight = keyboardView.height
        val suggestionsHeight = suggestionsStrip.height

        passwordView = layoutInflater.inflate(R.layout.keyboard_password, null)
        passwordView!!.layoutParams =
                FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        if (keyboardHeight > 0) keyboardHeight + suggestionsHeight else 600
                )

        val recycler = passwordView!!.findViewById<RecyclerView>(R.id.recyclerPasswords)
        recycler.layoutManager = LinearLayoutManager(this)

        val emptyView = passwordView!!.findViewById<View>(R.id.txtEmptyPasswords)
        val currentPackage = getCurrentPackageName()

        val adapter =
                com.samiuysal.keyboard.features.password.PasswordPanelAdapter(
                        onFillUsername = { username -> inputHandler.commitText(username) },
                        onFillPassword = { password -> inputHandler.commitText(password) }
                )
        recycler.adapter = adapter

        // Load passwords for current package
        serviceScope.launch {
            com.samiuysal.keyboard.data.AppDatabase.getInstance(this@MPKeyboardService)
                    .passwordDao()
                    .getByPackageName(currentPackage)
                    .collect { passwords ->
                        handler.post {
                            if (passwords.isEmpty()) {
                                emptyView.visibility = View.VISIBLE
                                recycler.visibility = View.GONE
                            } else {
                                emptyView.visibility = View.GONE
                                recycler.visibility = View.VISIBLE
                                adapter.setPasswords(
                                        passwords.map { entity ->
                                            com.samiuysal.keyboard.data.password.Password(
                                                    id = entity.id,
                                                    siteName = entity.siteName,
                                                    packageName = entity.packageName,
                                                    username = entity.username,
                                                    password =
                                                            try {
                                                                com.samiuysal.keyboard.data.password
                                                                        .PasswordCrypto()
                                                                        .decrypt(
                                                                                entity.encryptedPassword
                                                                        )
                                                            } catch (e: Exception) {
                                                                ""
                                                            }
                                            )
                                        }
                                )
                            }
                        }
                    }
        }

        passwordView!!.findViewById<View>(R.id.btnClosePassword).setOnClickListener {
            suggestionsStrip.visibility = View.VISIBLE
            switchToQwerty()
        }

        passwordView!!.findViewById<View>(R.id.btnAddPassword).setOnClickListener {
            // Direct save to DB without opening settings
            val currentUrl = MPAccessibilityService.currentUrl
            val currentPkg =
                    MPAccessibilityService.currentPackage.ifEmpty { getCurrentPackageName() }
            val siteName =
                    if (currentUrl.isNotEmpty() && currentUrl != currentPkg) currentUrl
                    else currentPkg
            val currentText = currentInputConnection?.getTextBeforeCursor(100, 0)?.toString() ?: ""

            val inputType = currentInputEditorInfo?.inputType ?: 0
            val isPasswordField =
                    (inputType and android.text.InputType.TYPE_MASK_VARIATION) ==
                            android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                            (inputType and android.text.InputType.TYPE_MASK_VARIATION) ==
                                    android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                            (inputType and android.text.InputType.TYPE_MASK_VARIATION) ==
                                    android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

            val capturedUsername =
                    if (!isPasswordField && currentText.isNotEmpty()) currentText else ""
            val capturedPassword =
                    if (isPasswordField && currentText.isNotEmpty()) currentText else ""

            if (siteName.isEmpty()) {
                Toast.makeText(this, "Site bilgisi alınamadı", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            passwordManager.savePassword(
                    siteName = siteName,
                    packageName = currentPkg,
                    username = capturedUsername,
                    password = capturedPassword
            ) { success ->
                Handler(Looper.getMainLooper()).post {
                    if (success) {
                        Toast.makeText(this, "Parola Kaydedildi", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Hata oluştu", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        mainKeyboardFrame.removeAllViews()
        mainKeyboardFrame.addView(passwordView)

        suggestionsStrip.visibility = View.GONE
        toolbarManager.hideToolsPanel(animate = false)
        applyTheme()
    }

    private fun getCurrentPackageName(): String {
        return currentInputEditorInfo?.packageName ?: ""
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
                        inputHandler.commitText(result)

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
        serviceScope.cancel()
    }

    private fun applyTheme() {
        currentLanguage = keyboardPreferences.language

        serviceScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            predictionEngine.loadLanguage(currentLanguage)
        }

        layoutManager.applyTheme()

        val bgColor = keyboardPreferences.backgroundColor
        val keyColor = keyboardPreferences.keyColor

        val parsedBgColor = Color.parseColor(bgColor)
        val parsedKeyColor = Color.parseColor(keyColor)
        currentKeyColor = parsedKeyColor

        window.window?.let { win ->
            win.decorView.setBackgroundColor(parsedBgColor)
            @Suppress("DEPRECATION") win.navigationBarColor = parsedBgColor
        }

        if (::toolsPanel.isInitialized) {
            toolsPanel.setBackgroundColor(parsedBgColor)
        }
        if (::suggestionsStrip.isInitialized) {
            suggestionsStrip.setBackgroundColor(parsedBgColor)
        }

        if (::translationBarContainer.isInitialized) {
            translationBarContainer.setBackgroundColor(parsedBgColor)
        }
        clipboardView?.setBackgroundColor(parsedBgColor)
        passwordView?.setBackgroundColor(parsedBgColor)

        clipboardView?.findViewById<RecyclerView>(R.id.clipboardList)?.adapter?.let {
            if (it is ClipboardAdapter) {
                it.setTheme(parsedKeyColor, Color.WHITE)
            }
        }

        passwordView?.findViewById<RecyclerView>(R.id.recyclerPasswords)?.adapter?.let {
            if (it is com.samiuysal.keyboard.features.password.PasswordPanelAdapter) {
                it.setTheme(Color.WHITE)
            }
        }
    }

    private fun switchToNumbers() {
        currentLayout = "numbers"
        layoutManager.setupNumbersLayout(
                mainKeyboardFrame,
                onSwitchToQwerty = { switchToQwerty() },
                onSwitchToSymbols = { switchToSymbols() },
                onSpace = { handleSpace() },
                onEnter = { handleEnter() },
                onBackspace = { handleBackspace() }
        )
        keyboardView = layoutManager.keyboardView!!
    }

    private fun switchToQwerty() {
        currentLayout = "qwerty"
        layoutManager.setupQwertyLayout(
                mainKeyboardFrame,
                onOpenEmoji = { openEmojiBoard() },
                onShift = {
                    isShifted = !isShifted
                    updateShiftState()
                },
                onSwitchToNumbers = { switchToNumbers() },
                onSpace = { handleSpace() },
                onEnter = { handleEnter() },
                onBackspace = { handleBackspace() }
        )
        keyboardView = layoutManager.keyboardView!!
        suggestionsStrip.visibility = View.VISIBLE
        toolbarManager.updateSuggestions(composingText.toString(), false)
    }

    private fun getStringByLocale(context: Context, resId: Int, localeCode: String): String {
        val resources = context.resources
        val config = android.content.res.Configuration(resources.configuration)
        config.setLocale(java.util.Locale.forLanguageTag(localeCode))
        return context.createConfigurationContext(config).getText(resId).toString()
    }

    private fun sendKeyChar(text: String) {
        if (isGifSearchMode) {
            handleGifSearchInput(text)
        } else if (isTranslationMode && translationInputField != null) {
            translationInputField!!.append(text)
        } else {
            // Password fields don't support composing text - commit directly
            if (isPasswordField()) {
                inputHandler.commitText(text)
            } else {
                composingText.append(text)
                inputHandler.setComposingText(composingText)
                toolbarManager.updateSuggestions(composingText.toString(), isNewWord = false)
            }
        }
    }

    private fun isPasswordField(): Boolean {
        val inputType = currentInputEditorInfo?.inputType ?: 0
        val variation = inputType and android.text.InputType.TYPE_MASK_VARIATION
        val clazz = inputType and android.text.InputType.TYPE_MASK_CLASS

        return clazz == android.text.InputType.TYPE_CLASS_TEXT &&
                (variation == android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                        variation == android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
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

            val expansion = shortcutManager.getExpansion(text)
            if (expansion != null) {
                inputHandler.setComposingText("$expansion ")
                inputHandler.finishComposingText()
                toolbarManager.showShortcutFeedback(text, expansion)
            } else {
                inputHandler.commitText("$text ")
                predictionEngine.learn(text)
            }

            composingText.clear()
            toolbarManager.updateSuggestions("", isNewWord = true)
        } else {
            inputHandler.commitText(" ")
        }
    }

    private fun switchToSymbols() {
        currentLayout = "symbols"
        layoutManager.setupSymbolsLayout(
                mainKeyboardFrame,
                onSwitchToNumbers = { switchToNumbers() },
                onSpace = { handleSpace() },
                onEnter = { handleEnter() },
                onBackspace = { handleBackspace() },
                onTab = { handleTab() }
        )
        keyboardView = layoutManager.keyboardView!!
    }

    private fun updateShiftState() {
        layoutManager.updateShiftState(isShifted)
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
            // Password fields don't support composing text - commit directly
            if (isPasswordField()) {
                inputHandler.commitText(text)
            } else {
                composingText.append(text)
                inputHandler.setComposingText(composingText)
                toolbarManager.updateSuggestions(composingText.toString(), isNewWord = false)
            }
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

    private fun handleTab() {
        if (composingText.isNotEmpty()) {
            inputHandler.commitText(composingText.toString())
            predictionEngine.learn(composingText.toString())
            composingText.clear()
            toolbarManager.updateSuggestions("", isNewWord = true)
        }
        inputHandler.commitText("\t")
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
                    inputHandler.commitText("")
                    toolbarManager.updateSuggestions("", isNewWord = true)
                } else {
                    inputHandler.setComposingText(composingText)
                    toolbarManager.updateSuggestions(composingText.toString(), isNewWord = false)
                }
            } else {
                inputHandler.deleteBackward(1)
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
                inputHandler.commitText(text)

                predictionEngine.learn(text)

                composingText.clear()
                toolbarManager.updateSuggestions("", isNewWord = true)
            }

            inputHandler.sendEnterKey(currentInputEditorInfo)
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

        val bgColor = android.graphics.Color.parseColor(keyboardPreferences.backgroundColor)
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
                val emojis = EmojiManager.getEmojis(this@MPKeyboardService)
                val localizedCategories =
                        EmojiManager.CATEGORY_ORDER
                                .map { key ->
                                    CategoryItem(
                                            key,
                                            run {
                                                val resId = EmojiManager.getCategoryResId(key)
                                                if (resId != 0) {
                                                    getStringByLocale(
                                                            this@MPKeyboardService,
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

    private fun openGifSearchStrip() {
        suggestionsStrip.visibility = View.GONE
        gifStripContainer.removeAllViews()
        gifStripContainer.visibility = View.VISIBLE

        val params = gifStripContainer.layoutParams
        params.height = (resources.displayMetrics.density * 160).toInt()
        gifStripContainer.layoutParams = params

        val gifStripView = layoutInflater.inflate(R.layout.layout_gif_strip, null)
        gifStripContainer.addView(gifStripView)

        val inputGifSearch = gifStripView.findViewById<TextView>(R.id.input_gif_search_strip)
        val btnCloseGif = gifStripView.findViewById<View>(R.id.btn_close_gif_strip)
        val recyclerGifResults = gifStripView.findViewById<RecyclerView>(R.id.recycler_gif_results)

        val bgColor = android.graphics.Color.parseColor(keyboardPreferences.backgroundColor)
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

                val fileBytes = Glide.with(this@MPKeyboardService).asFile().load(url).submit().get()

                fileBytes.copyTo(file, overwrite = true)

                val contentUri: Uri =
                        FileProvider.getUriForFile(
                                this@MPKeyboardService,
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
                    Toast.makeText(this@MPKeyboardService, "GIF gönderilemedi", Toast.LENGTH_SHORT)
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

    override fun onCreateInputView(): View {
        val mainContainer =
                layoutInflater.inflate(R.layout.layout_main_container, null) as LinearLayout
        toolbarManager = ToolbarManager(this, mainContainer)
        toolbarManager.setEngine(predictionEngine)
        toolbarManager.setShortcutManager(shortcutManager)

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
}
