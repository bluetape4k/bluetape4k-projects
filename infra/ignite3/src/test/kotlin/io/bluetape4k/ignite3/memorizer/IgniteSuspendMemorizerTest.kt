package io.bluetape4k.ignite3.memorizer

import io.bluetape4k.ignite3.AbstractIgnite3Test
import io.bluetape4k.ignite3.cache.nearCacheManager
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.apache.ignite.table.KeyValueView
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.time.Duration.Companion.seconds

/**
 * [IgniteSuspendMemorizer]의 코루틴 기반 캐싱 동작을 검증하는 통합 테스트입니다.
 *
 * Docker로 Ignite 3.x 서버를 실행하고 [KeyValueView]를 저장소로 사용하는
 * suspend Memorizer를 테스트합니다.
 * [io.bluetape4k.ignite3.cache.IgniteNearCacheManager]가 테이블을 자동으로 생성하므로
 * 별도의 DDL이 필요하지 않습니다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IgniteSuspendMemorizerTest: AbstractIgnite3Test() {

    companion object: KLogging() {
        private const val HEAVY_TABLE = "SUSPEND_MEMORIZER_HEAVY"
        private const val FACTORIAL_TABLE = "SUSPEND_MEMORIZER_FACTORIAL"
        private const val FIBONACCI_TABLE = "SUSPEND_MEMORIZER_FIBONACCI"
    }

    private val manager by lazy { igniteClient.nearCacheManager() }

    private val heavyFunc: IgniteSuspendMemorizer<Int, Int> by lazy {
        manager.keyValueView<Int, Int>(HEAVY_TABLE)
            .suspendMemorizer { x ->
                delay(100)
                x * x
            }
    }

    private val factorial: SuspendFactorialProvider by lazy {
        object: SuspendFactorialProvider {
            override val cachedCalc: suspend (Long) -> Long =
                manager.keyValueView<Long, Long>(FACTORIAL_TABLE).suspendMemorizer { calc(it) }
        }
    }

    private val fibonacci: SuspendFibonacciProvider by lazy {
        object: SuspendFibonacciProvider {
            override val cachedCalc: suspend (Long) -> Long =
                manager.keyValueView<Long, Long>(FIBONACCI_TABLE).suspendMemorizer { calc(it) }
        }
    }

    @Test
    fun `run heavy function`() = runTest(timeout = 30.seconds) {
        heavyFunc(10) shouldBeEqualTo 100

        val start = System.currentTimeMillis()
        heavyFunc(10) shouldBeEqualTo 100
        val elapsed = System.currentTimeMillis() - start
        assert(elapsed < 2000) { "캐시 조회가 2초를 초과했습니다: ${elapsed}ms" }
    }

    @Test
    fun `run factorial`() = runTest(timeout = 30.seconds) {
        val x1 = factorial.calc(100)

        val start = System.currentTimeMillis()
        factorial.calc(100) shouldBeEqualTo x1
        val elapsed = System.currentTimeMillis() - start
        assert(elapsed < 2000) { "캐시 조회가 2초를 초과했습니다: ${elapsed}ms" }
    }

    @Test
    fun `run fibonacci`() = runTest(timeout = 30.seconds) {
        val x1 = fibonacci.calc(100)

        val start = System.currentTimeMillis()
        fibonacci.calc(100) shouldBeEqualTo x1
        val elapsed = System.currentTimeMillis() - start
        assert(elapsed < 2000) { "캐시 조회가 2초를 초과했습니다: ${elapsed}ms" }
    }

    /**
     * Ignite 3.x [KeyValueView]를 활용한 suspend Factorial 계산 인터페이스입니다.
     */
    interface SuspendFactorialProvider {

        companion object: KLogging()

        val cachedCalc: suspend (Long) -> Long

        suspend fun calc(n: Long): Long {
            log.trace { "factorial($n)" }
            return when {
                n <= 1L -> 1L
                else    -> n * cachedCalc(n - 1)
            }
        }
    }

    /**
     * Ignite 3.x [KeyValueView]를 활용한 suspend Fibonacci 계산 인터페이스입니다.
     */
    interface SuspendFibonacciProvider {

        companion object: KLogging()

        val cachedCalc: suspend (Long) -> Long

        suspend fun calc(n: Long): Long {
            log.trace { "fibonacci($n)" }
            return when {
                n <= 0L -> 0L
                n <= 2L -> 1L
                else    -> cachedCalc(n - 1) + cachedCalc(n - 2)
            }
        }
    }
}
