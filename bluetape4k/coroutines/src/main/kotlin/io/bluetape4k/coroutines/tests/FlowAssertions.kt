package io.bluetape4k.coroutines.tests

import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.collections.eclipse.toUnifiedSet
import io.bluetape4k.coroutines.flow.extensions.toFastList
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
    toFastList().shouldBeEmpty()
}

/**
 * Flow 요소가 [expected] 와 동일한지 검사합니다.
 */
suspend inline fun <T> Flow<T>.assertResult(expected: Flow<T>) {
    toFastList() shouldBeEqualTo expected.toFastList()
}

/**
 * Flow 요소가 [values] 와 동일한지 검사합니다.
 */
suspend inline fun <T> Flow<T>.assertResult(vararg values: T) {
    toFastList() shouldBeEqualTo values.toFastList()
}

/**
 * Flow 요소들을 [values] 와 Set 형태로 비교하여 동일한지 검사합니다.
 */
suspend inline fun <T> Flow<T>.assertResultSet(vararg values: T) {
    toFastList().toUnifiedSet() shouldBeEqualTo values.toUnifiedSet()
}

/**
 * Flow 요소들을 [values] 와 Set 형태로 비교하여 동일한지 검사합니다.
 */
suspend inline fun <T> Flow<T>.assertResultSet(values: Iterable<T>) {
    toFastList().toUnifiedSet() shouldBeEqualTo values.toUnifiedSet()
}

/**
 * Flow 요소에 [E] 타입의 에러가 발생하고, 다른 요소들은 [values] 와 동일한지 검사합니다.
 */
suspend inline fun <T, reified E: Throwable> Flow<T>.assertFailure(vararg values: T) {
    val list = fastListOf<T>()
    assertFailsWith<E> {
        this@assertFailure.toList(list)
    }
    list shouldBeEqualTo values.toList()
}

/**
 * Flow 요소에 [E] 타입의 에러가 발생하는지 확인합니다.
 */
suspend inline fun <reified E: Throwable> Flow<*>.assertError() {
    this.catch { it shouldBeInstanceOf E::class }.collect()
}
