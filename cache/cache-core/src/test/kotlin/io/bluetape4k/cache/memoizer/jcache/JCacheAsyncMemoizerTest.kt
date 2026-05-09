package io.bluetape4k.cache.memoizer.jcache

import io.bluetape4k.cache.jcache.JCaching
import io.bluetape4k.cache.memoizer.AbstractAsyncMemoizerTest
import io.bluetape4k.cache.memoizer.AsyncFactorialProvider
import io.bluetape4k.cache.memoizer.AsyncFibonacciProvider
import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.util.concurrent.CompletableFuture

class JCacheAsyncMemoizerTest: AbstractAsyncMemoizerTest() {

    companion object: KLoggingChannel()

    private val cache = JCaching.Caffeine.getOrCreate<Int, Int>("jcache-async-memoizer-heavy")

    override val heavyFunc: (Int) -> CompletableFuture<Int> = cache.asyncMemoizer {
        CompletableFuture.supplyAsync {
            Thread.sleep(100)
            it * it
        }
    }

    override val factorial: AsyncFactorialProvider = object: AsyncFactorialProvider {
        private val cache = JCaching.Caffeine.getOrCreate<Long, Long>("jcache-async-memoizer-factorial")

        override val cachedCalc: (Long) -> CompletableFuture<Long> by lazy {
            cache.asyncMemoizer { calc(it) }
        }
    }

    override val fibonacci: AsyncFibonacciProvider = object: AsyncFibonacciProvider {
        private val cache = JCaching.Caffeine.getOrCreate<Long, Long>("jcache-async-memoizer-fibonacci")

        override val cachedCalc: (Long) -> CompletableFuture<Long> by lazy {
            cache.asyncMemoizer { calc(it) }
        }
    }
}
