package io.bluetape4k.cache.memoizer

import com.hazelcast.map.IMap
import io.bluetape4k.cache.HazelcastServers
import io.bluetape4k.cache.memoizer.hazelcast.asyncMemoizer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeout
import org.testcontainers.utility.Base58
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

class AsyncHazelcastMemoizerTest {

    companion object: KLogging() {
        private val hazelcastClient by lazy { HazelcastServers.hazelcastClient }

        private fun <K: Any, V: Any> newMap(name: String = Base58.randomString(8)): IMap<K, V> =
            hazelcastClient.getMap<K, V>("async:memoizer:$name").apply { clear() }
    }

    private val heavyMap: IMap<Int, Int> = newMap("heavy")

    val heavyFunc: (Int) -> CompletableFuture<Int> = heavyMap.asyncMemoizer { x ->
        Thread.sleep(100)
        x * x
    }

    private val factorial = object: AsyncFactorialProvider {
        override val cachedCalc: (Long) -> CompletableFuture<Long> = newMap<Long, Long>("factorial")
            .asyncMemoizer { calc(it).join() }
    }

    private val fibonacci = object: AsyncFibonacciProvider {
        override val cachedCalc: (Long) -> CompletableFuture<Long> = newMap<Long, Long>("fibonacci")
            .asyncMemoizer { calc(it).join() }
    }

    @Test
    fun `run heavy function`() {
        measureTimeMillis {
            heavyFunc(10).get() shouldBeEqualTo 100
        }

        assertTimeout(Duration.ofMillis(1000)) {
            heavyFunc(10).get() shouldBeEqualTo 100
        }
    }

    @Test
    fun `run factorial`() {
        val x1 = factorial.calc(100).get()

        assertTimeout(Duration.ofMillis(1000)) {
            factorial.calc(100).get()
        } shouldBeEqualTo x1
    }

    @Test
    fun `run fibonacci`() {
        val x1 = fibonacci.calc(100).get()

        assertTimeout(Duration.ofMillis(1000)) {
            fibonacci.calc(100).get()
        } shouldBeEqualTo x1
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
                1 -> throw IllegalStateException("boom")
                else -> key * key
            }
        }

        try {
            org.junit.jupiter.api.assertThrows<ExecutionException> {
                memoizer(5).get(2, TimeUnit.SECONDS)
            }
            memoizer(5).get(2, TimeUnit.SECONDS) shouldBeEqualTo 25
            evaluateCount.get() shouldBeEqualTo 2
        } finally {
            map.destroy()
        }
    }

    interface AsyncFactorialProvider {
        companion object: KLogging()

        val cachedCalc: (Long) -> CompletableFuture<Long>

        fun calc(x: Long): CompletableFuture<Long> {
            log.trace { "factorial($x)" }
            return when {
                x <= 1L -> CompletableFuture.completedFuture(1L)
                else -> cachedCalc(x - 1).thenApplyAsync { x * it }
            }
        }
    }

    interface AsyncFibonacciProvider {
        companion object: KLogging()

        val cachedCalc: (Long) -> CompletableFuture<Long>

        fun calc(x: Long): CompletableFuture<Long> {
            log.trace { "fibonacci($x)" }
            return when {
                x <= 0L -> CompletableFuture.completedFuture(0L)
                x <= 2L -> CompletableFuture.completedFuture(1L)
                else -> cachedCalc(x - 1).thenComposeAsync { x1 ->
                    cachedCalc(x - 2).thenApplyAsync { x2 -> x1 + x2 }
                }
            }
        }
    }
}
