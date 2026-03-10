package io.bluetape4k.cache.memoizer.ehcache

import io.bluetape4k.cache.ehcache.ehcacheManager
import io.bluetape4k.cache.ehcache.getOrCreateCache
import io.bluetape4k.cache.memoizer.AbstractAsyncMemoizerTest
import io.bluetape4k.cache.memoizer.AsyncFactorialProvider
import io.bluetape4k.cache.memoizer.AsyncFibonacciProvider
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.ehcache.Cache
import org.ehcache.CacheManager
import java.util.concurrent.CompletableFuture

class EhcacheAsyncMemoizerTest: AbstractAsyncMemoizerTest() {

    companion object: KLoggingChannel()

    private val ehcacheManager: CacheManager = ehcacheManager { }

    val cache: Cache<Int, Int> = ehcacheManager.getOrCreateCache<Int, Int>("async-heavy")

    override val heavyFunc: (Int) -> CompletableFuture<Int> = cache.asyncMemoizer {
        CompletableFuture.supplyAsync {
            Thread.sleep(100)
            it * it
        }
    }

    override val factorial: AsyncFactorialProvider = object: AsyncFactorialProvider {
        val cache = ehcacheManager.getOrCreateCache<Long, Long>("async-factorial")
        override val cachedCalc: (Long) -> CompletableFuture<Long> by lazy {
            cache.asyncMemoizer { calc(it) }
        }
    }

    override val fibonacci: AsyncFibonacciProvider = object: AsyncFibonacciProvider {
        val cache = ehcacheManager.getOrCreateCache<Long, Long>("async-fibonacci")
        override val cachedCalc: (Long) -> CompletableFuture<Long> by lazy {
            cache.asyncMemoizer { calc(it) }
        }
    }
}
