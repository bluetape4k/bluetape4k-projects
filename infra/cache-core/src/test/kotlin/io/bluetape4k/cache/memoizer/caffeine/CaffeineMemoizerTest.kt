package io.bluetape4k.cache.memoizer.caffeine

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.bluetape4k.cache.caffeine.cache
import io.bluetape4k.cache.caffeine.caffeine
import io.bluetape4k.cache.memoizer.AbstractMemoizerTest
import io.bluetape4k.cache.memoizer.FactorialProvider
import io.bluetape4k.cache.memoizer.FibonacciProvider
import io.bluetape4k.logging.KLogging
import java.util.concurrent.ForkJoinPool

class CaffeineMemoizerTest: AbstractMemoizerTest() {

    companion object: KLogging()

    private val caffeine: Caffeine<Any, Any> = caffeine {
        executor(ForkJoinPool.commonPool())
    }

    val cache: Cache<Int, Int> = caffeine.cache()

    override val heavyFunc: (Int) -> Int = cache.memoizer {
        Thread.sleep(100)
        it * it
    }

    override val factorial: FactorialProvider = object: FactorialProvider {
        val cache = caffeine.cache<Long, Long>()

        override val cachedCalc: (Long) -> Long by lazy {
            cache.memoizer { calc(it) }
        }
    }

    override val fibonacci: FibonacciProvider = object: FibonacciProvider {
        val cache = caffeine.cache<Long, Long>()

        override val cachedCalc: (Long) -> Long by lazy {
            cache.memoizer { calc(it) }
        }
    }
}
