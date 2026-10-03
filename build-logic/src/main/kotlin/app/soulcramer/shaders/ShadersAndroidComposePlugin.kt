package app.soulcramer.shaders

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

internal class ShadersAndroidComposePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.plugin.compose")
            apply(plugin = "app.soulcramer.shaders.android")

            android {
                buildFeatures.compose = true
            }

            dependencies {
                add("implementation", platform(spark().libraries.`androidx-compose-bom`))
            }
        }
    }
}
