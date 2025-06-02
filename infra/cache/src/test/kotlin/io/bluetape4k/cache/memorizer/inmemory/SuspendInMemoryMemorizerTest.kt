package io.bluetape4k.cache.memorizer.inmemory

import io.bluetape4k.cache.memorizer.AbstractSuspendMemorizerTest
import io.bluetape4k.cache.memorizer.SuspendFactorialProvider
import io.bluetape4k.cache.memorizer.SuspendFibonacciProvider
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import kotlinx.coroutines.delay

class SuspendInMemoryMemorizerTest: AbstractSuspendMemorizerTest() {

    companion object: KLoggingChannel()

    override val heavyFunc: suspend (Int) -> Int = SuspendInMemoryMemorizer { x ->
        log.trace { "heavy($x)" }
        delay(100L)
        x * x
    }

    override val factorial: SuspendFactorialProvider = object: SuspendFactorialProvider {
        override val cachedCalc: suspend (Long) -> Long by lazy {
            SuspendInMemoryMemorizer { n -> calc(n) }
        }
    }

    override val fibonacci: SuspendFibonacciProvider = object: SuspendFibonacciProvider {
        override val cachedCalc: suspend (Long) -> Long by lazy {
            SuspendInMemoryMemorizer { n -> calc(n) }
        }
    }
}
