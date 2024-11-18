package io.bluetape4k.cache.memorizer.caffeine

import io.bluetape4k.cache.caffeine.cache
import io.bluetape4k.cache.caffeine.caffeine
import io.bluetape4k.cache.memorizer.AbstractMemorizerTest
import io.bluetape4k.cache.memorizer.FactorialProvider
import io.bluetape4k.cache.memorizer.FibonacciProvider
import io.bluetape4k.logging.KLogging
import java.util.concurrent.ForkJoinPool

class CaffeineMemorizerTest: AbstractMemorizerTest() {

    companion object: KLogging()

    override val factorial: FactorialProvider = CaffeineFactorialProvider()
    override val fibonacci: FibonacciProvider = CaffeineFibonacciProvider()

    private val caffeine = caffeine {
        executor(ForkJoinPool.commonPool())
    }

    val cache = caffeine.cache<Int, Int>()

    override val heavyFunc: (Int) -> Int = cache.memorizer {
        Thread.sleep(100)
        it * it
    }

    private class CaffeineFactorialProvider: FactorialProvider() {
        val caffeine = caffeine {
            executor(ForkJoinPool.commonPool())
        }
        val cache = caffeine.cache<Long, Long>()

        override val cachedCalc: (Long) -> Long by lazy {
            cache.memorizer { calc(it) }
        }
    }

    private class CaffeineFibonacciProvider: FibonacciProvider() {
        val caffeine = caffeine {
            executor(ForkJoinPool.commonPool())
        }
        val cache = caffeine.cache<Long, Long>()

        override val cachedCalc: (Long) -> Long by lazy {
            cache.memorizer { calc(it) }
        }
    }
}
