package io.bluetape4k.hazelcast.memorizer

import io.bluetape4k.hazelcast.AbstractHazelcastTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertTimeout
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

/**
 * [AsyncHazelcastMemorizer]의 비동기 캐싱 동작을 검증하는 통합 테스트입니다.
 *
 * Docker로 Hazelcast 서버를 실행하고 [AsyncHazelcastMemorizer]를 통해
 * CompletableFuture 기반 비동기 호출의 캐싱 동작을 검증합니다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AsyncHazelcastMemorizerTest: AbstractHazelcastTest() {

    companion object: KLogging() {
        private const val HEAVY_MAP = "asyncMemorizer:hazelcast:heavy"
        private const val FACTORIAL_MAP = "asyncMemorizer:hazelcast:factorial"
        private const val FIBONACCI_MAP = "asyncMemorizer:hazelcast:fibonacci"
    }

    private val heavyFunc: (Int) -> CompletableFuture<Int> by lazy {
        hazelcastClient.getMap<Int, Int>(HEAVY_MAP)
            .apply { clear() }
            .asyncMemorizer { x ->
                CompletableFuture.supplyAsync {
                    Thread.sleep(100)
                    x * x
                }
            }
    }

    private val factorial: AsyncFactorialProvider by lazy {
        object: AsyncFactorialProvider {
            override val cachedCalc: (Long) -> CompletableFuture<Long> =
                hazelcastClient.getMap<Long, Long>(FACTORIAL_MAP)
                    .asyncMemorizer { calc(it) }
        }
    }

    private val fibonacci: AsyncFibonacciProvider by lazy {
        object: AsyncFibonacciProvider {
            override val cachedCalc: (Long) -> CompletableFuture<Long> =
                hazelcastClient.getMap<Long, Long>(FIBONACCI_MAP)
                    .asyncMemorizer { calc(it) }
        }
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
    fun `동일한 키 동시 호출 시 evaluator는 한 번만 실행`() {
        val evaluateCount = AtomicInteger(0)
        val map = hazelcastClient.getMap<Int, Int>("asyncMemorizer:hazelcast:concurrent").apply { clear() }
        val memorizer = map.asyncMemorizer { key ->
            CompletableFuture.supplyAsync {
                evaluateCount.incrementAndGet()
                Thread.sleep(50)
                key * key
            }
        }

        val futures = List(16) { memorizer(7) }
        futures.forEach { it.get(2, TimeUnit.SECONDS) shouldBeEqualTo 49 }
        evaluateCount.get() shouldBeEqualTo 1
    }

    /**
     * 분산 캐시를 활용한 비동기 Factorial 계산 인터페이스입니다.
     */
    interface AsyncFactorialProvider {

        companion object: KLogging()

        val cachedCalc: (Long) -> CompletableFuture<Long>

        fun calc(n: Long): CompletableFuture<Long> {
            log.trace { "factorial($n)" }
            return when {
                n <= 1L -> CompletableFuture.completedFuture(1L)
                else    -> cachedCalc(n - 1).thenApplyAsync { n * it }
            }
        }
    }

    /**
     * 분산 캐시를 활용한 비동기 Fibonacci 계산 인터페이스입니다.
     */
    interface AsyncFibonacciProvider {

        companion object: KLogging()

        val cachedCalc: (Long) -> CompletableFuture<Long>

        fun calc(n: Long): CompletableFuture<Long> {
            log.trace { "fibonacci($n)" }
            return when {
                n <= 0L -> CompletableFuture.completedFuture(0L)
                n <= 2L -> CompletableFuture.completedFuture(1L)
                else    -> cachedCalc(n - 1).thenComposeAsync { x1 ->
                    cachedCalc(n - 2).thenApplyAsync { x2 -> x1 + x2 }
                }
            }
        }
    }
}
