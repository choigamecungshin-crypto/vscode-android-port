package com.vscode.android

import android.content.Context
import android.util.Log
import android.webkit.WebResourceResponse
import androidx.webkit.WebViewAssetLoader
import java.io.IOException

/**
 * Where the mobile bundle lives and the HTTPS origin it is served from.
 *
 * [WebViewAssetLoader] turns `https://appassets.androidplatform.net/assets/...`
 * into `AssetManager` reads inside `shouldInterceptRequest`, so the workbench
 * runs in a real secure origin instead of the opaque `file://` one. That origin
 * is what ES module CORS checks, IndexedDB, fetch() and web workers require.
 */
internal object VSCodeAssetPaths {
    const val DOMAIN = "appassets.androidplatform.net"

    /** Directory under `assets/` that holds the bundle (see sync-android-assets.mjs). */
    const val ASSET_DIRECTORY = "vscode-mobile"

    /**
     * Path prefix registered with the asset loader. It replaces the builder's
     * default `/assets/` handler instead of competing with it, so the MIME table
     * below is always the one used.
     */
    const val ASSET_PREFIX = "/assets/"

    const val ORIGIN = "https://$DOMAIN$ASSET_PREFIX"

    const val ENTRY_URL = ORIGIN + "index.html"
}

internal fun createAssetLoader(context: Context): WebViewAssetLoader =
    WebViewAssetLoader.Builder()
        .setDomain(VSCodeAssetPaths.DOMAIN)
        .addPathHandler(VSCodeAssetPaths.ASSET_PREFIX, BundlePathHandler(context.applicationContext))
        .build()

/**
 * Serves `assets/vscode-mobile/` with an explicit MIME type per extension.
 *
 * The default `AssetsPathHandler` guesses types through `MimeTypeMap`, which
 * does not know `.wasm`, `.woff2` or `.mjs` on every API level, and it never
 * declares a charset. Both matter here: a wrong MIME type makes ES module and
 * WebAssembly loading fail, and a charset on binary files corrupts fonts.
 */
private class BundlePathHandler(context: Context) : WebViewAssetLoader.PathHandler {

    private val assets = context.assets

    override fun handle(path: String): WebResourceResponse? {
        // The loader strips the registered "/assets/" prefix, keeping any leading slash.
        val relativePath = path.trimStart('/')
        if (relativePath.isEmpty()) {
            return null
        }
        // Never let a request escape assets/.
        if (relativePath.split('/').any { it == ".." }) {
            Log.e(TAG, "refusing asset path traversal: $path")
            return null
        }

        val assetPath = relativePath
        val stream = try {
            assets.open(assetPath)
        } catch (error: IOException) {
            Log.e(TAG, "asset not found in APK: $assetPath")
            return null
        }

        val mimeType = guessMimeType(relativePath)
        return WebResourceResponse(mimeType, encodingFor(mimeType), stream)
    }

    private fun encodingFor(mimeType: String): String? = when {
        mimeType.startsWith("text/") -> "UTF-8"
        mimeType == "application/javascript" -> "UTF-8"
        mimeType == "application/json" -> "UTF-8"
        mimeType == "application/manifest+json" -> "UTF-8"
        mimeType == "application/xml" -> "UTF-8"
        mimeType == "image/svg+xml" -> "UTF-8"
        else -> null
    }

    private fun guessMimeType(path: String): String {
        val lower = path.lowercase()
        return when {
            lower.endsWith(".html") || lower.endsWith(".htm") -> "text/html"
            lower.endsWith(".css") -> "text/css"
            lower.endsWith(".js") || lower.endsWith(".mjs") -> "application/javascript"
            lower.endsWith(".json") -> "application/json"
            lower.endsWith(".webmanifest") -> "application/manifest+json"
            lower.endsWith(".svg") -> "image/svg+xml"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".gif") -> "image/gif"
            lower.endsWith(".webp") -> "image/webp"
            lower.endsWith(".ico") -> "image/x-icon"
            lower.endsWith(".woff") -> "font/woff"
            lower.endsWith(".woff2") -> "font/woff2"
            lower.endsWith(".ttf") -> "font/ttf"
            lower.endsWith(".otf") -> "font/otf"
            lower.endsWith(".wasm") -> "application/wasm"
            lower.endsWith(".xml") -> "application/xml"
            lower.endsWith(".map") -> "application/json"
            else -> "application/octet-stream"
        }
    }

    private companion object {
        const val TAG = "VSCode-AssetLoader"
    }
}
