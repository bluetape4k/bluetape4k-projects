package io.bluetape4k.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.Serializable

/**
 * Scans test sources for JUnit disabled-test annotations and writes a release
 * report that keeps skipped tests visible.
 */
abstract class DisabledTestReportTask : DefaultTask() {

    @get:Internal
    abstract val sourceRoot: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val sourceFiles: ConfigurableFileCollection = project.objects.fileCollection()

    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    @get:Input
    abstract val failOnKnownBugWithoutIssue: Property<Boolean>

    init {
        failOnKnownBugWithoutIssue.convention(true)
    }

    @TaskAction
    fun generateReport() {
        val scanResult = DisabledTestScanner.scan(sourceRoot.get().asFile, sourceFiles.files)
        val outputFile = reportFile.get().asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(DisabledTestReportRenderer.render(scanResult))

        if (failOnKnownBugWithoutIssue.get() && scanResult.violations.isNotEmpty()) {
            val summary = scanResult.violations.joinToString(separator = "\n") { violation ->
                "- ${violation.entry.relativePath}:${violation.entry.line}: ${violation.message}"
            }
            throw GradleException(
                "Known-bug disabled tests must include a GitHub issue reference.\n" +
                        "Report: ${outputFile.relativeTo(sourceRoot.get().asFile)}\n" +
                        summary,
            )
        }

        logger.lifecycle("Disabled test report written to {}", outputFile)
    }
}

object DisabledTestScanner {

    private val disabledRegex = Regex("""@Disabled(?![A-Za-z])(?:\((.*)\))?""")
    private val conditionalDisabledRegex = Regex("""@DisabledIf[A-Za-z]*\((.*)\)""")
    private val stringLiteralRegex = Regex(""""((?:\\.|[^"\\])*)"""")
    private val issueRegex = Regex("""#\d+""")
    private val testSourceRegex = Regex("""(^|/)src/test/(kotlin|java)/""")

    fun scan(rootDir: File, sourceFiles: Iterable<File>? = null): DisabledTestScanResult {
        val candidateFiles = sourceFiles?.asSequence()
            ?: rootDir.walkTopDown()
                .onEnter { file -> file.name !in ignoredDirectories }

        val entries = candidateFiles
            .filter { file -> file.isFile && file.extension in setOf("kt", "java") }
            .filter { file -> testSourceRegex.containsMatchIn(file.toRelativeString(rootDir).normalizePath()) }
            .flatMap { file -> scanFile(rootDir, file).asSequence() }
            .sortedWith(compareBy<DisabledTestEntry> { it.relativePath }.thenBy { it.line })
            .toList()

        val violations = entries
            .filter { entry -> entry.category == DisabledTestCategory.KNOWN_BUG.id && entry.trackingIssue == null }
            .map { entry ->
                DisabledTestViolation(
                    entry = entry,
                    message = "category=${entry.category} requires a tracking issue in the @Disabled reason",
                )
            }

        return DisabledTestScanResult(entries = entries, violations = violations)
    }

    private fun scanFile(rootDir: File, file: File): List<DisabledTestEntry> {
        val relativePath = file.toRelativeString(rootDir).normalizePath()
        val lines = file.readLines()

        return lines.mapIndexedNotNull { index, line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("//") || trimmed.startsWith("*")) {
                return@mapIndexedNotNull null
            }

            val disabledMatch = disabledRegex.find(trimmed)
            val conditionalMatch = conditionalDisabledRegex.find(trimmed)
            val annotation = when {
                disabledMatch != null -> "@Disabled"
                conditionalMatch != null -> "@DisabledIf"
                else -> return@mapIndexedNotNull null
            }
            val argumentText = disabledMatch?.groupValues?.getOrNull(1)
                ?: conditionalMatch?.groupValues?.getOrNull(1)
                ?: ""
            val reason = extractReason(argumentText)
            val category = inferCategory(annotation, reason, relativePath)
            val target = findAnnotatedTarget(lines, index)

            DisabledTestEntry(
                module = inferModule(relativePath),
                relativePath = relativePath,
                line = index + 1,
                target = target.name,
                level = target.level,
                category = category.id,
                trackingIssue = issueRegex.find(reason)?.value,
                reason = reason.ifBlank { "(no reason)" },
                annotation = annotation,
            )
        }
    }

    private fun extractReason(argumentText: String): String {
        val literal = stringLiteralRegex.find(argumentText)?.groupValues?.getOrNull(1)
        return literal?.replace("\\\"", "\"")?.replace("\\n", " ") ?: argumentText.trim()
    }

    private fun inferCategory(annotation: String, reason: String, relativePath: String): DisabledTestCategory {
        if (annotation == "@DisabledIf") {
            return DisabledTestCategory.CONDITIONAL_ENVIRONMENT
        }

        val text = "${reason.lowercase()} ${relativePath.lowercase()}"
        return when {
            unsupportedCapabilityRegex.containsMatchIn(text) ->
                DisabledTestCategory.UNSUPPORTED_CAPABILITY

            listOf("slow", "expensive", "large", "오래", "빈도가 낮", "사이즈").any(text::contains) ->
                DisabledTestCategory.SLOW_OPTIONAL

            environmentRegex.containsMatchIn(text) ->
                DisabledTestCategory.ENVIRONMENT_MANUAL

            relativePath.startsWith("examples/") ->
                DisabledTestCategory.INTENTIONAL_EXAMPLE

            listOf("bug", "failure", "fail", "error", "exception", "regression", "broken", "flaky", "race", "버그", "실패", "오류", "예외", "불안정", "레이스").any(text::contains) ->
                DisabledTestCategory.KNOWN_BUG

            else -> DisabledTestCategory.UNCATEGORIZED
        }
    }

