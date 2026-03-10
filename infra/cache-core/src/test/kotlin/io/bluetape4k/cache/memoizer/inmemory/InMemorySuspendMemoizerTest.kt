package io.bluetape4k.cache.memoizer.inmemory

import io.bluetape4k.cache.memoizer.AbstractSuspendMemoizerTest
import io.bluetape4k.cache.memoizer.SuspendFactorialProvider
import io.bluetape4k.cache.memoizer.SuspendFibonacciProvider
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import kotlinx.coroutines.delay

class InMemorySuspendMemoizerTest: AbstractSuspendMemoizerTest() {

    companion object: KLoggingChannel()

    override val heavyFunc: suspend (Int) -> Int = InMemorySuspendMemoizer { x ->
        log.trace { "heavy($x)" }
        delay(100L)
        x * x
    }

    override val factorial: SuspendFactorialProvider = object: SuspendFactorialProvider {
        override val cachedCalc: suspend (Long) -> Long by lazy {
            InMemorySuspendMemoizer { n -> calc(n) }
        }
    }

    override val fibonacci: SuspendFibonacciProvider = object: SuspendFibonacciProvider {
        override val cachedCalc: suspend (Long) -> Long by lazy {
            InMemorySuspendMemoizer { n -> calc(n) }
        }
    }
}
