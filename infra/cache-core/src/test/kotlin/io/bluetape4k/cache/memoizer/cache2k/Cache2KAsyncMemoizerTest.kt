package io.bluetape4k.cache.memoizer.cache2k

import io.bluetape4k.cache.cache2k.cache2k
import io.bluetape4k.cache.memoizer.AbstractAsyncMemoizerTest
import io.bluetape4k.cache.memoizer.AsyncFactorialProvider
import io.bluetape4k.cache.memoizer.AsyncFibonacciProvider
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.cache2k.Cache
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool

class Cache2KAsyncMemoizerTest: AbstractAsyncMemoizerTest() {

    companion object: KLoggingChannel()


    val cache: Cache<Int, Int> = cache2k<Int, Int> {
        this.name("async-heavyFunc")
        this.executor(ForkJoinPool.commonPool())
    }.build()

    override val heavyFunc: (Int) -> CompletableFuture<Int> = cache.asyncMemoizer {
        CompletableFuture.supplyAsync {
            Thread.sleep(100)
            it * it
        }
    }

    override val factorial: AsyncFactorialProvider = object: AsyncFactorialProvider {
        val cache = cache2k<Long, Long> {
            this.name("async-factorial")
            this.executor(ForkJoinPool.commonPool())
        }.build()

        override val cachedCalc: (Long) -> CompletableFuture<Long> by lazy {
            cache.asyncMemoizer { calc(it) }
        }
    }

    override val fibonacci: AsyncFibonacciProvider = object: AsyncFibonacciProvider {
        val cache = cache2k<Long, Long> {
            this.name("async-fibonacci")
            this.executor(ForkJoinPool.commonPool())
        }.build()

        override val cachedCalc: (Long) -> CompletableFuture<Long> by lazy {
            cache.asyncMemoizer { calc(it) }
        }
    }
}
