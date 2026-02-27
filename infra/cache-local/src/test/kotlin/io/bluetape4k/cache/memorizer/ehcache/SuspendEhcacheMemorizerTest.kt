package io.bluetape4k.cache.memorizer.ehcache

import io.bluetape4k.cache.ehcache.ehcacheManager
import io.bluetape4k.cache.ehcache.getOrCreateCache
import io.bluetape4k.cache.memorizer.AbstractSuspendMemorizerTest
import io.bluetape4k.cache.memorizer.SuspendFactorialProvider
import io.bluetape4k.cache.memorizer.SuspendFibonacciProvider
import io.bluetape4k.logging.coroutines.KLoggingChannel

class SuspendEhcacheMemorizerTest: AbstractSuspendMemorizerTest() {

    companion object: KLoggingChannel()

    private val ehcacheManager = ehcacheManager { }
    private val cache = ehcacheManager.getOrCreateCache<Int, Int>("suspend-heavy")

    override val heavyFunc: suspend (Int) -> Int = cache.suspendMemorizer {
        Thread.sleep(100)
        it * it
    }

    override val factorial: SuspendFactorialProvider = object: SuspendFactorialProvider {
        private val cache = ehcacheManager.getOrCreateCache<Long, Long>("suspend-factorial")
        override val cachedCalc: suspend (Long) -> Long = cache.suspendMemorizer { calc(it) }
    }
    override val fibonacci: SuspendFibonacciProvider = object: SuspendFibonacciProvider {
        private val cache = ehcacheManager.getOrCreateCache<Long, Long>("suspend-fibonacci")
        override val cachedCalc: suspend (Long) -> Long = cache.suspendMemorizer { calc(it) }
    }
}
