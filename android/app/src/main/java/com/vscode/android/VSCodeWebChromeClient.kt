package com.vscode.android

import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebStorage
import android.webkit.WebView

/**
 * WebChromeClient that logs JS console output to Logcat and routes JS dialogs into
 * the native activity back stack (no native alert/confirm/prompt windows).
 */
class VSCodeWebChromeClient : WebChromeClient() {

    private val logTag = "VSCodeWebChromeClient"

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        consoleMessage ?: return true
        val level = when (consoleMessage.messageLevel()) {
            ConsoleMessage.MessageLevel.DEBUG -> Log.DEBUG
            ConsoleMessage.MessageLevel.LOG -> Log.INFO
            ConsoleMessage.MessageLevel.WARNING -> Log.WARN
            ConsoleMessage.MessageLevel.ERROR -> Log.ERROR
            else -> Log.DEBUG
        }
        val msg = "${consoleMessage.message()} [${consoleMessage.sourceId()}:${consoleMessage.lineNumber()}]"
        Log.println(level, logTag, msg)
        return true
    }

    override fun onExceededDatabaseQuota(
        url: String,
        databaseIdentifier: String,
        quota: Long,
        estimatedDatabaseSize: Long,
        totalQuota: Long,
        quotaUpdater: WebStorage.QuotaUpdater
    ) {
        Log.w(logTag, "IndexedDB quota exceeded for $url; granting current quota.")
        quotaUpdater.updateQuota(quota)
    }

    override fun onJsAlert(
        view: WebView,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        Log.d(logTag, "JS alert: $message")
        result?.confirm()
        return true
    }

    override fun onJsConfirm(
        view: WebView,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        Log.d(logTag, "JS confirm: $message")
        result?.confirm()
        return true
    }

    override fun onJsPrompt(
        view: WebView,
        url: String?,
        message: String?,
        defaultValue: String?,
        result: JsPromptResult?
    ): Boolean {
        Log.d(logTag, "JS prompt: $message (default: $defaultValue)")
        result?.cancel()
        return true
    }
}
