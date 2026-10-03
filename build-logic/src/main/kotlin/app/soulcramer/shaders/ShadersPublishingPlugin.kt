package app.soulcramer.shaders

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.gradle.plugins.signing.SigningExtension

internal class ShadersPublishingPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.gradle.maven-publish")
            apply(plugin = "org.gradle.signing")

            configureRepository()
            registerPublication()
            configureSigning()
        }
    }

    private fun Project.configureRepository() = configure<PublishingExtension> {
        repositories {
            mavenLocal {
                name = "Local"
                url = uri(isolated.rootProject.projectDirectory.dir("build/.m2/repository"))
            }
            maven {
                name = "OSSRH"
                url = when (version.toString().endsWith("-SNAPSHOT")) {
                    true -> "https://s01.oss.sonatype.org/content/repositories/snapshots/"
                    false -> "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
                }.let(::uri)
                credentials {
                    username = System.getenv("OSSRH_USERNAME")
                    password = System.getenv("OSSRH_TOKEN")
                }
            }
        }
    }

    private fun Project.registerPublication() = configure<PublishingExtension> {
        publications {
            register<MavenPublication>("maven") {
                when {
                    isAndroidLibrary -> configureAndroidPublication(this)
                    isJavaPlatform -> from(components["javaPlatform"])
                    else -> TODO("Unsupported project type $this")
                }
                configurePom()
            }
        }
    }

    private fun Project.configureAndroidPublication(publication: MavenPublication) {
        configure<LibraryExtension> {
            publishing {
                singleVariant("release") {
                    withSourcesJar()
                    withJavadocJar()
                }
            }
        }
        // AGP creates software components during the afterEvaluate callback step...
        afterEvaluate {
            publication.from(components.getByName("release"))
        }
    }

    private fun MavenPublication.configurePom() = pom {
        name = "Acerola Shaders"
        description = "Acerola Shaders"
        url = "https://github.com/soulcramer/AcerolaShaders"
        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/licenses/MIT"
            }
        }
        scm {
            url = "https://github.com/soulcramer/AcerolaShaders"
        }
        developers {
            developer {
                name = "Scott Rayapoullé"
            }
        }
    }

    private fun Project.configureSigning() = configure<SigningExtension> signing@{
        val signingKey = findProperty("signingKey") as String?
        val signingPassword = findProperty("signingPassword") as String?
        if (signingKey == null || signingPassword == null) return@signing
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(the<PublishingExtension>().publications)
    }
}
