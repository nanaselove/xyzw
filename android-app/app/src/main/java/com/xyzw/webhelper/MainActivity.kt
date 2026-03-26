package com.xyzw.webhelper

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.InputStream
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.URLConnection
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
  private lateinit var webView: WebView
  private var filePathCallback: ValueCallback<Array<Uri>>? = null

  private val fileChooserLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      val callback = filePathCallback
      filePathCallback = null
      callback?.onReceiveValue(
        WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data),
      )
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    ensureHttpCookieHandler()
    setContentView(R.layout.activity_main)

    webView = findViewById(R.id.webView)
    configureWebView()

    if (savedInstanceState == null) {
      webView.loadUrl("file:///android_asset/www/index.html#/tokens")
    }
  }

  override fun onBackPressed() {
    if (webView.canGoBack()) {
      webView.goBack()
    } else {
      super.onBackPressed()
    }
  }

  override fun onDestroy() {
    if (::webView.isInitialized) {
      webView.apply {
        stopLoading()
        loadUrl("about:blank")
        clearHistory()
        removeAllViews()
        (parent as? ViewGroup)?.removeView(this)
        destroy()
      }
    }
    super.onDestroy()
  }

  fun openExternalUrl(url: String) {
    try {
      startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (error: ActivityNotFoundException) {
      Toast.makeText(this, R.string.no_handler, Toast.LENGTH_SHORT).show()
    }
  }

  fun shareText(text: String, title: String) {
    val intent =
      Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
      }

    startActivity(Intent.createChooser(intent, title))
  }

  fun shareFile(file: File, mimeType: String, title: String) {
    val uri =
      FileProvider.getUriForFile(
        this,
        "${packageName}.fileprovider",
        file,
      )

    val intent =
      Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }

    startActivity(Intent.createChooser(intent, title))
  }

  fun deliverNativeHttpResponse(requestId: String, payloadJson: String) {
    val script =
      "window.__xyzwNativeHttpReceive && window.__xyzwNativeHttpReceive(" +
        JSONObject.quote(requestId) +
        ", " +
        JSONObject.quote(payloadJson) +
        ")"

    runOnUiThread {
      if (::webView.isInitialized) {
        webView.evaluateJavascript(script, null)
      }
    }
  }

  private fun configureWebView() {
    WebView.setWebContentsDebuggingEnabled(isDebuggable())

    webView.settings.apply {
      javaScriptEnabled = true
      domStorageEnabled = true
      databaseEnabled = true
      allowFileAccess = true
      allowContentAccess = true
      allowFileAccessFromFileURLs = true
      allowUniversalAccessFromFileURLs = true
      mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
      useWideViewPort = true
      loadWithOverviewMode = true
      setSupportZoom(false)
      setBuiltInZoomControls(false)
      displayZoomControls = false
    }

    webView.addJavascriptInterface(WebAppBridge(this), "AndroidBridge")
    webView.webChromeClient = createChromeClient()
    webView.webViewClient = createWebViewClient()
  }

  private fun createChromeClient() =
    object : WebChromeClient() {
      override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?,
      ): Boolean {
        this@MainActivity.filePathCallback?.onReceiveValue(null)
        this@MainActivity.filePathCallback = filePathCallback

        val chooserIntent =
          Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = resolveMimeType(fileChooserParams)
            putExtra(
              Intent.EXTRA_ALLOW_MULTIPLE,
              fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE,
            )
          }

        return try {
          fileChooserLauncher.launch(
            Intent.createChooser(
              chooserIntent,
              fileChooserParams?.title ?: getString(R.string.select_file_title),
            ),
          )
          true
        } catch (error: Exception) {
          this@MainActivity.filePathCallback?.onReceiveValue(null)
          this@MainActivity.filePathCallback = null
          Toast.makeText(
            this@MainActivity,
            R.string.file_chooser_failed,
            Toast.LENGTH_SHORT,
          ).show()
          false
        }
      }
    }

  private fun createWebViewClient() =
    object : WebViewClient() {
      override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?,
      ): WebResourceResponse? {
        val uri = request?.url ?: return super.shouldInterceptRequest(view, request)
        return interceptAssetRequest(uri) ?: super.shouldInterceptRequest(view, request)
      }

      @Suppress("DEPRECATION")
      override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
        val uri = url?.let(Uri::parse) ?: return super.shouldInterceptRequest(view, url)
        return interceptAssetRequest(uri) ?: super.shouldInterceptRequest(view, url)
      }

      override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?,
      ): Boolean {
        val url = request?.url ?: return false

        return if (isInternalUrl(url)) {
          false
        } else {
          openExternalUrl(url.toString())
          true
        }
      }

      override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        if (url == null) return false
        return if (isInternalUrl(Uri.parse(url))) {
          false
        } else {
          openExternalUrl(url)
          true
        }
      }
    }

  private fun interceptAssetRequest(uri: Uri): WebResourceResponse? {
    if (uri.scheme != "file") {
      return null
    }

    val path = uri.path ?: return null
    if (path.startsWith("/android_asset/")) {
      return null
    }

    val assetPath = "www/${path.removePrefix("/")}"

    return try {
      val inputStream: InputStream = assets.open(assetPath)
      WebResourceResponse(guessMimeType(assetPath), guessEncoding(assetPath), inputStream)
    } catch (_: Exception) {
      null
    }
  }

  private fun guessMimeType(assetPath: String): String {
    val lowerPath = assetPath.lowercase()
    return when {
      lowerPath.endsWith(".svg") -> "image/svg+xml"
      lowerPath.endsWith(".png") -> "image/png"
      lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg") -> "image/jpeg"
      lowerPath.endsWith(".webp") -> "image/webp"
      lowerPath.endsWith(".gif") -> "image/gif"
      lowerPath.endsWith(".css") -> "text/css"
      lowerPath.endsWith(".js") || lowerPath.endsWith(".mjs") -> "application/javascript"
      lowerPath.endsWith(".json") -> "application/json"
      lowerPath.endsWith(".html") || lowerPath.endsWith(".htm") -> "text/html"
      else -> URLConnection.guessContentTypeFromName(assetPath) ?: "application/octet-stream"
    }
  }

  private fun guessEncoding(assetPath: String): String? {
    val mimeType = guessMimeType(assetPath)
    return when {
      mimeType.startsWith("text/") -> "UTF-8"
      mimeType == "application/javascript" -> "UTF-8"
      mimeType == "application/json" -> "UTF-8"
      mimeType == "image/svg+xml" -> "UTF-8"
      else -> null
    }
  }

  private fun isInternalUrl(uri: Uri): Boolean {
    if (uri.scheme == "file") {
      return uri.toString().startsWith("file:///android_asset/")
    }

    return uri.scheme == "about" || uri.scheme == "javascript"
  }

  private fun resolveMimeType(params: WebChromeClient.FileChooserParams?): String {
    val acceptTypes = params?.acceptTypes.orEmpty()
      .map { it.trim() }
      .filter { it.isNotEmpty() }

    return acceptTypes.firstOrNull() ?: "*/*"
  }

  private fun isDebuggable(): Boolean {
    return (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
  }

  private fun ensureHttpCookieHandler() {
    if (CookieHandler.getDefault() == null) {
      CookieHandler.setDefault(CookieManager(null, CookiePolicy.ACCEPT_ALL))
    }
  }
}
