package io.bluetape4k.opentelemetry.context

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.junit5.coroutines.DEFAULT_CANCELLATION_CONTRACT_TIMEOUT
import io.bluetape4k.junit5.observability.ContextPropagationBoundary
import io.bluetape4k.junit5.observability.ContextPropagationScenario
import io.bluetape4k.junit5.observability.ContextPropagationTerminal
import io.bluetape4k.junit5.observability.assertContextIsolation
import io.bluetape4k.junit5.observability.assertContextPropagationConformance
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.Duration

@Timeout(
    value = 15,
    unit = TimeUnit.SECONDS,
    threadMode = Timeout.ThreadMode.SAME_THREAD,
)
class ContextPropagationConformanceTest {

    @Test
    fun `coroutine success propagates and restores context`() = runTest {
        val captured = runCoroutineScenario(ContextPropagationScenario.SUCCESS)
        captured.thrown.shouldBeNull()
        assertContextPropagationConformance(
            captured.observation,
            propagationExpectation(
                ContextPropagationBoundary.COROUTINE,
                ContextPropagationScenario.SUCCESS,
                ContextPropagationTerminal.SUCCESS,
            ),
        )
    }

    @Test
    fun `coroutine failure propagates and restores context`() = runTest {
        val captured = runCoroutineScenario(ContextPropagationScenario.FAILURE)
        captured.assertThrownExactly<IllegalStateException>()
        assertContextPropagationConformance(
            captured.observation,
            propagationExpectation(
                ContextPropagationBoundary.COROUTINE,
                ContextPropagationScenario.FAILURE,
                ContextPropagationTerminal.FAILURE,
            ),
        )
    }

    @Test
    fun `coroutine cancellation propagates and restores context`() = runTest {
        val captured = runCoroutineScenario(ContextPropagationScenario.CANCELLATION)
        captured.assertThrownExactly<CancellationException>()
        assertContextPropagationConformance(
            captured.observation,
            propagationExpectation(
                ContextPropagationBoundary.COROUTINE,
                ContextPropagationScenario.CANCELLATION,
                ContextPropagationTerminal.CANCELLATION,
            ),
        )
    }

    @Test
    fun `coroutine deadline propagates and restores context`() = runTest {
        val captured = runCoroutineScenario(ContextPropagationScenario.DEADLINE)
        captured.assertThrownExactly<TimeoutCancellationException>()
        assertContextPropagationConformance(
            captured.observation,
            propagationExpectation(
                ContextPropagationBoundary.COROUTINE,
                ContextPropagationScenario.DEADLINE,
                ContextPropagationTerminal.DEADLINE_EXCEEDED,
            ),
        )
    }

    @Test
    fun `coroutine siblings keep isolated parent contexts`() = runTest {
        val observation = runCoroutineIsolationScenario()
        assertContextIsolation(observation, coroutineIsolationExpectation())
    }

    @Test
    fun `coroutine cleanup under cancelled parent attempts every child`() = runTest {
        val attempted = mutableListOf<String>()
        val cancelledParent = launch {
            coroutineContext.job.cancel(CancellationException("synthetic cancelled parent"))
            coroutineContext.job.isCancelled.shouldBeTrue()

            cleanupCoroutineActions(
                actions = listOf(
                    { attempted += "child-A" },
                    { attempted += "child-B" },
                ),
            )
        }

        cancelledParent.join()

        check(attempted == listOf("child-A", "child-B"))
    }

    @Test
    fun `coroutine cleanup preserves cancellation and deadline identity`() = runTest {
        val cancellation = CancellationException("synthetic primary cancellation")
        val deadline = try {
            withTimeout(Duration.ZERO) {
                awaitCancellation()
            }
            error("Expected deadline")
        } catch (e: TimeoutCancellationException) {
            e
        }

        listOf(cancellation, deadline).forEach { primary ->
            val thrown = assertFailsWith<CancellationException> {
                withCoroutineChildCleanup(
                    children = emptyList(),
                    beforeCleanup = {},
                ) {
                    throw primary
                }
            }

            (thrown === primary).shouldBeTrue()
        }
    }

