import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
}

fun String.asBuildConfigString(): String = buildString {
  append('"')
  for (character in this@asBuildConfigString) {
    when (character) {
      '\\' -> append("\\\\")
      '"' -> append("\\\"")
      '\n' -> append("\\n")
      '\r' -> append("\\r")
      else -> append(character)
    }
  }
  append('"')
}

/**
 * A typed task keeps release credential validation compatible with Gradle's
 * configuration cache. It deliberately accepts only a boolean and keystore
 * path as task inputs—never a password or key alias.
 */
abstract class VerifyReleaseSigningTask : DefaultTask() {
  @get:Input
  abstract val signingConfigured: Property<Boolean>

  @get:Input
  abstract val keystorePath: Property<String>

  @TaskAction
  fun verify() {
    check(signingConfigured.get()) {
      "Release signing requires KEYSTORE_PATH, STORE_PASSWORD, KEY_ALIAS, and KEY_PASSWORD."
    }
    check(File(keystorePath.get()).isFile) {
      "KEYSTORE_PATH does not point to a readable release keystore."
    }
  }
}

val localProperties = Properties().apply {
  val localPropertiesFile = rootProject.file("local.properties")
  if (localPropertiesFile.isFile) {
    localPropertiesFile.inputStream().use(::load)
  }
}

val releaseKeystorePath = providers.environmentVariable("KEYSTORE_PATH").orNull
val releaseStorePassword = providers.environmentVariable("STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("KEY_PASSWORD").orNull
val releaseSigningConfigured = listOf(
  releaseKeystorePath,
  releaseStorePassword,
  releaseKeyAlias,
  releaseKeyPassword
).all { !it.isNullOrBlank() }
val releaseKeystorePathForValidation = releaseKeystorePath
  ?.let { file(it).absolutePath }
  .orEmpty()

android {
  namespace = "com.cinnamon.app"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.cinnamon.app"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    // A gateway URL is public configuration, never a provider credential. Keep it
    // blank by default so unreleased builds fail closed into offline guided practice.
    val aiGatewayBaseUrl = providers.gradleProperty("AI_GATEWAY_BASE_URL").orNull
      ?: localProperties.getProperty("AI_GATEWAY_BASE_URL").orEmpty()
    buildConfigField("String", "AI_GATEWAY_BASE_URL", aiGatewayBaseUrl.asBuildConfigString())

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      // Never select a developer-local key as a release fallback. An unconfigured
      // release is deliberately invalid and release tasks fail before signing.
      storeFile = file(releaseKeystorePath ?: "__release_keystore_not_configured__")
      storePassword = releaseStorePassword
      keyAlias = releaseKeyAlias
      keyPassword = releaseKeyPassword
    }
  }

  buildTypes {
    release {
      isCrunchPngs = true
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      // Use Android Gradle Plugin's standard per-user debug signing config.
      // It is intentionally separate from the explicit release credential path.
    }
    create("benchmark") {
      // Manual Baseline Profile collection requires a non-debuggable build
      // without R8 optimization. This variant is never a production fallback:
      // it is explicitly signed with the per-user debug key and keeps release
      // signing fail-closed.
      initWith(getByName("release"))
      isDebuggable = false
      isMinifyEnabled = false
      isShrinkResources = false
      signingConfig = signingConfigs.getByName("debug")
      matchingFallbacks += listOf("release")
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
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

val verifyReleaseSigning = tasks.register<VerifyReleaseSigningTask>("verifyReleaseSigning") {
  group = "verification"
  description = "Fails release-producing tasks unless explicit signing credentials are configured."
  signingConfigured.set(releaseSigningConfigured)
  keystorePath.set(releaseKeystorePathForValidation)
}

tasks.matching { task ->
  task.name.startsWith("assembleRelease") ||
    task.name.startsWith("bundleRelease") ||
    task.name.startsWith("packageRelease") ||
    task.name.startsWith("validateSigningRelease")
}.configureEach {
  dependsOn(verifyReleaseSigning)
}

ksp {
  arg("room.schemaLocation", file("schemas").path)
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.androidx.profileinstaller)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
