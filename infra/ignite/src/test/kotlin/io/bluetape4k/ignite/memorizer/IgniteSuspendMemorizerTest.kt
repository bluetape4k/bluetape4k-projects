package io.bluetape4k.ignite.memorizer

import io.bluetape4k.ignite.igniteEmbedded
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.utils.ShutdownQueue
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.apache.ignite.Ignite
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.time.Duration.Companion.seconds

/**
 * [IgniteSuspendMemorizer]의 코루틴 기반 캐싱 동작을 검증하는 단위 테스트입니다.
 *
 * 임베디드 Ignite 노드를 사용하여 Docker 서버 없이 테스트합니다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IgniteSuspendMemorizerTest {

    companion object: KLogging() {
        private const val HEAVY_CACHE = "suspendMemorizer:ignite:heavy"
        private const val FACTORIAL_CACHE = "suspendMemorizer:ignite:factorial"
        private const val FIBONACCI_CACHE = "suspendMemorizer:ignite:fibonacci"

        /** 임베디드 Ignite 노드 (테스트 JVM 내 실행) */
        val ignite: Ignite by lazy {
            igniteEmbedded {
                igniteInstanceName = "bt4k-suspend-memorizer-test"
            }.also { ShutdownQueue.register { it.close() } }
        }
    }

    private val heavyFunc: IgniteSuspendMemorizer<Int, Int> by lazy {
        ignite.getOrCreateCache<Int, Int>(HEAVY_CACHE)
            .apply { clear() }
            .suspendMemorizer { x ->
                delay(100)
                x * x
            }
    }

    private val factorial: SuspendFactorialProvider by lazy {
        val factCache = ignite.getOrCreateCache<Long, Long>(FACTORIAL_CACHE)
        object: SuspendFactorialProvider {
            override val cachedCalc: suspend (Long) -> Long = factCache.suspendMemorizer { calc(it) }
        }
    }

    private val fibonacci: SuspendFibonacciProvider by lazy {
        val fibCache = ignite.getOrCreateCache<Long, Long>(FIBONACCI_CACHE)
        object: SuspendFibonacciProvider {
            override val cachedCalc: suspend (Long) -> Long = fibCache.suspendMemorizer { calc(it) }
        }
    }

    @Test
    fun `run heavy function`() = runTest(timeout = 30.seconds) {
        heavyFunc(10) shouldBeEqualTo 100

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
     * Ignite 2.x 분산 캐시를 활용한 suspend Factorial 계산 인터페이스입니다.
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
     * Ignite 2.x 분산 캐시를 활용한 suspend Fibonacci 계산 인터페이스입니다.
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
