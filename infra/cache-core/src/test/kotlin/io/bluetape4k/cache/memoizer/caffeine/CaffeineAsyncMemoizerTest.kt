package io.bluetape4k.cache.memoizer.caffeine

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.bluetape4k.cache.caffeine.cache
import io.bluetape4k.cache.caffeine.caffeine
import io.bluetape4k.cache.memoizer.AbstractAsyncMemoizerTest
import io.bluetape4k.cache.memoizer.AsyncFactorialProvider
import io.bluetape4k.cache.memoizer.AsyncFibonacciProvider
import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool

class CaffeineAsyncMemoizerTest: AbstractAsyncMemoizerTest() {

    companion object: KLoggingChannel()

    private val caffeine: Caffeine<Any, Any> = caffeine {
        executor(ForkJoinPool.commonPool())
    }
    val cache: Cache<Int, Int> = caffeine.cache()

    override val heavyFunc: (Int) -> CompletableFuture<Int> = cache.asyncMemoizer {
        CompletableFuture.supplyAsync {
            Thread.sleep(100)
            it * it
        }
    }

    override val factorial: AsyncFactorialProvider = object: AsyncFactorialProvider {
        val cache = caffeine.cache<Long, Long>()

        override val cachedCalc: (Long) -> CompletableFuture<Long> by lazy {
            cache.asyncMemoizer { calc(it) }
        }
    }

    override val fibonacci: AsyncFibonacciProvider = object: AsyncFibonacciProvider {
        val cache = caffeine.cache<Long, Long>()

        override val cachedCalc: (Long) -> CompletableFuture<Long> by lazy {
            cache.asyncMemoizer { calc(it) }
        }
    }
}
