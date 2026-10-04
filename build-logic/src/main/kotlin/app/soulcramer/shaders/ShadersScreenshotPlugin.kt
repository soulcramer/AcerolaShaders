package app.soulcramer.shaders

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

internal class ShadersScreenshotPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "app.cash.paparazzi")

            val variant = spark().ciUnitTestVariant.get().capitalized()
            // test<Variant>UnitTest records but never compares; only verifyPaparazzi compares.
            tasks.named { it == "ciUnitTest" }.configureEach {
                dependsOn("verifyPaparazzi$variant")
            }
        }
    }
}
