package app.soulcramer.shaders

import org.gradle.api.Plugin
import org.gradle.api.Project

internal class ShadersRootPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            ShadersUnitTests.configureRootProject(project)
        }
    }
}
