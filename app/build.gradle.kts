/*
 * Copyright (c) 2024 Adevinta
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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

    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.coilCompose)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
