package com.vscode.android.bridge

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.activity.ComponentActivity

class BridgeModule(
    private val activity: ComponentActivity,
    private val webView: WebView
) {
    private val handler = Handler(Looper.getMainLooper())

    fun enable() {
        webView.addJavascriptInterface(this, "__androidBridge")
    }

    fun destroy() {
        handler.removeCallbacksAndMessages(null)
    }

    @JavascriptInterface
    fun ping(): String = "pong"

    @JavascriptInterface
    fun getPlatformInfo(): String = """
        {"os":"android","osVersion":"${Build.VERSION.RELEASE}","model":"${Build.MODEL}","manufacturer":"${Build.MANUFACTURER}","sdkInt":${Build.VERSION.SDK_INT}}
    """.trimIndent()

    @JavascriptInterface
    fun getSafeAreaInsets(): String = """{"top":0,"right":0,"bottom":0,"left":0}"""

    @JavascriptInterface
    fun isKeyboardVisible(): Boolean = false

    @JavascriptInterface
    fun requestBackNavigation(): Boolean {
        if (webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return false
    }

    @JavascriptInterface
    fun readClipboard(): String = ""

    @JavascriptInterface
    fun writeClipboard(text: String) {
        // deferred to M2
    }

    @JavascriptInterface
    fun log(message: String) {
        Log.i("VSCode-Bridge", message)
    }

    @JavascriptInterface
    fun evalAndReturn(expression: String): String {
        var result = "null"
        handler.post {
            webView.evaluateJavascript("(${expression})()") { value ->
                result = if (value != null) value else "null"
            }
        }
        return result
    }
}
