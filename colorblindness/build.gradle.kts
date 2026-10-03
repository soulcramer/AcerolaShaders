plugins {
    id("app.soulcramer.shaders.android-library")
    id("app.soulcramer.shaders.android-compose")
    id("app.soulcramer.shaders.dokka")
    id("app.soulcramer.shaders.publishing")
    id("app.soulcramer.shaders.dependencyGuard")
    id("app.soulcramer.shaders.spotless")
}

android {
    namespace = "app.soulcramer.shaders.colorblindness"
    defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
