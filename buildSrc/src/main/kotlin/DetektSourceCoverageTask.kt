package io.bluetape4k.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Detekt가 분석하는 Kotlin 소스 범위를 검증하고 보고서로 기록합니다.
 *
 * 선언된 Gradle property와 file collection만 전달받으며, configuration
 * cache가 직렬화할 수 없는 [org.gradle.api.Project] 또는 빌드 스크립트
 * 객체를 task action이 보유하지 않습니다.
 */
abstract class DetektSourceCoverageTask : DefaultTask() {

    @get:Input
    abstract val projectPaths: ListProperty<String>

    @get:Input
    abstract val projectRoots: MapProperty<String, String>

    @get:Input
    abstract val explicitExclusions: MapProperty<String, String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    @TaskAction
    fun generateReport() {
        val files = sourceFiles.files
        val rows = projectPaths.get().map { projectPath ->
            val moduleRoot = Paths.get(projectRoots.get().getValue(projectPath)).normalize()
            val mainRoot = moduleRoot.resolve("src/main/kotlin")
            val testRoot = moduleRoot.resolve("src/test/kotlin")
            val mainCount = files.count { it.normalizedPath().isUnder(mainRoot) }
            val testCount = files.count { it.normalizedPath().isUnder(testRoot) }
            DetektSourceCoverageRow(
                projectPath = projectPath,
                mainCount = mainCount,
                testCount = testCount,
            )
        }
        val emptyModules = rows.filter { it.totalCount == 0 }
        val report = reportFile.get().asFile
        report.parentFile.mkdirs()
        report.writeText(DetektSourceCoverageReportRenderer.render(rows, explicitExclusions.get()))

        check(emptyModules.isEmpty()) {
            "Detekt source coverage is empty for included modules: " +
                emptyModules.joinToString { it.projectPath }
        }
        logger.lifecycle(
            "Detekt source coverage: ${rows.size} modules, ${rows.sumOf { it.totalCount }} Kotlin files " +
                "(main: ${rows.sumOf { it.mainCount }}, test: ${rows.sumOf { it.testCount }})",
        )
    }

    private fun Path.isUnder(root: Path): Boolean = startsWith(root.normalize())

    private fun File.normalizedPath(): Path = Paths.get(toURI()).normalize()
}

data class DetektSourceCoverageRow(
    val projectPath: String,
    val mainCount: Int,
    val testCount: Int,
) {
    val totalCount: Int get() = mainCount + testCount
}

object DetektSourceCoverageReportRenderer {

    fun render(
        rows: List<DetektSourceCoverageRow>,
        exclusions: Map<String, String>,
    ): String = buildString {
        val emptyModules = rows.count { it.totalCount == 0 }
        val totalMain = rows.sumOf { it.mainCount }
        val totalTest = rows.sumOf { it.testCount }
        val totalFiles = rows.sumOf { it.totalCount }

        appendLine("# Detekt source coverage")
        appendLine()
        appendLine("- Included modules: ${rows.size}")
        appendLine("- Kotlin source files: $totalFiles (main: $totalMain, test: $totalTest)")
        appendLine("- Empty included modules: $emptyModules")
        appendLine()
        appendLine("## Included modules")
        appendLine()
        appendLine("| Project | Main Kotlin | Test Kotlin | Total |")
        appendLine("| --- | ---: | ---: | ---: |")
        rows.forEach { row ->
            appendLine("| `${row.projectPath}` | ${row.mainCount} | ${row.testCount} | ${row.totalCount} |")
        }
        appendLine()
        appendLine("## Explicit exclusions")
        appendLine()
        if (exclusions.isEmpty()) {
            appendLine("No explicit exclusions.")
        } else {
            exclusions.toSortedMap().forEach { (modulePath, reason) ->
                appendLine("- `$modulePath` — $reason")
            }
        }
    }
}
