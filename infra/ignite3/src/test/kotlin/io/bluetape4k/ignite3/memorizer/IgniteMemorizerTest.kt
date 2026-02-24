package io.bluetape4k.ignite3.memorizer

import io.bluetape4k.ignite3.AbstractIgnite3Test
import io.bluetape4k.ignite3.cache.nearCacheManager
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.apache.ignite.table.KeyValueView
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertTimeout
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

/**
 * [IgniteMemorizer]의 캐싱 동작을 검증하는 통합 테스트입니다.
 *
 * Docker로 Ignite 3.x 서버를 실행하고 [KeyValueView]를 저장소로 사용하는 Memorizer를 테스트합니다.
 * [io.bluetape4k.ignite3.cache.IgniteNearCacheManager]가 테이블을 자동으로 생성하므로
 * 별도의 DDL이 필요하지 않습니다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IgniteMemorizerTest: AbstractIgnite3Test() {

    companion object: KLogging() {
        private const val TABLE_NAME = "MEMORIZER_TABLE"
        private const val HEAVY_TABLE = "MEMORIZER_HEAVY"
        private const val FACTORIAL_TABLE = "MEMORIZER_FACTORIAL"
        private const val FIBONACCI_TABLE = "MEMORIZER_FIBONACCI"
    }

    private val manager by lazy { igniteClient.nearCacheManager() }

    private val callCount = AtomicInteger(0)
    private lateinit var memorizer: IgniteMemorizer<Long, Long>

    @BeforeEach
    fun setup() {
        // nearCacheManager가 테이블을 자동 생성하므로 @BeforeAll DDL 불필요
        val view = manager.keyValueView<Long, Long>(TABLE_NAME)
        igniteClient.sql().execute(null, "DELETE FROM $TABLE_NAME").close()
        callCount.set(0)
        memorizer = view.memorizer { key ->
            callCount.incrementAndGet()
            key * key
        }
    }

    @Test
    fun `첫 번째 호출 시 evaluator를 실행하고 결과를 저장`() {
        val result = memorizer(4L)

        result shouldBeEqualTo 16L
        callCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `동일한 키 재호출 시 저장된 값을 반환하고 evaluator를 다시 실행하지 않음`() {
        memorizer(5L)
        val result = memorizer(5L)

        result shouldBeEqualTo 25L
        callCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `서로 다른 키는 각각 evaluator를 실행`() {
        memorizer(3L)
        memorizer(4L)
        memorizer(5L)

        callCount.get() shouldBeEqualTo 3
    }

    @Test
    fun `memorizer 결과값이 올바른 계산 결과임을 검증`() {
        val result = memorizer(7L)

        result shouldBeEqualTo 49L
        callCount.get() shouldBeEqualTo 1
    }

    @Test
    fun `여러 키에 대한 결과를 순차적으로 캐싱`() {
        val keys = listOf(1L, 2L, 3L, 4L, 5L)
        val results = keys.map { memorizer(it) }

        results[0] shouldBeEqualTo 1L
        results[1] shouldBeEqualTo 4L
        results[2] shouldBeEqualTo 9L
        results[3] shouldBeEqualTo 16L
        results[4] shouldBeEqualTo 25L
        callCount.get() shouldBeEqualTo 5

        // 동일한 키 재호출 시 evaluator를 다시 실행하지 않음
        keys.map { memorizer(it) }
        callCount.get() shouldBeEqualTo 5
    }

    // --- Heavy / Factorial / Fibonacci ---

    private val heavyFunc: (Int) -> Int by lazy {
        manager.keyValueView<Int, Int>(HEAVY_TABLE)
            .memorizer { x ->
                Thread.sleep(100)
                x * x
            }
    }

    private val factorial: FactorialProvider by lazy {
        object: FactorialProvider {
            override val cachedCalc: (Long) -> Long =
                manager.keyValueView<Long, Long>(FACTORIAL_TABLE).memorizer { calc(it) }
        }
    }

    private val fibonacci: FibonacciProvider by lazy {
        object: FibonacciProvider {
            override val cachedCalc: (Long) -> Long =
                manager.keyValueView<Long, Long>(FIBONACCI_TABLE).memorizer { calc(it) }
        }
    }

    @Test
    fun `run heavy function`() {
        measureTimeMillis {
            heavyFunc(10) shouldBeEqualTo 100
        }

        assertTimeout(Duration.ofMillis(2000)) {
            heavyFunc(10) shouldBeEqualTo 100
        }
    }

    @Test
    fun `run factorial`() {
        val x1 = factorial.calc(100)

        assertTimeout(Duration.ofMillis(2000)) {
            factorial.calc(100)
        } shouldBeEqualTo x1
    }

    @Test
    fun `run fibonacci`() {
        val x1 = fibonacci.calc(100)

        assertTimeout(Duration.ofMillis(2000)) {
            fibonacci.calc(100)
        } shouldBeEqualTo x1
    }

    /**
     * Ignite 3.x [KeyValueView]를 활용한 Factorial 계산 인터페이스입니다.
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
     * Ignite 3.x [KeyValueView]를 활용한 Fibonacci 계산 인터페이스입니다.
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
