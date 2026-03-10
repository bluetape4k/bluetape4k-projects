package io.bluetape4k.cache.memoizer.inmemory

import io.bluetape4k.cache.memoizer.AbstractAsyncMemoizerTest
import io.bluetape4k.cache.memoizer.AsyncFactorialProvider
import io.bluetape4k.cache.memoizer.AsyncFibonacciProvider
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import java.util.concurrent.CompletableFuture

class InMemoryAsyncMemoizerTest: AbstractAsyncMemoizerTest() {

    companion object: KLoggingChannel()

    override val heavyFunc: (Int) -> CompletableFuture<Int> = InMemoryMemoizer { x ->
        CompletableFuture.supplyAsync {
            log.trace { "heavy($x)" }

            Thread.sleep(100)
            x * x
        }
    }

    override val factorial: AsyncFactorialProvider = object: AsyncFactorialProvider {
        override val cachedCalc: (Long) -> CompletableFuture<Long> by lazy {
            InMemoryAsyncMemoizer { calc(it) }
        }
    }

    override val fibonacci: AsyncFibonacciProvider = object: AsyncFibonacciProvider {
        override val cachedCalc: (Long) -> CompletableFuture<Long> by lazy {
            InMemoryAsyncMemoizer { calc(it) }
        }
    }
}
