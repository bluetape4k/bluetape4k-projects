package io.bluetape4k.hazelcast.memorizer

import com.hazelcast.client.config.ClientConfig
import io.bluetape4k.hazelcast.AbstractHazelcastTest
import io.bluetape4k.hazelcast.cache.HazelcastNearCacheConfig
import io.bluetape4k.hazelcast.hazelcastClient
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeout
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

/**
 * [HazelcastMemorizer]의 캐싱 동작을 검증하는 통합 테스트입니다.
 *
 * Docker로 Hazelcast 서버를 실행하고 [HazelcastMemorizer]를 통해
 * evaluator 호출 횟수와 캐시 동작을 검증합니다.
 */
class HazelcastMemorizerTest: AbstractHazelcastTest() {

    companion object: KLogging() {
        private const val MAP_NAME = "test-memorizer-map"
        private const val HEAVY_MAP = "memorizer:heavy"
        private const val FACTORIAL_MAP = "memorizer:factorial"
        private const val FIBONACCI_MAP = "memorizer:fibonacci"
    }

    private lateinit var memorizer: HazelcastMemorizer<Int, Int>
    private lateinit var callCount: AtomicInteger

    @BeforeEach
    fun setup() {
        callCount = AtomicInteger(0)

        val nearCacheConfig = HazelcastNearCacheConfig(cacheName = MAP_NAME)
        val clientConfig = ClientConfig().apply {
            networkConfig.addAddress(hazelcastServer.url)
            addNearCacheConfig(nearCacheConfig.toNearCacheConfig())
        }
        val client = hazelcastClient(clientConfig)
        val map = client.getMap<Int, Int>(MAP_NAME)
        map.clear()

        memorizer = map.memorizer { key ->
            callCount.incrementAndGet()
            key * key
        }
    }

    @Test
    fun `첫 번째 호출 시 evaluator를 실행하고 결과를 캐시에 저장`() {
        val result = memorizer(4)

        result shouldBeEqualTo 16
        callCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `동일한 키 재호출 시 캐시에서 조회하고 evaluator를 실행하지 않음`() {
        memorizer(5)
        val result = memorizer(5)

        result shouldBeEqualTo 25
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
    fun `여러 키에 대한 결과가 올바르게 캐싱됨`() {
        val keys = listOf(2, 3, 4, 5, 6)

        keys.forEach { key ->
            memorizer(key) shouldBeEqualTo key * key
        }

        // 캐시에서 재조회 시 evaluator가 추가로 호출되지 않음
        keys.forEach { key ->
            memorizer(key) shouldBeEqualTo key * key
        }

        callCount.get() shouldBeEqualTo keys.size
    }

    // --- Heavy / Factorial / Fibonacci ---

    private val heavyFunc: (Int) -> Int by lazy {
        hazelcastClient.getMap<Int, Int>(HEAVY_MAP)
            .apply { clear() }
            .memorizer { x ->
                Thread.sleep(100)
                x * x
            }
    }

    private val factorial: FactorialProvider by lazy {
        object: FactorialProvider {
            override val cachedCalc: (Long) -> Long = hazelcastClient
                .getMap<Long, Long>(FACTORIAL_MAP)
                .memorizer { calc(it) }
        }
    }

    private val fibonacci: FibonacciProvider by lazy {
        object: FibonacciProvider {
            override val cachedCalc: (Long) -> Long = hazelcastClient
                .getMap<Long, Long>(FIBONACCI_MAP)
                .memorizer { calc(it) }
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
