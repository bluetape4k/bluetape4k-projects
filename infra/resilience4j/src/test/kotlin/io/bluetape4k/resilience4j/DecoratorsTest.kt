package io.bluetape4k.resilience4j

import io.bluetape4k.concurrent.futureOf
import io.bluetape4k.logging.KLogging
import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.decorators.Decorators
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.retry.Retry
import io.mockk.clearMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors

class DecoratorsTest {

    companion object: KLogging()

    private var state = false
    private val helloWorldService = mockk<HelloWorldService>(relaxUnitFun = true)

    @BeforeEach
    fun setup() {
        clearMocks(helloWorldService)
    }

    @Test
    fun `decorate supplier`() {

        every { helloWorldService.returnHelloWorld() } returns "Hello world"

        val circuitBreaker = CircuitBreaker.ofDefaults("helloBackend")
        val supplier = Decorators
            .ofSupplier { helloWorldService.returnHelloWorld() }
            .withCircuitBreaker(circuitBreaker)
            .withRetry(Retry.ofDefaults("id"))
            .withRateLimiter(RateLimiter.ofDefaults("testName"))
            .withBulkhead(Bulkhead.ofDefaults("testName"))
            .decorate()

        val result = supplier.get()

        result shouldBeEqualTo "Hello world"
        with(circuitBreaker.metrics) {
            numberOfBufferedCalls shouldBeEqualTo 1
            numberOfSuccessfulCalls shouldBeEqualTo 1
            numberOfFailedCalls shouldBeEqualTo 0
        }
        verify(exactly = 1) { helloWorldService.returnHelloWorld() }
        confirmVerified(helloWorldService)
    }

    @Test
    fun `decorate checkedSupplier`() {
        every { helloWorldService.returnHelloWorldWithException() } returns "Hello world"

        val circuitBreaker = CircuitBreaker.ofDefaults("helloBackend")
        val checkedSupplier = Decorators
            .ofCheckedSupplier { helloWorldService.returnHelloWorldWithException() }
            .withCircuitBreaker(circuitBreaker)
            .withRetry(Retry.ofDefaults("id"))
            .withRateLimiter(RateLimiter.ofDefaults("testName"))
            .withBulkhead(Bulkhead.ofDefaults("testName"))
            .decorate()

        val result = checkedSupplier.get()

        result shouldBeEqualTo "Hello world"
        with(circuitBreaker.metrics) {
            numberOfBufferedCalls shouldBeEqualTo 1
            numberOfSuccessfulCalls shouldBeEqualTo 1
            numberOfFailedCalls shouldBeEqualTo 0
        }
        verify(exactly = 1) { helloWorldService.returnHelloWorldWithException() }
        confirmVerified(helloWorldService)
    }

    @Test
    fun `decorate runnable`() {
        val circuitBreaker = CircuitBreaker.ofDefaults("helloBackend")
        val runnable = Decorators
            .ofRunnable { helloWorldService.sayHelloWorld() }
            .withCircuitBreaker(circuitBreaker)
            .withRetry(Retry.ofDefaults("id"))
            .withRateLimiter(RateLimiter.ofDefaults("testName"))
            .withBulkhead(Bulkhead.ofDefaults("testName"))
            .decorate()

        runnable.run()

        with(circuitBreaker.metrics) {
            numberOfBufferedCalls shouldBeEqualTo 1
            numberOfSuccessfulCalls shouldBeEqualTo 1
            numberOfFailedCalls shouldBeEqualTo 0
        }
        verify(exactly = 1) { helloWorldService.sayHelloWorld() }
        confirmVerified(helloWorldService)
    }

    @Test
    fun `decorate checked runnable`() {
        val circuitBreaker = CircuitBreaker.ofDefaults("helloBackend")
        val checkedRunnable = Decorators
            .ofCheckedRunnable { helloWorldService.sayHelloWorldWithException() }
            .withCircuitBreaker(circuitBreaker)
            .withRetry(Retry.ofDefaults("id"))
            .withRateLimiter(RateLimiter.ofDefaults("testName"))
            .withBulkhead(Bulkhead.ofDefaults("testName"))
            .decorate()

        checkedRunnable.run()

        with(circuitBreaker.metrics) {
            numberOfBufferedCalls shouldBeEqualTo 1
            numberOfSuccessfulCalls shouldBeEqualTo 1
            numberOfFailedCalls shouldBeEqualTo 0
        }
        verify(exactly = 1) { helloWorldService.sayHelloWorldWithException() }
        confirmVerified(helloWorldService)
    }

    @Test
    fun `decorate completionStage`() {

        every { helloWorldService.returnHelloWorld() } returns "Hello world"
        val circuitBreaker = CircuitBreaker.ofDefaults("helloBackend")

        val completionStageSupplier = Decorators
            .ofCompletionStage { futureOf { helloWorldService.returnHelloWorld() } }
            .withCircuitBreaker(circuitBreaker)
            .withRetry(Retry.ofDefaults("id"), Executors.newSingleThreadScheduledExecutor())
            .withRateLimiter(RateLimiter.ofDefaults("testName"))
            .withBulkhead(Bulkhead.ofDefaults("testName"))
            .get()

        val value = completionStageSupplier.toCompletableFuture().get()

        value shouldBeEqualTo "Hello world"
        with(circuitBreaker.metrics) {
            numberOfBufferedCalls shouldBeEqualTo 1
            numberOfSuccessfulCalls shouldBeEqualTo 1
            numberOfFailedCalls shouldBeEqualTo 0
        }
        verify(exactly = 1) { helloWorldService.returnHelloWorld() }
        confirmVerified(helloWorldService)
    }
}
