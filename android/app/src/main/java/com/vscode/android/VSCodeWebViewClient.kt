package com.vscode.android

import android.net.http.SslError
import android.util.Log
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader

/**
 * Serves the packaged bundle through [WebViewAssetLoader] and reports main-frame
 * failures so the activity can show an error page instead of a white WebView.
 *
 * Requests are only intercepted for the asset loader's own HTTPS origin; every
 * other URL goes to the network as usual, with no CORS or web-security relaxing.
 */
internal class VSCodeWebViewClient(
    private val assetLoader: WebViewAssetLoader,
    private val onMainFrameLoadError: ((Int, String?, String?) -> Unit)? = null
) : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        // Returns null for foreign origins and a 404 for missing assets.
        return assetLoader.shouldInterceptRequest(request.url)
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError
    ) {
        super.onReceivedError(view, request, error)
        val description = error.description?.toString()
        val url = request.url?.toString()
        Log.e(TAG, "load error: ${error.errorCode} ($description) url=$url mainFrame=${request.isForMainFrame}")
        if (request.isForMainFrame) {
            onMainFrameLoadError?.invoke(error.errorCode, description, url)
        }
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        Log.e(
            TAG,
            "http error: ${errorResponse.statusCode} ${errorResponse.reasonPhrase} " +
                "url=${request.url} mainFrame=${request.isForMainFrame}"
        )
        // A missing entry page is reported as a 404 from the asset loader rather
        // than as a load error, so treat it the same way: keep the WebView from
        // staying blank when the APK ships without the bundle.
        if (request.isForMainFrame && errorResponse.statusCode >= 400) {
            onMainFrameLoadError?.invoke(errorResponse.statusCode, errorResponse.reasonPhrase, request.url?.toString())
        }
    }

    // Assets ship with the APK, so a TLS failure here is never expected: report it
    // and cancel the request instead of silently continuing.
    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        Log.e(TAG, "ssl error: ${error.primaryError} url=${error.url}")
        handler.cancel()
    }

    private companion object {
        const val TAG = "VSCode-WebViewClient"
    }
}
