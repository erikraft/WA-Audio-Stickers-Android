package com.erikraft.soundstickers

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.webkit.WebViewAssetLoader
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private val shareExecutor = Executors.newSingleThreadExecutor()

    private val assetLoader by lazy {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = Color.rgb(37, 211, 102)
        window.navigationBarColor = Color.rgb(16, 16, 25)

        webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.rgb(16, 16, 25))
            configureWebView()
        }
        setContentView(FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(16, 16, 25))
            addView(webView)
        })
        webView.loadUrl(APP_ENTRY_URL)
        cleanupExpiredShares()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun WebView.configureWebView() {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = true
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            mediaPlaybackRequiresUserGesture = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            setGeolocationEnabled(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = true
            }
        }

        addJavascriptInterface(NativeShareBridge(), "AndroidBridge")
        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val uri = request?.url ?: return forbiddenResponse()
                if (uri.scheme != "https" || uri.host != ASSET_HOST ||
                    uri.port != -1 || !uri.path.orEmpty().startsWith(ASSET_PATH)
                ) return forbiddenResponse()
                return assetLoader.shouldInterceptRequest(uri) ?: forbiddenResponse()
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val uri = request?.url ?: return true
                if (uri.scheme == "https" && uri.host == ASSET_HOST &&
                    uri.port == -1 && uri.path.orEmpty().startsWith(ASSET_PATH)
                ) return false

                if (uri.scheme == "https" && uri.host == "github.com" &&
                    uri.userInfo == null && uri.port == -1
                ) {
                    runCatching {
                        startActivity(Intent(Intent.ACTION_VIEW, uri))
                    }.onFailure {
                        showToast("Não foi possível abrir o link.")
                    }
                }
                return true
            }
        }

        webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean = openFilePicker(callback, params)

            override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
                request?.deny()
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: android.os.Message?
            ): Boolean = false
        }
    }

    private fun forbiddenResponse() = WebResourceResponse(
        "text/plain", "utf-8", 403, "Forbidden", emptyMap(),
        ByteArrayInputStream(ByteArray(0))
    )

    private fun openFilePicker(
        callback: ValueCallback<Array<Uri>>?,
        params: WebChromeClient.FileChooserParams?
    ): Boolean {
        fileChooserCallback?.onReceiveValue(null)
        fileChooserCallback = callback

        val mimeTypes = params?.acceptTypes
            ?.flatMap { it.split(",") }
            ?.mapNotNull(::acceptedMimeType)
            ?.distinct()
            ?.toTypedArray()
            ?: emptyArray()
        val requestedTypes = if (mimeTypes.isEmpty()) arrayOf("*/*") else mimeTypes

        val picker = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, requestedTypes)
            putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            putExtra(
                Intent.EXTRA_ALLOW_MULTIPLE,
                params?.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE
            )
        }
        return try {
            startActivityForResult(
                Intent.createChooser(picker, "Selecionar mídia"), FILE_PICKER_REQUEST
            )
            true
        } catch (_: ActivityNotFoundException) {
            fileChooserCallback?.onReceiveValue(null)
            fileChooserCallback = null
            showToast("Nenhum seletor de arquivos está disponível.")
            false
        }
    }

    private fun acceptedMimeType(value: String): String? {
        val type = value.trim().lowercase()
        if (type.startsWith("video/") || type.startsWith("image/") ||
            type.startsWith("audio/")
        ) return type
        return when (type.substringAfterLast('.', "")) {
            "mp4", "mov", "m4v", "webm", "3gp" -> "video/*"
            "gif", "webp", "png", "jpg", "jpeg", "avif" -> "image/*"
            "m4a", "aac", "ogg", "opus", "mp3", "wav", "flac" -> "audio/*"
            else -> null
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != FILE_PICKER_REQUEST) return
        val selectedUris = when {
            resultCode != RESULT_OK -> null
            data?.clipData != null -> {
                val clipData = data.clipData!!
                Array(clipData.itemCount) { index -> clipData.getItemAt(index).uri }
            }
            data?.data != null -> arrayOf(data.data!!)
            else -> null
        }
        fileChooserCallback?.onReceiveValue(selectedUris)
        fileChooserCallback = null
    }

    private fun queueShare(
        encoded: String?,
        mime: String?,
        name: String?,
        targetWhatsApp: Boolean
    ) {
        val request = SharePayloadValidator.validate(encoded, mime, name)
        if (request == null) {
            showToast("Arquivo inválido ou maior que o limite de compartilhamento.")
            return
        }
        shareExecutor.execute {
            val result = runCatching {
                val bytes = Base64.decode(request.base64Content, Base64.NO_WRAP)
                if (bytes.isEmpty() || bytes.size.toLong() > SharePayloadValidator.MAX_SHARE_BYTES) {
                    throw IOException("Tamanho de arquivo inválido.")
                }
                if (request.mimeType == "application/zip" && !hasZipSignature(bytes)) {
                    throw IOException("O arquivo ZIP não passou pela validação.")
                }
                val shareDirectory = File(cacheDir, "shared")
                if (!shareDirectory.exists() && !shareDirectory.mkdirs()) {
                    throw IOException("Não foi possível preparar o diretório temporário.")
                }
                val outputFile = File(
                    shareDirectory, UUID.randomUUID().toString() + "-" + request.fileName
                )
                outputFile.outputStream().use { it.write(bytes) }
                outputFile
            }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                result.onSuccess { file ->
                    openShareSheet(file, request.mimeType, targetWhatsApp)
                }.onFailure {
                    showToast("Não foi possível preparar o arquivo para compartilhar.")
                }
            }
        }
    }

    private fun hasZipSignature(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        val firstTwo = bytes[0] == 0x50.toByte() && bytes[1] == 0x4b.toByte()
        val third = bytes[2]
        val fourth = bytes[3]
        return firstTwo && (
            (third == 0x03.toByte() && fourth == 0x04.toByte()) ||
                (third == 0x05.toByte() && fourth == 0x06.toByte()) ||
                (third == 0x07.toByte() && fourth == 0x08.toByte())
            )
    }

    private fun cleanupExpiredShares() {
        val directory = File(cacheDir, "shared")
        val cutoff = System.currentTimeMillis() - CACHE_TTL_MS
        directory.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < cutoff) file.delete()
        }
    }

    private fun openShareSheet(file: File, mimeType: String, targetWhatsApp: Boolean) {
        val uri = FileProvider.getUriForFile(this, "$packageName.shared", file)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(contentResolver, file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        if (targetWhatsApp) {
            val targets = listOf(
                "WhatsApp" to "com.whatsapp",
                "WhatsApp Business" to "com.whatsapp.w4b"
            ).filter { (_, packageName) ->
                packageManager.getLaunchIntentForPackage(packageName) != null
            }

            if (targets.isEmpty()) {
                showToast("WhatsApp e WhatsApp Business não foram encontrados.")
                openGeneralShareChooser(sendIntent)
                return
            }

            AlertDialog.Builder(this)
                .setTitle("Enviar clipe")
                .setItems(targets.map { it.first }.toTypedArray()) { _, index ->
                    val targetPackage = targets.getOrNull(index)?.second ?: return@setItems
                    runCatching {
                        startActivity(Intent(sendIntent).setPackage(targetPackage))
                    }.onFailure {
                        showToast("Não foi possível abrir o aplicativo escolhido.")
                        openGeneralShareChooser(sendIntent)
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
            return
        }

        openGeneralShareChooser(sendIntent)
    }

    private fun openGeneralShareChooser(sendIntent: Intent) {
        sendIntent.setPackage(null)
        runCatching {
            startActivity(Intent.createChooser(sendIntent, "Compartilhar arquivo"))
        }.onFailure {
            showToast("Nenhum app compatível foi encontrado.")
        }
    }

    private fun showToast(message: String) {
        runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
    }

    inner class NativeShareBridge {
        @JavascriptInterface
        fun shareFile(base64: String?, mimeType: String?, fileName: String?) {
            queueShare(base64, mimeType, fileName, targetWhatsApp = false)
        }

        @JavascriptInterface
        fun shareToWhatsApp(base64: String?, mimeType: String?, fileName: String?) {
            queueShare(base64, mimeType, fileName, targetWhatsApp = true)
        }
    }

    override fun onDestroy() {
        fileChooserCallback?.onReceiveValue(null)
        fileChooserCallback = null
        if (::webView.isInitialized) {
            webView.removeJavascriptInterface("AndroidBridge")
            webView.stopLoading()
            webView.destroy()
        }
        shareExecutor.shutdown()
        super.onDestroy()
    }

    private companion object {
        const val APP_ENTRY_URL = "https://appassets.androidplatform.net/assets/www/index.html"
        const val ASSET_HOST = "appassets.androidplatform.net"
        const val ASSET_PATH = "/assets/www/"
        const val FILE_PICKER_REQUEST = 7301
        const val CACHE_TTL_MS = 24L * 60L * 60L * 1000L
    }
}
