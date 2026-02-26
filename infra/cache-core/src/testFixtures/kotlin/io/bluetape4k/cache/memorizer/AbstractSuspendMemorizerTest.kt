package io.bluetape4k.cache.memorizer

import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.withTimeoutOrNull
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.seconds

abstract class AbstractSuspendMemorizerTest {

    companion object: KLoggingChannel()

    protected abstract val factorial: SuspendFactorialProvider
    protected abstract val fibonacci: SuspendFibonacciProvider

    protected abstract val heavyFunc: suspend (Int) -> Int

    @Test
    fun `run suspend heavy function`() = runSuspendDefault {
        measureTimeMillis {
            heavyFunc(10) shouldBeEqualTo 100
        }

        val result = withTimeoutOrNull(10.seconds) {
            heavyFunc(10)
        }
        result shouldBeEqualTo 100
    }

    @Test
    fun `run suspend factorial`() = runSuspendDefault {
        val x1 = factorial.calc(500)

        val result = withTimeoutOrNull(1.seconds) {
            factorial.calc(500)
        }
        result shouldBeEqualTo x1
    }

    @Test
    fun `run suspend fibonacci`() = runSuspendDefault {
        val x1 = fibonacci.calc(500)

        val result = withTimeoutOrNull(1.seconds) {
            fibonacci.calc(500)
        }
        result shouldBeEqualTo x1
    }
}
