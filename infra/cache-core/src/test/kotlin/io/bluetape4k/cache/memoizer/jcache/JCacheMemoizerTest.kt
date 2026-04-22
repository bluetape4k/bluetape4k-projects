package io.bluetape4k.cache.memoizer.jcache

import io.bluetape4k.cache.jcache.JCaching
import io.bluetape4k.cache.memoizer.AbstractMemoizerTest
import io.bluetape4k.cache.memoizer.FactorialProvider
import io.bluetape4k.cache.memoizer.FibonacciProvider
import io.bluetape4k.logging.KLogging

class JCacheMemoizerTest: AbstractMemoizerTest() {

    companion object: KLogging()

    private val cache = JCaching.Caffeine.getOrCreate<Int, Int>("jcache-memoizer-heavy")

    override val heavyFunc: (Int) -> Int = cache.memoizer {
        Thread.sleep(100)
        it * it
    }

    override val factorial: FactorialProvider = object: FactorialProvider {
        private val cache = JCaching.Caffeine.getOrCreate<Long, Long>("jcache-memoizer-factorial")

        override val cachedCalc: (Long) -> Long by lazy {
            cache.memoizer { calc(it) }
        }
    }

    override val fibonacci: FibonacciProvider = object: FibonacciProvider {
        private val cache = JCaching.Caffeine.getOrCreate<Long, Long>("jcache-memoizer-fibonacci")

        override val cachedCalc: (Long) -> Long by lazy {
            cache.memoizer { calc(it) }
        }
    }
}
