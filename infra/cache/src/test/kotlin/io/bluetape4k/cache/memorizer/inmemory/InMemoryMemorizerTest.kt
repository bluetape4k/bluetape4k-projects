package io.bluetape4k.cache.memorizer.inmemory

import io.bluetape4k.cache.memorizer.AbstractMemorizerTest
import io.bluetape4k.cache.memorizer.FactorialProvider
import io.bluetape4k.cache.memorizer.FibonacciProvider
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace

class InMemoryMemorizerTest: AbstractMemorizerTest() {

    companion object: KLogging()

    override val heavyFunc: (Int) -> Int = InMemoryMemorizer { x ->
        log.trace { "heavy($x)" }
        Thread.sleep(100)
        x * x
    }

    override val factorial: FactorialProvider = object: FactorialProvider {
        override val cachedCalc: (Long) -> Long by lazy { InMemoryMemorizer { calc(it) } }
    }

    override val fibonacci: FibonacciProvider = object: FibonacciProvider {
        override val cachedCalc: (Long) -> Long by lazy { InMemoryMemorizer { calc(it) } }
    }

}
