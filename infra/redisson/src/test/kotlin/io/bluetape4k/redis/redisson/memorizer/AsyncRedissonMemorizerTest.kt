package io.bluetape4k.redis.redisson.memorizer

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import io.bluetape4k.redis.redisson.AbstractRedissonTest
import org.amshove.kluent.shouldBeEqualTo
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeout
import org.redisson.api.RMap
import org.redisson.client.codec.IntegerCodec
import org.redisson.client.codec.LongCodec
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class AsyncRedissonMemorizerTest: AbstractRedissonTest() {

    companion object: KLoggingChannel()

    private val heavyMap: RMap<Int, Int> by lazy {
        redisson.getMap<Int, Int>("asyncMemorizer:heavy", IntegerCodec()).apply { clear() }
    }

    val heavyFunc: (Int) -> CompletableFuture<Int> = heavyMap.asyncMemorizer { x ->
        CompletableFuture.supplyAsync {
            Thread.sleep(100)
            x * x
        }
    }

    private val factorial: AsyncFactorialProvider = object: AsyncFactorialProvider {
        override val cachedCalc: (Long) -> CompletableFuture<Long> = redisson
            .getMap<Long, Long>("asyncMemorizer:factorial", LongCodec())
            .asyncMemorizer { calc(it) }
    }

    private val fibonacci: AsyncFibonacciProvider = object: AsyncFibonacciProvider {
        override val cachedCalc: (Long) -> CompletableFuture<Long> = redisson
            .getMap<Long, Long>("asyncMemorizer:fibonacci", LongCodec())
            .asyncMemorizer { calc(it) }
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
    fun `async memorizer should evaluate once for same key in concurrent calls`() {
        val map = redisson.getMap<Int, Int>(randomName(), IntegerCodec()).apply { clear() }
        val evaluateCount = AtomicInteger(0)
        val memorizer = map.asyncMemorizer { key ->
            CompletableFuture.supplyAsync {
                evaluateCount.incrementAndGet()
                Thread.sleep(100)
                key * key
            }
        }

        try {
            val futures = List(16) { memorizer(7) }
            futures.forEach { it.get(2, TimeUnit.SECONDS) shouldBeEqualTo 49 }
            evaluateCount.get() shouldBeEqualTo 1
        } finally {
            map.delete()
        }
    }

    @Test
    fun `cached value bypasses evaluator`() {
        val map = redisson.getMap<Int, Int>(randomName(), IntegerCodec()).apply {
            clear()
            put(9, 81)
        }
        val evaluateCount = AtomicInteger(0)
        val memorizer = map.asyncMemorizer { key ->
            evaluateCount.incrementAndGet()
            CompletableFuture.completedFuture(key * key)
        }

        try {
            memorizer(9).get(2, TimeUnit.SECONDS) shouldBeEqualTo 81
            evaluateCount.get() shouldBeEqualTo 0
        } finally {
            map.delete()
        }
    }

    @Test
    fun `failed evaluation is removed from in-flight and next call re-evaluates`() {
        val map = redisson.getMap<Int, Int>(randomName(), IntegerCodec()).apply { clear() }
        val evaluateCount = AtomicInteger(0)
        val memorizer = map.asyncMemorizer { key ->
            when (evaluateCount.incrementAndGet()) {
                1 -> CompletableFuture.failedFuture(IllegalStateException("boom"))
                else -> CompletableFuture.completedFuture(key * key)
            }
        }

        try {
            org.junit.jupiter.api.assertThrows<ExecutionException> {
                memorizer(5).get(2, TimeUnit.SECONDS)
            }

            memorizer(5).get(2, TimeUnit.SECONDS) shouldBeEqualTo 25
            evaluateCount.get() shouldBeEqualTo 2
        } finally {
            map.delete()
        }
    }

    @Test
    fun `clear removes redis entries and resets local in-flight state`() {
        val map = redisson.getMap<Int, Int>(randomName(), IntegerCodec()).apply { clear() }
        val evaluateCount = AtomicInteger(0)
        val firstEvaluation = CompletableFuture<Int>()
        val secondEvaluation = CompletableFuture<Int>()
        val memorizer = map.asyncMemorizer { _ ->
            when (evaluateCount.incrementAndGet()) {
                1 -> firstEvaluation
                else -> secondEvaluation
            }
        }

        try {
            val first = memorizer(3)
            memorizer.clear()
            val second = memorizer(3)

            await.atMost(2.seconds.toJavaDuration()).until { evaluateCount.get() == 2 }

            firstEvaluation.complete(9)
            secondEvaluation.complete(9)

            first.get(2, TimeUnit.SECONDS) shouldBeEqualTo 9
            second.get(2, TimeUnit.SECONDS) shouldBeEqualTo 9
        } finally {
            map.delete()
        }
    }


    interface AsyncFactorialProvider {

        companion object: KLoggingChannel()

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

        companion object: KLoggingChannel()

        val cachedCalc: (Long) -> CompletableFuture<Long>

        fun calc(x: Long): CompletableFuture<Long> {
            log.trace { "factorial($x)" }
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
