package io.bluetape4k.ignite.memorizer

import io.bluetape4k.ignite.igniteEmbedded
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.utils.ShutdownQueue
import org.amshove.kluent.shouldBeEqualTo
import org.apache.ignite.Ignite
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertTimeout
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

/**
 * [AsyncIgniteMemorizer]의 비동기 캐싱 동작을 검증하는 단위 테스트입니다.
 *
 * 임베디드 Ignite 노드를 사용하여 Docker 서버 없이 테스트합니다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AsyncIgniteMemorizerTest {

    companion object: KLogging() {
        private const val HEAVY_CACHE = "asyncMemorizer:ignite:heavy"
        private const val FACTORIAL_CACHE = "asyncMemorizer:ignite:factorial"
        private const val FIBONACCI_CACHE = "asyncMemorizer:ignite:fibonacci"

        /** 임베디드 Ignite 노드 (테스트 JVM 내 실행) */
        val ignite: Ignite by lazy {
            igniteEmbedded {
                igniteInstanceName = "bt4k-async-memorizer-test"
            }.also { ShutdownQueue.register { it.close() } }
        }
    }

    private val heavyFunc: (Int) -> CompletableFuture<Int> by lazy {
        ignite.getOrCreateCache<Int, Int>(HEAVY_CACHE)
            .apply { clear() }
            .asyncMemorizer { x ->
                CompletableFuture.supplyAsync {
                    Thread.sleep(100)
                    x * x
                }
            }
    }

    private val factorial: AsyncFactorialProvider by lazy {
        val factCache = ignite.getOrCreateCache<Long, Long>(FACTORIAL_CACHE)
        object: AsyncFactorialProvider {
            override val cachedCalc: (Long) -> CompletableFuture<Long> =
                factCache.asyncMemorizer { calc(it) }
        }
    }

    private val fibonacci: AsyncFibonacciProvider by lazy {
        val fibCache = ignite.getOrCreateCache<Long, Long>(FIBONACCI_CACHE)
        object: AsyncFibonacciProvider {
            override val cachedCalc: (Long) -> CompletableFuture<Long> =
                fibCache.asyncMemorizer { calc(it) }
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
        val cache = ignite.getOrCreateCache<Int, Int>("asyncMemorizer:ignite:concurrent").apply { clear() }
        val memorizer = cache.asyncMemorizer { key ->
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
     * Ignite 2.x 분산 캐시를 활용한 비동기 Factorial 계산 인터페이스입니다.
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
     * Ignite 2.x 분산 캐시를 활용한 비동기 Fibonacci 계산 인터페이스입니다.
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