    @Test
    fun `coroutine cleanup suppresses distinct failures without self suppression`() = runTest {
        val primary = CancellationException("synthetic primary cancellation")
        val cleanupFailureA = AssertionError("synthetic cleanup failure A")
        val cleanupFailureB = IllegalStateException("synthetic cleanup failure B")
        val attempted = mutableListOf<String>()

        val thrown = assertFailsWith<CancellationException> {
            cleanupCoroutineActions(
                primaryFailure = primary,
                actions = listOf(
                    {
                        attempted += "primary"
                        throw primary
                    },
                    {
                        attempted += "failure-A"
                        throw cleanupFailureA
                    },
                    {
                        attempted += "failure-B"
                        throw cleanupFailureB
                    },
                ),
            )
        }

        (thrown === primary).shouldBeTrue()
        check(thrown.suppressed.toList() == listOf(cleanupFailureA, cleanupFailureB)) {
            "Unexpected cleanup suppression order: ${thrown.suppressed.map { it.javaClass.simpleName }}"
        }
        check(attempted == listOf("primary", "failure-A", "failure-B"))
    }

    @Test
    fun `reactor success propagates and restores context`() {
        val captured = runReactorScenario(ContextPropagationScenario.SUCCESS)
        captured.thrown.shouldBeNull()
        assertContextPropagationConformance(
            captured.observation,
            propagationExpectation(
                ContextPropagationBoundary.REACTOR,
                ContextPropagationScenario.SUCCESS,
                ContextPropagationTerminal.SUCCESS,
            ),
        )
    }

    @Test
    fun `executor success propagates and restores context`() {
        val captured = runExecutorScenario(ContextPropagationScenario.SUCCESS)
        captured.thrown.shouldBeNull()
        assertContextPropagationConformance(
            captured.observation,
            propagationExpectation(
                ContextPropagationBoundary.TASK_EXECUTOR,
                ContextPropagationScenario.SUCCESS,
                ContextPropagationTerminal.SUCCESS,
            ),
        )
    }

    @Test
    fun `interrupted executor teardown forces shutdown and restores interrupt status`() {
        val executor = InterruptingExecutorService()

        try {
            assertFailsWith<AssertionError> {
                executor.shutdownAndAssertTermination()
            }
            executor.shutdownNowCalled.shouldBeTrue()
            Thread.currentThread().isInterrupted.shouldBeTrue()
        } finally {
            Thread.interrupted()
        }
    }

    @Test
    fun `executor terminal capture restores interrupt status and rethrows interruption`() {
        val future = CompletableFuture<Unit>()

        Thread.currentThread().interrupt()
        try {
            assertFailsWith<InterruptedException> {
                captureExecutorTerminal(future, DEFAULT_CANCELLATION_CONTRACT_TIMEOUT)
            }
            Thread.currentThread().isInterrupted.shouldBeTrue()
        } finally {
            Thread.interrupted()
        }
    }

    @Test
    fun `executor terminal capture unwraps execution failure cause`() {
        val failure = IllegalStateException("synthetic executor failure")
        val future = CompletableFuture<Unit>().also {
            it.completeExceptionally(failure)
        }

        val captured = captureExecutorTerminal(future, DEFAULT_CANCELLATION_CONTRACT_TIMEOUT)

        (captured === failure).shouldBeTrue()
    }

    @Test
    fun `executor terminal capture rethrows fatal execution cause with identity`() {
        val fatal = SyntheticFatalError()
        val future = CompletableFuture<Unit>().also {
            it.completeExceptionally(fatal)
        }

        val thrown = assertFailsWith<SyntheticFatalError> {
            captureExecutorTerminal(future, DEFAULT_CANCELLATION_CONTRACT_TIMEOUT)
        }

        (thrown === fatal).shouldBeTrue()
    }

    @Test
    fun `executor terminal capture preserves cancellation identity`() {
        val cancellation = CancellationException("synthetic executor cancellation")
        val future = mockk<Future<Unit>> {
            every { get(any(), any()) } throws cancellation
        }

        val captured = captureExecutorTerminal(future, DEFAULT_CANCELLATION_CONTRACT_TIMEOUT)

        (captured === cancellation).shouldBeTrue()
    }

