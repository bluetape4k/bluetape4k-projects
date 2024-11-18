package io.bluetape4k.cache.memorizer

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeout
import java.time.Duration
import kotlin.system.measureTimeMillis

abstract class AbstractMemorizerTest {

    companion object: KLogging()

    protected abstract val factorial: FactorialProvider
    protected abstract val fibonacci: FibonacciProvider

    protected abstract val heavyFunc: (Int) -> Int

    @Test
    fun `run heavy function`() {
        measureTimeMillis {
            heavyFunc(10) shouldBeEqualTo 100
        }

        assertTimeout(Duration.ofMillis(1000)) {
            heavyFunc(10) shouldBeEqualTo 100
        }
    }

    @Test
    fun `run factorial`() {
        val x1 = factorial.calc(500)

        assertTimeout(Duration.ofMillis(1000)) {
            factorial.calc(500)
        } shouldBeEqualTo x1
    }

    @Test
    fun `run fibonacci`() {
        val x1 = fibonacci.calc(500)

        assertTimeout(Duration.ofMillis(1000)) {
            fibonacci.calc(500)
        } shouldBeEqualTo x1
    }
}
