package com.vscode.android

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import com.vscode.android.bridge.BridgeModule

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private var bridgeModule: BridgeModule? = null
    private var missingBundleErrorShown = false

    companion object {
        private const val TAG = "VSCode-WebView"

        private val MISSING_BUNDLE_HTML = """
            <!doctype html>
            <html>
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>Android shell could not load the VS Code bundle</title>
              <style>
                body { font-family: system-ui, -apple-system, sans-serif; margin: 0; padding: 24px; }
                h1 { color: #b00020; }
                code { background: #f4f4f4; padding: 2px 6px; border-radius: 4px; }
              </style>
            </head>
            <body>
              <h1>Android shell could not load the VS Code bundle</h1>
              <p>The mobile bundle did not load, so this device is running an older
              or incompatible VS Code Mobile build.</p>
              <p>Missing or refused asset URL:</p>
              <ul>
                <li><code>https://appassets.androidplatform.net/assets/vscode-mobile/out/vs/sessions/agents.web.mobile.main.internal.js</code></li>
                <li><code>https://appassets.androidplatform.net/assets/vscode-mobile/out/vs/sessions/agents.web.mobile.main.internal.css</code></li>
                <li><code>https://appassets.androidplatform.net/assets/vscode-mobile/mobile.json</code></li>
              </ul>
              <p>Check the Android shell log for WebView and bridge errors.
              Verify <code>android/app/src/main/assets/vscode-mobile/</code> contains
              those files, then rebuild and reinstall.</p>
            </body>
            </html>
        """.trimIndent()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)

        // Registered before loadUrl(), so the workbench sees window.__androidBridge
        // on its first script execution.
        bridgeModule = BridgeModule(this, webView).apply {
            enable()
        }

        webView.webViewClient = VSCodeWebViewClient(createAssetLoader(this), ::handleMainFrameLoadError)
        webView.webChromeClient = VSCodeWebChromeClient()

        val settings = webView.settings
        settings.setJavaScriptEnabled(true)
        settings.setDomStorageEnabled(true)
        settings.setDatabaseEnabled(true)
        settings.setGeolocationEnabled(false)
        // Everything, including the entry page, is served over HTTPS by the asset
        // loader, so the WebView never needs file:// access.
        settings.setAllowFileAccess(false)
        settings.setAllowFileAccessFromFileURLs(false)
        settings.setAllowUniversalAccessFromFileURLs(false)
        settings.setJavaScriptCanOpenWindowsAutomatically(true)
        settings.setLoadWithOverviewMode(true)
        settings.setUseWideViewPort(true)
        settings.setSupportMultipleWindows(true)
        settings.setMediaPlaybackRequiresUserGesture(false)
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW)
        settings.setSafeBrowsingEnabled(true)

        // Served from assets by WebViewAssetLoader on a secure HTTPS origin.
        webView.loadUrl(VSCodeAssetPaths.ENTRY_URL)
    }

    fun recordPageStart(url: String?) {
        Log.i(TAG, "page load start url=$url")
    }

    fun recordPageFinish(url: String?) {
        Log.i(TAG, "page load finish url=$url")
    }

    fun recordPageError(errorCode: Int, description: String?, url: String?) {
        Log.e(TAG, "load failed: ${describeLoadError(errorCode)}, url=$url, description=$description")
    }

    private fun describeLoadError(errorCode: Int): String = when (errorCode) {
        WebViewClient.ERROR_UNKNOWN -> "unknown"
        WebViewClient.ERROR_HOST_LOOKUP -> "host lookup"
        WebViewClient.ERROR_CONNECT -> "connect failed"
        WebViewClient.ERROR_IO -> "io error"
        WebViewClient.ERROR_TIMEOUT -> "timeout"
        WebViewClient.ERROR_REDIRECT_LOOP -> "redirect loop"
        WebViewClient.ERROR_UNSUPPORTED_SCHEME -> "unsupported scheme"
        WebViewClient.ERROR_FAILED_SSL_HANDSHAKE -> "failed ssl handshake"
        WebViewClient.ERROR_BAD_URL -> "bad url"
        WebViewClient.ERROR_FILE_NOT_FOUND -> "file not found"
        WebViewClient.ERROR_FILE -> "file access denied"
        WebViewClient.ERROR_TOO_MANY_REQUESTS -> "too many requests"
        WebViewClient.ERROR_UNSAFE_RESOURCE -> "unsafe resource"
        else -> "unknown ($errorCode)"
    }

    /**
     * Called from [VSCodeWebViewClient] when the main frame (the bundle
     * entrypoint) fails to load, so the user gets a readable error instead of a
     * white WebView.
     */
    private fun handleMainFrameLoadError(errorCode: Int, description: String?, url: String?) {
        recordPageError(errorCode, description, url)
        showMissingBundleError()
    }

    fun showMissingBundleError() {
        if (missingBundleErrorShown) {
            return
        }
        missingBundleErrorShown = true
        Log.e(TAG, "showing missing-bundle error page: ${VSCodeAssetPaths.ENTRY_URL} did not load")
        webView.loadDataWithBaseURL(
            VSCodeAssetPaths.ORIGIN,
            MISSING_BUNDLE_HTML,
            "text/html",
            "UTF-8",
            null
        )
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
        webView.pauseTimers()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        webView.resumeTimers()
    }

    override fun onDestroy() {
        bridgeModule?.destroy()
        webView.destroy()
        super.onDestroy()
    }
}
