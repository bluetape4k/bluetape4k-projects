package io.bluetape4k.junit5.report

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.TestSource
import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

class MermaidTestExecutionListenerTest {

    @Test
    fun `finished 상태는 mermaid 상태 문자열로 렌더링된다`() {
        val listener = MermaidTestExecutionListener(
            clock = Clock.fixed(Instant.parse("2026-02-14T00:00:00Z"), ZoneOffset.UTC),
            printer = {}
        )

        val success = testIdentifier("uid-1", "success:task", className = "io.sample.DemoTest")
        val failed = testIdentifier("uid-2", "failed:task", className = "io.sample.DemoTest")

        listener.executionStarted(success)
        listener.executionFinished(success, TestExecutionResult.successful())
        listener.executionStarted(failed)
        listener.executionFinished(failed, TestExecutionResult.failed(IllegalStateException("boom")))

        val chart = listener.mermaidGanttChart()
        chart.contains("section io.sample.DemoTest").shouldBeTrue()
        chart.contains("success-task(✅) : active").shouldBeTrue()
        chart.contains("failed-task(🔥) : crit").shouldBeTrue()
    }

    @Test
    fun `testPlanExecutionFinished 는 완료 메시지와 mermaid 문자열을 출력한다`() {
        val outputs = mutableListOf<String>()
        val listener = MermaidTestExecutionListener(
            clock = Clock.fixed(Instant.parse("2026-02-14T00:00:00Z"), ZoneOffset.UTC),
            printer = { outputs.add(it) }
        )
        val test = testIdentifier("uid-1", "done")

        listener.executionStarted(test)
        listener.executionFinished(test, TestExecutionResult.successful())
        listener.testPlanExecutionFinished(mockk<TestPlan>(relaxed = true))

        outputs.size shouldBeEqualTo 2
        outputs[0] shouldBeEqualTo "Test execution completed."
        outputs[1].startsWith("gantt").shouldBeTrue()
    }

    @Test
    fun `테스트가 아닌 식별자는 기록하지 않는다`() {
        val listener = MermaidTestExecutionListener(
            clock = Clock.fixed(Instant.parse("2026-02-14T00:00:00Z"), ZoneOffset.UTC),
            printer = {}
        )
        val nonTest = testIdentifier("uid-nt", "container", isTest = false)

        listener.executionStarted(nonTest)
        listener.executionFinished(nonTest, TestExecutionResult.successful())

        val chart = listener.mermaidGanttChart()
        chart.contains("container").shouldBeFalse()
    }

    private fun testIdentifier(
        uniqueId: String,
        displayName: String,
        isTest: Boolean = true,
        className: String? = null,
    ): TestIdentifier {
        val source = className
            ?.let { name ->
                val methodSource = mockk<MethodSource>()
                every { methodSource.className } returns name
                Optional.of(methodSource as TestSource)
            }
            ?: Optional.empty()

        return mockk {
            every { this@mockk.uniqueId } returns uniqueId
            every { this@mockk.displayName } returns displayName
            every { this@mockk.isTest } returns isTest
            every { this@mockk.source } returns source
        }
    }
}
