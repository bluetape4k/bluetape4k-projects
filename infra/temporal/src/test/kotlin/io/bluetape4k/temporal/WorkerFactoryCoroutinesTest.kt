package io.bluetape4k.temporal

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.concurrent.await
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.temporal.worker.WorkerFactory
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class WorkerFactoryCoroutinesTest {

    companion object: KLoggingChannel()

    private val factory = mockk<WorkerFactory>(relaxUnitFun = true)

    @BeforeEach
    fun beforeEach() {
        clearMocks(factory)
    }

    @Test
    fun `graceful shutdown waits once and returns terminated state`() = runTest {
        every { factory.isShutdown } returns false
        every { factory.shutdown() } just runs
        every { factory.awaitTermination(250, TimeUnit.MILLISECONDS) } just runs
        every { factory.isTerminated } returns true

        factory.shutdownSuspending(250.milliseconds).shouldBeTrue()

        verify(exactly = 1) { factory.shutdown() }
        verify(exactly = 1) { factory.awaitTermination(250, TimeUnit.MILLISECONDS) }
        verify(exactly = 0) { factory.shutdownNow() }
    }

    @Test
    fun `timeout without force keeps graceful shutdown pending`() = runTest {
        every { factory.isShutdown } returns false
        every { factory.shutdown() } just runs
        every { factory.awaitTermination(100.milliseconds) } just runs
        every { factory.isTerminated } returns false

        factory.shutdownSuspending(100.milliseconds).shouldBeFalse()

        verify(exactly = 1) { factory.shutdown() }
        verify(exactly = 1) { factory.awaitTermination(100.milliseconds) }
        verify(exactly = 0) { factory.shutdownNow() }
    }

    @Test
    fun `timeout with force requests shutdownNow without a second wait`() = runTest {
        every { factory.isShutdown } returns false
        every { factory.shutdown() } just runs
        every { factory.awaitTermination(100.milliseconds) } just runs
        every { factory.isTerminated } returns false
        every { factory.shutdownNow() } just runs

        factory.shutdownSuspending(100.milliseconds, force = true).shouldBeFalse()

        verify(exactly = 1) { factory.shutdown() }
        verify(exactly = 1) { factory.awaitTermination(100.milliseconds) }
        verify(exactly = 1) { factory.shutdownNow() }
    }

    @Test
    fun `repeated shutdown is idempotent when factory is already shut down`() = runTest {
        every { factory.isShutdown } returns true
        every { factory.awaitTermination(100.milliseconds) } just runs
        every { factory.isTerminated } returns true

        factory.shutdownSuspending(100.milliseconds).shouldBeTrue()
        factory.shutdownSuspending(100.milliseconds).shouldBeTrue()

        verify(exactly = 0) { factory.shutdown() }
        verify(exactly = 2) { factory.awaitTermination(100.milliseconds) }
        verify(exactly = 0) { factory.shutdownNow() }
    }

    @Test
    fun `infinite timeout is rejected before touching the factory`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            factory.shutdownSuspending(Duration.INFINITE)
        }

        verify(exactly = 0) { factory.shutdown() }
        verify(exactly = 0) { factory.awaitTermination(any(), any()) }
    }

    @Test
    fun `cancellation after graceful wait does not escalate to shutdownNow`() = runTest {
        val waitEntered = CountDownLatch(1)
        val releaseWait = CountDownLatch(1)
        every { factory.isShutdown } returns false
        every { factory.shutdown() } just runs
        every { factory.awaitTermination(100.milliseconds) } answers {
            waitEntered.countDown()
            releaseWait.await()
        }
        every { factory.isTerminated } returns false
        every { factory.shutdownNow() } just runs

        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            factory.shutdownSuspending(100.milliseconds, force = true)
        }
        try {
            waitEntered.await(5.seconds).shouldBeTrue()
            job.cancel()
        } finally {
            releaseWait.countDown()
        }
        job.join()

        verify(exactly = 0) { factory.shutdownNow() }
    }
}