    @Test
    fun `executor terminal capture classifies harness timeout as assertion failure`() {
        val future = CompletableFuture<Unit>()

        val failure = assertFailsWith<AssertionError> {
            captureExecutorTerminal(future, Duration.ZERO)
        }

        failure.cause shouldBeInstanceOf TimeoutException::class
    }

    @Test
    fun `executor cleanup preserves primary interruption and still shuts down after cancel failure`() {
        val executor = InterruptingExecutorService()
        val primary = InterruptedException("synthetic primary interruption")
        val cancelFailure = AssertionError("synthetic cancel failure")
        var shutdownExecuted = false

        try {
            val thrown = assertFailsWith<InterruptedException> {
                withExecutorCleanup(
                    executor = executor,
                    cancel = { throw cancelFailure },
                    shutdown = { shutdownExecuted = true },
                ) {
                    Thread.currentThread().interrupt()
                    throw primary
                }
            }

            (thrown === primary).shouldBeTrue()
            (thrown.suppressed.single() === cancelFailure).shouldBeTrue()
            shutdownExecuted.shouldBeTrue()
            Thread.currentThread().isInterrupted.shouldBeTrue()
        } finally {
            Thread.interrupted()
        }
    }

    @Test
    fun `executor cleanup aggregates cancel and shutdown failures without primary`() {
        val executor = InterruptingExecutorService()
        val cancelFailure = AssertionError("synthetic cancel failure")
        val shutdownFailure = IllegalStateException("synthetic shutdown failure")

        val thrown = assertFailsWith<AssertionError> {
            withExecutorCleanup(
                executor = executor,
                cancel = { throw cancelFailure },
                shutdown = { throw shutdownFailure },
            ) {
                Unit
            }
        }

        (thrown === cancelFailure).shouldBeTrue()
        (thrown.suppressed.single() === shutdownFailure).shouldBeTrue()
    }

    @Test
    fun `executor cleanup preserves primary identity when both cleanup actions throw same instance`() {
        val executor = InterruptingExecutorService()
        val primary = InterruptedException("shared primary and cleanup interruption")

        try {
            val thrown = assertFailsWith<InterruptedException> {
                withExecutorCleanup(
                    executor = executor,
                    cancel = { throw primary },
                    shutdown = { throw primary },
                ) {
                    Thread.currentThread().interrupt()
                    throw primary
                }
            }

            (thrown === primary).shouldBeTrue()
            thrown.suppressed.isEmpty().shouldBeTrue()
            Thread.currentThread().isInterrupted.shouldBeTrue()
        } finally {
            Thread.interrupted()
        }
    }

    @Test
    fun `executor cleanup suppresses repeated cleanup instance only once`() {
        val executor = InterruptingExecutorService()
        val primary = InterruptedException("synthetic primary interruption")
        val repeatedCleanup = AssertionError("repeated cleanup failure")

        try {
            val thrown = assertFailsWith<InterruptedException> {
                withExecutorCleanup(
                    executor = executor,
                    cancel = { throw repeatedCleanup },
                    shutdown = { throw repeatedCleanup },
                ) {
                    Thread.currentThread().interrupt()
                    throw primary
                }
            }

            (thrown === primary).shouldBeTrue()
            (thrown.suppressed.single() === repeatedCleanup).shouldBeTrue()
            Thread.currentThread().isInterrupted.shouldBeTrue()
        } finally {
            Thread.interrupted()
        }
    }
}

private class SyntheticFatalError: Error("synthetic fatal error")

private class InterruptingExecutorService: AbstractExecutorService() {
    var shutdownNowCalled = false
        private set

    private var shutdown = false
    private var awaitCalls = 0

    override fun shutdown() {
        shutdown = true
    }

    override fun shutdownNow(): List<Runnable> {
        shutdown = true
        shutdownNowCalled = true
        return emptyList()
    }

    override fun isShutdown(): Boolean = shutdown

    override fun isTerminated(): Boolean = shutdownNowCalled

    override fun awaitTermination(
        timeout: Long,
        unit: TimeUnit,
    ): Boolean {
        awaitCalls++
        if (awaitCalls == 1) {
            throw InterruptedException("synthetic teardown interruption")
        }
        return shutdownNowCalled
    }

    override fun execute(command: Runnable) {
        error("No task submission expected")
    }
}
