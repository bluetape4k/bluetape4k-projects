package io.bluetape4k.cache.memorizer.cache2k

import io.bluetape4k.cache.cache2k.cache2k
import io.bluetape4k.cache.memorizer.AbstractMemorizerTest
import io.bluetape4k.cache.memorizer.FactorialProvider
import io.bluetape4k.cache.memorizer.FibonacciProvider
import io.bluetape4k.logging.KLogging
import org.cache2k.Cache
import java.util.concurrent.ForkJoinPool

class Cache2kMemorizerTest: AbstractMemorizerTest() {

    companion object: KLogging()

    val cache: Cache<Int, Int> = cache2k<Int, Int> {
        this.name("heavyFunc")
        this.executor(ForkJoinPool.commonPool())
    }.build()

    override val heavyFunc: (Int) -> Int = cache.memorizer {
        Thread.sleep(100)
        it * it
    }

    override val factorial: FactorialProvider = object: FactorialProvider {
        val cache = cache2k<Long, Long> {
            this.name("factorial")
            this.executor(ForkJoinPool.commonPool())
        }.build()

        override val cachedCalc: (Long) -> Long by lazy {
            cache.memorizer { calc(it) }
        }
    }

    override val fibonacci: FibonacciProvider = object: FibonacciProvider {
        val cache = cache2k<Long, Long> {
            this.name("fibonacci")
            this.executor(ForkJoinPool.commonPool())
        }.build()

        override val cachedCalc: (Long) -> Long by lazy {
            cache.memorizer { calc(it) }
        }
    }
}
