package io.bluetape4k.resilience4j.timelimiter

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.github.resilience4j.timelimiter.TimeLimiter
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import kotlinx.coroutines.delay
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeoutException
import io.bluetape4k.assertions.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds

class TimeLimiterExtensionsTest {

    companion object: KLoggingChannel()

    private fun defaultTimeLimiter() = TimeLimiter.ofDefaults("test-${System.nanoTime()}")

    private fun shortTimeLimiter() = TimeLimiter.of(
        "test-short-${System.nanoTime()}",
        TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofMillis(50))
            .build()
    )

    @Test
    fun `withTimeLimiter - 성공하는 함수가 정상 실행된다`() = runSuspendTest {
        val tl = defaultTimeLimiter()
        val result = withTimeLimiter(tl) { "hello" }

        result shouldBeEqualTo "hello"
    }

    @Test
    fun `withTimeLimiter - 1개 파라미터 함수에 적용한다`() = runSuspendTest {
        val tl = defaultTimeLimiter()
        val result = withTimeLimiter(tl, 21) { input -> input * 2 }

        result shouldBeEqualTo 42
    }

    @Test
    fun `withTimeLimiter - 2개 파라미터 함수에 적용한다`() = runSuspendTest {
        val tl = defaultTimeLimiter()
        val result = withTimeLimiter(tl, 20, 22) { a, b -> a + b }

        result shouldBeEqualTo 42
    }

    @Test
    fun `withTimeLimiter - 시간 초과 시 TimeoutException이 발생한다`() = runSuspendTest {
        val tl = shortTimeLimiter()

        assertFailsWith<TimeoutException> {
            withTimeLimiter(tl) {
                delay(1000.milliseconds) // 타임아웃보다 오래 실행
                "should not reach here"
            }
        }
    }

    @Test
    fun `decorateSuspendFunction1 - 정상 실행된다`() = runSuspendTest {
        val tl = defaultTimeLimiter()
        val decorated = tl.decorateSuspendFunction1 { input: Int -> input * 2 }

        decorated(21) shouldBeEqualTo 42
    }

    @Test
    fun `decorateSuspendBiFunction - 정상 실행된다`() = runSuspendTest {
        val tl = defaultTimeLimiter()
        val decorated = tl.decorateSuspendBiFunction { a: Int, b: Int -> a + b }

        decorated(20, 22) shouldBeEqualTo 42
    }

    @Test
    fun `decorateSuspendFunction1 - 시간 초과 시 TimeoutException이 발생한다`() = runSuspendTest {
        val tl = shortTimeLimiter()
        val decorated = tl.decorateSuspendFunction1 { _: Int ->
            delay(1000.milliseconds)
            "should not reach"
        }

        assertFailsWith<TimeoutException> {
            decorated(1)
        }
    }

    @Test
    fun `futureSupplier - 성공하는 Future 결과를 반환한다`() {
        val tl = defaultTimeLimiter()
        val result = tl.futureSupplier {
            CompletableFuture.supplyAsync { 42 }
        }.invoke()
        result shouldBeEqualTo 42
    }

    @Test
    fun `completionStage - 성공하는 비동기 결과를 반환한다`() {
        val tl = defaultTimeLimiter()
        val supplier = tl.completionStage {
            CompletableFuture.supplyAsync { 42 }
        }
        val result = supplier.invoke()
        result shouldBeEqualTo 42
    }

    @Test
    fun `completableFuture - 비동기 결과를 변환한다`() {
        val tl = defaultTimeLimiter()
        @Suppress("UNCHECKED_CAST")
        val func = tl.completableFuture { input: Int ->
            CompletableFuture.supplyAsync { input * 2 } as CompletableFuture<Int>
        }
        val result = func(21).get()
        result shouldBeEqualTo 42
    }

    @Test
    fun `decorateCompletableFuture - 비동기 결과를 변환한다`() {
        val tl = defaultTimeLimiter()
        @Suppress("UNCHECKED_CAST")
        val func = tl.decorateCompletableFuture { input: Int ->
            CompletableFuture.supplyAsync { input * 2 } as CompletableFuture<Int>
        }
        val result = func(21).get()
        result shouldBeEqualTo 42
    }
}
