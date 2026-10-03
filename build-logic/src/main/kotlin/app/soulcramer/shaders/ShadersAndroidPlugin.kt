package app.soulcramer.shaders

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

public class ShadersAndroidPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            configureKotlin()

            configureAndroid {
                compileSdk = spark().versions.compileSdk.toString().toInt()
                defaultConfig.minSdk = spark().versions.minSdk.toString().toInt()
                packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
                lint.apply {
                    warningsAsErrors = true
                    lintConfig = file("lint.xml").takeIf(File::exists)
                }
            }

            addKotlinBom()
            ShadersUnitTests.configureSubproject(this)
        }
    }
}
