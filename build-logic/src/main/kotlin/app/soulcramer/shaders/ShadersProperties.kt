package app.soulcramer.shaders

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.kotlin.dsl.provideDelegate
import kotlin.reflect.KProperty

internal fun Project.spark() = SparkProperties(this)

internal class SparkProperties private constructor(project: Project) {
    private val catalog by lazy(project::getVersionsCatalog)
    val libraries by lazy { SparkLibraries(catalog) }
    val versions by lazy { SparkVersions(catalog) }
    val ciUnitTestVariant = project.providers.gradleProperty("spark.ci-unit-test.variant").orElse("debug")

    companion object {
        private const val EXT_KEY = "app.soulcramer.shaders.SparkProperties"
        operator fun invoke(project: Project): SparkProperties = project.getOrCreateExtra(EXT_KEY, ::SparkProperties)
    }
}

@Suppress("HasPlatformType", "PropertyName")
internal class SparkVersions(catalog: VersionCatalog) {
    val `targetSdk` by catalog
    val `minSdk` by catalog
    val `minCompileSdk` by catalog
    val `compileSdk` by catalog
    val `ktlint` by catalog

    private operator fun VersionCatalog.getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ) = findVersion(property.name).orElseThrow {
        IllegalStateException("Missing catalog version ${property.name}")
    }
}

@Suppress("HasPlatformType", "PropertyName")
internal class SparkLibraries(catalog: VersionCatalog) {
    val `androidx-compose-bom` by catalog
    val `dokka-android-documentation-plugin` by catalog
    val `kotlin-bom` by catalog

    private operator fun VersionCatalog.getValue(
        thisRef: Any?,
        property: KProperty<*>,
    ) = findLibrary(property.name).orElseThrow {
        IllegalStateException("Missing catalog library ${property.name}")
    }
}
