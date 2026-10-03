package app.soulcramer.shaders

import com.dropbox.gradle.plugins.dependencyguard.DependencyGuardPluginExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

internal class ShadersDependencyGuardPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.dropbox.dependency-guard")

            configure<DependencyGuardPluginExtension> {
                configuration("releaseRuntimeClasspath")
            }
        }
    }
}
