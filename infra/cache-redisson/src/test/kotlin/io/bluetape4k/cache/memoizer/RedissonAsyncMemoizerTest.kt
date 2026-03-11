package io.bluetape4k.cache.memoizer

import io.bluetape4k.cache.RedisServers.randomName
import io.bluetape4k.cache.RedisServers.redisson
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeLessThan
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
            assertThrows<ExecutionException> {
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

    /**
     * [MultithreadingTester]를 사용하여 여러 스레드에서 동시에 memoizer를 호출할 때
     * 중복 평가 없이 올바른 결과를 반환하는지 검증하는 동시성 테스트입니다.
     */
    @Test
    fun `multithreading - memoizer should not evaluate duplicately under concurrent access`() {
        val map = redisson.getMap<Int, Int>(randomName(), IntegerCodec()).apply { clear() }
        val evaluateCount = AtomicInteger(0)
        val memoizer = map.asyncMemoizer { key ->
            CompletableFuture.supplyAsync {
                evaluateCount.incrementAndGet()
                Thread.sleep(50)
                key * key
            }
        }

        try {
            MultithreadingTester()
                .workers(16)
                .rounds(4)
                .add {
                    memoizer(7).get(5, TimeUnit.SECONDS) shouldBeEqualTo 49
                }
                .run()

            // 16 스레드 * 4 라운드 = 64번 호출하더라도 평가 횟수는 훨씬 적어야 함
            evaluateCount.get() shouldBeLessThan 64
        } finally {
            map.delete()
        }
    }

    /**
     * [StructuredTaskScopeTester]를 사용하여 Virtual Thread 기반으로 memoizer를 동시에 호출할 때
     * 중복 평가 없이 올바른 결과를 반환하는지 검증하는 동시성 테스트입니다.
     */
    @Test
    fun `structured task scope - memoizer should not evaluate duplicately under concurrent access`() {
        val map = redisson.getMap<Int, Int>(randomName(), IntegerCodec()).apply { clear() }
        val evaluateCount = AtomicInteger(0)
        val memoizer = map.asyncMemoizer { key ->
            CompletableFuture.supplyAsync {
                evaluateCount.incrementAndGet()
                Thread.sleep(50)
                key * key
            }
        }

        try {
            StructuredTaskScopeTester()
                .rounds(32)
                .add {
                    memoizer(7).get(5, TimeUnit.SECONDS) shouldBeEqualTo 49
                }
                .run()

            // 32번 호출하더라도 평가 횟수는 훨씬 적어야 함
            evaluateCount.get() shouldBeLessThan 32
        } finally {
            map.delete()
        }
    }
}
