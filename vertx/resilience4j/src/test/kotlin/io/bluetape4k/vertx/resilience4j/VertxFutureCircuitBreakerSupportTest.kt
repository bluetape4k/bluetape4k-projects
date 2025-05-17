package io.bluetape4k.vertx.resilience4j

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.vertx.tests.withTestContext
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.TimeUnit

@Suppress("UNUSED_PARAMETER")
class VertxFutureCircuitBreakerSupportTest: AbstractVertxFutureTest() {

    companion object: KLoggingChannel()

    @Test
    fun `decorate future and return with success`(vertx: Vertx, testContext: VertxTestContext) {
        withTestContext(testContext) {
            val service = VertxHelloWorldService()
            val circuitBreaker = CircuitBreaker.ofDefaults("test").applyEventPublisher()
            val metrics = circuitBreaker.metrics

            metrics.numberOfBufferedCalls shouldBeEqualTo 0

            val supplier = circuitBreaker.decorateVertxFuture { service.returnHelloWorld() }
            val future = supplier.invoke()

            future.succeeded().shouldBeTrue()
            future.result() shouldBeEqualTo "Hello world"
            metrics.numberOfBufferedCalls shouldBeEqualTo 1
            metrics.numberOfFailedCalls shouldBeEqualTo 0
            metrics.numberOfSuccessfulCalls shouldBeEqualTo 1

            service.invocationCount shouldBeEqualTo 1
        }
    }

    @Test
    fun `execute future and return with success`(vertx: Vertx, testContext: VertxTestContext) {
        withTestContext(testContext) {
            val service = VertxHelloWorldService()
            val circuitBreaker = CircuitBreaker.ofDefaults("test").applyEventPublisher()
            val metrics = circuitBreaker.metrics

            metrics.numberOfBufferedCalls shouldBeEqualTo 0

            val future = circuitBreaker.executeVertxFuture { service.returnHelloWorld() }

            future.succeeded().shouldBeTrue()
            future.result() shouldBeEqualTo "Hello world"
            metrics.numberOfBufferedCalls shouldBeEqualTo 1
            metrics.numberOfFailedCalls shouldBeEqualTo 0
            metrics.numberOfSuccessfulCalls shouldBeEqualTo 1

            service.invocationCount shouldBeEqualTo 1
        }
    }

    @Test
    fun `execute future and return with exception`(vertx: Vertx, testContext: VertxTestContext) {
        withTestContext(testContext) {
            val service = VertxHelloWorldService()
            val circuitBreaker = CircuitBreaker.ofDefaults("test").applyEventPublisher()
            val metrics = circuitBreaker.metrics

            metrics.numberOfBufferedCalls shouldBeEqualTo 0

            val future = circuitBreaker.executeVertxFuture { service.throwException() }

            future.failed().shouldBeTrue()
            metrics.numberOfBufferedCalls shouldBeEqualTo 1
            metrics.numberOfFailedCalls shouldBeEqualTo 1
            metrics.numberOfSuccessfulCalls shouldBeEqualTo 0

            service.invocationCount shouldBeEqualTo 1
        }
    }

    @Test
    fun `return failure with CircuitBreakerOpenException`(vertx: Vertx, testContext: VertxTestContext) {
        withTestContext(testContext) {
            val service = VertxHelloWorldService()
            val circuitBreakerConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .permittedNumberOfCallsInHalfOpenState(2)
                .failureRateThreshold(50.0F)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .build()
            val circuitBreaker = CircuitBreaker.of("test", circuitBreakerConfig).applyEventPublisher()
            circuitBreaker.onError(0, TimeUnit.NANOSECONDS, RuntimeException())
            circuitBreaker.onError(0, TimeUnit.NANOSECONDS, RuntimeException())
            circuitBreaker.state shouldBeEqualTo CircuitBreaker.State.OPEN

            val metrics = circuitBreaker.metrics
            metrics.numberOfBufferedCalls shouldBeEqualTo 2
            metrics.numberOfFailedCalls shouldBeEqualTo 2

            val future = circuitBreaker.executeVertxFuture { service.returnHelloWorld() }

            future.failed().shouldBeTrue()
            future.cause() shouldBeInstanceOf CallNotPermittedException::class

            metrics.numberOfBufferedCalls shouldBeEqualTo 2
            metrics.numberOfFailedCalls shouldBeEqualTo 2
            metrics.numberOfNotPermittedCalls shouldBeEqualTo 1

            service.invocationCount shouldBeEqualTo 0
        }
    }
}
