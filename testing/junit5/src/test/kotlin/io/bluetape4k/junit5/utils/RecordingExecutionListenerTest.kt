package io.bluetape4k.junit5.utils

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestExecutionResult

class RecordingExecutionListenerTest {

    @Test
    fun `finished status 로 이벤트를 필터링할 수 있다`() {
        val listener = RecordingExecutionListener()
        val descriptor = mockk<TestDescriptor>(relaxed = true)

        listener.executionFinished(descriptor, TestExecutionResult.successful())
        listener.executionFinished(descriptor, TestExecutionResult.failed(RuntimeException("boom")))

        val successful = listener.getFinishedEventsByStatus(TestExecutionResult.Status.SUCCESSFUL)
        val failed = listener.getFinishedEventsByStatus(TestExecutionResult.Status.FAILED)

        successful shouldHaveSize 1
        failed shouldHaveSize 1
    }

    @Test
    fun `ExecutionEvent 는 payload 타입 안전 조회를 지원한다`() {
        val descriptor = mockk<TestDescriptor>(relaxed = true)
        val event = ExecutionEvent.executionFinished(descriptor, TestExecutionResult.successful())

        val payload = event.getPayload(TestExecutionResult::class.java)
        payload?.status shouldBeEqualTo TestExecutionResult.Status.SUCCESSFUL
    }
}
