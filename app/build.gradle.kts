import java.util.Properties

plugins {
    id("app.soulcramer.shaders.android-application")
    id("app.soulcramer.shaders.android-compose")
    id("app.soulcramer.shaders.spotless")
}

android {
    namespace = "app.soulcramer.shaders.app"
    defaultConfig.applicationId = "app.soulcramer.shaders.app"
    defaultConfig {
        versionName = version.toString()
        if (providers.environmentVariable("GITHUB_ACTION").isPresent) {
            versionName = version.toString().replace("SNAPSHOT", System.getenv("GITHUB_SHA").take(7))
        }
    }

    val keystore = isolated.rootProject.projectDirectory.file("keystore.properties").asFile
        .takeIf { it.exists() }
        ?.let { Properties().apply { load(it.inputStream()) } }

    val debug = signingConfigs.getByName("debug")
    val release = signingConfigs.create("release") {
        if (keystore == null) return@create
        keyAlias = keystore.getProperty("keyAlias")
        keyPassword = keystore.getProperty("keyPassword")
        storeFile = file(keystore.getProperty("storeFile"))
        storePassword = keystore.getProperty("storePassword")
    }

    buildTypes.named("release") {
        signingConfig = if (keystore != null) release else debug
    }
}

dependencies {
    implementation(projects.colorblindness)
    implementation(projects.crt)
    implementation(projects.differenceofgaussians)
    implementation(projects.paletteswap)

    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.coilCompose)
    implementation(libs.colorpickerCompose)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
