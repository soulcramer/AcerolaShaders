package app.soulcramer.shaders

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

internal class ShadersSpotlessPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.diffplug.spotless")

            val ktlint = spark().versions.ktlint
            configure<SpotlessExtension> {
                format("misc") {
                    // A file tree, because Spotless reads the build directory of every subproject
                    // for `**/` string targets, which isolated projects forbids.
                    target(
                        fileTree(projectDir) {
                            include("**/*.md", "**/.gitignore")
                            exclude(".git", ".gradle", "**/build")
                        },
                    )
                    endWithNewline()
                }
                kotlin {
                    target("src/**/*.kt")
                    ktlint(ktlint.toString())
                    trimTrailingWhitespace()
                    endWithNewline()
                }
                kotlinGradle {
                    ktlint(ktlint.toString())
                    trimTrailingWhitespace()
                    endWithNewline()
                }
            }
        }
    }
}
