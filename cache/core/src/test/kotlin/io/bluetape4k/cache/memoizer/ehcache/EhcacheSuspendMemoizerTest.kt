package io.bluetape4k.cache.memoizer.ehcache

import io.bluetape4k.cache.ehcache.ehcacheManager
import io.bluetape4k.cache.ehcache.getOrCreateCache
import io.bluetape4k.cache.memoizer.AbstractSuspendMemoizerTest
import io.bluetape4k.cache.memoizer.SuspendFactorialProvider
import io.bluetape4k.cache.memoizer.SuspendFibonacciProvider
import io.bluetape4k.logging.coroutines.KLoggingChannel

class EhcacheSuspendMemoizerTest: AbstractSuspendMemoizerTest() {

    companion object: KLoggingChannel()

    private val ehcacheManager = ehcacheManager { }
    private val cache = ehcacheManager.getOrCreateCache<Int, Int>("suspend-heavy")

    override val heavyFunc: suspend (Int) -> Int = cache.suspendMemoizer {
        Thread.sleep(100)
        it * it
    }

    override val factorial: SuspendFactorialProvider = object: SuspendFactorialProvider {
        private val cache = ehcacheManager.getOrCreateCache<Long, Long>("suspend-factorial")
        override val cachedCalc: suspend (Long) -> Long = cache.suspendMemoizer { calc(it) }
    }
    override val fibonacci: SuspendFibonacciProvider = object: SuspendFibonacciProvider {
        private val cache = ehcacheManager.getOrCreateCache<Long, Long>("suspend-fibonacci")
        override val cachedCalc: suspend (Long) -> Long = cache.suspendMemoizer { calc(it) }
    }
}
