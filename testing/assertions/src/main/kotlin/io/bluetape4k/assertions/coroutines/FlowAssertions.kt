package io.bluetape4k.assertions.coroutines

import io.bluetape4k.assertions.internal.Failures
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList

/**
 * Assert that a Flow emits no values.
 */
suspend fun <T> Flow<T>.assertEmpty() {
    val items = toList()
    if (items.isNotEmpty()) {
        Failures.fail("Expected Flow to be empty, but had ${items.size} items: $items")
    }
}

/**
 * Assert that this Flow emits the same values, in the same order, as [expected].
 */
suspend fun <T> Flow<T>.assertResult(expected: Flow<T>) {
    val actual = toList()
    val expectedList = expected.toList()
    if (actual != expectedList) {
        Failures.failComparison(
            "Flow results differ",
            expectedList,
            actual
        )
    }
}

/**
 * Assert that this Flow emits exactly [values], in order.
 */
suspend fun <T> Flow<T>.assertResult(vararg values: T) {
    val actual = toList()
    val expected = values.toList()
    if (actual != expected) {
        Failures.failComparison(
            "Flow results differ (order-sensitive)",
            expected,
            actual
        )
    }
}

/**
 * Assert that this Flow emits exactly [values], regardless of order.
 */
suspend fun <T> Flow<T>.assertResultSet(vararg values: T) {
    assertResultSet(values.toList())
}

/**
 * Assert that this Flow emits exactly [values], regardless of order.
 */
suspend fun <T> Flow<T>.assertResultSet(values: Iterable<T>) {
    val actual = toList()
    val expected = values.toList()
    if (actual.size != expected.size || actual.toSet() != expected.toSet()) {
        Failures.failComparison(
            "Flow result sets differ (order-insensitive)",
            expected.sortedBy { it.toString() },
            actual.sortedBy { it.toString() }
        )
    }
}

/**
 * Assert that this Flow emits exactly [values] (in order) and then fails with [E].
 */
suspend inline fun <T, reified E : Throwable> Flow<T>.assertFailure(vararg values: T) {
    val collected = mutableListOf<T>()
    var caught: Throwable? = null
    try {
        collect { item -> collected.add(item) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        caught = e
    }

    val expected = values.toList()
    if (collected != expected) {
        Failures.failComparison(
            "Flow emitted values before failure differ",
            expected,
            collected
        )
    }

    if (caught == null) {
        Failures.fail("Expected Flow to throw ${E::class.simpleName}, but completed normally")
    }
    if (caught !is E) {
        Failures.fail(
            "Expected Flow to throw ${E::class.simpleName}, " +
                "but threw ${caught::class.simpleName}: ${caught.message}"
        )
    }
}

/**
 * Assert that this Flow fails with [E] (without inspecting emitted values).
 */
suspend inline fun <reified E : Throwable> Flow<*>.assertError() {
    var caught: Throwable? = null
    try {
        collect { }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        caught = e
    }

    if (caught == null) {
        Failures.fail("Expected Flow to throw ${E::class.simpleName}, but completed normally")
    }
    if (caught !is E) {
        Failures.fail(
            "Expected Flow to throw ${E::class.simpleName}, " +
                "but threw ${caught::class.simpleName}: ${caught.message}"
        )
    }
}
