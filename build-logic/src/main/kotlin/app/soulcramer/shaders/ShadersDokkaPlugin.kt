package app.soulcramer.shaders

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.plugins.DokkaHtmlPluginParameters
import java.time.Year

internal class ShadersDokkaPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.dokka")

            when {
                this === rootProject -> configureRootProject()
                else -> configureSubProject()
            }

            dependencies {
                add("dokkaPlugin", spark().libraries.`dokka-android-documentation-plugin`)
            }
        }
    }

    private fun Project.configureRootProject() = configure<DokkaExtension> {
        moduleName = "Acerola Shaders"
        dokkaPublications.named("html") {
            outputDirectory = layout.buildDirectory.dir("dokka")
        }
        pluginsConfiguration.withType<DokkaHtmlPluginParameters>().configureEach { configureFooterMessage() }
    }

    private fun Project.configureSubProject() {
        configure<DokkaExtension> {
            dokkaSourceSets.configureEach {
                // Parse Module and Package docs
                // https://kotlinlang.org/docs/dokka-module-and-package-docs.html
                projectDir.resolve("src").walk()
                    .filter { it.isFile && it.extension == "md" }.toList()
                    .let { includes.from(project.files(), it) }

                // https://kotlinlang.org/docs/dokka-gradle.html#source-link-configuration
                sourceLink {
                    localDirectory = projectDir.resolve("src")
                    remoteUrl("https://github.com/soulcramer/AcerolaShaders/tree/main/${project.name}/src")
                    remoteLineSuffix = "#L"
                }
            }
            pluginsConfiguration.withType<DokkaHtmlPluginParameters>().configureEach { configureFooterMessage() }
        }
    }

    private fun DokkaHtmlPluginParameters.configureFooterMessage() {
        footerMessage = "© ${Year.now().value} Scott Rayapoullé"
    }
}
