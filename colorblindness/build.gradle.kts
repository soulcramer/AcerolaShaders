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
plugins {
    id("app.soulcramer.shaders.android-library")
    id("app.soulcramer.shaders.android-compose")
    id("app.soulcramer.shaders.ksp")
    id("app.soulcramer.shaders.dokka")
    id("app.soulcramer.shaders.publishing")
    id("app.soulcramer.shaders.dependencyGuard")
}

android {
    namespace = "app.soulcramer.shaders.colorblindness"
    resourcePrefix = "spark_"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=app.soulcramer.shaders.InternalSparkApi",
            "-opt-in=app.soulcramer.shaders.ExperimentalSparkApi",
        )
    }
}

dependencies {
    lintChecks(libs.slack.lint.compose)

    implementation(libs.androidx.appCompat.resources)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.savedstate)
    implementation(libs.androidx.window)

    api(libs.androidx.compose.animation.graphics)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.text)
    api(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.util)
    api(libs.kotlinx.collections.immutable)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.androidx.test.truth)
    testImplementation(libs.androidx.test.runner)
    testImplementation(libs.testParameterInjector)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test)
    testImplementation(libs.androidx.compose.ui.testManifest)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.kotlin.test)
    androidTestImplementation(libs.androidx.test.truth)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.compose.ui.test)
    androidTestImplementation(libs.androidx.compose.ui.testManifest)
}
