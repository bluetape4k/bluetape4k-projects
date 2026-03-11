package io.bluetape4k.cache.memoizer

import io.bluetape4k.cache.IgniteServers
import io.bluetape4k.concurrent.VirtualThreadExecutor
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.apache.ignite.client.ClientCache
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.utility.Base58
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertFailsWith

class IgniteAsyncMemoizerTest: AbstractAsyncMemoizerTest() {

    companion object: KLogging() {
        private val igniteClient by lazy { IgniteServers.igniteClient }

        private fun <K: Any, V: Any> newCache(name: String = Base58.randomString(8)): ClientCache<K, V> =
            igniteClient.getOrCreateCache("async:memoizer:$name")

        @BeforeAll
        @JvmStatic
        fun warmUp() {
            // arm64 Ignite 에서 ForkJoinPool / VirtualThread 기반 첫 번째 연결 초기화가 느릴 수 있으므로
            // 테스트 시작 전에 두 스레드 풀 모두 warm-up 을 수행합니다.
            val warmUpCache = newCache<Int, Int>("warmup")
            warmUpCache.put(0, 0)
            // ForkJoinPool thread 가 사용하는 Ignite 연결 warm-up
            CompletableFuture.supplyAsync { warmUpCache.get(0) }.get(60, TimeUnit.SECONDS)
            // VirtualThread 가 사용하는 Ignite 연결 warm-up (arm64 첫 연결 초기화 지연 방지)
            CompletableFuture.supplyAsync({ warmUpCache.get(0) }, VirtualThreadExecutor).get(60, TimeUnit.SECONDS)
        }
    }

    private val heavyCache: ClientCache<Int, Int> = newCache("heavy")

    override val heavyFunc: (Int) -> CompletableFuture<Int> = heavyCache.asyncMemoizer { x ->
        Thread.sleep(100)
        x * x
    }

    override val factorial = object: AsyncFactorialProvider {
        override val cachedCalc: (Long) -> CompletableFuture<Long> = newCache<Long, Long>("factorial")
            .asyncMemoizer { calc(it).join() }
    }

    override val fibonacci = object: AsyncFibonacciProvider {
        override val cachedCalc: (Long) -> CompletableFuture<Long> = newCache<Long, Long>("fibonacci")
            .asyncMemoizer { calc(it).join() }
    }

    @Test
    fun `async memoizer should evaluate once for same key in concurrent calls`() {
        val cache: ClientCache<Int, Int> = newCache()
        val evaluateCount = AtomicInteger(0)
        val memoizer = cache.asyncMemoizer { key ->
            evaluateCount.incrementAndGet()
            Thread.sleep(100)
            key * key
        }

        try {
            val futures = List(16) { memoizer(7) }
            futures.forEach { it.get(2, TimeUnit.SECONDS) shouldBeEqualTo 49 }
            evaluateCount.get() shouldBeEqualTo 1
        } finally {
            runCatching { cache.clear() }
        }
    }

    @Test
    fun `cached value bypasses evaluator`() {
        val cache: ClientCache<Int, Int> = newCache()
        cache.put(9, 81)
        val evaluateCount = AtomicInteger(0)
        val memoizer = cache.asyncMemoizer { key ->
            evaluateCount.incrementAndGet()
            key * key
        }

        try {
            memoizer(9).get(2, TimeUnit.SECONDS) shouldBeEqualTo 81
            evaluateCount.get() shouldBeEqualTo 0
        } finally {
            runCatching { cache.clear() }
        }
    }

    @Test
    fun `failed evaluation is removed from in-flight and next call re-evaluates`() {
        val cache: ClientCache<Int, Int> = newCache()
        val evaluateCount = AtomicInteger(0)
        val memoizer = cache.asyncMemoizer { key ->
            when (evaluateCount.incrementAndGet()) {
                1 -> error("boom")
                else -> key * key
            }
        }

        try {
            assertFailsWith<ExecutionException> {
                memoizer(5).get(10, TimeUnit.SECONDS)
            }
            memoizer(5).get(10, TimeUnit.SECONDS) shouldBeEqualTo 25
            evaluateCount.get() shouldBeEqualTo 2
        } finally {
            runCatching { cache.clear() }
        }
    }
}
