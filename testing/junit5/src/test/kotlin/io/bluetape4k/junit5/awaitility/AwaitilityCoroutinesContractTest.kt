package io.bluetape4k.junit5.awaitility

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.junit5.coroutines.runSuspendIO
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.awaitility.core.ConditionTimeoutException
import org.awaitility.core.TerminalFailureException
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class AwaitilityCoroutinesContractTest {

    @Test
    fun `untilSuspending preserves atLeast contract`() = runSuspendIO {
        val exception = assertFailsWith<ConditionTimeoutException> {
            await
                .atLeast(Duration.ofMillis(150))
                .atMost(Duration.ofMillis(500))
                .pollDelay(Duration.ZERO)
                .untilSuspending { true }
        }

        assertTrue(exception.message.orEmpty().contains("earlier than expected minimum timeout"))
    }

    @Test
    fun `untilSuspending preserves during contract`() = runSuspendIO {
        val startedAt = System.nanoTime()
        await
            .during(Duration.ofMillis(150))
            .atMost(Duration.ofMillis(500))
            .pollDelay(Duration.ZERO)
            .pollInterval(Duration.ofMillis(20))
            .untilSuspending {
                (System.nanoTime() - startedAt).toDurationMillis() >= 100
            }

        val elapsedMillis = (System.nanoTime() - startedAt).toDurationMillis()
        assertTrue(elapsedMillis >= 220, "during contract completed too early: ${elapsedMillis}ms")
    }

    @Test
    fun `untilSuspending includes initial poll delay in atMost deadline`() = runSuspendIO {
        assertFailsWith<ConditionTimeoutException> {
            await
                .atMost(Duration.ofMillis(100))
                .pollDelay(Duration.ofMillis(500))
                .untilSuspending { true }
        }
    }

    @Test
    fun `untilSuspending evaluates atLeast after initial poll delay`() = runSuspendIO {
        await
            .atLeast(Duration.ofMillis(50))
            .atMost(Duration.ofMillis(300))
            .pollDelay(Duration.ofMillis(100))
            .untilSuspending { true }
    }

    @Test
    fun `untilSuspending preserves failFast contract`() = runSuspendIO {
        assertFailsWith<TerminalFailureException> {
            await
                .atMost(Duration.ofMillis(500))
                .pollDelay(Duration.ZERO)
                .failFast(Callable { true })
                .untilSuspending { false }
        }
    }

    @Test
    fun `untilSuspending derives fixed poll delay when it is not explicit`() = runSuspendIO {
        val firstPollAt = AtomicLong()
        val startedAt = System.nanoTime()

        await
            .atMost(Duration.ofMillis(500))
            .pollInterval(Duration.ofMillis(80))
            .untilSuspending {
                firstPollAt.compareAndSet(0, System.nanoTime())
                true
            }

        val elapsedMillis = (firstPollAt.get() - startedAt).toDurationMillis()
        assertTrue(elapsedMillis >= 60, "default fixed poll delay was lost: ${elapsedMillis}ms")
    }

    @Test
    fun `untilSuspending preserves ignored exception behavior`() = runSuspendIO {
        val attempts = AtomicInteger()

        await
            .atMost(Duration.ofMillis(500))
            .pollDelay(Duration.ZERO)
            .pollInterval(Duration.ofMillis(10))
            .ignoreExceptionsInstanceOf(IllegalStateException::class.java)
            .untilSuspending {
                if (attempts.incrementAndGet() < 3) {
                    throw IllegalStateException("retry")
                }
                true
            }

        assertTrue(attempts.get() >= 3)
    }

    @Test
    fun `untilSuspending applies ignored exception behavior to condition timeout exceptions`() = runSuspendIO {
        val attempts = AtomicInteger()

        await
            .atMost(Duration.ofMillis(500))
            .pollDelay(Duration.ZERO)
            .pollInterval(Duration.ofMillis(10))
            .ignoreExceptionsInstanceOf(ConditionTimeoutException::class.java)
            .untilSuspending {
                if (attempts.incrementAndGet() == 1) {
                    throw ConditionTimeoutException("retry")
                }
                true
            }

        assertTrue(attempts.get() >= 2)
    }

    @Test
    fun `untilSuspending propagates outer cancellation`() = runSuspendIO {
        assertFailsWith<TimeoutCancellationException> {
            withTimeout(50.milliseconds) {
                await
                    .atMost(Duration.ofSeconds(1))
                    .pollDelay(Duration.ZERO)
                    .untilSuspending {
                        delay(500.milliseconds)
                        false
                    }
            }
        }
    }

    private fun Long.toDurationMillis(): Long = this / 1_000_000
}
