plugins {
  id("com.android.application")
  kotlin("android")
}

buildDir = file("${System.getProperty("java.io.tmpdir")}/xyzw-web-helper-android-build/app")

val androidTempAssetsRootDir = file("${System.getProperty("java.io.tmpdir")}/xyzw-web-helper-android-assets")

android {
  namespace = "com.xyzw.webhelper"
  compileSdk = 34

  sourceSets {
    getByName("main") {
      // Use a temp directory outside SynologyDrive so WebView assets are always physical files.
      assets.setSrcDirs(listOf(androidTempAssetsRootDir))
    }
  }

  defaultConfig {
    applicationId = "com.xyzw.webhelper"
    minSdk = 24
    targetSdk = 34
    versionCode = 4
    versionName = "1.0.4"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
      )
    }
    debug {
      isMinifyEnabled = false
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlinOptions {
    jvmTarget = "17"
  }
}

dependencies {
  implementation("androidx.activity:activity-ktx:1.9.2")
  implementation("androidx.appcompat:appcompat:1.7.0")
  implementation("androidx.constraintlayout:constraintlayout:2.1.4")
  implementation("androidx.core:core-ktx:1.13.1")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
  implementation("androidx.webkit:webkit:1.11.0")
  implementation("com.google.android.material:material:1.12.0")
}
