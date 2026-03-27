package com.xyzw.webhelper

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class ApkUpdateManager(private val activity: MainActivity) {
  sealed class UpdateCheckResult {
    data class Latest(
      val currentVersionCode: Int,
      val currentVersionName: String,
      val serverVersionCode: Int,
      val serverVersionName: String,
    ) : UpdateCheckResult()

    data class NormalUpdate(val info: ApkUpdateInfo) : UpdateCheckResult()

    data class ForceUpdate(val info: ApkUpdateInfo) : UpdateCheckResult()

    data class Error(val message: String) : UpdateCheckResult()
  }

  fun checkApkUpdate(
    versionUrl: String = DEFAULT_VERSION_URL,
    onResult: (UpdateCheckResult) -> Unit,
  ) {
    Thread {
      val result =
        try {
          val info = fetchUpdateInfo(versionUrl)
          val currentVersionCode = getCurrentVersionCode()
          val currentVersionName = getCurrentVersionName()
          when {
            currentVersionCode < info.minSupportedVersionCode -> UpdateCheckResult.ForceUpdate(info)
            currentVersionCode < info.versionCode -> {
              if (info.force) UpdateCheckResult.ForceUpdate(info) else UpdateCheckResult.NormalUpdate(info)
            }
            else -> UpdateCheckResult.Latest(
              currentVersionCode = currentVersionCode,
              currentVersionName = currentVersionName,
              serverVersionCode = info.versionCode,
              serverVersionName = info.versionName,
            )
          }
        } catch (error: Exception) {
          UpdateCheckResult.Error(error.message ?: "检查更新失败")
        }

      activity.runOnUiThread {
        onResult(result)
      }
    }.start()
  }

  fun downloadApk(
    info: ApkUpdateInfo,
    onLoadingChanged: (Boolean) -> Unit,
    onSuccess: (File) -> Unit,
    onError: (String) -> Unit,
  ) {
    activity.runOnUiThread {
      onLoadingChanged(true)
    }

    Thread {
      try {
        val apkFile = downloadApkInternal(info)
        activity.runOnUiThread {
          onLoadingChanged(false)
          onSuccess(apkFile)
        }
      } catch (error: Exception) {
        activity.runOnUiThread {
          onLoadingChanged(false)
          onError(error.message ?: "下载更新失败")
        }
      }
    }.start()
  }

  fun installApk(apkFile: File, force: Boolean) {
    val uri =
      FileProvider.getUriForFile(
        activity,
        "${activity.packageName}.fileprovider",
        apkFile,
      )

    val intent =
      Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, APK_MIME_TYPE)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }

    try {
      activity.startActivity(intent)
      if (force) {
        activity.finishAffinity()
      }
    } catch (error: ActivityNotFoundException) {
      throw IOException("未找到可用的安装程序")
    }
  }

  private fun fetchUpdateInfo(versionUrl: String): ApkUpdateInfo {
    val connection =
      (URL(versionUrl).openConnection() as HttpURLConnection).apply {
        instanceFollowRedirects = true
        requestMethod = "GET"
        connectTimeout = NETWORK_TIMEOUT_MS
        readTimeout = NETWORK_TIMEOUT_MS
        useCaches = false
        setRequestProperty("Accept", "application/json")
        setRequestProperty("Cache-Control", "no-cache")
        setRequestProperty("Pragma", "no-cache")
      }

    try {
      val status = connection.responseCode
      if (status !in 200..299) {
        throw IOException("版本信息请求失败: HTTP $status")
      }

      val payload = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
      val json = JSONObject(payload)
      val versionCode = json.optInt("versionCode", -1)
      val versionName = json.optString("versionName", "").trim()
      val apkUrl = json.optString("apkUrl", "").trim()
      val notes = json.optString("notes", "").trim()
      val minSupportedVersionCode = json.optInt("minSupportedVersionCode", 0)
      val force = json.optBoolean("force", false)

      if (versionCode <= 0 || versionName.isBlank() || apkUrl.isBlank()) {
        throw IOException("版本信息格式不正确")
      }

      return ApkUpdateInfo(
        versionCode = versionCode,
        versionName = versionName,
        apkUrl = apkUrl,
        force = force,
        notes = notes,
        minSupportedVersionCode = minSupportedVersionCode,
      )
    } finally {
      connection.disconnect()
    }
  }

  private fun downloadApkInternal(info: ApkUpdateInfo): File {
    val connection =
      (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
        instanceFollowRedirects = true
        requestMethod = "GET"
        connectTimeout = NETWORK_TIMEOUT_MS
        readTimeout = NETWORK_TIMEOUT_MS
        useCaches = false
      }

    val downloadDir = File(activity.cacheDir, UPDATE_CACHE_DIR).apply { mkdirs() }
    val fileName = buildApkFileName(info)
    val apkFile = File(downloadDir, fileName)

    try {
      val status = connection.responseCode
      if (status !in 200..299) {
        throw IOException("APK 下载失败: HTTP $status")
      }

      connection.inputStream.use { input ->
        FileOutputStream(apkFile).use { output ->
          input.copyTo(output)
          output.flush()
        }
      }

      if (!apkFile.exists() || apkFile.length() <= 0) {
        throw IOException("APK 下载结果为空")
      }

      return apkFile
    } catch (error: Exception) {
      apkFile.delete()
      throw error
    } finally {
      connection.disconnect()
    }
  }

  private fun getCurrentVersionCode(): Int {
    val packageInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
    return androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(packageInfo).toInt()
  }

  private fun getCurrentVersionName(): String {
    val packageInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
    return packageInfo.versionName.orEmpty()
  }

  private fun buildApkFileName(info: ApkUpdateInfo): String {
    val safeVersionName = info.versionName.replace(Regex("""[^\w.\-]"""), "_")
    return "xyzw-web-helper-${safeVersionName}-${info.versionCode}.apk"
  }

  companion object {
    const val DEFAULT_VERSION_URL = "https://xyzw-9bj.pages.dev/apk/version.json"
    private const val NETWORK_TIMEOUT_MS = 15_000
    private const val UPDATE_CACHE_DIR = "apk-update"
    private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
  }
}
