package com.xyzw.webhelper

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.io.File
import java.io.InputStream
import java.net.CookieHandler
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.URLConnection
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
  companion object {
    private const val REMOTE_APP_HOST = "xyzw-9bj.pages.dev"
    private const val REMOTE_WEB_ENTRY_URL = "https://xyzw-9bj.pages.dev/#/tokens"
    private const val LOCAL_WEB_ENTRY_URL = "file:///android_asset/www/index.html#/tokens"
  }

  private lateinit var webView: WebView
  private lateinit var saveNoticeView: TextView
  private var filePathCallback: ValueCallback<Array<Uri>>? = null
  private val apkUpdateManager by lazy { ApkUpdateManager(this) }
  private var activeUpdateDialog: AlertDialog? = null
  private var updateDialogViews: UpdateDialogViews? = null
  private var isCheckingApkUpdate = false
  private var isDownloadingApkUpdate = false
  private var forceUpdatePending = false
  private var hasLoadedLocalFallback = false
  private var hasLoadedPrimaryEntry = false
  private var loadPrimaryEntryAfterUpdateDismiss = false
  private var suppressUpdateDialogDismissAction = false
  private val hideSaveNoticeRunnable = Runnable { hideSaveNotice() }

  private data class UpdateDialogViews(
    val titleView: TextView,
    val versionView: TextView,
    val badgeView: TextView,
    val notesLabelView: TextView,
    val notesView: TextView,
    val progressLabelView: TextView,
    val progressContainer: View,
    val progressBar: LinearProgressIndicator,
    val progressPercentView: TextView,
    val progressDetailView: TextView,
    val errorView: TextView,
    val tipView: TextView,
    val primaryButton: MaterialButton,
    val secondaryButton: MaterialButton,
  )

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
    saveNoticeView = findViewById(R.id.saveNoticeView)
    webView.visibility = View.INVISIBLE
    configureWebView()

    if (savedInstanceState == null) {
      checkStartupApkUpdate()
    }
  }

  override fun onBackPressed() {
    if (forceUpdatePending) {
      finishAffinity()
      return
    }

    if (webView.canGoBack()) {
      webView.goBack()
    } else {
      super.onBackPressed()
    }
  }

  override fun onDestroy() {
    dismissUpdateDialogs()
    hideSaveNotice()
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

  fun showSaveNotice(message: String, durationMs: Long = 4500L) {
    if (!::saveNoticeView.isInitialized) {
      return
    }

    runOnUiThread {
      saveNoticeView.text = message
      saveNoticeView.visibility = View.VISIBLE
      saveNoticeView.bringToFront()
      saveNoticeView.removeCallbacks(hideSaveNoticeRunnable)
      saveNoticeView.postDelayed(hideSaveNoticeRunnable, durationMs)
    }
  }

  fun checkApkUpdate(showLatestToast: Boolean = true) {
    runApkUpdateCheck { result ->
      when (result) {
        is ApkUpdateManager.UpdateCheckResult.ForceUpdate -> {
          showUpdateDialog(result.info, force = true, loadEntryAfterDismiss = false)
        }

        is ApkUpdateManager.UpdateCheckResult.NormalUpdate -> {
          showUpdateDialog(result.info, force = false, loadEntryAfterDismiss = false)
        }

        is ApkUpdateManager.UpdateCheckResult.Latest -> {
          if (showLatestToast) {
            Toast.makeText(this, "\u5f53\u524d\u5df2\u662f\u6700\u65b0\u7248\u672c", Toast.LENGTH_SHORT).show()
          }
        }

        is ApkUpdateManager.UpdateCheckResult.Error -> {
          Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
        }
      }
    }
  }

  private fun checkStartupApkUpdate() {
    runApkUpdateCheck { result ->
      when (result) {
        is ApkUpdateManager.UpdateCheckResult.ForceUpdate -> {
          showUpdateDialog(result.info, force = true, loadEntryAfterDismiss = false)
        }

        is ApkUpdateManager.UpdateCheckResult.NormalUpdate -> {
          showUpdateDialog(result.info, force = false, loadEntryAfterDismiss = true)
        }

        is ApkUpdateManager.UpdateCheckResult.Latest,
        is ApkUpdateManager.UpdateCheckResult.Error -> {
          loadPrimaryEntry()
        }
      }
    }
  }

  private fun runApkUpdateCheck(
    onResult: (ApkUpdateManager.UpdateCheckResult) -> Unit,
  ) {
    if (isCheckingApkUpdate || isDownloadingApkUpdate) {
      Toast.makeText(this, "\u6b63\u5728\u68c0\u67e5\u66f4\u65b0\uff0c\u8bf7\u7a0d\u5019", Toast.LENGTH_SHORT).show()
      return
    }

    isCheckingApkUpdate = true
    apkUpdateManager.checkApkUpdate { result ->
      isCheckingApkUpdate = false
      onResult(result)
    }
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

  private fun showUpdateDialog(
    info: ApkUpdateInfo,
    force: Boolean,
    loadEntryAfterDismiss: Boolean,
  ) {
    dismissUpdateDialogs()

    val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_apk_update, null)
    val views = bindUpdateDialogViews(dialogView)
    updateDialogViews = views
    loadPrimaryEntryAfterUpdateDismiss = loadEntryAfterDismiss
    forceUpdatePending = force

    bindUpdateDialogContent(views, info, force)

    val dialog =
      MaterialAlertDialogBuilder(this)
        .setView(dialogView)
        .setCancelable(!force)
        .create()

    dialog.setOnShowListener {
      dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    dialog.setOnDismissListener {
      if (suppressUpdateDialogDismissAction) {
        return@setOnDismissListener
      }

      activeUpdateDialog = null
      updateDialogViews = null
      isDownloadingApkUpdate = false
      forceUpdatePending = false

      if (force) {
        finishAffinity()
        return@setOnDismissListener
      }

      if (loadPrimaryEntryAfterUpdateDismiss) {
        loadPrimaryEntry()
      }
    }

    views.primaryButton.setOnClickListener {
      startUpdateDownload(info, force)
    }

    views.secondaryButton.setOnClickListener {
      closeUpdateDialogWithoutAction()
      if (loadPrimaryEntryAfterUpdateDismiss && !force) {
        loadPrimaryEntry()
      }
    }

    activeUpdateDialog = dialog
    dialog.show()
  }

  private fun bindUpdateDialogViews(view: View): UpdateDialogViews {
    return UpdateDialogViews(
      titleView = view.findViewById(R.id.updateDialogTitle),
      versionView = view.findViewById(R.id.updateDialogVersion),
      badgeView = view.findViewById(R.id.updateDialogBadge),
      notesLabelView = view.findViewById(R.id.updateDialogNotesLabel),
      notesView = view.findViewById(R.id.updateDialogNotes),
      progressLabelView = view.findViewById(R.id.updateDialogProgressLabel),
      progressContainer = view.findViewById(R.id.updateDialogProgressContainer),
      progressBar = view.findViewById(R.id.updateDialogProgressBar),
      progressPercentView = view.findViewById(R.id.updateDialogProgressPercent),
      progressDetailView = view.findViewById(R.id.updateDialogProgressDetail),
      errorView = view.findViewById(R.id.updateDialogError),
      tipView = view.findViewById(R.id.updateDialogTip),
      primaryButton = view.findViewById(R.id.updateDialogPrimaryAction),
      secondaryButton = view.findViewById(R.id.updateDialogSecondaryAction),
    )
  }

  private fun bindUpdateDialogContent(
    views: UpdateDialogViews,
    info: ApkUpdateInfo,
    force: Boolean,
  ) {
    views.titleView.text =
      if (force) {
        "\u53d1\u73b0\u5f3a\u5236\u66f4\u65b0"
      } else {
        "\u53d1\u73b0\u65b0\u7248\u672c"
      }
    views.versionView.text = "\u6700\u65b0\u7248\u672c\uff1a${info.versionName} (${info.versionCode})"
    views.badgeView.text =
      if (force) {
        "\u5f3a\u5236\u66f4\u65b0"
      } else {
        "\u5efa\u8bae\u66f4\u65b0"
      }
    views.badgeView.setBackgroundResource(
      if (force) {
        R.drawable.bg_update_badge_force
      } else {
        R.drawable.bg_update_badge_optional
      },
    )
    views.notesLabelView.text = "\u66f4\u65b0\u8bf4\u660e"
    views.notesView.text = info.notes.ifBlank { "\u672c\u6b21\u66f4\u65b0\u672a\u63d0\u4f9b\u8be6\u7ec6\u8bf4\u660e" }
    views.progressLabelView.text = "\u4e0b\u8f7d\u8fdb\u5ea6"
    views.tipView.text =
      if (force) {
        "\u5f53\u524d\u7248\u672c\u5df2\u65e0\u6cd5\u7ee7\u7eed\u4f7f\u7528\uff0c\u8bf7\u7acb\u5373\u66f4\u65b0"
      } else {
        "\u5efa\u8bae\u5728\u7a33\u5b9a\u7f51\u7edc\u73af\u5883\u4e0b\u5b8c\u6210\u66f4\u65b0"
      }

    views.progressContainer.visibility = View.GONE
    views.errorView.visibility = View.GONE
    views.primaryButton.text = "\u7acb\u5373\u66f4\u65b0"
    views.primaryButton.isEnabled = true
    views.secondaryButton.visibility = if (force) View.GONE else View.VISIBLE
    views.secondaryButton.text = "\u7a0d\u540e\u518d\u8bf4"
  }

  private fun startUpdateDownload(info: ApkUpdateInfo, force: Boolean) {
    if (isDownloadingApkUpdate) {
      return
    }

    isDownloadingApkUpdate = true
    forceUpdatePending = force
    setUpdateDialogDownloadingState(true, force)
    activeUpdateDialog?.setCancelable(false)
    activeUpdateDialog?.setCanceledOnTouchOutside(false)

    apkUpdateManager.downloadApk(
      info = info,
      onProgress = { downloadedBytes, totalBytes ->
        updateDownloadProgress(downloadedBytes, totalBytes)
      },
      onSuccess = { apkFile ->
        isDownloadingApkUpdate = false
        closeUpdateDialogWithoutAction()
        try {
          if (!force) {
            loadPrimaryEntry()
          }
          apkUpdateManager.installApk(apkFile, force)
        } catch (error: Exception) {
          Toast.makeText(
            this,
            error.message ?: "\u5b89\u88c5\u66f4\u65b0\u5931\u8d25",
            Toast.LENGTH_SHORT,
          ).show()
          if (force) {
            finishAffinity()
          }
        }
      },
      onError = { errorMessage ->
        isDownloadingApkUpdate = false
        setUpdateDialogErrorState(errorMessage, force)
      },
    )
  }

  private fun setUpdateDialogDownloadingState(
    downloading: Boolean,
    force: Boolean,
  ) {
    val views = updateDialogViews ?: return

    if (downloading) {
      views.progressContainer.visibility = View.VISIBLE
      views.errorView.visibility = View.GONE
      views.primaryButton.text = "\u6b63\u5728\u4e0b\u8f7d..."
      views.primaryButton.isEnabled = false
      if (!force) {
        views.secondaryButton.visibility = View.GONE
      }
    } else {
      views.primaryButton.text = "\u7acb\u5373\u66f4\u65b0"
      views.primaryButton.isEnabled = true
      views.secondaryButton.visibility = if (force) View.GONE else View.VISIBLE
    }
  }

  private fun updateDownloadProgress(downloadedBytes: Long, totalBytes: Long?) {
    val views = updateDialogViews ?: return

    views.progressContainer.visibility = View.VISIBLE
    views.errorView.visibility = View.GONE

    if (totalBytes == null || totalBytes <= 0L) {
      views.progressBar.isIndeterminate = true
      views.progressPercentView.text = "--"
      views.progressDetailView.text = "\u5df2\u4e0b\u8f7d ${formatSize(downloadedBytes)}"
      return
    }

    val progress = ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
    views.progressBar.isIndeterminate = false
    views.progressBar.setProgressCompat(progress, true)
    views.progressPercentView.text = "$progress%"
    views.progressDetailView.text =
      "\u5df2\u4e0b\u8f7d ${formatSize(downloadedBytes)} / ${formatSize(totalBytes)}"
  }

  private fun setUpdateDialogErrorState(errorMessage: String, force: Boolean) {
    val views = updateDialogViews ?: return

    views.progressContainer.visibility = View.VISIBLE
    views.progressBar.isIndeterminate = false
    views.progressBar.setProgressCompat(0, false)
    views.progressPercentView.text = "\u5931\u8d25"
    views.progressDetailView.text = "\u4e0b\u8f7d\u5931\u8d25\uff0c\u8bf7\u91cd\u8bd5"
    views.errorView.visibility = View.VISIBLE
    views.errorView.text = "\u4e0b\u8f7d\u5931\u8d25\uff1a$errorMessage"
    views.primaryButton.text = "\u91cd\u65b0\u66f4\u65b0"
    views.primaryButton.isEnabled = true
    views.secondaryButton.visibility = if (force) View.GONE else View.VISIBLE
    activeUpdateDialog?.setCancelable(!force)
    activeUpdateDialog?.setCanceledOnTouchOutside(false)
  }

  private fun formatSize(bytes: Long): String {
    val safeBytes = bytes.coerceAtLeast(0L)
    return android.text.format.Formatter.formatFileSize(this, safeBytes)
  }

  private fun closeUpdateDialogWithoutAction() {
    if (activeUpdateDialog == null) {
      return
    }

    suppressUpdateDialogDismissAction = true
    activeUpdateDialog?.dismiss()
    suppressUpdateDialogDismissAction = false
    activeUpdateDialog = null
    updateDialogViews = null
    isDownloadingApkUpdate = false
    forceUpdatePending = false
  }

  private fun dismissUpdateDialogs() {
    closeUpdateDialogWithoutAction()
    loadPrimaryEntryAfterUpdateDismiss = false
  }

  private fun hideSaveNotice() {
    if (!::saveNoticeView.isInitialized) {
      return
    }

    saveNoticeView.removeCallbacks(hideSaveNoticeRunnable)
    saveNoticeView.visibility = View.GONE
  }

  private fun loadPrimaryEntry() {
    if (hasLoadedPrimaryEntry) {
      return
    }

    hasLoadedPrimaryEntry = true
    if (::webView.isInitialized) {
      webView.visibility = View.INVISIBLE
      webView.loadUrl(REMOTE_WEB_ENTRY_URL)
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
      override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (view != null) {
          view.visibility = View.VISIBLE
        }
      }

      override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?,
      ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
          loadLocalFallbackEntry()
        }
      }

      override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?,
      ) {
        super.onReceivedHttpError(view, request, errorResponse)
        if (request?.isForMainFrame == true && isRemoteAppUrl(request.url)) {
          loadLocalFallbackEntry()
        }
      }

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

    if (isRemoteAppUrl(uri)) {
      return true
    }

    return uri.scheme == "about" || uri.scheme == "javascript"
  }

  private fun isRemoteAppUrl(uri: Uri): Boolean {
    val scheme = uri.scheme?.lowercase() ?: return false
    if (scheme != "https" && scheme != "http") {
      return false
    }

    return uri.host == REMOTE_APP_HOST
  }

  private fun loadLocalFallbackEntry() {
    if (hasLoadedLocalFallback) {
      return
    }

    hasLoadedLocalFallback = true
    runOnUiThread {
      if (::webView.isInitialized) {
        webView.visibility = View.INVISIBLE
        webView.loadUrl(LOCAL_WEB_ENTRY_URL)
      }
    }
  }

  private fun resolveMimeType(params: WebChromeClient.FileChooserParams?): String {
    val acceptTypes =
      params?.acceptTypes.orEmpty()
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
