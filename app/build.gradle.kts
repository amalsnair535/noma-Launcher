import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
  alias(libs.plugins.firebase.crashlytics)
}

android {
  namespace = "com.freelauncher.app"
  compileSdk = 37

  defaultConfig {
    applicationId = "com.freelauncher.app"
    minSdk = 24
    targetSdk = 37
    versionCode = 43
    versionName = "5.2"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "$rootDir/my-upload-key.jks"
      val keystoreFile = file(keystorePath)
      if (keystoreFile.exists()) {
        storeFile = keystoreFile
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
        keyPassword = System.getenv("KEY_PASSWORD")
      }
    }
  }

  splits {
    abi {
      // Disable ABI splits for App Bundle compatibility and resource shrinking (AGP issue 402800800).
      // Play App Bundles (.aab) automatically split ABIs on Google Play servers.
      isEnable = false
      reset()
      include("arm64-v8a", "armeabi-v7a")
      isUniversalApk = true
    }
  }

  buildTypes {
    release {
      isCrunchPngs = true
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "$rootDir/my-upload-key.jks"
      if (file(keystorePath).exists()) {
        signingConfig = signingConfigs.getByName("release")
      } else {
        signingConfig = signingConfigs.getByName("debug")
      }
      ndk {
        debugSymbolLevel = "SYMBOL_TABLE"
      }
    }
    debug {
      signingConfig = signingConfigs.getByName("debug")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }

  lint {
    checkReleaseBuilds = true
    abortOnError = true
  }

  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
  packaging {
    jniLibs {
      // Store native libraries uncompressed for 16 KB page boundary zipalign in App Bundles (.aab)
      useLegacyPackaging = false
    }
  }
}

// Automatic 16 KB ELF Segment Alignment Gradle Task for Google Play Compliance
val buildDirFile = layout.buildDirectory.get().asFile
tasks.register("patchNativeLibs16KB") {
  val nativeDirs = listOf(
    File(buildDirFile, "intermediates/merged_native_libs"),
    File(buildDirFile, "intermediates/stripped_native_libs"),
    File(buildDirFile, "intermediates/intermediary_bundle")
  )
  doLast {
    for (baseDir in nativeDirs) {
      if (baseDir.exists()) {
        baseDir.walkTopDown().filter { it.extension == "so" }.forEach { soFile ->
          try {
            val bytes = soFile.readBytes()
            if (bytes.size > 64 && bytes[0] == 0x7F.toByte() && bytes[1] == 'E'.code.toByte() && bytes[2] == 'L'.code.toByte() && bytes[3] == 'F'.code.toByte()) {
              var modified = false
              val is64Bit = (bytes[4] == 2.toByte())
              if (is64Bit) {
                val phoff = ByteBuffer.wrap(bytes, 32, 8).order(ByteOrder.LITTLE_ENDIAN).long
                val phentsize = ByteBuffer.wrap(bytes, 54, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
                val phnum = ByteBuffer.wrap(bytes, 56, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()

                for (i in 0 until phnum) {
                  val offset = (phoff + (i * phentsize)).toInt()
                  if (offset + 56 <= bytes.size) {
                    val pType = ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int
                    if (pType == 1) { // PT_LOAD
                      val pAlignOffset = offset + 48
                      val pAlign = ByteBuffer.wrap(bytes, pAlignOffset, 8).order(ByteOrder.LITTLE_ENDIAN).long
                      if (pAlign < 16384L) {
                        val newAlignBytes = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(16384L).array()
                        System.arraycopy(newAlignBytes, 0, bytes, pAlignOffset, 8)
                        modified = true
                      }
                    }
                  }
                }
              } else {
                val phoff = ByteBuffer.wrap(bytes, 28, 4).order(ByteOrder.LITTLE_ENDIAN).int
                val phentsize = ByteBuffer.wrap(bytes, 42, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
                val phnum = ByteBuffer.wrap(bytes, 44, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()

                for (i in 0 until phnum) {
                  val offset = phoff + (i * phentsize)
                  if (offset + 32 <= bytes.size) {
                    val pType = ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int
                    if (pType == 1) { // PT_LOAD
                      val pAlignOffset = offset + 28
                      val pAlign = ByteBuffer.wrap(bytes, pAlignOffset, 4).order(ByteOrder.LITTLE_ENDIAN).int
                      if (pAlign < 16384) {
                        val newAlignBytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(16384).array()
                        System.arraycopy(newAlignBytes, 0, bytes, pAlignOffset, 4)
                        modified = true
                      }
                    }
                  }
                }
              }
              if (modified) {
                soFile.writeBytes(bytes)
                logger.quiet("16KB Alignment Patch: Successfully patched ${soFile.name} ELF segment alignment to 16 KB (0x4000)")
              }
            }
          } catch (e: Exception) {
            logger.warn("Failed to patch ELF alignment for ${soFile.name}: ${e.message}")
          }
        }
      }
    }
  }
}

tasks.matching {
  (it.name.contains("package") || it.name.contains("strip") || it.name.contains("Bundle")) && it.name != "patchNativeLibs16KB"
}.configureEach {
  finalizedBy("patchNativeLibs16KB")
}



// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  testImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.fragment.ktx)
  implementation(libs.androidx.biometric)
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
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.retrofit)
  implementation(libs.firebase.crashlytics)
  implementation(libs.firebase.analytics)
  implementation(libs.playAppUpdate)
  implementation(libs.playAppUpdateKtx)
  implementation(libs.play.services.location)
  implementation(libs.mlkitDigitalInk)
  implementation(libs.kotlinx.coroutines.play.services)
  testImplementation(libs.androidx.core)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.runner)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
