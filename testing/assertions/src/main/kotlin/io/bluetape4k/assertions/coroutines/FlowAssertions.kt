package io.bluetape4k.assertions.coroutines

import io.bluetape4k.assertions.internal.Failures
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList

/**
 * Flow가 값을 방출하지 않는지 검증한다.
 *
 * @receiver 검증할 Flow
 * @throws org.opentest4j.AssertionFailedError Flow가 값을 방출한 경우
 */
suspend fun <T> Flow<T>.assertEmpty() {
    val items = toList()
    if (items.isNotEmpty()) {
        Failures.fail("Expected Flow to be empty, but had ${items.size} items: $items")
    }
}

/**
 * 이 Flow가 [expected] Flow와 같은 값을 같은 순서로 방출하는지 검증한다.
 *
 * @receiver 검증할 Flow
 * @param expected 기대하는 Flow
 * @throws org.opentest4j.AssertionFailedError 결과가 다른 경우
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
 * 이 Flow가 [values]를 정확히 같은 순서로 방출하는지 검증한다.
 *
 * @receiver 검증할 Flow
 * @param values 기대하는 값들 (순서 중요)
 * @throws org.opentest4j.AssertionFailedError 결과가 다른 경우
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
 * 이 Flow가 [values]를 정확히 방출하는지 검증한다 (순서 무관).
 *
 * @receiver 검증할 Flow
 * @param values 기대하는 값들 (순서 무관, 집합 비교)
 * @throws org.opentest4j.AssertionFailedError 결과 집합이 다른 경우
 */
suspend fun <T> Flow<T>.assertResultSet(vararg values: T) {
    assertResultSet(values.toList())
}

/**
 * 이 Flow가 [values]를 정확히 방출하는지 검증한다 (순서 무관).
 *
 * @receiver 검증할 Flow
 * @param values 기대하는 값들 (순서 무관, 집합 비교)
 * @throws org.opentest4j.AssertionFailedError 결과 집합이 다른 경우
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
 * 이 Flow가 [values]를 정확히 방출한 후 [E] 타입 예외로 실패하는지 검증한다.
 *
 * [CancellationException]은 catch하지 않고 즉시 rethrow한다 (코루틴 취소 보존).
 *
 * @receiver 검증할 Flow
 * @param values 기대하는 값들 (순서 중요)
 * @throws org.opentest4j.AssertionFailedError 예상과 다른 경우
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
 * 이 Flow가 [E] 타입 예외로 실패하는지 검증한다 (방출된 값은 검사하지 않음).
 *
 * [CancellationException]은 catch하지 않고 즉시 rethrow한다 (코루틴 취소 보존).
 *
 * @receiver 검증할 Flow
 * @throws org.opentest4j.AssertionFailedError 예상과 다른 경우
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
