plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.dependencyGuard) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.compose) apply false

    id("app.soulcramer.shaders.root")
    id("app.soulcramer.shaders.dokka")
    id("app.soulcramer.shaders.spotless")
}

dependencies {
    dokka(projects.colorblindness)
}

tasks.named("globalCiUnitTest") {
    dependsOn(":app:ciUnitTest", ":colorblindness:ciUnitTest")
}
