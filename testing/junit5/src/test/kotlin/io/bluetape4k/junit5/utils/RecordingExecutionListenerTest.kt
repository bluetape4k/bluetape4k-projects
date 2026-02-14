package io.bluetape4k.junit5.utils

import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.platform.engine.TestExecutionResult
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecordingExecutionListenerTest {

    @Test
    fun `finished status 로 이벤트를 필터링할 수 있다`() {
        val listener = RecordingExecutionListener()
        val descriptor = mockk<org.junit.platform.engine.TestDescriptor>(relaxed = true)

        listener.executionFinished(descriptor, TestExecutionResult.successful())
        listener.executionFinished(descriptor, TestExecutionResult.failed(RuntimeException("boom")))

        val successful = listener.getFinishedEventsByStatus(TestExecutionResult.Status.SUCCESSFUL)
        val failed = listener.getFinishedEventsByStatus(TestExecutionResult.Status.FAILED)

        assertEquals(1, successful.size)
        assertEquals(1, failed.size)
    }

    @Test
    fun `ExecutionEvent 는 payload 타입 안전 조회를 지원한다`() {
        val descriptor = mockk<org.junit.platform.engine.TestDescriptor>(relaxed = true)
        val event = ExecutionEvent.executionFinished(descriptor, TestExecutionResult.successful())

        val payload = event.getPayload(TestExecutionResult::class.java)
        assertTrue(payload != null)
        assertEquals(TestExecutionResult.Status.SUCCESSFUL, payload.status)
    }
}
