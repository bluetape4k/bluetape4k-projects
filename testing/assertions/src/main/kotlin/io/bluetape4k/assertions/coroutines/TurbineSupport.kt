package io.bluetape4k.assertions.coroutines

import app.cash.turbine.ReceiveTurbine
import io.bluetape4k.assertions.internal.Failures
import io.bluetape4k.assertions.shouldBeEqualTo

/**
 * Awaits next item and asserts it equals [expected].
 * Requires Turbine: `testImplementation(libs.turbine)`.
 */
suspend fun <T> ReceiveTurbine<T>.awaitItemAndAssert(expected: T): T {
    val actual = awaitItem()
    actual shouldBeEqualTo expected
    return actual
}

/**
 * Awaits next item and asserts [predicate] is true.
 * Requires Turbine: `testImplementation(libs.turbine)`.
 */
suspend fun <T> ReceiveTurbine<T>.awaitItemMatching(predicate: (T) -> Boolean): T {
    val actual = awaitItem()
    if (!predicate(actual)) {
        Failures.fail("Awaited item did not match predicate: $actual")
    }
    return actual
}

/**
 * Awaits error and asserts it is of type [E].
 * Requires Turbine: `testImplementation(libs.turbine)`.
 */
suspend inline fun <reified E : Throwable> ReceiveTurbine<*>.awaitErrorOfType(): E {
    val error = awaitError()
    if (error !is E) {
        Failures.fail("Expected error of type ${E::class.simpleName}, but was ${error::class.simpleName}: ${error.message}")
    }
    return error as E
}
