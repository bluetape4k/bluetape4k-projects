package io.bluetape4k.cache.memoizer

import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeout
import java.time.Duration
import java.util.concurrent.CompletableFuture
import kotlin.system.measureTimeMillis

abstract class AbstractAsyncMemoizerTest {

    companion object: KLoggingChannel()

    protected abstract val factorial: AsyncFactorialProvider
    protected abstract val fibonacci: AsyncFibonacciProvider

    protected abstract val heavyFunc: (Int) -> CompletableFuture<Int>

    @Test
    fun `run heavy function`() {
        measureTimeMillis {
            heavyFunc(10).get() shouldBeEqualTo 100
        }

        assertTimeout(Duration.ofMillis(1000)) {
            heavyFunc(10).get() shouldBeEqualTo 100
        }
    }

    @Test
    fun `run factorial`() {
        val x1 = factorial.calc(100).get()

        assertTimeout(Duration.ofSeconds(1)) {
            factorial.calc(100).get()
        } shouldBeEqualTo x1
    }

    @Test
    fun `run fibonacci`() {
        val x1 = fibonacci.calc(100).get()

        assertTimeout(Duration.ofSeconds(1)) {
            fibonacci.calc(100).get()
        } shouldBeEqualTo x1
    }
}
