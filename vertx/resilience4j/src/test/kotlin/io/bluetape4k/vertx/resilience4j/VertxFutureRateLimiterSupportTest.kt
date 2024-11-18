package io.bluetape4k.vertx.resilience4j

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.vertx.asCompletableFuture
import io.bluetape4k.vertx.tests.withTestContext
import io.github.resilience4j.kotlin.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiter
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.time.Duration

@Suppress("UNUSED_PARAMETER")
class VertxFutureRateLimiterSupportTest: AbstractVertxFutureTest() {

    companion object: KLogging() {
        private const val RATE_LIMIT = 10
    }

    private fun noWaitConfig() = RateLimiterConfig {
        limitRefreshPeriod(Duration.ofSeconds(10))
        limitForPeriod(RATE_LIMIT)
        timeoutDuration(Duration.ZERO)
    }

    private fun RateLimiter.applyEventPublishing() = apply {
        eventPublisher
            .onSuccess { log.debug { "Success" } }
            .onEvent { log.debug { "FlowEvent. permits=${it.numberOfPermits}" } }
    }

    @Test
    fun `메소드 호출 성공 시 Permission은 1 감소`(vertx: Vertx, testContext: VertxTestContext) =
        withTestContext(testContext) {
            val rateLimiter = RateLimiter.of("test", noWaitConfig()).applyEventPublishing()
            val metrics = rateLimiter.metrics
            val service = VertxHelloWorldService()

            val result = rateLimiter.executeVertxFuture {
                service.returnHelloWorld()
            }.asCompletableFuture().get()

            result shouldBeEqualTo "Hello world"
            metrics.availablePermissions shouldBeEqualTo RATE_LIMIT - 1
            metrics.numberOfWaitingThreads shouldBeEqualTo 0
            service.invocationCount shouldBeEqualTo 1
        }

    @Test
    fun `메소드 실행 실패 시에도 Permission은 1 감소`(vertx: Vertx, testContext: VertxTestContext) =
        withTestContext(testContext) {
            val rateLimiter = RateLimiter.of("test", noWaitConfig()).applyEventPublishing()
            val metrics = rateLimiter.metrics
            val service = VertxHelloWorldService()

            try {
                rateLimiter.executeVertxFuture {
                    service.throwException()
                }.asCompletableFuture().get()
                fail("예외가 발생해야 합니다")
            } catch (e: Throwable) {
                // no op
            }

            metrics.availablePermissions shouldBeEqualTo RATE_LIMIT - 1
            metrics.numberOfWaitingThreads shouldBeEqualTo 0
            service.invocationCount shouldBeEqualTo 1
        }

    @Test
    fun `rate limit 에 걸리면 메소드 실행이 안되고 즉시 예외를 발생합니다`(vertx: Vertx, testContext: VertxTestContext) =
        withTestContext(testContext) {
            val rateLimiter = RateLimiter.of("test", noWaitConfig()).applyEventPublishing()
            val metrics = rateLimiter.metrics
            val service = VertxHelloWorldService()

            // Rate Limiter 에 걸리게 한다
            repeat(RATE_LIMIT) {
                rateLimiter.executeVertxFuture {
                    service.returnHelloWorld()
                }.asCompletableFuture().get()
            }

            try {
                rateLimiter.executeVertxFuture {
                    service.returnHelloWorld()
                }.asCompletableFuture().get()
                fail("RateLimiter에 걸려야 합니다.")
            } catch (e: Throwable) {
                // no op
            }

            metrics.availablePermissions shouldBeEqualTo 0
            metrics.numberOfWaitingThreads shouldBeEqualTo 0
            service.invocationCount shouldBeEqualTo RATE_LIMIT
        }

    @Test
    fun `decorate successful function`(vertx: Vertx, testContext: VertxTestContext) =
        withTestContext(testContext) {
            val rateLimiter = RateLimiter.of("test", noWaitConfig()).applyEventPublishing()
            val metrics = rateLimiter.metrics
            val service = VertxHelloWorldService()

            val decorated = rateLimiter.decorateVertxFuture {
                service.returnHelloWorld()
            }
            val result = decorated().asCompletableFuture().get()

            result shouldBeEqualTo "Hello world"
            metrics.availablePermissions shouldBeEqualTo RATE_LIMIT - 1
            metrics.numberOfWaitingThreads shouldBeEqualTo 0
            service.invocationCount shouldBeEqualTo 1
        }
}
