package io.bluetape4k.cache.memoizer.caffeine

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.bluetape4k.cache.caffeine.cache
import io.bluetape4k.cache.caffeine.caffeine
import io.bluetape4k.cache.memoizer.AbstractAsyncMemoizerTest
import io.bluetape4k.cache.memoizer.AsyncFactorialProvider
import io.bluetape4k.cache.memoizer.AsyncFibonacciProvider
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

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

    @Test
    fun `clear 후 캐시가 무효화된다`() {
        val evalCount = AtomicInteger(0)
        val localCache = caffeine.cache<String, Int>()
        val memo = localCache.asyncMemoizer { key: String ->
            evalCount.incrementAndGet()
            CompletableFuture.completedFuture(key.length)
        }

        memo("hello").get() shouldBeEqualTo 5
        evalCount.get() shouldBeEqualTo 1

        // Cached — evaluator must not run again
        memo("hello").get() shouldBeEqualTo 5
        evalCount.get() shouldBeEqualTo 1

        memo.clear()

        // After clear — fresh evaluation
        memo("hello").get() shouldBeEqualTo 5
        evalCount.get() shouldBeEqualTo 2
    }

    @Test
    fun `clear 중 진행 중인 비동기 결과는 캐시에 저장되지 않는다`() {
        val evalStarted = CountDownLatch(1)
        val evalProceed = CountDownLatch(1)
        val localCache = caffeine.cache<String, Int>()

        val memo = localCache.asyncMemoizer { key: String ->
            CompletableFuture.supplyAsync {
                evalStarted.countDown()
                evalProceed.await(2, TimeUnit.SECONDS)
                key.length
            }
        }

        val future = memo("hello")
        evalStarted.await(2, TimeUnit.SECONDS)   // wait for evaluator to start

        memo.clear()                              // invalidate while in-flight
        evalProceed.countDown()                   // let evaluator finish

        // Caller still receives the computed value
        future.get(2, TimeUnit.SECONDS) shouldBeEqualTo 5
        // Stale result must NOT be written back into the cleared cache
        localCache.getIfPresent("hello").shouldBeNull()
    }
}
