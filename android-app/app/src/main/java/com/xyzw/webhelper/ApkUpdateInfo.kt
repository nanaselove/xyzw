package com.xyzw.webhelper

data class ApkUpdateInfo(
  val versionCode: Int,
  val versionName: String,
  val apkUrl: String,
  val force: Boolean,
  val notes: String,
  val minSupportedVersionCode: Int,
)
