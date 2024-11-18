package io.bluetape4k.cache.memorizer.ehcache

import io.bluetape4k.cache.ehcache.ehcacheManager
import io.bluetape4k.cache.ehcache.getOrCreateCache
import io.bluetape4k.cache.memorizer.AbstractMemorizerTest
import io.bluetape4k.cache.memorizer.FactorialProvider
import io.bluetape4k.cache.memorizer.FibonacciProvider
import io.bluetape4k.logging.KLogging

class EhcacheMemorizerTest: AbstractMemorizerTest() {

    companion object: KLogging()

    override val factorial: FactorialProvider = EhcacheFactorialProvider()
    override val fibonacci: FibonacciProvider = EhcacheFibonacciProvider()

    private val ehcacheManager = ehcacheManager { }

    val cache = ehcacheManager.getOrCreateCache<Int, Int>("heavy")

    override val heavyFunc: (Int) -> Int = cache.memorizer {
        Thread.sleep(100)
        it * it
    }

    private class EhcacheFactorialProvider: FactorialProvider() {
        private val ehcacheManager = ehcacheManager {}
        val cache = ehcacheManager.getOrCreateCache<Long, Long>("factorial")

        override val cachedCalc: (Long) -> Long by lazy {
            cache.memorizer { calc(it) }
        }
    }

    private class EhcacheFibonacciProvider: FibonacciProvider() {
        private val ehcacheManager = ehcacheManager {}
        val cache = ehcacheManager.getOrCreateCache<Long, Long>("fibonacci")

        override val cachedCalc: (Long) -> Long by lazy {
            cache.memorizer { calc(it) }
        }
    }
}
