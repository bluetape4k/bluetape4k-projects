package io.bluetape4k.cache.memoizer

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.system.measureTimeMillis

/**
 * Memoizer tests intentionally exercise synchronous work behind a real thread boundary.
 * The dedicated worker makes the timeout deterministic without changing the memoizer API.
 */
internal fun <T> withBlockingTimeout(timeout: Duration, block: () -> T): T {
    val watchdog = Executors.newSingleThreadExecutor()
    val task = watchdog.submit<T> { block() }
    return try {
        task.get(timeout.toMillis(), TimeUnit.MILLISECONDS)
    } catch (e: ExecutionException) {
        throw (e.cause ?: e)
    } catch (e: TimeoutException) {
        task.cancel(true)
        throw AssertionError("Blocking memoizer test exceeded $timeout", e)
    } finally {
        watchdog.shutdownNow()
    }
}

abstract class AbstractMemoizerTest {

    companion object: KLogging()

    protected abstract val factorial: FactorialProvider
    protected abstract val fibonacci: FibonacciProvider

    protected abstract val heavyFunc: (Int) -> Int

    @Test
    fun `run heavy function`() {
        measureTimeMillis {
            heavyFunc(10) shouldBeEqualTo 100
        }

        withBlockingTimeout(Duration.ofMillis(1000)) {
            heavyFunc(10) shouldBeEqualTo 100
        }
    }

    @Test
    fun `run factorial`() {
        val x1 = factorial.calc(500)

        withBlockingTimeout(Duration.ofMillis(1000)) {
            factorial.calc(500)
        } shouldBeEqualTo x1
    }

    @Test
    fun `run fibonacci`() {
        val x1 = fibonacci.calc(500)

        withBlockingTimeout(Duration.ofMillis(1000)) {
            fibonacci.calc(500)
        } shouldBeEqualTo x1
    }

    /**
     * 멀티스레드 환경에서 memoizer가 동일한 키에 대해 올바른 결과를 반환하는지 검증합니다.
     * [MultithreadingTester]를 사용하여 동시 호출 시 결과 일관성을 확인합니다.
     */
    @Test
    fun `memoizer는 멀티스레드 환경에서 동일한 결과를 반환해야 한다`() {
        val expected = factorial.calc(100)

        MultithreadingTester()
            .workers(16)
            .rounds(4)
            .add {
                factorial.calc(100) shouldBeEqualTo expected
            }
            .run()
    }

    /**
     * 멀티스레드 환경에서 fibonacci memoizer의 결과 일관성을 검증합니다.
     * [MultithreadingTester]를 사용하여 동시 호출 시 결과 일관성을 확인합니다.
     */
    @Test
    fun `fibonacci memoizer는 멀티스레드 환경에서 동일한 결과를 반환해야 한다`() {
        val expected = fibonacci.calc(100)

        MultithreadingTester()
            .workers(16)
            .rounds(4)
            .add {
                fibonacci.calc(100) shouldBeEqualTo expected
            }
            .run()
    }

    /**
     * Virtual Thread 기반 [StructuredTaskScopeTester]를 사용하여
     * memoizer의 동시 호출 시 결과 일관성을 검증합니다.
     */
    @Test
    fun `memoizer는 Virtual Thread 환경에서 동일한 결과를 반환해야 한다`() {
        val expected = factorial.calc(100)

        StructuredTaskScopeTester()
            .rounds(32)
            .add {
                factorial.calc(100) shouldBeEqualTo expected
            }
            .run()
    }

    /**
     * Virtual Thread 기반 [StructuredTaskScopeTester]를 사용하여
     * fibonacci memoizer의 동시 호출 시 결과 일관성을 검증합니다.
     */
    @Test
    fun `fibonacci memoizer는 Virtual Thread 환경에서 동일한 결과를 반환해야 한다`() {
        val expected = fibonacci.calc(100)

        StructuredTaskScopeTester()
            .rounds(32)
            .add {
                fibonacci.calc(100) shouldBeEqualTo expected
            }
            .run()
    }
}
