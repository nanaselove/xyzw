package com.xyzw.webhelper

import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.JavascriptInterface
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class WebAppBridge(private val activity: MainActivity) {
  @JavascriptInterface
  fun saveBase64File(base64: String, fileName: String, mimeType: String) {
    Thread {
      try {
        persistFile(base64, fileName, mimeType, shareable = false)
        activity.runOnUiThread {
          Toast.makeText(
            activity,
            activity.getString(R.string.file_saved, fileName),
            Toast.LENGTH_SHORT,
          ).show()
        }
      } catch (error: Exception) {
        activity.runOnUiThread {
          Toast.makeText(
            activity,
            activity.getString(R.string.file_save_failed),
            Toast.LENGTH_SHORT,
          ).show()
        }
      }
    }.start()
  }

  @JavascriptInterface
  fun shareBase64File(base64: String, fileName: String, mimeType: String) {
    Thread {
      try {
        val file = persistFile(base64, fileName, mimeType, shareable = true)
        activity.runOnUiThread {
          activity.shareFile(file, mimeType, activity.getString(R.string.share_title))
        }
      } catch (error: Exception) {
        activity.runOnUiThread {
          Toast.makeText(
            activity,
            activity.getString(R.string.share_failed),
            Toast.LENGTH_SHORT,
          ).show()
        }
      }
    }.start()
  }

  @JavascriptInterface
  fun shareText(text: String, title: String) {
    activity.runOnUiThread {
      activity.shareText(text, title)
    }
  }

  @JavascriptInterface
  fun openExternalUrl(url: String) {
    activity.runOnUiThread {
      activity.openExternalUrl(url)
    }
  }

  @JavascriptInterface
  fun requestText(
    requestId: String,
    url: String,
    method: String,
    headersJson: String,
    body: String,
    timeoutMs: Int,
  ) {
    Thread {
      val responseJson = try {
        performTextRequest(url, method, headersJson, body, timeoutMs)
      } catch (error: Exception) {
        JSONObject().apply {
          put("status", -1)
          put("body", "")
          put("url", url)
          put("error", error.message ?: error.toString())
        }.toString()
      }

      activity.deliverNativeHttpResponse(requestId, responseJson)
    }.start()
  }

  private fun persistFile(
    base64: String,
    fileName: String,
    mimeType: String,
    shareable: Boolean,
  ): File {
    val cleanName = sanitizeFileName(fileName)
    val bytes = Base64.decode(stripDataPrefix(base64), Base64.DEFAULT)

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !shareable) {
      saveToMediaStore(cleanName, mimeType, bytes)
    } else {
      saveToAppStorage(cleanName, bytes, shareable)
    }
  }

  private fun saveToMediaStore(
    fileName: String,
    mimeType: String,
    bytes: ByteArray,
  ): File {
    val resolver = activity.contentResolver
    val values =
      ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(
          MediaStore.MediaColumns.RELATIVE_PATH,
          Environment.DIRECTORY_DOWNLOADS + File.separator + "XYZW",
        )
        put(MediaStore.MediaColumns.IS_PENDING, 1)
      }

    val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
    val uri =
      resolver.insert(collection, values)
        ?: throw IllegalStateException("Unable to create media store entry")

    resolver.openOutputStream(uri)?.use { output ->
      output.write(bytes)
      output.flush()
    } ?: throw IllegalStateException("Unable to open output stream")

    values.clear()
    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
    resolver.update(uri, values, null, null)

    return File(activity.cacheDir, fileName)
  }

  private fun saveToAppStorage(
    fileName: String,
    bytes: ByteArray,
    shareable: Boolean,
  ): File {
    val directory =
      if (shareable) {
        File(activity.cacheDir, "share").apply { mkdirs() }
      } else {
        activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
          ?: File(activity.filesDir, "downloads")
      }

    if (!directory.exists()) {
      directory.mkdirs()
    }

    val file = File(directory, fileName)
    FileOutputStream(file).use { output ->
      output.write(bytes)
      output.flush()
    }
    return file
  }

  private fun stripDataPrefix(base64: String): String {
    val trimmed = base64.trim()
    val commaIndex = trimmed.indexOf(",")
    return if (commaIndex >= 0) trimmed.substring(commaIndex + 1) else trimmed
  }

  private fun sanitizeFileName(fileName: String): String {
    return fileName.replace(Regex("""[\\/:*?"<>|]"""), "_")
  }

  private fun performTextRequest(
    url: String,
    method: String,
    headersJson: String,
    body: String,
    timeoutMs: Int,
  ): String {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
      instanceFollowRedirects = true
      requestMethod = method.ifBlank { "GET" }.uppercase()
      connectTimeout = timeoutMs.coerceAtLeast(1000)
      readTimeout = timeoutMs.coerceAtLeast(1000)
      useCaches = false
      doInput = true
      if (requestMethod != "GET" && requestMethod != "HEAD") {
        doOutput = true
      }
    }

    try {
      applyHeaders(connection, headersJson)

      if (connection.doOutput && body.isNotEmpty()) {
        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
          writer.write(body)
          writer.flush()
        }
      }

      val status = connection.responseCode
      val responseBody = readResponseBody(connection, status)
      val responseHeaders =
        JSONObject().apply {
          connection.headerFields.forEach { (name, values) ->
            if (!name.isNullOrBlank() && !values.isNullOrEmpty()) {
              put(name, values.joinToString(", "))
            }
          }
        }

      return JSONObject().apply {
        put("status", status)
        put("body", responseBody)
        put("url", connection.url.toString())
        put("headers", responseHeaders)
      }.toString()
    } finally {
      connection.disconnect()
    }
  }

  private fun applyHeaders(connection: HttpURLConnection, headersJson: String) {
    if (headersJson.isBlank()) {
      return
    }

    val headers = JSONObject(headersJson)
    val keys = headers.keys()
    while (keys.hasNext()) {
      val key = keys.next()
      val value = headers.optString(key, "")
      if (value.isNotBlank()) {
        connection.setRequestProperty(key, value)
      }
    }
  }

  private fun readResponseBody(connection: HttpURLConnection, status: Int): String {
    val stream =
      if (status >= HttpURLConnection.HTTP_BAD_REQUEST) {
        connection.errorStream ?: connection.inputStream
      } else {
        connection.inputStream
      }

    return try {
      stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
    } catch (error: Exception) {
      ""
    }
  }
}
