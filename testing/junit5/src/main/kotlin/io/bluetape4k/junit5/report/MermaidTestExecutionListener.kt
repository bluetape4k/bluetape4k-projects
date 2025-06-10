package io.bluetape4k.junit5.report

import io.bluetape4k.logging.KLogging
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import java.io.Serializable
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.jvm.optionals.getOrNull

class MermaidTestExecutionListener: TestExecutionListener {

    companion object: KLogging()

    data class Task(
        val uniqueId: String,
        val className: String?,
        val displayName: String,
        val startTime: Instant,
        val endTime: Instant? = null,
        val resultStatus: TestExecutionResult.Status? = null,
    ): Serializable

    private val tasks = CopyOnWriteArrayList<Task>()

    private val TestIdentifier.className: String?
        get() = (this.source.getOrNull() as? MethodSource)?.className

    override fun executionStarted(testIdentifier: TestIdentifier) {
        if (testIdentifier.isTest) {
            val task = Task(
                uniqueId = testIdentifier.uniqueId,
                className = testIdentifier.className,
                displayName = testIdentifier.displayName,
                startTime = Instant.now()
            )
            tasks.add(task)
        }
    }

    override fun executionFinished(testIdentifier: TestIdentifier, testExecutionResult: TestExecutionResult) {
        if (testIdentifier.isTest) {
            val task = tasks.find {
                it.uniqueId == testIdentifier.uniqueId
            }
            task?.let {
                tasks[tasks.indexOf(it)] = it.copy(endTime = Instant.now(), resultStatus = testExecutionResult.status)
            }
            // log.info { "Test execution finished: ${tasks.find { it.uniqueId == testIdentifier.uniqueId }}" }
        }
    }

    override fun testPlanExecutionFinished(testPlan: TestPlan) {
        println("Test execution completed.")
        val mermaid = toMermaidGanttChart(tasks)
        println(mermaid)
    }

    private fun toMermaidGanttChart(tasks: List<Task>): String = buildString {
        appendLine("gantt")
        appendLine("    title Test Execution Timeline")
        appendLine("    dateFormat  YYYY-MM-DDTHH:mm:ss.SSS")
        appendLine("    axisFormat  %H:%M:%S")

        tasks
            .groupBy { it.className ?: "Test" }
            .forEach { (className, tasks) ->
                appendLine("    section $className")
                tasks.forEachIndexed { index, task ->
                    val start = task.startTime.toString()
                    val duration = task.endTime?.let { Duration.between(task.startTime, it).toMillis() } ?: 0
                    val result = task.resultStatus.toResult()
                    val status = task.resultStatus.toMermaidStatus()
                    appendLine("        ${task.displayName}($result) : $status, ${start}, ${duration}ms")
                }
            }
    }

    private fun TestExecutionResult.Status?.toMermaidStatus(): String = when (this) {
        TestExecutionResult.Status.SUCCESSFUL -> "active"
        TestExecutionResult.Status.FAILED -> "crit"
        TestExecutionResult.Status.ABORTED -> "done"
        else -> ""
    }

    private fun TestExecutionResult.Status?.toResult(): String = when (this) {
        TestExecutionResult.Status.SUCCESSFUL -> "✅"
        TestExecutionResult.Status.FAILED -> "🔥"
        TestExecutionResult.Status.ABORTED -> "\uD83D\uDEAB"
        else -> ""
    }
}
