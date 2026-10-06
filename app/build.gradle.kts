
plugins {
  id("org.jetbrains.kotlin.android")
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
}

android {
  namespace = "com.ultimate.nossl"
  compileSdk = 35
  ndkVersion = "26.1.10909125"

  defaultConfig {
    applicationId = "com.ultimate.nossl"
    minSdk = 24
    targetSdk = 35
    versionCode = 2
    versionName = "2.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    externalNativeBuild {
      cmake {
        cppFlags("-std=c++17")
        abiFilters("armeabi-v7a", "arm64-v8a")
      }
    }
  }

  externalNativeBuild {
    cmake {
      path = file("src/main/cpp/CMakeLists.txt")
      version = "3.22.1"
    }
  }

  signingConfigs {
    create("release") {
        providers.gradleProperty("ULTIMATE_KEYSTORE_PATH").orNull?.let {
            storeFile = file(it)
            storePassword = providers.gradleProperty("ULTIMATE_STORE_PASSWORD").orNull
            keyAlias = providers.gradleProperty("ULTIMATE_KEY_ALIAS").orNull
            keyPassword = providers.gradleProperty("ULTIMATE_KEY_PASSWORD").orNull
        }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.findByName("release")
    }
    debug { }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions {
    jvmTarget = "17"
  }
  buildFeatures {
    compose = true
    buildConfig = true
    prefab = true
  }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  ksp(libs.androidx.room.compiler)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.shadowhook)
  compileOnly("de.robv.android.xposed:api:82")
  testImplementation(libs.junit)
  debugImplementation(libs.androidx.compose.ui.tooling)
}
