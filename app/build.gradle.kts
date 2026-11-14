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
    versionCode = 33
    versionName = "4.5"

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

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "$rootDir/my-upload-key.jks"
      signingConfig = if (file(keystorePath).exists()) {
        signingConfigs.getByName("release")
      } else {
        signingConfigs.getByName("debug")
      }
      ndk {
        debugSymbolLevel = "FULL"
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
      useLegacyPackaging = false
    }
  }
}

abstract class AlignElf16KbTask : DefaultTask() {
  @get:InputDirectory
  @get:Optional
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val jniDir1: DirectoryProperty

  @get:OutputDirectory
  @get:Optional
  abstract val outDir1: DirectoryProperty

  @get:InputDirectory
  @get:Optional
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val jniDir2: DirectoryProperty

  @get:OutputDirectory
  @get:Optional
  abstract val outDir2: DirectoryProperty

  @get:InputDirectory
  @get:Optional
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val jniDir3: DirectoryProperty

  @get:OutputDirectory
  @get:Optional
  abstract val outDir3: DirectoryProperty

  @TaskAction
  fun process() {
    listOfNotNull(jniDir1.orNull, jniDir2.orNull, jniDir3.orNull).forEach { dirProp ->
      val dir = dirProp.asFile
      if (dir.exists()) {
        dir.walkTopDown().filter { it.isFile && it.extension == "so" }.forEach { file ->
          patchElf(file)
        }
      }
    }
  }

  private fun patchElf(file: File): Boolean {
    if (!file.exists() || file.extension != "so") return false
    val bytes = file.readBytes()
    if (bytes.size < 64) return false
    if (bytes[0] != 0x7F.toByte() || bytes[1] != 0x45.toByte() || bytes[2] != 0x4C.toByte() || bytes[3] != 0x46.toByte()) return false
    if (bytes[4] != 2.toByte() || bytes[5] != 1.toByte()) return false

    val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    val e_phoff = buffer.getLong(0x20)
    val e_phentsize = buffer.getShort(0x36).toInt()
    val e_phnum = buffer.getShort(0x38).toInt()

    var modified = false
    for (i in 0 until e_phnum) {
      val phOffset = (e_phoff + i * e_phentsize).toInt()
      if (phOffset + 0x38 > bytes.size) break
      val p_type = buffer.getInt(phOffset)
      if (p_type == 1) { // PT_LOAD
        val p_align = buffer.getLong(phOffset + 0x30)
        if (p_align < 0x4000L) { // Less than 16 KB (16384)
          buffer.putLong(phOffset + 0x30, 0x4000L)
          modified = true
        }
      }
    }

    if (modified) {
      file.writeBytes(bytes)
    }
    return modified
  }
}

val alignElfTask = tasks.register("alignElf16Kb", AlignElf16KbTask::class.java) {
  description = "Aligns ELF program headers for 16 KB page size compatibility"
  val d1 = layout.buildDirectory.dir("intermediates/merged_jni_libs")
  val d2 = layout.buildDirectory.dir("intermediates/merged_native_libs")
  val d3 = layout.buildDirectory.dir("intermediates/stripped_native_libs")
  jniDir1.set(d1)
  outDir1.set(d1)
  jniDir2.set(d2)
  outDir2.set(d2)
  jniDir3.set(d3)
  outDir3.set(d3)
  mustRunAfter(tasks.matching { 
    (it.name.startsWith("merge") && (it.name.contains("Jni") || it.name.contains("Native"))) || it.name.startsWith("strip") || it.name.startsWith("extract")
  })
}

tasks.matching { it.name.startsWith("package") }.configureEach {
  dependsOn(alignElfTask)
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
