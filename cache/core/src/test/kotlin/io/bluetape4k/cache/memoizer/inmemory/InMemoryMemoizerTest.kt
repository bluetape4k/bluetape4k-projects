package io.bluetape4k.cache.memoizer.inmemory

import io.bluetape4k.cache.memoizer.AbstractMemoizerTest
import io.bluetape4k.cache.memoizer.FactorialProvider
import io.bluetape4k.cache.memoizer.FibonacciProvider
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace

class InMemoryMemoizerTest: AbstractMemoizerTest() {

    companion object: KLogging()

    override val heavyFunc: (Int) -> Int = InMemoryMemoizer { x ->
        log.trace { "heavy($x)" }
        Thread.sleep(100)
        x * x
    }

    override val factorial: FactorialProvider = object: FactorialProvider {
        override val cachedCalc: (Long) -> Long by lazy { InMemoryMemoizer { calc(it) } }
    }

    override val fibonacci: FibonacciProvider = object: FibonacciProvider {
        override val cachedCalc: (Long) -> Long by lazy { InMemoryMemoizer { calc(it) } }
    }

}
