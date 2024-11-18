package io.bluetape4k.cache.memorizer.cache2k

import io.bluetape4k.cache.cache2k.cache2k
import io.bluetape4k.cache.memorizer.AbstractAsyncMemorizerTest
import io.bluetape4k.cache.memorizer.AsyncFactorialProvider
import io.bluetape4k.cache.memorizer.AsyncFibonacciProvider
import io.bluetape4k.logging.KLogging
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool

class AsyncCache2kMemorizerTest: AbstractAsyncMemorizerTest() {

    companion object: KLogging()

    override val factorial: AsyncFactorialProvider = AsyncCache2kFactorialProvider()
    override val fibonacci: AsyncFibonacciProvider = AsyncCache2kFibonacciProvider()

    val cache = cache2k<Int, Int> {
        this.executor(ForkJoinPool.commonPool())
    }.build()

    override val heavyFunc: (Int) -> CompletableFuture<Int> = cache.asyncMemorizer {
        CompletableFuture.supplyAsync {
            Thread.sleep(100)
            it * it
        }
    }

    private class AsyncCache2kFactorialProvider: AsyncFactorialProvider() {
        val cache = cache2k<Long, Long> {
            this.executor(ForkJoinPool.commonPool())
        }.build()

        override val cachedCalc: (Long) -> CompletableFuture<Long> by lazy {
            cache.asyncMemorizer { calc(it) }
        }
    }

    private class AsyncCache2kFibonacciProvider: AsyncFibonacciProvider() {
        val cache = cache2k<Long, Long> {
            this.executor(ForkJoinPool.commonPool())
        }.build()

        override val cachedCalc: (Long) -> CompletableFuture<Long> by lazy {
            cache.asyncMemorizer { calc(it) }
        }
    }
}
