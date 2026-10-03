package app.soulcramer.shaders

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

internal class ShadersAndroidLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.library")
            apply(plugin = "app.soulcramer.shaders.android")
            androidLibrary {
                defaultConfig {
                    consumerProguardFile("consumer-rules.pro")
                    aarMetadata.minCompileSdk = spark().versions.minCompileSdk.toString().toInt()
                }
            }
        }
    }
}
