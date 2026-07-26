package io.bluetape4k.resilience4j.retry

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Duration
import java.util.concurrent.CompletableFuture

class RetryExtensionsTest {

    companion object: KLoggingChannel()

    private val retry: Retry = Retry.of("test") {
        RetryConfig.custom<Any?>()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(10))
            .build()
    }

    @Test
    fun `withRetry - 성공하는 함수는 한 번만 실행된다`() = runSuspendTest {
        var count = 0
        // 타입 파라미터 명시로 오버로드 충돌 해결
        val result = withRetry<String>(retry) {
            count++
            "success"
        }

        result shouldBeEqualTo "success"
        count shouldBeEqualTo 1
    }

    @Test
    fun `withRetry - 예외 발생 시 maxAttempts만큼 재시도한다`() = runSuspendTest {
        var count = 0

        assertFailsWith<IOException> {
            withRetry<String>(retry) {
                count++
                throw IOException("fail")
            }
        }

        count shouldBeEqualTo retry.retryConfig.maxAttempts
    }

    @Test
    fun `withRetry - 1개 파라미터 함수에 적용한다`() = runSuspendTest {
        var count = 0
        val result = withRetry(retry, 21) { input: Int ->
            count++
            input * 2
        }

        result shouldBeEqualTo 42
        count shouldBeEqualTo 1
    }

    @Test
    fun `withRetry - 1개 파라미터 함수 실패 시 재시도한다`() = runSuspendTest {
        var count = 0

        assertFailsWith<IOException> {
            withRetry(retry, "input") { _: String ->
                count++
                throw IOException("fail")
            }
        }

        count shouldBeEqualTo retry.retryConfig.maxAttempts
    }

    @Test
    fun `withRetry - 2개 파라미터 함수에 적용한다`() = runSuspendTest {
        var count = 0
        val result = withRetry(retry, 21, 21) { a: Int, b: Int ->
            count++
            a + b
        }

        result shouldBeEqualTo 42
        count shouldBeEqualTo 1
    }

    @Test
    fun `withRetry - 2개 파라미터 함수 실패 시 재시도한다`() = runSuspendTest {
        var count = 0

        assertFailsWith<IOException> {
            withRetry(retry, 1, 2) { _: Int, _: Int ->
                count++
                throw IOException("fail")
            }
        }

        count shouldBeEqualTo retry.retryConfig.maxAttempts
    }

    @Test
    fun `decorateSuspendFunction1 - 성공 시 정상 반환한다`() = runSuspendTest {
        val decorated = retry.decorateSuspendFunction1 { input: Int ->
            input * 2
        }

        decorated(21) shouldBeEqualTo 42
    }

    @Test
    fun `decorateSuspendBiFunction - 성공 시 정상 반환한다`() = runSuspendTest {
        val decorated = retry.decorateSuspendBiFunction { a: Int, b: Int ->
            a + b
        }

        decorated(20, 22) shouldBeEqualTo 42
    }

    @Test
    fun `decorateSuspendFunction1 - 재시도 후 성공하면 결과를 반환한다`() = runSuspendTest {
        var attempt = 0
        val decorated = retry.decorateSuspendFunction1 { input: Int ->
            attempt++
            if (attempt < 2) throw IOException("retry me")
            input * 2
        }

        val result = decorated(21)
        result shouldBeEqualTo 42
        (attempt >= 2).shouldBeTrue()
    }

    @Test
    fun `runnable - 성공 시 실행된다`() {
        var executed = false
        retry.runnable { executed = true }.run()
        executed shouldBeEqualTo true
    }

    @Test
    fun `runnable - 예외 발생 시 maxAttempts만큼 재시도한다`() {
        var count = 0
        assertFailsWith<RuntimeException> {
            retry.runnable {
                count++
                throw RuntimeException("fail")
            }.run()
        }
        count shouldBeEqualTo retry.retryConfig.maxAttempts
    }

    @Test
    fun `checkedRunnable - 성공 시 실행된다`() {
        var executed = false
        retry.checkedRunnable { executed = true }.run()
        executed shouldBeEqualTo true
    }

    @Test
    fun `callable - 결과를 반환한다`() {
        val result = retry.callable { 42 }.invoke()
        result shouldBeEqualTo 42
    }

    @Test
    fun `callable - 예외 발생 시 재시도한다`() {
        var count = 0
        assertFailsWith<IOException> {
            retry.callable {
                count++
                throw IOException("fail")
                @Suppress("UNREACHABLE_CODE")
                42
            }.invoke()
        }
        count shouldBeEqualTo retry.retryConfig.maxAttempts
    }

    @Test
    fun `supplier - 결과를 반환한다`() {
        val result = retry.supplier { "hello" }.invoke()
        result shouldBeEqualTo "hello"
    }

    @Test
    fun `checkedSupplier - 결과를 반환한다`() {
        val result = retry.checkedSupplier { 42 }.invoke()
        result shouldBeEqualTo 42
    }

    @Test
    fun `function - 결과를 변환한다`() {
        val result = retry.function { input: Int -> input * 2 }.invoke(21)
        result shouldBeEqualTo 42
    }

    @Test
    fun `checkedFunction - 결과를 변환한다`() {
        val result = retry.checkedFunction { input: Int -> input * 2 }.invoke(21)
        result shouldBeEqualTo 42
    }

    @Test
    fun `completionStage - 비동기 결과를 반환한다`() {
        val supplier = retry.completionStage {
            CompletableFuture.supplyAsync { 42 }
        }
        val result = supplier().toCompletableFuture().get()
        result shouldBeEqualTo 42
    }

    @Test
    fun `completableFutureFunction - 비동기 결과를 변환한다`() {
        val func = retry.completableFutureFunction { input: Int ->
            CompletableFuture.supplyAsync { input * 2 }
        }
        val result = func(21).get()
        result shouldBeEqualTo 42
    }

    @Test
    fun `completableFuture - 비동기 결과를 변환한다`() {
        val func = retry.completableFuture { input: Int ->
            CompletableFuture.supplyAsync { input * 2 }
        }
        val result = func(21).get()
        result shouldBeEqualTo 42
    }

    @Test
    fun `withRetry async - 비동기 결과를 변환한다`() {
        val func = withRetry<Int, Int>(retry) { input ->
            CompletableFuture.supplyAsync { input * 2 }
        }
        val result = func(21).get()
        result shouldBeEqualTo 42
    }
}
