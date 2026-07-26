package io.bluetape4k.resilience4j.ratelimiter

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RequestNotPermitted
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CompletableFuture

class RateLimiterExtensionsTest {

    companion object: KLoggingChannel()

    private fun unlimitedRateLimiter() = RateLimiter.of("test-${System.nanoTime()}") {
        RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofSeconds(10))
            .limitForPeriod(100)
            .timeoutDuration(Duration.ZERO)
            .build()
    }

    private fun singlePermitRateLimiter() = RateLimiter.of("test-single-${System.nanoTime()}") {
        RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofSeconds(10))
            .limitForPeriod(1)
            .timeoutDuration(Duration.ZERO)
            .build()
    }

    @Test
    fun `withRateLimiter - 성공하는 함수가 정상 실행된다`() = runSuspendTest {
        val rl = unlimitedRateLimiter()
        val result = withRateLimiter(rl) { "hello" }

        result shouldBeEqualTo "hello"
    }

    @Test
    fun `withRateLimiter - 1개 파라미터 함수에 적용한다`() = runSuspendTest {
        val rl = unlimitedRateLimiter()
        val result = withRateLimiter(rl, 21) { input -> input * 2 }

        result shouldBeEqualTo 42
    }

    @Test
    fun `withRateLimiter - 2개 파라미터 함수에 적용한다`() = runSuspendTest {
        val rl = unlimitedRateLimiter()
        val result = withRateLimiter(rl, 20, 22) { a, b -> a + b }

        result shouldBeEqualTo 42
    }

    @Test
    fun `withRateLimiter - 허용량 초과 시 RequestNotPermitted 발생한다`() = runSuspendTest {
        val rl = singlePermitRateLimiter()

        // 첫 번째 호출은 성공
        withRateLimiter(rl) { "ok" }

        // 두 번째 호출은 실패
        assertFailsWith<RequestNotPermitted> {
            withRateLimiter(rl) { error("should fail") }
        }
    }

    @Test
    fun `decorateSuspendFunction1 - 정상 실행된다`() = runSuspendTest {
        val rl = unlimitedRateLimiter()
        val decorated = rl.decorateSuspendFunction1 { input: Int -> input * 2 }

        decorated(21) shouldBeEqualTo 42
    }

    @Test
    fun `decorateSuspendBiFunction - 정상 실행된다`() = runSuspendTest {
        val rl = unlimitedRateLimiter()
        val decorated = rl.decorateSuspendBiFunction { a: Int, b: Int -> a + b }

        decorated(20, 22) shouldBeEqualTo 42
    }

    @Test
    fun `decorateSuspendFunction1 - 허용량 초과 시 RequestNotPermitted 발생한다`() = runSuspendTest {
        val rl = singlePermitRateLimiter()
        val decorated = rl.decorateSuspendFunction1 { input: Int -> input * 2 }

        decorated(21) shouldBeEqualTo 42

        assertFailsWith<RequestNotPermitted> {
            decorated(21)
        }
    }

    @Test
    fun `runnable - 성공 시 실행된다`() {
        val rl = unlimitedRateLimiter()
        var executed = false
        rl.runnable { executed = true }.invoke()
        executed shouldBeEqualTo true
    }

    @Test
    fun `checkedRunnable - 성공 시 실행된다`() {
        val rl = unlimitedRateLimiter()
        var executed = false
        rl.checkedRunnable { executed = true }.run()
        executed shouldBeEqualTo true
    }

    @Test
    fun `callable - 결과를 반환한다`() {
        val rl = unlimitedRateLimiter()
        val result = rl.callable { 42 }.invoke()
        result shouldBeEqualTo 42
    }

    @Test
    fun `supplier - 결과를 반환한다`() {
        val rl = unlimitedRateLimiter()
        val result = rl.supplier { "hello" }.invoke()
        result shouldBeEqualTo "hello"
    }

    @Test
    fun `checkedSupplier - 결과를 반환한다`() {
        val rl = unlimitedRateLimiter()
        val result = rl.checkedSupplier { 42 }.invoke()
        result shouldBeEqualTo 42
    }

    @Test
    fun `consumer - 입력을 처리한다`() {
        val rl = unlimitedRateLimiter()
        var received: String? = null
        rl.consumer<String> { received = it }.invoke("hello")
        received shouldBeEqualTo "hello"
    }

    @Test
    fun `function - 결과를 변환한다`() {
        val rl = unlimitedRateLimiter()
        val result = rl.function { input: Int -> input * 2 }.invoke(21)
        result shouldBeEqualTo 42
    }

    @Test
    fun `checkedFunction - 결과를 변환한다`() {
        val rl = unlimitedRateLimiter()
        val result = rl.checkedFunction { input: Int -> input * 2 }.invoke(21)
        result shouldBeEqualTo 42
    }

    @Test
    fun `completionStage - 비동기 결과를 반환한다`() {
        val rl = unlimitedRateLimiter()
        val supplier = rl.completionStage {
            CompletableFuture.supplyAsync { 42 }
        }
        val result = supplier().toCompletableFuture().get()
        result shouldBeEqualTo 42
    }

    @Test
    fun `completableFuture - 비동기 결과를 변환한다`() {
        val rl = unlimitedRateLimiter()
        val func = rl.completableFuture { input: Int ->
            CompletableFuture.supplyAsync { input * 2 }
        }
        val result = func(21).get()
        result shouldBeEqualTo 42
    }

    @Test
    fun `decorateCompletableFuture - 비동기 결과를 변환한다`() {
        val rl = unlimitedRateLimiter()
        val func = rl.decorateCompletableFuture { input: Int ->
            CompletableFuture.supplyAsync { input * 2 }
        }
        val result = func(21).get()
        result shouldBeEqualTo 42
    }
}
