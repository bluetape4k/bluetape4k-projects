package io.bluetape4k.vertx.resilience4j

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.vertx.asCompletableFuture
import io.bluetape4k.vertx.tests.withTestContext
import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.timelimiter.TimeLimiter
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class VertxDecoratorsOverloadTest: AbstractVertxFutureTest() {

    @Test
    fun `builder applies all resilience components and fallback handlers`(
        vertx: Vertx,
        testContext: VertxTestContext,
    ) = withTestContext(testContext) {
        val plain = VertxDecorators.ofSupplier { Future.succeededFuture("plain") }
        plain.invoke().result() shouldBeEqualTo "plain"

        val decorated = VertxDecorators.ofSupplier { Future.succeededFuture("decorated") }
            .withBulkhead(Bulkhead.ofDefaults("builder-bulkhead"))
            .withCircuitBreaker(CircuitBreaker.ofDefaults("builder-circuit"))
            .withRateLimiter(RateLimiter.ofDefaults("builder-rate"))
            .withTimeLimiter(TimeLimiter.ofDefaults("builder-time"))
            .invoke().asCompletableFuture().get(5, TimeUnit.SECONDS)
        decorated shouldBeEqualTo "decorated"

        val exceptionHandler: (Throwable?) -> String = { "exception" }
        VertxDecorators.ofSupplier<String> { Future.failedFuture(IllegalStateException("boom")) }
            .withFallback(exceptionHandler)
            .invoke().result() shouldBeEqualTo "exception"

        val resultAndErrorHandler: (String?, Throwable?) -> String = { result, error ->
            result ?: error?.message ?: "missing"
        }
        VertxDecorators.ofSupplier<String> { Future.failedFuture(IllegalStateException("both")) }
            .withFallback(resultAndErrorHandler)
            .invoke().result() shouldBeEqualTo "both"

        val resultPredicate: (String) -> Boolean = { it == "original" }
        val resultHandler: (String) -> String = { "predicate" }
        VertxDecorators.ofSupplier { Future.succeededFuture("original") }
            .withFallback(resultPredicate, resultHandler)
            .invoke().result() shouldBeEqualTo "predicate"

        VertxDecorators.ofSupplier<String> { Future.failedFuture(IllegalStateException("single")) }
            .withFallback(IllegalStateException::class.java, exceptionHandler)
            .invoke().result() shouldBeEqualTo "exception"

        val exceptionTypes: Iterable<Class<out Throwable>> = listOf(IllegalStateException::class.java)
        VertxDecorators.ofSupplier<String> { Future.failedFuture(IllegalStateException("iterable")) }
            .withFallback(exceptionTypes, exceptionHandler)
            .invoke().result() shouldBeEqualTo "exception"
    }
}
