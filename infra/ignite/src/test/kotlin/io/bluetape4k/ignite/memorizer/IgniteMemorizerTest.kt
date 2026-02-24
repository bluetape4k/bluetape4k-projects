package io.bluetape4k.ignite.memorizer

import io.bluetape4k.ignite.igniteEmbedded
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.utils.ShutdownQueue
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.apache.ignite.Ignite
import org.apache.ignite.IgniteCache
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertTimeout
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

/**
 * [IgniteMemorizer]의 캐시 저장 및 재사용 동작을 검증하는 단위 테스트입니다.
 *
 * 임베디드 Ignite 노드를 사용하여 Docker 서버 없이 테스트합니다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IgniteMemorizerTest {

    companion object: KLogging() {
        private const val CACHE_NAME = "memorizer-test-cache"
        private const val HEAVY_CACHE = "memorizer:heavy"
        private const val FACTORIAL_CACHE = "memorizer:factorial"
        private const val FIBONACCI_CACHE = "memorizer:fibonacci"

        /** 임베디드 Ignite 노드 (테스트 JVM 내 실행) */
        val ignite: Ignite by lazy {
            igniteEmbedded {
                igniteInstanceName = "bt4k-memorizer-test"
            }.also { ShutdownQueue.register { it.close() } }
        }
    }

    private lateinit var cache: IgniteCache<Int, Int>
    private lateinit var memorizer: IgniteMemorizer<Int, Int>
    private val callCount = AtomicInteger(0)

    @BeforeEach
    fun setup() {
        cache = ignite.getOrCreateCache(CACHE_NAME)
        cache.clear()
        callCount.set(0)
        memorizer = cache.memorizer { key ->
            callCount.incrementAndGet()
            key * key
        }
    }

    @Test
    fun `첫 번째 호출 시 evaluator를 실행하고 결과를 캐시에 저장`() {
        val result = memorizer(5)

        result shouldBeEqualTo 25
        callCount.get() shouldBeEqualTo 1
        cache.get(5) shouldBeEqualTo 25
    }

    @Test
    fun `동일한 키 재호출 시 캐시에서 조회하고 evaluator를 실행하지 않음`() {
        memorizer(3)
        memorizer(3)
        memorizer(3)

        callCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `서로 다른 키는 각각 evaluator를 실행`() {
        memorizer(1)
        memorizer(2)
        memorizer(3)

        callCount.get() shouldBeEqualTo 3
    }

    @Test
    fun `clear 후 재호출 시 evaluator를 다시 실행`() {
        memorizer(7)
        callCount.get() shouldBeEqualTo 1

        memorizer.clear()
        memorizer(7)
        callCount.get() shouldBeEqualTo 2
    }

    @Test
    fun `memorizer 결과값이 올바른 계산 결과임을 검증`() {
        val testKeys = listOf(1, 2, 3, 4, 5, 10)
        testKeys.forEach { key ->
            val result = memorizer(key)
            result.shouldNotBeNull()
            result shouldBeEqualTo key * key
        }
    }

    // --- Heavy / Factorial / Fibonacci ---

    private val heavyFunc: (Int) -> Int by lazy {
        ignite.getOrCreateCache<Int, Int>(HEAVY_CACHE)
            .apply { clear() }
            .memorizer { x ->
                Thread.sleep(100)
                x * x
            }
    }

    private val factorial: FactorialProvider by lazy {
        val factCache = ignite.getOrCreateCache<Long, Long>(FACTORIAL_CACHE)
        object: FactorialProvider {
            override val cachedCalc: (Long) -> Long = factCache.memorizer { calc(it) }
        }
    }

    private val fibonacci: FibonacciProvider by lazy {
        val fibCache = ignite.getOrCreateCache<Long, Long>(FIBONACCI_CACHE)
        object: FibonacciProvider {
            override val cachedCalc: (Long) -> Long = fibCache.memorizer { calc(it) }
        }
    }

    @Test
    fun `run heavy function`() {
        measureTimeMillis {
            heavyFunc(10) shouldBeEqualTo 100
        }

        assertTimeout(Duration.ofMillis(1000)) {
            heavyFunc(10) shouldBeEqualTo 100
        }
    }

    @Test
    fun `run factorial`() {
        val x1 = factorial.calc(100)

        assertTimeout(Duration.ofMillis(1000)) {
            factorial.calc(100)
        } shouldBeEqualTo x1
    }

    @Test
    fun `run fibonacci`() {
        val x1 = fibonacci.calc(100)

        assertTimeout(Duration.ofMillis(1000)) {
            fibonacci.calc(100)
        } shouldBeEqualTo x1
    }

    /**
     * 분산 캐시를 활용한 Factorial 계산 인터페이스입니다.
     *
     * [cachedCalc]을 통해 중간 결과를 캐싱하여 재귀 계산을 최적화합니다.
     */
    interface FactorialProvider {

        companion object: KLogging()

        val cachedCalc: (Long) -> Long

        fun calc(n: Long): Long {
            log.trace { "factorial($n)" }
            return when {
                n <= 1L -> 1L
                else -> n * cachedCalc(n - 1)
            }
        }
    }

    /**
     * 분산 캐시를 활용한 Fibonacci 계산 인터페이스입니다.
     *
     * [cachedCalc]을 통해 중간 결과를 캐싱하여 재귀 계산을 최적화합니다.
     */
    interface FibonacciProvider {

        companion object: KLogging()

        val cachedCalc: (Long) -> Long

        fun calc(n: Long): Long {
            log.trace { "fibonacci($n)" }
            return when {
                n <= 0L -> 0L
                n <= 2L -> 1L
                else -> cachedCalc(n - 1) + cachedCalc(n - 2)
            }
        }
    }
}
