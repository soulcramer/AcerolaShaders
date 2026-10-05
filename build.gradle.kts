plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.dependencyGuard) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.paparazzi) apply false

    id("app.soulcramer.shaders.root")
    id("app.soulcramer.shaders.dokka")
    id("app.soulcramer.shaders.spotless")
}

dependencies {
    dokka(projects.colorblindness)
    dokka(projects.crt)
    dokka(projects.differenceofgaussians)
    dokka(projects.shadersCore)
}

tasks.named("globalCiUnitTest") {
    dependsOn(
        ":app:ciUnitTest",
        ":colorblindness:ciUnitTest",
        ":crt:ciUnitTest",
        ":differenceofgaussians:ciUnitTest",
        ":shaders-core:ciUnitTest",
    )
}
