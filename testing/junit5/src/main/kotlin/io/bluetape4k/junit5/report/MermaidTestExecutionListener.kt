package io.bluetape4k.junit5.report

import io.bluetape4k.logging.KLogging
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import java.io.Serializable
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.optionals.getOrNull

/**
 * JUnit 실행 이벤트를 수집해 Mermaid Gantt 차트 문자열로 출력하는 리스너입니다.
 *
 * ## 동작/계약
 * - `isTest=true`인 식별자만 대상으로 시작/종료 시각과 결과를 기록합니다.
 * - 기록 컨테이너는 `CopyOnWriteArrayList`를 사용해 동시 접근에 안전하게 동작합니다.
 * - 테스트 플랜 종료 시점에 `printer`로 Mermaid 차트 문자열을 출력합니다.
 *
 * ```kotlin
 * val listener = MermaidTestExecutionListener(printer = ::println)
 * // testPlanExecutionFinished 호출 시 gantt 텍스트가 출력됨
 * ```
 *
 * @param clock 시각 기록에 사용할 시계입니다.
 * @param printer 생성된 Mermaid 문자열 출력 함수입니다.
 */
class MermaidTestExecutionListener(
    private val clock: Clock = Clock.systemUTC(),
    private val printer: (String) -> Unit = ::println,
): TestExecutionListener {

    companion object: KLogging()

    /**
     * Mermaid 차트 생성을 위한 테스트 실행 단위 모델입니다.
     *
     * @property uniqueId JUnit 고유 식별자입니다.
     * @property className 테스트 클래스 이름입니다.
     * @property displayName 테스트 표시 이름입니다.
     * @property startTime 시작 시각입니다.
     * @property endTime 종료 시각입니다.
     * @property resultStatus 종료 결과 상태입니다.
     */
    data class Task(
        val uniqueId: String,
        val className: String?,
        val displayName: String,
        val startTime: Instant,
        val endTime: Instant? = null,
        val resultStatus: TestExecutionResult.Status? = null,
    ): Serializable

    // uniqueId를 키로 사용해 find+set 비원자적 접근에 의한 race condition을 제거합니다.
    private val taskMap = ConcurrentHashMap<String, Task>()

    private val TestIdentifier.className: String?
        get() = when (val source = this.source.getOrNull()) {
            is MethodSource -> source.className
            is ClassSource -> source.className
            else -> null
        }

    override fun executionStarted(testIdentifier: TestIdentifier) {
        if (testIdentifier.isTest) {
            val task = Task(
                uniqueId = testIdentifier.uniqueId,
                className = testIdentifier.className,
                displayName = testIdentifier.displayName,
                startTime = Instant.now(clock)
            )
            taskMap[testIdentifier.uniqueId] = task
        }
    }

    override fun executionFinished(
        testIdentifier: TestIdentifier,
        testExecutionResult: TestExecutionResult,
    ) {
        if (testIdentifier.isTest) {
            taskMap.compute(testIdentifier.uniqueId) { _, existing ->
                existing?.copy(endTime = Instant.now(clock), resultStatus = testExecutionResult.status)
            }
        }
    }

    override fun testPlanExecutionFinished(testPlan: TestPlan) {
        printer("Test execution completed.")
        printer(toMermaidGanttChart(taskMap.values.toList()))
    }

    /**
     * 현재까지 수집된 이벤트를 Mermaid Gantt 차트 문자열로 반환합니다.
     */
    internal fun mermaidGanttChart(): String = toMermaidGanttChart(taskMap.values.toList())

    private fun toMermaidGanttChart(tasks: List<Task>): String = buildString {
        appendLine("gantt")
        appendLine("    title Test Execution Timeline")
        appendLine("    dateFormat  YYYY-MM-DDTHH:mm:ss.SSS")
        appendLine("    axisFormat  %H:%M:%S")

        tasks
            .groupBy { it.className ?: "Test" }
            .forEach { (className, tasks) ->
                appendLine("    section ${className.sanitizeMermaidLabel()}")
                tasks.forEach { task ->
                    val start = task.startTime.toString()
                    val duration =
                        task.endTime?.let { Duration.between(task.startTime, it).toMillis().coerceAtLeast(0L) } ?: 0L
                    val result = task.resultStatus.toResult()
                    val status = task.resultStatus.toMermaidStatus()
                    appendLine("        ${task.displayName.sanitizeMermaidLabel()}($result) : $status, ${start}, ${duration}ms")
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
        TestExecutionResult.Status.ABORTED -> "🚫"
        else -> ""
    }

    private fun String.sanitizeMermaidLabel(): String = replace(":", "-")
}
