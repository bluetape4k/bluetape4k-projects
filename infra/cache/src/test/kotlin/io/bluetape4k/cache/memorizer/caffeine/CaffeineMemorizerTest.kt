package io.bluetape4k.cache.memorizer.caffeine

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.bluetape4k.cache.caffeine.cache
import io.bluetape4k.cache.caffeine.caffeine
import io.bluetape4k.cache.memorizer.AbstractMemorizerTest
import io.bluetape4k.cache.memorizer.FactorialProvider
import io.bluetape4k.cache.memorizer.FibonacciProvider
import io.bluetape4k.logging.KLogging
import java.util.concurrent.ForkJoinPool

class CaffeineMemorizerTest: AbstractMemorizerTest() {

    companion object: KLogging()

    private val caffeine: Caffeine<Any, Any> = caffeine {
        executor(ForkJoinPool.commonPool())
    }

    val cache: Cache<Int, Int> = caffeine.cache()

    override val heavyFunc: (Int) -> Int = cache.memorizer {
        Thread.sleep(100)
        it * it
    }

    override val factorial: FactorialProvider = object: FactorialProvider {
        val cache = caffeine.cache<Long, Long>()

        override val cachedCalc: (Long) -> Long by lazy {
            cache.memorizer { calc(it) }
        }
    }

    override val fibonacci: FibonacciProvider = object: FibonacciProvider {
        val cache = caffeine.cache<Long, Long>()

        override val cachedCalc: (Long) -> Long by lazy {
            cache.memorizer { calc(it) }
        }
    }
}
