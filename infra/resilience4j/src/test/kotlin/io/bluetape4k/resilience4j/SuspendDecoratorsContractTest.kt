package io.bluetape4k.resilience4j

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.resilience4j.retry.withRetry
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.IOException
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SuspendDecoratorsContractTest {

    @Test
    fun `fallback does not recover cancellation from supplier`() = runTest {
        val fallbackCalled = AtomicBoolean(false)
        val cancellation = CancellationException("supplier cancelled")

        val decorated = SuspendDecorators.ofSupplier<String> {
            throw cancellation
        }.withFallback { _: Throwable? ->
            fallbackCalled.set(true)
            "fallback"
        }

        assertFailsWith<CancellationException> {
            decorated.invoke()
        } shouldBeEqualTo cancellation

        fallbackCalled.get() shouldBeEqualTo false
    }

    @Test
    fun `typed fallback does not recover cancellation even when type matches`() = runTest {
        val fallbackCalled = AtomicBoolean(false)
        val cancellation = CancellationException("typed cancelled")

        val decorated = SuspendDecorators.ofSupplier<String> {
            throw cancellation
        }.withFallback(CancellationException::class) {
            fallbackCalled.set(true)
            "fallback"
        }

        assertFailsWith<CancellationException> {
            decorated.invoke()
        } shouldBeEqualTo cancellation

        fallbackCalled.get() shouldBeEqualTo false
    }

    @Test
    fun `broad typed fallback does not recover cancellation`() = runTest {
        val fallbackCalled = AtomicBoolean(false)
        val cancellation = CancellationException("broad cancelled")

        val decorated = SuspendDecorators.ofSupplier<String> {
            throw cancellation
        }.withFallback(Throwable::class) {
            fallbackCalled.set(true)
            "fallback"
        }

        assertFailsWith<CancellationException> {
            decorated.invoke()
        } shouldBeEqualTo cancellation

        fallbackCalled.get() shouldBeEqualTo false
    }

    @Test
    fun `fallback after retry observes retry exhaustion once`() = runTest {
        val retry = retry("fallback-after-retry")
        val attempts = AtomicInteger(0)
        val fallbackCalls = AtomicInteger(0)

        val decorated = SuspendDecorators.ofSupplier<String> {
            attempts.incrementAndGet()
            throw IOException("boom")
        }.withRetry(retry)
            .withFallback { _: Throwable? ->
                fallbackCalls.incrementAndGet()
                "fallback"
            }

        decorated.invoke() shouldBeEqualTo "fallback"
        attempts.get() shouldBeEqualTo retry.retryConfig.maxAttempts
        fallbackCalls.get() shouldBeEqualTo 1
    }

    @Test
    fun `fallback before retry is retried when fallback fails`() = runTest {
        val retry = retry("retry-after-fallback")
        val attempts = AtomicInteger(0)
        val fallbackCalls = AtomicInteger(0)

        val decorated = SuspendDecorators.ofSupplier<String> {
            attempts.incrementAndGet()
            throw IOException("inner")
        }.withFallback { _: Throwable? ->
            fallbackCalls.incrementAndGet()
            throw IOException("fallback")
        }.withRetry(retry)

        assertFailsWith<IOException> {
            decorated.invoke()
        }

        attempts.get() shouldBeEqualTo retry.retryConfig.maxAttempts
        fallbackCalls.get() shouldBeEqualTo retry.retryConfig.maxAttempts
    }

    @Test
    fun `retry helper does not retry cancellation before decorators`() = runTest {
        val attempts = AtomicInteger(0)
        val retry = retry("helper-cancel")

        assertFailsWith<CancellationException> {
            withRetry<String>(retry) {
                attempts.incrementAndGet()
                throw CancellationException("cancelled")
            }
        }

        attempts.get() shouldBeEqualTo 1
    }

    @Test
    fun `supplier retry helper does not retry cancellation`() = runTest {
        val attempts = AtomicInteger(0)
        val retry = retry("supplier-cancel")

        val decorated = SuspendDecorators.ofSupplier<String> {
            attempts.incrementAndGet()
            throw CancellationException("cancelled")
        }.withRetry(retry)

        assertFailsWith<CancellationException> {
            decorated.invoke()
        }

        attempts.get() shouldBeEqualTo 1
    }

    private fun retry(name: String): Retry =
        Retry.of(name) {
            RetryConfig.custom<Any?>()
                .maxAttempts(3)
                .waitDuration(Duration.ZERO)
                .build()
        }
}
