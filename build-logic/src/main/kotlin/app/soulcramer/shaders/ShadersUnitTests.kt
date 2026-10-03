package app.soulcramer.shaders

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
import org.gradle.api.tasks.testing.logging.TestLogEvent.STARTED
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * Inspired by https://github.com/slackhq/slack-gradle-plugin
 */
internal object ShadersUnitTests {
    private const val GLOBAL_CI_UNIT_TEST_TASK_NAME = "globalCiUnitTest"
    private const val CI_UNIT_TEST_TASK_NAME = "ciUnitTest"
    private const val COMPILE_CI_UNIT_TEST_NAME = "compileCiUnitTest"
    private const val LOG = "SparkUnitTests:"

    fun configureRootProject(project: Project): TaskProvider<Task> =
        project.tasks.register(GLOBAL_CI_UNIT_TEST_TASK_NAME) {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "Global lifecycle task to run all ciUnitTest tasks."
        }

    fun configureSubproject(project: Project) {
        project.pluginManager.withPlugin("com.android.base") {
            createAndroidCiUnitTestTask(project)
        }
        configureTestTasks(project)
    }

    private fun createAndroidCiUnitTestTask(project: Project) {
        val variant = project.spark().ciUnitTestVariant.get().capitalized()
        val variantUnitTestTaskName = "test${variant}UnitTest"
        val variantCompileUnitTestTaskName = "compile${variant}UnitTestSources"
        project.logger.debug("$LOG Creating CI unit test tasks for project '$project' and variant '$variant'")
        project.tasks.register(CI_UNIT_TEST_TASK_NAME) {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            dependsOn(variantUnitTestTaskName)
        }
        project.tasks.register(COMPILE_CI_UNIT_TEST_NAME) {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            dependsOn(variantCompileUnitTestTaskName)
        }
    }

    private fun configureTestTasks(project: Project) {
        project.tasks.withType<Test>().configureEach {
            // Bump max heap space for paparazzi
            // https://github.com/cashapp/paparazzi/issues/915
            maxHeapSize = "1g"
            testLogging {
                showStandardStreams = true
                showStackTraces = true
                exceptionFormat = FULL
                events(STARTED, PASSED, FAILED, SKIPPED)
            }
        }
    }
}
