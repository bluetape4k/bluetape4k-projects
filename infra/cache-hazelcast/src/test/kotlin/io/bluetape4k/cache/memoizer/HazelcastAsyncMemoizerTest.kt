package io.bluetape4k.cache.memoizer

import com.hazelcast.map.IMap
import io.bluetape4k.cache.HazelcastServers.hazelcastClient
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.testcontainers.utility.Base58
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertFailsWith

class HazelcastAsyncMemoizerTest: AbstractAsyncMemoizerTest() {

    companion object: KLogging() {
        private fun <K: Any, V: Any> newMap(name: String = Base58.randomString(8)): IMap<K, V> =
            hazelcastClient.getMap<K, V>("async:memoizer:$name").apply { clear() }
    }

    private val heavyMap: IMap<Int, Int> = newMap("heavy")

    override val heavyFunc: (Int) -> CompletableFuture<Int> = heavyMap.asyncMemoizer { x ->
        Thread.sleep(100)
        x * x
    }

    override val factorial = object: AsyncFactorialProvider {
        override val cachedCalc: (Long) -> CompletableFuture<Long> = newMap<Long, Long>("factorial")
            .asyncMemoizer { calc(it).join() }
    }

    override val fibonacci = object: AsyncFibonacciProvider {
        override val cachedCalc: (Long) -> CompletableFuture<Long> = newMap<Long, Long>("fibonacci")
            .asyncMemoizer { calc(it).join() }
    }

    @Test
    fun `async memoizer should evaluate once for same key in concurrent calls`() {
        val map: IMap<Int, Int> = newMap()
        val evaluateCount = AtomicInteger(0)
        val memoizer = map.asyncMemoizer { key ->
            evaluateCount.incrementAndGet()
            Thread.sleep(100)
            key * key
        }

        try {
            val futures = List(16) { memoizer(7) }
            futures.forEach { it.get(2, TimeUnit.SECONDS) shouldBeEqualTo 49 }
            evaluateCount.get() shouldBeEqualTo 1
        } finally {
            map.destroy()
        }
    }

    @Test
    fun `cached value bypasses evaluator`() {
        val map: IMap<Int, Int> = newMap()
        map.put(9, 81)
        val evaluateCount = AtomicInteger(0)
        val memoizer = map.asyncMemoizer { key ->
            evaluateCount.incrementAndGet()
            key * key
        }

        try {
            memoizer(9).get(2, TimeUnit.SECONDS) shouldBeEqualTo 81
            evaluateCount.get() shouldBeEqualTo 0
        } finally {
            map.destroy()
        }
    }

    @Test
    fun `failed evaluation is removed from in-flight and next call re-evaluates`() {
        val map: IMap<Int, Int> = newMap()
        val evaluateCount = AtomicInteger(0)
        val memoizer = map.asyncMemoizer { key ->
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
            map.destroy()
        }
    }
}
