plugins {
    id("app.soulcramer.shaders.android-library")
    id("app.soulcramer.shaders.android-compose")
    id("app.soulcramer.shaders.dokka")
    id("app.soulcramer.shaders.publishing")
    id("app.soulcramer.shaders.dependencyGuard")
    id("app.soulcramer.shaders.spotless")
    id("app.soulcramer.shaders.screenshot")
}

android {
    namespace = "app.soulcramer.shaders.core"
    defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.compose.foundation)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
