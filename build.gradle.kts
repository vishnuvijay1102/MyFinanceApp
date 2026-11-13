// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
}

// Automatically reconstitute debug.keystore from debug.keystore.base64 if missing when building in local Android Studio
val debugKeystore = file("${rootDir}/debug.keystore")
val base64File = file("${rootDir}/debug.keystore.base64")
if (!debugKeystore.exists() && base64File.exists()) {
  try {
    val base64Content = base64File.readText().trim()
    if (base64Content.isNotEmpty()) {
      val decoded = java.util.Base64.getDecoder().decode(base64Content)
      debugKeystore.writeBytes(decoded)
    }
  } catch (e: Exception) {
    println("Could not auto-generate debug.keystore: ${e.message}")
  }
}
