package io.bluetape4k.vertx.resilience4j

import io.bluetape4k.vertx.asCompletableFuture
import io.bluetape4k.vertx.tests.withTestContext
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.retry.Retry
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

@Suppress("UNUSED_PARAMETER")
class VertxDecoratorsTest: AbstractVertxFutureTest() {

    private val retry = Retry.ofDefaults("coDecorator").applyEventPublisher()
    private val circuitBreaker = CircuitBreaker.ofDefaults("coDecorator").applyEventPublisher()
    private val rateLimiter = RateLimiter.ofDefaults("coDecorator")

    @Test
    fun `성공하는 함수에 retry decorate 하기`(vertx: Vertx, testContext: VertxTestContext) =
        withTestContext(testContext) {
            val service = VertxHelloWorldService()

            val decorated = VertxDecorators.ofSupplier { service.returnHelloWorld() }
                .withRetry(retry)
                .decorate()

            val result = runCatching { decorated().asCompletableFuture().get() }

            result.isSuccess.shouldBeTrue()
            result.getOrNull() shouldBeEqualTo "Hello world"

            service.invocationCount shouldBeEqualTo 1
        }

    @Test
    fun `성공하는 함수에 retry와 circuit breaker decorate 하기`(vertx: Vertx, testContext: VertxTestContext) =
        withTestContext(testContext) {
            val service = VertxHelloWorldService()

            val decorated = VertxDecorators.ofSupplier { service.returnHelloWorld() }
                .withRetry(retry)
                .withCircuitBreaker(circuitBreaker)
                .decorate()

            val result = runCatching { decorated().asCompletableFuture().get() }

            result.isSuccess.shouldBeTrue()
            result.getOrNull() shouldBeEqualTo "Hello world"

            service.invocationCount shouldBeEqualTo 1
        }

    @Test
    fun `성공하는 함수에 fallback decorate 하기`(vertx: Vertx, testContext: VertxTestContext) =
        withTestContext(testContext) {
            val service = VertxHelloWorldService()

            val decorated = VertxDecorators.ofSupplier { service.returnHelloWorld() }
                .withFallback({ it == "Hello world" }, { "fallback" })
                .decorate()

            val result = runCatching { decorated().asCompletableFuture().get() }

            result.isSuccess.shouldBeTrue()
            result.getOrNull() shouldBeEqualTo "fallback"

            service.invocationCount shouldBeEqualTo 1
        }

    @Test
    fun `실패하는 함수에 retry decorate 하기`(vertx: Vertx, testContext: VertxTestContext) =
        withTestContext(testContext) {
            val service = VertxHelloWorldService()

            val decorated = VertxDecorators.ofSupplier { service.throwException() }
                .withRetry(retry)
                .decorate()

            val result = runCatching { decorated().asCompletableFuture().get() }

            result.isFailure.shouldBeTrue()
            service.invocationCount shouldBeEqualTo retry.retryConfig.maxAttempts
        }

    @Test
    fun `실패하는 함수에 retry 와 circuit breaker decorate 하기`(vertx: Vertx, testContext: VertxTestContext) =
        withTestContext(testContext) {
            val service = VertxHelloWorldService()

            val decorated = VertxDecorators.ofSupplier { service.throwException() }
                .withRetry(retry)
                .withCircuitBreaker(circuitBreaker)
                .decorate()

            val result = runCatching { decorated().asCompletableFuture().get() }

            result.isFailure.shouldBeTrue()
            service.invocationCount shouldBeEqualTo retry.retryConfig.maxAttempts
        }
}
