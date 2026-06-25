package io.bluetape4k.resilience4j.circuitbreaker

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.CompletableFuture
import io.bluetape4k.assertions.assertFailsWith

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
            withCircuitBreaker(cb) { error("should not run") }
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

    @Test
    fun `runnable - 성공 시 실행된다`() {
        val cb = CircuitBreaker.ofDefaults("test-runnable")
        var executed = false
        cb.runnable { executed = true }.invoke()
        executed shouldBeEqualTo true
        cb.metrics.numberOfSuccessfulCalls shouldBeEqualTo 1
    }

    @Test
    fun `checkedRunnable - 성공 시 실행된다`() {
        val cb = CircuitBreaker.ofDefaults("test-checked-runnable")
        var executed = false
        cb.checkedRunnable { executed = true }.run()
        executed shouldBeEqualTo true
    }

    @Test
    fun `callable - 결과를 반환한다`() {
        val cb = CircuitBreaker.ofDefaults("test-callable")
        val result = cb.callable { 42 }.invoke()
        result shouldBeEqualTo 42
        cb.metrics.numberOfSuccessfulCalls shouldBeEqualTo 1
    }

    @Test
    fun `supplier - 결과를 반환한다`() {
        val cb = CircuitBreaker.ofDefaults("test-supplier")
        val result = cb.supplier { "hello" }.invoke()
        result shouldBeEqualTo "hello"
    }

    @Test
    fun `checkedSupplier - 결과를 반환한다`() {
        val cb = CircuitBreaker.ofDefaults("test-checked-supplier")
        val result = cb.checkedSupplier { 42 }.invoke()
        result shouldBeEqualTo 42
    }

    @Test
    fun `consumer - 입력을 처리한다`() {
        val cb = CircuitBreaker.ofDefaults("test-consumer")
        var received: String? = null
        cb.consumer<String> { received = it }.invoke("hello")
        received shouldBeEqualTo "hello"
    }

    @Test
    fun `checkedConsumer - 입력을 처리한다`() {
        val cb = CircuitBreaker.ofDefaults("test-checked-consumer")
        var received: String? = null
        cb.checkedConsumer<String> { received = it }.accept("hello")
        received shouldBeEqualTo "hello"
    }

    @Test
    fun `function - 결과를 변환한다`() {
        val cb = CircuitBreaker.ofDefaults("test-function")
        val result = cb.function { input: Int -> input * 2 }.invoke(21)
        result shouldBeEqualTo 42
    }

    @Test
    fun `checkedFunction - 결과를 변환한다`() {
        val cb = CircuitBreaker.ofDefaults("test-checked-function")
        val result = cb.checkedFunction { input: Int -> input * 2 }.invoke(21)
        result shouldBeEqualTo 42
    }

    @Test
    fun `completionStatge - 비동기 결과를 반환한다`() {
        val cb = CircuitBreaker.ofDefaults("test-cs")
        val supplier = cb.completionStatge {
            CompletableFuture.supplyAsync { 42 }
        }
        val result = supplier().toCompletableFuture().get()
        result shouldBeEqualTo 42
    }

    @Test
    fun `completableFuture - 비동기 결과를 변환한다`() {
        val cb = CircuitBreaker.ofDefaults("test-cf")
        val func = cb.completableFuture { input: Int ->
            CompletableFuture.supplyAsync { input * 2 }
        }
        val result = func(21).get()
        result shouldBeEqualTo 42
    }

    @Test
    fun `decorateCompletableFuture - 비동기 결과를 변환한다`() {
        val cb = CircuitBreaker.ofDefaults("test-dcf")
        val func = cb.decorateCompletableFuture { input: Int ->
            CompletableFuture.supplyAsync { input * 2 }
        }
        val result = func(21).get()
        result shouldBeEqualTo 42
        cb.metrics.numberOfSuccessfulCalls shouldBeEqualTo 1
    }
}
