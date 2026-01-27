package io.bluetape4k.vertx.resilience4j

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.vertx.asCompletableFuture
import io.bluetape4k.vertx.tests.withTestContext
import io.github.resilience4j.kotlin.retry.RetryConfig
import io.github.resilience4j.retry.Retry
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.Executors

@Suppress("UNUSED_PARAMETER")
class VertxFutureRetrySupportTest: AbstractVertxFutureTest() {

    companion object: KLoggingChannel()

    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    @Test
    fun `성공한 함수는 retry를 하지 않습니다`(vertx: Vertx, testContext: VertxTestContext) =
        withTestContext(testContext) {
            val service = VertxHelloWorldService()
            val retry = Retry.ofDefaults("test").applyEventPublisher()
            val metrics = retry.metrics

            val future = retry.executeVertxFuture(scheduler) { service.returnHelloWorld() }
            val result = runCatching { future.asCompletableFuture().get() }

            result.isSuccess.shouldBeTrue()
            result.getOrNull() shouldBeEqualTo "Hello world"

            metrics.numberOfSuccessfulCallsWithoutRetryAttempt shouldBeEqualTo 1
            metrics.numberOfSuccessfulCallsWithRetryAttempt shouldBeEqualTo 0
            metrics.numberOfFailedCallsWithoutRetryAttempt shouldBeEqualTo 0
            metrics.numberOfFailedCallsWithRetryAttempt shouldBeEqualTo 0

            service.invocationCount shouldBeEqualTo 1
        }

    @Test
    fun `예외가 발생하면 재시도합니다`(vertx: Vertx, testContext: VertxTestContext) =
        withTestContext(testContext) {
            val service = VertxHelloWorldService()
            val retry = Retry.of("test") {
                RetryConfig {
                    maxAttempts(3)
                    waitDuration(Duration.ofMillis(10))
                }
            }.applyEventPublisher()
            val metrics = retry.metrics

            val future = retry.executeVertxFuture(scheduler) {
                when (service.invocationCount) {
                    0    -> service.throwException()
                    else -> service.returnHelloWorld()
                }
            }

            val result = runCatching { future.asCompletableFuture().get() }

            result.isSuccess.shouldBeTrue()

            metrics.numberOfSuccessfulCallsWithoutRetryAttempt shouldBeEqualTo 0
            metrics.numberOfSuccessfulCallsWithRetryAttempt shouldBeEqualTo 1
            metrics.numberOfFailedCallsWithoutRetryAttempt shouldBeEqualTo 0
            metrics.numberOfFailedCallsWithRetryAttempt shouldBeEqualTo 0

            service.invocationCount shouldBeEqualTo 2
        }

    @Test
    fun `retryOnResult 를 기준으로 재시도를 수행합니다`(vertx: Vertx, testContext: VertxTestContext) =
        withTestContext(testContext) {
            val service = VertxHelloWorldService()
            val retry = Retry.of("test") {
                RetryConfig {
                    waitDuration(Duration.ofMillis(10))
                    retryOnResult {
                        log.debug { "invocation count=${service.invocationCount}" }
                        service.invocationCount < 2
                    }
                }
            }.applyEventPublisher()
            val metrics = retry.metrics

            val future = retry.executeVertxFuture(scheduler) { service.returnHelloWorld() }
            val result = runCatching { future.asCompletableFuture().get() }

            result.isSuccess.shouldBeTrue()

            metrics.numberOfSuccessfulCallsWithoutRetryAttempt shouldBeEqualTo 0
            metrics.numberOfSuccessfulCallsWithRetryAttempt shouldBeEqualTo 1
            metrics.numberOfFailedCallsWithoutRetryAttempt shouldBeEqualTo 0
            metrics.numberOfFailedCallsWithRetryAttempt shouldBeEqualTo 0

            service.invocationCount shouldBeEqualTo 2
        }

    @Test
    fun `반복된 예외 시에는 함수 실행이 실패한다`(vertx: Vertx, testContext: VertxTestContext) =
        withTestContext(testContext) {
            val service = VertxHelloWorldService()
            val retry = Retry.of("test") {
                RetryConfig {
                    waitDuration(Duration.ofMillis(10))
                }
            }.applyEventPublisher()
            val metrics = retry.metrics

            val future = retry.executeVertxFuture(scheduler) { service.throwException() }
            val result = runCatching { future.asCompletableFuture().get() }

            result.isFailure.shouldBeTrue()
            metrics.numberOfSuccessfulCallsWithoutRetryAttempt shouldBeEqualTo 0
            metrics.numberOfSuccessfulCallsWithRetryAttempt shouldBeEqualTo 0
            metrics.numberOfFailedCallsWithoutRetryAttempt shouldBeEqualTo 0
            metrics.numberOfFailedCallsWithRetryAttempt shouldBeEqualTo 1

            service.invocationCount shouldBeEqualTo retry.retryConfig.maxAttempts
        }

    @Test
    fun `decorate future function`(vertx: Vertx, testContext: VertxTestContext) =
        withTestContext(testContext) {
            val service = VertxHelloWorldService()
            val retry = Retry.ofDefaults("test").applyEventPublisher()
            val metrics = retry.metrics

            val decorated = retry.decorateVertxFuture(scheduler) { service.returnHelloWorld() }
            val result = runCatching { decorated().asCompletableFuture().get() }

            result.isSuccess.shouldBeTrue()
            result.getOrNull() shouldBeEqualTo "Hello world"

            metrics.numberOfSuccessfulCallsWithoutRetryAttempt shouldBeEqualTo 1
            metrics.numberOfSuccessfulCallsWithRetryAttempt shouldBeEqualTo 0
            metrics.numberOfFailedCallsWithoutRetryAttempt shouldBeEqualTo 0
            metrics.numberOfFailedCallsWithRetryAttempt shouldBeEqualTo 0

            service.invocationCount shouldBeEqualTo 1
        }
}
