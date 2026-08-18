package io.bluetape4k.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.io.File

class DetektSourceCoverageTaskIntegrationTest {

    @Test
    fun `detekt coverage task stores and reuses configuration cache`() {
        val projectDir = createFixtureProject()
        try {
            val arguments = arrayOf(
                "detekt",
                "--configuration-cache",
                "--configuration-cache-problems=fail",
                "--console=plain",
            )

            val firstRun = run(projectDir, arguments)
            assertEquals(TaskOutcome.SUCCESS, firstRun.task(":detekt")?.outcome)
            assertContains(firstRun.output, "Configuration cache entry stored")

            val secondRun = run(projectDir, arguments)
            assertContains(secondRun.output, "Configuration cache entry reused")

            val report = File(projectDir, "build/reports/detekt/source-coverage.md").readText()
            assertContains(report, "- Included modules: 1")
            assertContains(report, "- Kotlin source files: 2 (main: 1, test: 1)")
            assertContains(report, "- Empty included modules: 0")
            assertTrue(report.indexOf("`:alpha`") < report.indexOf("`:zeta`"))
        } finally {
            projectDir.deleteRecursively()
        }
    }

    @Test
    fun `detekt coverage task fails closed for an empty included module`() {
        val projectDir = createFixtureProject(includeEmptyModule = true)
        try {
            val result = runAndFail(
                projectDir,
                "detekt",
                "-PincludeEmptyModule=true",
                "--configuration-cache",
                "--configuration-cache-problems=fail",
                "--console=plain",
            )

            assertContains(result.output, "Detekt source coverage is empty for included modules: :empty")
        } finally {
            projectDir.deleteRecursively()
        }
    }

    private fun createFixtureProject(includeEmptyModule: Boolean = false): File {
        val projectDir = createTempDirectory("detekt-source-coverage-fixture").toFile()
        val mainSource = File(projectDir, "src/main/kotlin/Fixture.kt")
        val testSource = File(projectDir, "src/test/kotlin/FixtureTest.kt")
        mainSource.parentFile.mkdirs()
        testSource.parentFile.mkdirs()
        mainSource.writeText("class Fixture")
        testSource.writeText("class FixtureTest")
        if (includeEmptyModule) {
            File(projectDir, "empty").mkdirs()
        }

        File(projectDir, "settings.gradle.kts").writeText("rootProject.name = \"detekt-source-coverage-fixture\"\n")
        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                id("io.bluetape4k.detekt-fixture")
            }

            tasks.register("detekt") {
                dependsOn("detektSourceCoverage")
            }
            """.trimIndent() + "\n",
        )
        return projectDir
    }

    private fun run(projectDir: File, arguments: Array<String>) =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(*arguments)
            .withPluginClasspath()
            .build()

    private fun runAndFail(projectDir: File, vararg arguments: String) =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(*arguments)
            .withPluginClasspath()
            .buildAndFail()
}
