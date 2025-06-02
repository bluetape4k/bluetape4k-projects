package io.bluetape4k.cache.memorizer.inmemory

import io.bluetape4k.cache.memorizer.AbstractAsyncMemorizerTest
import io.bluetape4k.cache.memorizer.AsyncFactorialProvider
import io.bluetape4k.cache.memorizer.AsyncFibonacciProvider
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import java.util.concurrent.CompletableFuture

class AsyncInMemoryMemorizerTest: AbstractAsyncMemorizerTest() {

    companion object: KLoggingChannel()

    override val heavyFunc: (Int) -> CompletableFuture<Int> = InMemoryMemorizer { x ->
        CompletableFuture.supplyAsync {
            log.trace { "heavy($x)" }

            Thread.sleep(100)
            x * x
        }
    }

    override val factorial: AsyncFactorialProvider = object: AsyncFactorialProvider {
        override val cachedCalc: (Long) -> CompletableFuture<Long> by lazy {
            AsyncInMemoryMemorizer { calc(it) }
        }
    }

    override val fibonacci: AsyncFibonacciProvider = object: AsyncFibonacciProvider {
        override val cachedCalc: (Long) -> CompletableFuture<Long> by lazy {
            AsyncInMemoryMemorizer { calc(it) }
        }
    }
}
