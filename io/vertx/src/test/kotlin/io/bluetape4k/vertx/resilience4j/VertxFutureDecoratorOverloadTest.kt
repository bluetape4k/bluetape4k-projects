package io.bluetape4k.vertx.resilience4j

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.vertx.asCompletableFuture
import io.bluetape4k.vertx.tests.withTestContext
import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.timelimiter.TimeLimiter
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class VertxFutureDecoratorOverloadTest: AbstractVertxFutureTest() {

    @Test
    fun `bulkhead and rate limiter execute and decorate Vertx futures`(
        vertx: Vertx,
        testContext: VertxTestContext,
    ) = withTestContext(testContext) {
        val bulkhead = Bulkhead.ofDefaults("bulkhead-overload")
        bulkhead.executeVertxFuture { Future.succeededFuture("bulk-execute") }
            .result() shouldBeEqualTo "bulk-execute"
        bulkhead.decorateVertxFuture { Future.succeededFuture("bulk-decorate") }
            .invoke().result() shouldBeEqualTo "bulk-decorate"

        val rateLimiter = RateLimiter.ofDefaults("rate-limiter-overload")
        rateLimiter.executeVertxFuture { Future.succeededFuture("rate-execute") }
            .result() shouldBeEqualTo "rate-execute"
        rateLimiter.decorateVertxFuture { Future.succeededFuture("rate-decorate") }
            .invoke().result() shouldBeEqualTo "rate-decorate"
    }

    @Test
    fun `time limiter execute and decorate complete a successful future`(
        vertx: Vertx,
        testContext: VertxTestContext,
    ) = withTestContext(testContext) {
        val timeLimiter = TimeLimiter.ofDefaults("time-limiter-overload")

        val executed = timeLimiter.executeVertxFuture { Future.succeededFuture("time-execute") }
            .asCompletableFuture().get(5, TimeUnit.SECONDS)
        executed shouldBeEqualTo "time-execute"

        val decorated = timeLimiter.decorateVertxFuture { Future.succeededFuture("time-decorate") }
            .invoke().asCompletableFuture().get(5, TimeUnit.SECONDS)
        decorated shouldBeEqualTo "time-decorate"
    }
}
