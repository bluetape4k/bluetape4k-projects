package io.bluetape4k.ignite3.memorizer

import io.bluetape4k.ignite3.AbstractIgnite3Test
import io.bluetape4k.ignite3.cache.nearCacheManager
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.apache.ignite.table.KeyValueView
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertTimeout
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

/**
 * [AsyncIgniteMemorizer]의 비동기 캐싱 동작을 검증하는 통합 테스트입니다.
 *
 * Docker로 Ignite 3.x 서버를 실행하고 [KeyValueView]를 저장소로 사용하는
 * 비동기 Memorizer를 테스트합니다.
 * [io.bluetape4k.ignite3.cache.IgniteNearCacheManager]가 테이블을 자동으로 생성하므로
 * 별도의 DDL이 필요하지 않습니다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AsyncIgniteMemorizerTest: AbstractIgnite3Test() {

    companion object: KLogging() {
        private const val HEAVY_TABLE = "ASYNC_MEMORIZER_HEAVY"
        private const val FACTORIAL_TABLE = "ASYNC_MEMORIZER_FACTORIAL"
        private const val FIBONACCI_TABLE = "ASYNC_MEMORIZER_FIBONACCI"
    }

    private val manager by lazy { igniteClient.nearCacheManager() }

    private val heavyFunc: (Int) -> CompletableFuture<Int> by lazy {
        manager.keyValueView<Int, Int>(HEAVY_TABLE)
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
                manager.keyValueView<Long, Long>(FACTORIAL_TABLE).asyncMemorizer { calc(it) }
        }
    }

    private val fibonacci: AsyncFibonacciProvider by lazy {
        object: AsyncFibonacciProvider {
            override val cachedCalc: (Long) -> CompletableFuture<Long> =
                manager.keyValueView<Long, Long>(FIBONACCI_TABLE).asyncMemorizer { calc(it) }
        }
    }

    @Test
    fun `run heavy function`() {
        measureTimeMillis {
            heavyFunc(10).get() shouldBeEqualTo 100
        }

        assertTimeout(Duration.ofMillis(2000)) {
            heavyFunc(10).get() shouldBeEqualTo 100
        }
    }

    @Test
    fun `run factorial`() {
        val x1 = factorial.calc(100).get()

        assertTimeout(Duration.ofMillis(2000)) {
            factorial.calc(100).get()
        } shouldBeEqualTo x1
    }

    @Test
    fun `run fibonacci`() {
        val x1 = fibonacci.calc(100).get()

        assertTimeout(Duration.ofMillis(2000)) {
            fibonacci.calc(100).get()
        } shouldBeEqualTo x1
    }

    @Test
    fun `동일한 키 동시 호출 시 evaluator는 한 번만 실행`() {
        val tableName = "ASYNC_MEMORIZER_CONCURRENT"
        igniteClient.sql().execute(null, "DROP TABLE IF EXISTS $tableName").close()
        igniteClient.sql().execute(
            null,
            "CREATE TABLE $tableName (ID INTEGER PRIMARY KEY, DATA INTEGER)"
        ).close()

        val evaluateCount = AtomicInteger(0)
        val view = igniteClient.tables().table(tableName)!!
            .keyValueView(Int::class.javaObjectType, Int::class.javaObjectType)
        val memorizer = view.asyncMemorizer { key ->
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
     * Ignite 3.x [KeyValueView]를 활용한 비동기 Factorial 계산 인터페이스입니다.
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
     * Ignite 3.x [KeyValueView]를 활용한 비동기 Fibonacci 계산 인터페이스입니다.
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
