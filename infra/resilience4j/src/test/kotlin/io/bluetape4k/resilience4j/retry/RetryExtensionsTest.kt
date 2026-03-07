package io.bluetape4k.resilience4j.retry

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Duration
import kotlin.test.assertFailsWith

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
}
