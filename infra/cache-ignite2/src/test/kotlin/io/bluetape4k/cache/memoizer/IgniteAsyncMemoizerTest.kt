package io.bluetape4k.cache.memoizer

import io.bluetape4k.cache.IgniteServers
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.apache.ignite.client.ClientCache
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
            igniteClient.getOrCreateCache<K, V>("async:memoizer:$name").apply { clear() }
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
            cache.clear()
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
            cache.clear()
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
                memoizer(5).get(2, TimeUnit.SECONDS)
            }
            memoizer(5).get(2, TimeUnit.SECONDS) shouldBeEqualTo 25
            evaluateCount.get() shouldBeEqualTo 2
        } finally {
            cache.clear()
        }
    }
}
