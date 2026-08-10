package io.bluetape4k.vertx.resilience4j

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.vertx.resilience4j.recover as recoverFuture
import io.bluetape4k.vertx.tests.withTestContext
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import org.junit.jupiter.api.Test

class VertxFutureSupportTest: AbstractVertxFutureTest() {

    @Test
    fun `future recover overloads handle matching and non matching exceptions`(
        vertx: Vertx,
        testContext: VertxTestContext,
    ) = withTestContext(testContext) {
        val failure = IllegalStateException("boom")

        with(Future.failedFuture<String>(failure)) {
            recoverFuture { "handler" }
        }
            .result() shouldBeEqualTo "handler"
        with(Future.failedFuture<String>(failure)) {
            recoverFuture(listOf(IllegalStateException::class.java)) { "iterable" }
        }
            .result() shouldBeEqualTo "iterable"
        with(Future.failedFuture<String>(failure)) {
            recoverFuture(IllegalStateException::class.java) { "single" }
        }
            .result() shouldBeEqualTo "single"

        val notRecovered = with(Future.failedFuture<String>(failure)) {
            recoverFuture(IllegalArgumentException::class.java) { "wrong" }
        }
        notRecovered.failed().shouldBeTrue()
        notRecovered.cause() shouldBeEqualTo failure

        val fallbackFailure = with(Future.failedFuture<String>(failure)) {
            recoverFuture(IllegalStateException::class.java) { throw IllegalArgumentException("fallback") }
        }
        fallbackFailure.failed().shouldBeTrue()
        fallbackFailure.cause() shouldBeInstanceOf IllegalArgumentException::class

        with(Future.succeededFuture("ok")) {
            recoverFuture(IllegalStateException::class.java) { "unused" }
        }
            .result() shouldBeEqualTo "ok"
    }

    @Test
    fun `supplier recover overloads cover result and exception handlers`(
        vertx: Vertx,
        testContext: VertxTestContext,
    ) = withTestContext(testContext) {
        val failure = IllegalStateException("boom")
        val failedSupplier: () -> Future<String> = { Future.failedFuture(failure) }

        with(failedSupplier) { recoverFuture { "handler" } }().result() shouldBeEqualTo "handler"
        with(failedSupplier) {
            recoverFuture { result: String?, error: Throwable? ->
                error?.message ?: result ?: "missing"
            }
        }().result() shouldBeEqualTo "boom"
        with(failedSupplier) {
            recoverFuture(IllegalStateException::class.java) { "single" }
        }().result() shouldBeEqualTo "single"
        with(failedSupplier) {
            recoverFuture(listOf(IllegalStateException::class.java)) { "iterable" }
        }().result() shouldBeEqualTo "iterable"

        val successfulSupplier: () -> Future<String> = { Future.succeededFuture("needs fallback") }
        val matching: (String) -> Boolean = { it.startsWith("needs") }
        with(successfulSupplier) { recoverFuture(matching) { "predicate" } }()
            .result() shouldBeEqualTo "predicate"
        val neverMatching: (String) -> Boolean = { false }
        with(successfulSupplier) { recoverFuture(neverMatching) { "unused" } }()
            .result() shouldBeEqualTo "needs fallback"

        val alwaysMatching: (String) -> Boolean = { true }
        val failureThroughPredicate = with(failedSupplier) { recoverFuture(alwaysMatching) { "unused" } }()
        failureThroughPredicate.failed().shouldBeTrue()
        failureThroughPredicate.succeeded().shouldBeFalse()
    }
}
