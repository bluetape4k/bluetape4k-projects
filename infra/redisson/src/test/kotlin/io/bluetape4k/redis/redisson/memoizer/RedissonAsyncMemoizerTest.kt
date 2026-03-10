package io.bluetape4k.redis.redisson.memoizer

import io.bluetape4k.cache.memoizer.AbstractAsyncMemoizerTest
import io.bluetape4k.cache.memoizer.AsyncFactorialProvider
import io.bluetape4k.cache.memoizer.AsyncFibonacciProvider
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.redisson.RedissonTestUtils.randomName
import io.bluetape4k.redis.redisson.RedissonTestUtils.redisson
import org.amshove.kluent.shouldBeEqualTo
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.redisson.api.RMap
import org.redisson.client.codec.IntegerCodec
import org.redisson.client.codec.LongCodec
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class RedissonAsyncMemoizerTest: AbstractAsyncMemoizerTest() {

    companion object: KLoggingChannel()

    private val heavyMap: RMap<Int, Int> by lazy {
        redisson.getMap<Int, Int>("asyncMemoizer:heavy", IntegerCodec()).apply { clear() }
    }

    override val heavyFunc: (Int) -> CompletableFuture<Int> = heavyMap.asyncMemoizer { x ->
        CompletableFuture.supplyAsync {
            Thread.sleep(100)
            x * x
        }
    }

    override val factorial: AsyncFactorialProvider = object: AsyncFactorialProvider {
        override val cachedCalc: (Long) -> CompletableFuture<Long> = redisson
            .getMap<Long, Long>("asyncMemoizer:factorial", LongCodec())
            .asyncMemoizer { calc(it) }
    }

    override val fibonacci: AsyncFibonacciProvider = object: AsyncFibonacciProvider {
        override val cachedCalc: (Long) -> CompletableFuture<Long> = redisson
            .getMap<Long, Long>("asyncMemoizer:fibonacci", LongCodec())
            .asyncMemoizer { calc(it) }
    }

    @Test
    fun `async memoizer should evaluate once for same key in concurrent calls`() {
        val map = redisson.getMap<Int, Int>(randomName(), IntegerCodec()).apply { clear() }
        val evaluateCount = AtomicInteger(0)
        val memoizer = map.asyncMemoizer { key ->
            CompletableFuture.supplyAsync {
                evaluateCount.incrementAndGet()
                Thread.sleep(100)
                key * key
            }
        }

        try {
            val futures = List(16) { memoizer(7) }
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
        val memoizer = map.asyncMemoizer { key ->
            evaluateCount.incrementAndGet()
            CompletableFuture.completedFuture(key * key)
        }

        try {
            memoizer(9).get(2, TimeUnit.SECONDS) shouldBeEqualTo 81
            evaluateCount.get() shouldBeEqualTo 0
        } finally {
            map.delete()
        }
    }

    @Test
    fun `failed evaluation is removed from in-flight and next call re-evaluates`() {
        val map = redisson.getMap<Int, Int>(randomName(), IntegerCodec()).apply { clear() }
        val evaluateCount = AtomicInteger(0)
        val memoizer = map.asyncMemoizer { key ->
            when (evaluateCount.incrementAndGet()) {
                1 -> CompletableFuture.failedFuture(IllegalStateException("boom"))
                else -> CompletableFuture.completedFuture(key * key)
            }
        }

        try {
            org.junit.jupiter.api.assertThrows<ExecutionException> {
                memoizer(5).get(2, TimeUnit.SECONDS)
            }

            memoizer(5).get(2, TimeUnit.SECONDS) shouldBeEqualTo 25
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
        val memoizer = map.asyncMemoizer { _ ->
            when (evaluateCount.incrementAndGet()) {
                1 -> firstEvaluation
                else -> secondEvaluation
            }
        }

        try {
            val first = memoizer(3)
            memoizer.clear()
            val second = memoizer(3)

            await.atMost(2.seconds.toJavaDuration()).until { evaluateCount.get() == 2 }

            firstEvaluation.complete(9)
            secondEvaluation.complete(9)

            first.get(2, TimeUnit.SECONDS) shouldBeEqualTo 9
            second.get(2, TimeUnit.SECONDS) shouldBeEqualTo 9
        } finally {
            map.delete()
        }
    }
}
