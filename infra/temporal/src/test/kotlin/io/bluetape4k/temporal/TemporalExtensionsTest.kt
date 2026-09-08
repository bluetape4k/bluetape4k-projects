package io.bluetape4k.temporal

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.temporal.api.common.v1.WorkflowExecution
import io.temporal.client.WorkflowStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

class TemporalExtensionsTest {

    private lateinit var workflowStub: WorkflowStub

    @BeforeEach
    fun setUp() {
        workflowStub = mockk()
    }

    @Test
    fun `start signal query cancel and terminate use the SDK stub`() = runTest {
        val execution = WorkflowExecution.newBuilder()
            .setWorkflowId("order-1")
            .setRunId("run-1")
            .build()
        every { workflowStub.start("order-1") } returns execution
        every { workflowStub.signal("approve", "order-1") } returns Unit
        every {
            workflowStub.query("status", String::class.java, String::class.java, "order-1")
        } returns "approved"
        every { workflowStub.cancel("test cancellation") } returns Unit
        every { workflowStub.terminate("test termination", "order-1") } returns Unit

        workflowStub.startSuspending("order-1") shouldBeEqualTo execution
        workflowStub.signalSuspending("approve", "order-1")
        workflowStub.querySuspending<String>("status", "order-1") shouldBeEqualTo "approved"
        workflowStub.cancelSuspending("test cancellation")
        workflowStub.terminateSuspending("test termination", "order-1")

        verify(exactly = 1) { workflowStub.start("order-1") }
        verify(exactly = 1) { workflowStub.signal("approve", "order-1") }
        verify(exactly = 1) {
            workflowStub.query("status", String::class.java, String::class.java, "order-1")
        }
        verify(exactly = 1) { workflowStub.cancel("test cancellation") }
        verify(exactly = 1) { workflowStub.terminate("test termination", "order-1") }
    }

    @Test
    fun `await result returns a reified result`() = runTest {
        every {
            workflowStub.getResultAsync(String::class.java, String::class.java)
        } returns CompletableFuture.completedFuture("completed")

        workflowStub.awaitResult<String>() shouldBeEqualTo "completed"
    }

    @Test
    fun `local cancellation cancels only the pending result future`() = runTest {
        val pending = CompletableFuture<String>()
        val requestStarted = AtomicBoolean()
        every {
            workflowStub.getResultAsync(String::class.java, String::class.java)
        } answers {
            requestStarted.set(true)
            pending
        }

        val job = launch { workflowStub.awaitResult<String>() }
        withTimeout(1_000) {
            while (!requestStarted.get()) {
                delay(1)
            }
        }

        job.cancel()
        job.join()

        pending.isCancelled.shouldBeTrue()
        verify(exactly = 0) { workflowStub.cancel() }
        verify(exactly = 0) { workflowStub.cancel(any<String>()) }
    }

    @Test
    fun `withTimeout cancels only the pending result future`() = runTest {
        val pending = CompletableFuture<String>()
        every {
            workflowStub.getResultAsync(String::class.java, String::class.java)
        } returns pending

        assertFailsWith<TimeoutCancellationException> {
            withTimeout(1) { workflowStub.awaitResult<String>() }
        }

        pending.isCancelled.shouldBeTrue()
        verify(exactly = 0) { workflowStub.cancel() }
        verify(exactly = 0) { workflowStub.cancel(any<String>()) }
    }
}