    private fun findAnnotatedTarget(lines: List<String>, annotationIndex: Int): AnnotatedTarget {
        lines.drop(annotationIndex + 1).take(8).forEach { line ->
            functionNameRegex.find(line)?.let { match ->
                return AnnotatedTarget(level = "method", name = match.groupValues[1])
            }
            classNameRegex.find(line)?.let { match ->
                return AnnotatedTarget(level = "class", name = match.groupValues[1])
            }
        }
        return AnnotatedTarget(level = "unknown", name = "(unknown)")
    }

    private fun inferModule(relativePath: String): String {
        val sourceStart = relativePath.indexOf("/src/")
        return when {
            sourceStart > 0 -> relativePath.substring(0, sourceStart)
            else -> "(root)"
        }
    }

    private fun String.normalizePath(): String = replace(File.separatorChar, '/')

    private val ignoredDirectories = setOf(".git", ".gradle", ".idea", ".worktrees", "build", "buildSrc")
    private val functionNameRegex = Regex("""\bfun\s+`?([^`(]+)`?\s*\(""")
    private val classNameRegex = Regex("""\b(?:class|object|interface)\s+([A-Za-z0-9_]+)""")
    private val unsupportedCapabilityRegex = Regex(
        """unsupported|does not support|not support|not available|unavailable|미지원|지원하지|불가|사용할 수 없습니다""",
    )
    private val environmentRegex = Regex(
        """\b(api key|proxy|ci|local|docker|macos|port|ryuk)\b|보안|환경|로컬|키|포트|발표용""",
    )
}

object DisabledTestReportRenderer {

    fun render(result: DisabledTestScanResult): String {
        val counts = result.entries
            .groupingBy { it.category }
            .eachCount()
            .toSortedMap()

        return buildString {
            appendLine("# Disabled Test Report")
            appendLine()
            appendLine("Generated by `./gradlew checkDisabledTests`.")
            appendLine()
            appendLine("## Summary")
            appendLine()
            appendLine("- Disabled annotations: ${result.entries.size}")
            appendLine("- Known-bug violations without tracking issue: ${result.violations.size}")
            counts.forEach { (category, count) ->
                appendLine("- `$category`: $count")
            }
            appendLine()
            appendLine("## Category Legend")
            appendLine()
            DisabledTestCategory.entries.forEach { category ->
                appendLine("- `${category.id}`: ${category.description}")
            }
            appendLine()
            appendLine("## Registry")
            appendLine()
            appendLine("| Module | File | Line | Target | Level | Category | Tracking Issue | Reason |")
            appendLine("|---|---|---:|---|---|---|---|---|")
            result.entries.forEach { entry ->
                appendLine(
                    "| `${entry.module}` | `${entry.relativePath}` | ${entry.line} | `${entry.target.escapeMarkdown()}` | " +
                            "${entry.level} | `${entry.category}` | ${entry.trackingIssue ?: "-"} | ${entry.reason.escapeMarkdown()} |",
                )
            }
            appendLine()
            appendLine("## Gate Rule")
            appendLine()
            appendLine("Any `known-bug` disabled test must include a GitHub issue reference such as `#497` in the annotation reason.")
            appendLine("Unsupported capabilities, manual environment requirements, conditional CI skips, and slow optional tests are reported but do not fail the gate.")
        }
    }

    private fun String.escapeMarkdown(): String = replace("|", "\\|").replace("\n", " ")
}

enum class DisabledTestCategory(
    val id: String,
    val description: String,
) {
    KNOWN_BUG("known-bug", "A product or test bug that must have a tracking issue."),
    UNSUPPORTED_CAPABILITY("unsupported-capability", "A backend, emulator, protocol, or library capability is intentionally unsupported."),
    ENVIRONMENT_MANUAL("environment-manual", "The test requires credentials, a local service, a security setup, or another manual environment."),
    SLOW_OPTIONAL("slow-optional", "The test is intentionally excluded because it is slow, large, or rarely useful in routine builds."),
    CONDITIONAL_ENVIRONMENT("conditional-environment", "A conditional JUnit disable annotation controls when the test is skipped."),
    INTENTIONAL_EXAMPLE("intentional-example", "Example code documents a failure mode or behavior that is not a release-blocking bug."),
    UNCATEGORIZED("uncategorized", "The disabled test does not match a known category and should be reviewed during release."),
}

data class DisabledTestScanResult(
    val entries: List<DisabledTestEntry>,
    val violations: List<DisabledTestViolation>,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

data class DisabledTestEntry(
    val module: String,
    val relativePath: String,
    val line: Int,
    val target: String,
    val level: String,
    val category: String,
    val trackingIssue: String?,
    val reason: String,
    val annotation: String,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

data class DisabledTestViolation(
    val entry: DisabledTestEntry,
    val message: String,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

private data class AnnotatedTarget(
    val level: String,
    val name: String,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
