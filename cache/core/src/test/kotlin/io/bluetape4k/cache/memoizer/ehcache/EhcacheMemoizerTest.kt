package io.bluetape4k.cache.memoizer.ehcache

import io.bluetape4k.cache.ehcache.ehcacheManager
import io.bluetape4k.cache.ehcache.getOrCreateCache
import io.bluetape4k.cache.memoizer.AbstractMemoizerTest
import io.bluetape4k.cache.memoizer.FactorialProvider
import io.bluetape4k.cache.memoizer.FibonacciProvider
import io.bluetape4k.logging.KLogging
import org.ehcache.Cache

class EhcacheMemoizerTest: AbstractMemoizerTest() {

    companion object: KLogging()

    private val ehcacheManager = ehcacheManager { }
    private val cache: Cache<Int, Int> = ehcacheManager.getOrCreateCache<Int, Int>("heavy")

    override val heavyFunc: (Int) -> Int = cache.memoizer {
        Thread.sleep(100)
        it * it
    }

    override val factorial: FactorialProvider = object: FactorialProvider {
        val cache = ehcacheManager.getOrCreateCache<Long, Long>("factorial")

        override val cachedCalc: (Long) -> Long by lazy {
            cache.memoizer { calc(it) }
        }
    }

    override val fibonacci: FibonacciProvider = object: FibonacciProvider {
        private val ehcacheManager = ehcacheManager {}
        val cache = ehcacheManager.getOrCreateCache<Long, Long>("fibonacci")

        override val cachedCalc: (Long) -> Long by lazy {
            cache.memoizer { calc(it) }
        }
    }
}
