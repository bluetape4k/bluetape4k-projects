package io.bluetape4k.resilience4j.circuitbreaker

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.IOException
import kotlin.test.assertFailsWith

class CircuitBreakerExtensionsTest {

    companion object: KLoggingChannel()

    @Test
    fun `withCircuitBreaker - 성공하는 함수가 정상 실행된다`() = runSuspendTest {
        val cb = CircuitBreaker.ofDefaults("test")
        val result = withCircuitBreaker(cb) { "hello" }

        result shouldBeEqualTo "hello"
        cb.metrics.numberOfSuccessfulCalls shouldBeEqualTo 1
    }

    @Test
    fun `withCircuitBreaker - 1개 파라미터 함수에 적용한다`() = runSuspendTest {
        val cb = CircuitBreaker.ofDefaults("test")
        val result = withCircuitBreaker(cb, 21) { input -> input * 2 }

        result shouldBeEqualTo 42
        cb.metrics.numberOfSuccessfulCalls shouldBeEqualTo 1
    }

    @Test
    fun `withCircuitBreaker - 2개 파라미터 함수에 적용한다`() = runSuspendTest {
        val cb = CircuitBreaker.ofDefaults("test")
        val result = withCircuitBreaker(cb, 20, 22) { a, b -> a + b }

        result shouldBeEqualTo 42
        cb.metrics.numberOfSuccessfulCalls shouldBeEqualTo 1
    }

    @Test
    fun `withCircuitBreaker - OPEN 상태이면 CallNotPermittedException이 발생한다`() = runSuspendTest {
        val cb = CircuitBreaker.ofDefaults("test")
        cb.transitionToOpenState()

        assertFailsWith<CallNotPermittedException> {
            withCircuitBreaker(cb) { "should not run" }
        }

        cb.metrics.numberOfNotPermittedCalls shouldBeEqualTo 1
    }

    @Test
    fun `withCircuitBreaker - 예외 발생 시 실패로 기록된다`() = runSuspendTest {
        val cb = CircuitBreaker.ofDefaults("test")

        assertFailsWith<IOException> {
            withCircuitBreaker(cb) { throw IOException("fail") }
        }

        cb.metrics.numberOfFailedCalls shouldBeEqualTo 1
    }

    @Test
    fun `decorateSuspendFunction1 - 정상 실행된다`() = runSuspendTest {
        val cb = CircuitBreaker.ofDefaults("test")
        val decorated = cb.decorateSuspendFunction1 { input: Int -> input * 2 }

        decorated(21) shouldBeEqualTo 42
        cb.metrics.numberOfSuccessfulCalls shouldBeEqualTo 1
    }

    @Test
    fun `decorateSuspendBiFunction - 정상 실행된다`() = runSuspendTest {
        val cb = CircuitBreaker.ofDefaults("test")
        val decorated = cb.decorateSuspendBiFunction { a: Int, b: Int -> a + b }

        decorated(20, 22) shouldBeEqualTo 42
        cb.metrics.numberOfSuccessfulCalls shouldBeEqualTo 1
    }

    @Test
    fun `decorateSuspendFunction1 - OPEN 상태이면 CallNotPermittedException이 발생한다`() = runSuspendTest {
        val cb = CircuitBreaker.ofDefaults("test")
        cb.transitionToOpenState()

        val decorated = cb.decorateSuspendFunction1 { input: Int -> input * 2 }

        assertFailsWith<CallNotPermittedException> {
            decorated(21)
        }
    }
}
