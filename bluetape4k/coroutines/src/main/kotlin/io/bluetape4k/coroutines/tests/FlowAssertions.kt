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
 * Flow 에 요소가 없음을 검사합니다.
 */
suspend inline fun <T> Flow<T>.assertEmpty() {
    toList().shouldBeEmpty()
}

/**
 * Flow 요소가 [expected] 와 동일한지 검사합니다.
 */
suspend inline fun <T> Flow<T>.assertResult(expected: Flow<T>) {
    toList() shouldBeEqualTo expected.toList()
}

/**
 * Flow 요소가 [values] 와 동일한지 검사합니다.
 */
suspend inline fun <T> Flow<T>.assertResult(vararg values: T) {
    toList() shouldBeEqualTo values.toList()
}

/**
 * Flow 요소들을 [values] 와 Set 형태로 비교하여 동일한지 검사합니다.
 */
suspend inline fun <T> Flow<T>.assertResultSet(vararg values: T) {
    toList().toSet() shouldBeEqualTo values.toSet()
}

/**
 * Flow 요소들을 [values] 와 Set 형태로 비교하여 동일한지 검사합니다.
 */
suspend inline fun <T> Flow<T>.assertResultSet(values: Iterable<T>) {
    toList().toSet() shouldBeEqualTo values.toSet()
}

/**
 * Flow 요소에 [E] 타입의 에러가 발생하고, 다른 요소들은 [values] 와 동일한지 검사합니다.
 */
suspend inline fun <T, reified E: Throwable> Flow<T>.assertFailure(vararg values: T) {
    val list = mutableListOf<T>()
    assertFailsWith<E> {
        toList(list)
    }
    list shouldBeEqualTo values.toList()
}

/**
 * Flow 요소에 [E] 타입의 에러가 발생하는지 확인합니다.
 */
suspend inline fun <reified E: Throwable> Flow<*>.assertError() {
    this.catch { it shouldBeInstanceOf E::class }.collect()
}
