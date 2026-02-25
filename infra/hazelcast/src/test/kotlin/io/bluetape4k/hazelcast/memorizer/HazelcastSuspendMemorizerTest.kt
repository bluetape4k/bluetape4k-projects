package io.bluetape4k.hazelcast.memorizer

import io.bluetape4k.hazelcast.AbstractHazelcastTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

/**
 * [HazelcastSuspendMemorizer]의 코루틴 기반 캐싱 동작을 검증하는 통합 테스트입니다.
 *
 * Docker로 Hazelcast 서버를 실행하고 [HazelcastSuspendMemorizer]를 통해
 * suspend 함수의 결과 캐싱 동작을 검증합니다.
 */
class HazelcastSuspendMemorizerTest: AbstractHazelcastTest() {

    companion object: KLogging() {
        private const val HEAVY_MAP = "suspendMemorizer:hazelcast:heavy"
        private const val FACTORIAL_MAP = "suspendMemorizer:hazelcast:factorial"
        private const val FIBONACCI_MAP = "suspendMemorizer:hazelcast:fibonacci"
    }

    private val heavyFunc: HazelcastSuspendMemorizer<Int, Int> by lazy {
        hazelcastClient.getMap<Int, Int>(HEAVY_MAP)
            .apply { clear() }
            .suspendMemorizer { x ->
                delay(100)
                x * x
            }
    }

    private val factorial: SuspendFactorialProvider by lazy {
        object: SuspendFactorialProvider {
            override val cachedCalc: suspend (Long) -> Long =
                hazelcastClient.getMap<Long, Long>(FACTORIAL_MAP)
                    .suspendMemorizer { calc(it) }
        }
    }

    private val fibonacci: SuspendFibonacciProvider by lazy {
        object: SuspendFibonacciProvider {
            override val cachedCalc: suspend (Long) -> Long =
                hazelcastClient.getMap<Long, Long>(FIBONACCI_MAP)
                    .suspendMemorizer { calc(it) }
        }
    }

    @Test
    fun `run heavy function`() = runTest(timeout = 30.seconds) {
        heavyFunc(10) shouldBeEqualTo 100

        // 캐시에서 조회 - delay 없이 빠르게 반환
        val start = System.currentTimeMillis()
        heavyFunc(10) shouldBeEqualTo 100
        val elapsed = System.currentTimeMillis() - start
        assert(elapsed < 1000) { "캐시 조회가 1초를 초과했습니다: ${elapsed}ms" }
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
     * Hazelcast 분산 캐시를 활용한 suspend Factorial 계산 인터페이스입니다.
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
     * Hazelcast 분산 캐시를 활용한 suspend Fibonacci 계산 인터페이스입니다.
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
