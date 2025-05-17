package io.bluetape4k.cache.memorizer.inmemory

import io.bluetape4k.cache.memorizer.AbstractAsyncMemorizerTest
import io.bluetape4k.cache.memorizer.AsyncFactorialProvider
import io.bluetape4k.cache.memorizer.AsyncFibonacciProvider
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import java.util.concurrent.CompletableFuture

class AsyncInMemoryMemorizerTest: AbstractAsyncMemorizerTest() {

    companion object: KLoggingChannel()

    override val factorial: AsyncFactorialProvider = InMemoryAsyncFactorialProvider()

    override val fibonacci: AsyncFibonacciProvider = InMemoryAsyncFibonacciProvider()

    override val heavyFunc: (Int) -> CompletableFuture<Int> = InMemoryMemorizer { x ->
        CompletableFuture.supplyAsync {
            log.trace { "heavy($x)" }

            Thread.sleep(100)
            x * x
        }
    }

    private class InMemoryAsyncFactorialProvider: AsyncFactorialProvider() {
        override val cachedCalc: (Long) -> CompletableFuture<Long> by lazy {
            AsyncInMemoryMemorizer { calc(it) }
        }
    }

    private class InMemoryAsyncFibonacciProvider: AsyncFibonacciProvider() {
        override val cachedCalc: (Long) -> CompletableFuture<Long> by lazy {
            AsyncInMemoryMemorizer { calc(it) }
        }
    }
}
