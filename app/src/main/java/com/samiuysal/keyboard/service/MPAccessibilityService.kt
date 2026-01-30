package com.samiuysal.keyboard.service

import android.accessibilityservice.AccessibilityService
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class MPAccessibilityService : AccessibilityService() {

    companion object {
        var currentUrl: String = ""
            private set

        var currentPackage: String = ""
            private set

        // Browser package -> URL bar View ID mapping
        private val BROWSER_URL_BAR_IDS =
                mapOf(
                        "com.android.chrome" to "com.android.chrome:id/url_bar",
                        "com.chrome.beta" to "com.chrome.beta:id/url_bar",
                        "com.chrome.dev" to "com.chrome.dev:id/url_bar",
                        "com.sec.android.app.sbrowser" to
                                "com.sec.android.app.sbrowser:id/location_bar_edit_text",
                        "org.mozilla.firefox" to
                                "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
                        "org.mozilla.fennec_fdroid" to
                                "org.mozilla.fennec_fdroid:id/mozac_browser_toolbar_url_view",
                        "org.mozilla.firefox_beta" to
                                "org.mozilla.firefox_beta:id/mozac_browser_toolbar_url_view",
                        "com.opera.browser" to "com.opera.browser:id/url_field",
                        "com.opera.mini.native" to "com.opera.mini.native:id/url_field",
                        "com.microsoft.emmx" to "com.microsoft.emmx:id/url_bar",
                        "com.brave.browser" to "com.brave.browser:id/url_bar",
                        "com.duckduckgo.mobile.android" to
                                "com.duckduckgo.mobile.android:id/omnibarTextInput",
                        "org.chromium.chrome" to "org.chromium.chrome:id/url_bar",
                        "com.vivaldi.browser" to "com.vivaldi.browser:id/url_bar",
                        "com.kiwibrowser.browser" to "com.kiwibrowser.browser:id/url_bar"
                )

        // Alternative URL bar IDs to try if main one fails
        private val FALLBACK_URL_PATTERNS =
                listOf(
                        "url_bar",
                        "location_bar",
                        "omnibox",
                        "address_bar",
                        "url_field",
                        "mozac_browser_toolbar_url_view"
                )
    }

    @Suppress("DEPRECATION")
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return
        currentPackage = packageName

        val rootNode = rootInActiveWindow ?: return

        try {
            // Step 1: Check if this is a known browser
            val urlBarId = BROWSER_URL_BAR_IDS[packageName]

            if (urlBarId != null) {
                // It's a browser - try to get URL from specific View ID
                val url =
                        extractUrlFromViewId(rootNode, urlBarId)
                                ?: extractUrlFromFallbackPatterns(rootNode, packageName)
                                        ?: extractUrlFromEditText(rootNode)

                if (!url.isNullOrEmpty()) {
                    currentUrl = normalizeUrl(url)
                } else {
                    // Browser detected but URL not found - use package as fallback
                    currentUrl = packageName
                }
            } else {
                // Not a browser - use app name or package
                currentUrl = getAppName(packageName) ?: packageName
            }
        } finally {
            rootNode.recycle()
        }
    }

    @Suppress("DEPRECATION")
    private fun extractUrlFromViewId(root: AccessibilityNodeInfo, viewId: String): String? {
        val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
        if (nodes != null && nodes.isNotEmpty()) {
            val text = nodes[0].text?.toString()
            nodes.forEach { it.recycle() }
            if (!text.isNullOrBlank()) {
                return text
            }
        }
        return null
    }

    @Suppress("DEPRECATION")
    private fun extractUrlFromFallbackPatterns(
            root: AccessibilityNodeInfo,
            packageName: String
    ): String? {
        for (pattern in FALLBACK_URL_PATTERNS) {
            val fullId = "$packageName:id/$pattern"
            val nodes = root.findAccessibilityNodeInfosByViewId(fullId)
            if (nodes != null && nodes.isNotEmpty()) {
                val text = nodes[0].text?.toString()
                nodes.forEach { it.recycle() }
                if (!text.isNullOrBlank() && looksLikeUrl(text)) {
                    return text
                }
            }
        }
        return null
    }

    private fun extractUrlFromEditText(root: AccessibilityNodeInfo): String? {
        return findUrlInNodeTree(root)
    }

    @Suppress("DEPRECATION")
    private fun findUrlInNodeTree(node: AccessibilityNodeInfo): String? {
        // Check if this is an EditText with URL-like content
        if (node.className?.toString()?.contains("EditText") == true) {
            val text = node.text?.toString()
            if (!text.isNullOrBlank() && looksLikeUrl(text)) {
                return text
            }
        }

        // Recursively check children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findUrlInNodeTree(child)
            child.recycle()
            if (result != null) {
                return result
            }
        }
        return null
    }

    private fun looksLikeUrl(text: String): Boolean {
        val lower = text.lowercase()
        return (lower.startsWith("http://") ||
                lower.startsWith("https://") ||
                lower.startsWith("www.") ||
                (lower.contains(".") &&
                        (lower.contains(".com") ||
                                lower.contains(".org") ||
                                lower.contains(".net") ||
                                lower.contains(".edu") ||
                                lower.contains(".gov") ||
                                lower.contains(".tr") ||
                                lower.contains(".io") ||
                                lower.contains(".co") ||
                                lower.contains("/"))))
    }

    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        return if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            "https://$trimmed"
        } else {
            trimmed
        }
    }

    private fun getAppName(packageName: String): String? {
        return try {
            val pm = applicationContext.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    override fun onInterrupt() {
        // Handle interruption
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = serviceInfo
        info.eventTypes =
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        info.feedbackType = android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_GENERIC
        serviceInfo = info
    }
}
