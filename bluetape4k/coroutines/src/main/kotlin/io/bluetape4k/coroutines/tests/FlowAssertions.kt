package io.bluetape4k.coroutines.tests

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import kotlin.test.assertFailsWith

/**
 * Flow가 아무 요소도 방출하지 않는지 검증합니다.
 *
 * ## 동작/계약
 * - Flow를 끝까지 수집해 결과 리스트가 비어있는지 확인합니다.
 * - 값이 하나라도 있으면 assertion 실패 예외를 던집니다.
 * - 수신 Flow를 수집(consuming)하므로 재수집 가능한지 여부는 원본 Flow 성질을 따릅니다.
 *
 * ```kotlin
 * emptyFlow<Int>().assertEmpty()
 * // assertion passed
 * ```
 */
@Deprecated(
    message = "Moved to io.bluetape4k.assertions.coroutines. Use assertEmpty() instead.",
    replaceWith = ReplaceWith(
        "assertEmpty()",
        "io.bluetape4k.assertions.coroutines.assertEmpty"
    ),
    level = DeprecationLevel.WARNING
)
suspend inline fun <T> Flow<T>.assertEmpty() {
    toList().shouldBeEmpty()
}

/**
 * Flow 결과가 기대 Flow 결과와 동일한지 검증합니다.
 *
 * ## 동작/계약
 * - 두 Flow를 모두 리스트로 수집한 뒤 순서/값을 비교합니다.
 * - 다르면 assertion 실패 예외를 던집니다.
 * - 두 Flow 모두 수집(consuming)됩니다.
 *
 * ```kotlin
 * flowOf(1, 2, 3).assertResult(flowOf(1, 2, 3))
 * // assertion passed
 * ```
 * @param expected 기대 결과 Flow입니다.
 */
@Deprecated(
    message = "Moved to io.bluetape4k.assertions.coroutines. Use assertResult(expected: Flow<T>) instead.",
    replaceWith = ReplaceWith(
        "assertResult(expected)",
        "io.bluetape4k.assertions.coroutines.assertResult"
    ),
    level = DeprecationLevel.WARNING
)
suspend inline fun <T> Flow<T>.assertResult(expected: Flow<T>) {
    toList() shouldBeEqualTo expected.toList()
}

/**
 * Flow 결과가 기대 값 시퀀스와 동일한지 검증합니다.
 *
 * ## 동작/계약
 * - Flow 결과를 리스트로 수집해 [values]와 순서까지 비교합니다.
 * - 다르면 assertion 실패 예외를 던집니다.
 * - 수신 Flow를 수집(consuming)합니다.
 *
 * ```kotlin
 * flowOf(1, 2, 3).assertResult(1, 2, 3)
 * // assertion passed
 * ```
 * @param values 기대 값 목록입니다.
 */
@Deprecated(
    message = "Moved to io.bluetape4k.assertions.coroutines. Use assertResult(*values) instead.",
    replaceWith = ReplaceWith(
        "assertResult(*values)",
        "io.bluetape4k.assertions.coroutines.assertResult"
    ),
    level = DeprecationLevel.WARNING
)
suspend inline fun <T> Flow<T>.assertResult(vararg values: T) {
    toList() shouldBeEqualTo values.toList()
}

/**
 * Flow 결과 집합이 기대 값 집합과 동일한지 검증합니다(순서 무시).
 *
 * ## 동작/계약
 * - Flow 결과를 `Set`으로 변환해 [values]와 비교합니다.
 * - 중복 요소는 집합 비교 과정에서 제거됩니다.
 * - 다르면 assertion 실패 예외를 던집니다.
 *
 * ```kotlin
 * flowOf(2, 1, 2).assertResultSet(1, 2)
 * // assertion passed
 * ```
 * @param values 기대 값 목록입니다.
 */
@Deprecated(
    message = "Moved to io.bluetape4k.assertions.coroutines. Use assertResultSet(*values) instead.",
    replaceWith = ReplaceWith(
        "assertResultSet(*values)",
        "io.bluetape4k.assertions.coroutines.assertResultSet"
    ),
    level = DeprecationLevel.WARNING
)
suspend inline fun <T> Flow<T>.assertResultSet(vararg values: T) {
    toList().toSet() shouldBeEqualTo values.toSet()
}

/**
 * Flow 결과 집합이 기대 iterable 집합과 동일한지 검증합니다(순서 무시).
 *
 * ## 동작/계약
 * - Flow 결과와 [values]를 각각 집합으로 변환해 비교합니다.
 * - 중복 요소는 집합 비교 과정에서 제거됩니다.
 * - 다르면 assertion 실패 예외를 던집니다.
 *
 * ```kotlin
 * flowOf(2, 1, 2).assertResultSet(listOf(1, 2))
 * // assertion passed
 * ```
 * @param values 기대 값 iterable입니다.
 */
@Deprecated(
    message = "Moved to io.bluetape4k.assertions.coroutines. Use assertResultSet(values) instead.",
    replaceWith = ReplaceWith(
        "assertResultSet(values)",
        "io.bluetape4k.assertions.coroutines.assertResultSet"
    ),
    level = DeprecationLevel.WARNING
)
suspend inline fun <T> Flow<T>.assertResultSet(values: Iterable<T>) {
    toList().toSet() shouldBeEqualTo values.toSet()
}

/**
 * Flow가 특정 예외 타입으로 실패하는지와 실패 전 방출값을 함께 검증합니다.
 *
 * ## 동작/계약
 * - Flow 수집 중 [E] 예외가 발생해야 assertion이 통과합니다.
 * - 예외 발생 전까지 수집된 값이 [values]와 같아야 합니다.
 * - 예외가 없거나 타입이 다르면 assertion 실패 예외를 던집니다.
 *
 * ```kotlin
 * failingFlow.assertFailure<Int, IllegalStateException>(1, 2)
 * // emitted == [1, 2] then throws IllegalStateException
 * ```
 * @param values 예외 발생 전 기대되는 방출값입니다.
 */
@Deprecated(
    message = "Moved to io.bluetape4k.assertions.coroutines. Use assertFailure<T, E>(*values) instead.",
    replaceWith = ReplaceWith(
        "assertFailure(*values)",
        "io.bluetape4k.assertions.coroutines.assertFailure"
    ),
    level = DeprecationLevel.WARNING
)
suspend inline fun <T, reified E: Throwable> Flow<T>.assertFailure(vararg values: T) {
    val list = mutableListOf<T>()
    assertFailsWith<E> {
        this@assertFailure.toList(list)
    }
    list shouldBeEqualTo values.toList()
}

/**
 * Flow가 지정한 예외 타입을 방출하는지 검증합니다.
 *
 * ## 동작/계약
 * - `catch`에서 포착된 예외가 [E] 타입인지 검증합니다.
 * - 예외가 발생하지 않으면 assertion 실패 예외를 던질 수 있습니다.
 * - 수신 Flow를 수집(consuming)합니다.
 *
 * ```kotlin
 * failingFlow.assertError<IllegalStateException>()
 * // assertion passed
 * ```
 */
@Deprecated(
    message = "Moved to io.bluetape4k.assertions.coroutines. Use assertError<E>() instead.",
    replaceWith = ReplaceWith(
        "assertError()",
        "io.bluetape4k.assertions.coroutines.assertError"
    ),
    level = DeprecationLevel.WARNING
)
suspend inline fun <reified E: Throwable> Flow<*>.assertError() {
    var caught = false
    this.catch { e ->
        e shouldBeInstanceOf E::class
        caught = true
    }.collect()
    caught shouldBeEqualTo true
}
