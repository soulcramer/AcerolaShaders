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
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import kotlin.reflect.KProperty

plugins {
    `kotlin-dsl`
    alias(libs.plugins.spotless)
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        allWarningsAsErrors = true
        jvmTarget = JvmTarget.JVM_17
    }
    explicitApi()
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(libs.gradlePlugins.android)
    compileOnly(libs.gradlePlugins.kotlin)
    compileOnly(libs.gradlePlugins.ksp)
    compileOnly(libs.gradlePlugins.compose)
    compileOnly(libs.gradlePlugins.dependencyGuard)
    compileOnly(libs.gradlePlugins.dokka)
    compileOnly(libs.gradlePlugins.spotless)
    implementation(libs.dokka.base)
}

gradlePlugin {
    plugins {
        create("app.soulcramer.shaders.ShadersRootPlugin", id = "app.soulcramer.shaders.root")
        create("app.soulcramer.shaders.ShadersAndroidPlugin", id = "app.soulcramer.shaders.android")
        create("app.soulcramer.shaders.ShadersAndroidApplicationPlugin", id = "app.soulcramer.shaders.android-application")
        create("app.soulcramer.shaders.ShadersAndroidLibraryPlugin", id = "app.soulcramer.shaders.android-library")
        create("app.soulcramer.shaders.ShadersAndroidComposePlugin", id = "app.soulcramer.shaders.android-compose")
        create("app.soulcramer.shaders.ShadersAndroidLintPlugin", id = "app.soulcramer.shaders.android-lint")
        create("app.soulcramer.shaders.ShadersPublishingPlugin", id = "app.soulcramer.shaders.publishing")
        create("app.soulcramer.shaders.ShadersKotlinJvmPlugin", id = "app.soulcramer.shaders.kotlin-jvm")
        create("app.soulcramer.shaders.ShadersKspPlugin", id = "app.soulcramer.shaders.ksp")
        create("app.soulcramer.shaders.ShadersDokkaPlugin", id = "app.soulcramer.shaders.dokka")
        create("app.soulcramer.shaders.ShadersDependencyGuardPlugin", id = "app.soulcramer.shaders.dependencyGuard")
        create("app.soulcramer.shaders.ShadersSpotlessPlugin", id = "app.soulcramer.shaders.spotless")
    }
}

fun NamedDomainObjectContainer<PluginDeclaration>.create(
    implementationClass: String,
    id: String,
    name: String = implementationClass.removeSuffix("Plugin"),
) = create(name) {
    this.id = id
    this.implementationClass = implementationClass
}

private operator fun VersionCatalog.getValue(
    thisRef: Any?,
    property: KProperty<*>,
) = findVersion(property.name).orElseThrow {
    IllegalStateException("Missing catalog version ${property.name}")
}

val ktlint: VersionConstraint by extensions.getByType<VersionCatalogsExtension>().named("libs")

// This block is a copy of SparkSpotlessPlugin since this included build can't use it's own plugins...
spotless {
    val licenseHeader = rootProject.file("./../spotless/spotless.kt")
    format("misc") {
        target("**/*.md", "**/.gitignore")
        endWithNewline()
    }
    kotlin {
        target("src/**/*.kt")
        ktlint(ktlint.toString())
        trimTrailingWhitespace()
        endWithNewline()
        licenseHeaderFile(licenseHeader)
    }
    kotlinGradle {
        ktlint(ktlint.toString())
        trimTrailingWhitespace()
        endWithNewline()
        licenseHeaderFile(
            licenseHeader,
            "(import |plugins|pluginManagement|rootProject|dependencyResolutionManagement|//)",
        )
    }
}
