package io.bluetape4k.resilience4j.ratelimiter

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RequestNotPermitted
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertFailsWith

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
            withRateLimiter(rl) { "should fail" }
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
}
