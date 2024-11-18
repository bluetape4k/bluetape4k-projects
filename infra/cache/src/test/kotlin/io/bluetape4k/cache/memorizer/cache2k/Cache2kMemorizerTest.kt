package io.bluetape4k.cache.memorizer.cache2k

import io.bluetape4k.cache.cache2k.cache2k
import io.bluetape4k.cache.memorizer.AbstractMemorizerTest
import io.bluetape4k.cache.memorizer.FactorialProvider
import io.bluetape4k.cache.memorizer.FibonacciProvider
import io.bluetape4k.logging.KLogging
import java.util.concurrent.ForkJoinPool

class Cache2kMemorizerTest: AbstractMemorizerTest() {

    companion object: KLogging()

    override val factorial: FactorialProvider = Cache2kFactorialProvider()
    override val fibonacci: FibonacciProvider = Cache2kFibonacciProvider()

    val cache = cache2k<Int, Int> {
        this.executor(ForkJoinPool.commonPool())
    }.build()

    override val heavyFunc: (Int) -> Int = cache.memorizer {
        Thread.sleep(100)
        it * it
    }

    private class Cache2kFactorialProvider: FactorialProvider() {
        val cache = cache2k<Long, Long> {
            this.executor(ForkJoinPool.commonPool())
        }.build()

        override val cachedCalc: (Long) -> Long by lazy {
            cache.memorizer { calc(it) }
        }
    }

    private class Cache2kFibonacciProvider: FibonacciProvider() {
        val cache = cache2k<Long, Long> {
            this.executor(ForkJoinPool.commonPool())
        }.build()

        override val cachedCalc: (Long) -> Long by lazy {
            cache.memorizer { calc(it) }
        }
    }
}
