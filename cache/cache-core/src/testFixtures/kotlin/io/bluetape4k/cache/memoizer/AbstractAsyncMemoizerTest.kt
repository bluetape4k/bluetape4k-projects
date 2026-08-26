package io.bluetape4k.cache.memoizer

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

abstract class AbstractAsyncMemoizerTest {

    companion object: KLoggingChannel()

    protected abstract val factorial: AsyncFactorialProvider
    protected abstract val fibonacci: AsyncFibonacciProvider

    protected abstract val heavyFunc: (Int) -> CompletableFuture<Int>

    @Test
    fun `run heavy function`() {
        measureTimeMillis {
            heavyFunc(10).get(5, TimeUnit.SECONDS) shouldBeEqualTo 100
        }

        withBlockingTimeout(Duration.ofMillis(1000)) {
            heavyFunc(10).get(5, TimeUnit.SECONDS) shouldBeEqualTo 100
        }
    }

    @Test
    fun `run factorial`() {
        val x1 = factorial.calc(100).get(5, TimeUnit.SECONDS)

        withBlockingTimeout(Duration.ofSeconds(1)) {
            factorial.calc(100).get(5, TimeUnit.SECONDS)
        } shouldBeEqualTo x1
    }

    @Test
    fun `run fibonacci`() {
        val x1 = fibonacci.calc(100).get(5, TimeUnit.SECONDS)

        withBlockingTimeout(Duration.ofSeconds(1)) {
            fibonacci.calc(100).get(5, TimeUnit.SECONDS)
        } shouldBeEqualTo x1
    }

    /**
     * 멀티스레드 환경에서 비동기 factorial memoizer가 동일한 결과를 반환하는지 검증합니다.
     * [MultithreadingTester]를 사용하여 동시 호출 시 결과 일관성을 확인합니다.
     */
    @Test
    fun `async factorial memoizer는 멀티스레드 환경에서 동일한 결과를 반환해야 한다`() {
        val expected = factorial.calc(100).get(5, TimeUnit.SECONDS)

        MultithreadingTester()
            .workers(16)
            .rounds(4)
            .add {
                factorial.calc(100).get(5, TimeUnit.SECONDS) shouldBeEqualTo expected
            }
            .run()
    }

    /**
     * 멀티스레드 환경에서 비동기 fibonacci memoizer가 동일한 결과를 반환하는지 검증합니다.
     * [MultithreadingTester]를 사용하여 동시 호출 시 결과 일관성을 확인합니다.
     */
    @Test
    fun `async fibonacci memoizer는 멀티스레드 환경에서 동일한 결과를 반환해야 한다`() {
        val expected = fibonacci.calc(100).get(5, TimeUnit.SECONDS)

        MultithreadingTester()
            .workers(16)
            .rounds(4)
            .add {
                fibonacci.calc(100).get(5, TimeUnit.SECONDS) shouldBeEqualTo expected
            }
            .run()
    }

    /**
     * Virtual Thread 환경에서 비동기 factorial memoizer가 동일한 결과를 반환하는지 검증합니다.
     * [StructuredTaskScopeTester]를 사용하여 Virtual Thread 동시 호출 시 결과 일관성을 확인합니다.
     */
    @Test
    fun `async factorial memoizer는 Virtual Thread 환경에서 동일한 결과를 반환해야 한다`() {
        val expected = factorial.calc(100).get(5, TimeUnit.SECONDS)

        StructuredTaskScopeTester()
            .rounds(64)
            .add {
                factorial.calc(100).get(5, TimeUnit.SECONDS) shouldBeEqualTo expected
            }
            .run()
    }

    /**
     * Virtual Thread 환경에서 비동기 fibonacci memoizer가 동일한 결과를 반환하는지 검증합니다.
     * [StructuredTaskScopeTester]를 사용하여 Virtual Thread 동시 호출 시 결과 일관성을 확인합니다.
     */
    @Test
    fun `async fibonacci memoizer는 Virtual Thread 환경에서 동일한 결과를 반환해야 한다`() {
        val expected = fibonacci.calc(100).get(5, TimeUnit.SECONDS)

        StructuredTaskScopeTester()
            .rounds(64)
            .add {
                fibonacci.calc(100).get(5, TimeUnit.SECONDS) shouldBeEqualTo expected
            }
            .run()
    }

}
