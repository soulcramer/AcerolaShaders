/*
 * Copyright (c) 2023 Adevinta
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
        // Aggregate this module into the root project documentation
        rootProject.dependencies.add("dokka", rootProject.dependencies.project(mapOf("path" to path)))

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
