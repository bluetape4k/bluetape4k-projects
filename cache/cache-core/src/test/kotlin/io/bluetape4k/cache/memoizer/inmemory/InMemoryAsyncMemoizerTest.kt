package io.bluetape4k.cache.memoizer.inmemory

import io.bluetape4k.cache.memoizer.AbstractAsyncMemoizerTest
import io.bluetape4k.cache.memoizer.AsyncFactorialProvider
import io.bluetape4k.cache.memoizer.AsyncFibonacciProvider
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

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

    @Test
    fun `clear 중 진행 중인 비동기 결과는 캐시에 저장되지 않는다`() {
        val evalCount = AtomicInteger(0)
        val evalStarted = CountDownLatch(1)
        val firstEvaluatorFuture = CompletableFuture<Int>()
        val memo = InMemoryAsyncMemoizer<String, Int> { key ->
            if (evalCount.incrementAndGet() == 1) {
                evalStarted.countDown()
                firstEvaluatorFuture
            } else {
                CompletableFuture.completedFuture(key.length)
            }
        }

        val first = memo("hello")
        evalStarted.await(2, TimeUnit.SECONDS)

        memo.clear()
        firstEvaluatorFuture.complete(5)

        first.get(2, TimeUnit.SECONDS) shouldBeEqualTo 5
        memo("hello").get(2, TimeUnit.SECONDS) shouldBeEqualTo 5
        evalCount.get() shouldBeEqualTo 2
    }

    @Test
    fun `null future 완료 후 같은 키를 다시 호출하면 새 계산으로 복구된다`() {
        val evalCount = AtomicInteger(0)
        val memo = InMemoryAsyncMemoizer<String, Int> { key ->
            if (evalCount.incrementAndGet() == 1) {
                @Suppress("UNCHECKED_CAST")
                CompletableFuture.completedFuture(null) as CompletableFuture<Int>
            } else {
                CompletableFuture.completedFuture(key.length)
            }
        }

        assertFailsWith<ExecutionException> {
            memo("recover").get(2, TimeUnit.SECONDS)
        }

        memo("recover").get(2, TimeUnit.SECONDS) shouldBeEqualTo 7
        evalCount.get() shouldBeEqualTo 2
    }
}
