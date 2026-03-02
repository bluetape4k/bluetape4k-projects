/**
 * JVM `assert` 기반의 경량 assertion 유틸리티 모음입니다.
 *
 * ## 동작/계약
 * - 모든 함수는 Kotlin/JVM의 `assert(...) { ... }`를 사용합니다.
 * - JVM assertions가 비활성화된 경우(기본), 검증이 수행되지 않을 수 있습니다. 필요 시 `-ea` 옵션을 사용하세요.
 * - 실패 시 기본적으로 [AssertionError]가 발생합니다.
 */
@file:OptIn(ExperimentalContracts::class)
@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.support

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * null이 아닌 값을 보장합니다.
 *
 * ## 동작/계약
 * - `assert` 기반으로 동작하며, JVM assertions가 활성화되어야 합니다.
 * - 실패 시 [AssertionError]가 발생합니다.
 * - Kotlin contracts를 사용하여 스마트 캐스트 힌트를 제공합니다.
 *
 * ```kotlin
 * val value: String? = "hello"
 * val nonNullValue = value.assertNotNull("value")
 * // nonNullValue.length == 5
 * ```
 */
inline fun <T: Any> T?.assertNotNull(parameterName: String): T {
    contract {
        returns() implies (this@assertNotNull != null)
    }
    assert(this != null) { "$parameterName[$this] must not be null." }
    return this!!
}

/**
 * 값이 null임을 보장합니다.
 *
 * ## 동작/계약
 * - `assert` 기반으로 동작하며, JVM assertions가 활성화되어야 합니다.
 * - 실패 시 [AssertionError]가 발생합니다.
 *
 * ```kotlin
 * val value: String? = null
 * value.assertNull("value")
 * ```
 */
inline fun <T: Any> T?.assertNull(parameterName: String): T? {
    contract {
        returns() implies (this@assertNull == null)
    }
    assert(this == null) { "$parameterName[$this] must be null." }
    return this
}

/**
 * null이 아니고 빈 문자열이 아님을 보장합니다.
 *
 * ## 동작/계약
 * - `assert` 기반으로 동작하며, JVM assertions가 활성화되어야 합니다.
 * - 실패 시 [AssertionError]가 발생합니다.
 * - Kotlin contracts를 사용하여 스마트 캐스트 힌트를 제공합니다.
 *
 * ```kotlin
 * val text: String? = "hello"
 * val nonEmptyText = text.assertNotEmpty("text")
 * // nonEmptyText.length == 5
 * ```
 */
inline fun <T: CharSequence> T?.assertNotEmpty(parameterName: String): T {
    contract {
        returns() implies (this@assertNotEmpty != null)
    }
    val self = this.assertNotNull(parameterName)
    assert(self.isNotEmpty()) { "$parameterName[$self] must not be empty." }
    return self
}

/**
 * null이거나 빈 문자열임을 보장합니다.
 *
 * ## 동작/계약
 * - `assert` 기반으로 동작하며, JVM assertions가 활성화되어야 합니다.
 * - 실패 시 [AssertionError]가 발생합니다.
 * - Kotlin contracts를 사용하여 스마트 캐스트 힌트를 제공합니다.
 *
 * ```kotlin
 * val text1: String? = null
 * val text2: String? = ""
 * text1.assertNullOrEmpty("text1")
 * text2.assertNullOrEmpty("text2")
 * ```
 */
inline fun <T: CharSequence> T?.assertNullOrEmpty(parameterName: String): T? {
    contract {
        returns() implies (this@assertNullOrEmpty == null)
    }
    assert(this.isNullOrEmpty()) { "$parameterName[$this] must be null or empty." }
    return this
}

/**
 * null이 아니고 공백이 아닌 문자열임을 보장합니다.
 *
 * ## 동작/계약
 * - `assert` 기반으로 동작하며, JVM assertions가 활성화되어야 합니다.
 * - 실패 시 [AssertionError]가 발생합니다.
 * - Kotlin contracts를 사용하여 스마트 캐스트 힌트를 제공합니다.
 *
 * ```kotlin
 * val text: String? = "hello"
 * val nonBlankText = text.assertNotBlank("text")
 * // nonBlankText.trim() == "hello"
 * ```
 */
inline fun <T: CharSequence> T?.assertNotBlank(parameterName: String): T {
    contract {
        returns() implies (this@assertNotBlank != null)
    }
    val self = this.assertNotNull(parameterName)
    assert(self.isNotBlank()) { "$parameterName[$self] must not be blank." }
    return self
}

/**
 * null이거나 공백 문자열임을 보장합니다.
 *
 * ## 동작/계약
 * - `assert` 기반으로 동작하며, JVM assertions가 활성화되어야 합니다.
 * - 실패 시 [AssertionError]가 발생합니다.
 * - Kotlin contracts를 사용하여 스마트 캐스트 힌트를 제공합니다.
 *
 * ```kotlin
 * val text1: String? = null
 * val text2: String? = "   "
 * text1.assertNullOrBlank("text1")
 * text2.assertNullOrBlank("text2")
 * ```
 */
inline fun <T: CharSequence> T?.assertNullOrBlank(parameterName: String): T? {
    contract {
        returns() implies (this@assertNullOrBlank == null)
    }
    assert(this.isNullOrBlank()) { "$parameterName[$this] must be null or blank." }
    return this
}

/**
 * 지정한 문자열을 포함함을 보장합니다.
 *
 * ## 동작/계약
 * - `assert` 기반으로 동작하며, JVM assertions가 활성화되어야 합니다.
 * - 실패 시 [AssertionError]가 발생합니다.
 *
 * ```kotlin
 * val text: String? = "hello world"
 * text.assertContains("world", "text")
 * ```
 */
inline fun <T: CharSequence> T?.assertContains(other: CharSequence, parameterName: String): T {
    this.assertNotNull(parameterName)
    assert(this.contains(other)) { "$parameterName[$this] must contain $other" }
    return this
}

/**
 * 지정한 접두사로 시작함을 보장합니다.
 *
 * ## 동작/계약
 * - `assert` 기반으로 동작하며, JVM assertions가 활성화되어야 합니다.
 * - 실패 시 [AssertionError]가 발생합니다.
 *
 * ```kotlin
 * val text: String? = "hello world"
 * text.assertStartsWith("hello", "text")
 * ```
 */
inline fun <T: CharSequence> T?.assertStartsWith(
    prefix: CharSequence,
    parameterName: String,
    ignoreCase: Boolean = false,
): T {
    this.assertNotNull(parameterName)
    assert(this.startsWith(prefix, ignoreCase)) { "$parameterName[$this] must start with $prefix" }
    return this
}

/**
 * 지정한 접미사로 끝남을 보장합니다.
 *
 * ## 동작/계약
 * - `assert` 기반으로 동작하며, JVM assertions가 활성화되어야 합니다.
 * - 실패 시 [AssertionError]가 발생합니다.
 *
 * ```kotlin
 * val text: String? = "hello world"
 * text.assertEndsWith("world", "text")
 * ```
 */
inline fun <T: CharSequence> T?.assertEndsWith(
    suffix: CharSequence,
    parameterName: String,
    ignoreCase: Boolean = false,
): T {
    this.assertNotNull(parameterName)
    assert(this.endsWith(suffix, ignoreCase)) { "$parameterName[$this] must end with $suffix" }
    return this
}

/**
 * 값이 예상값과 같음을 보장합니다.
 *
 * ## 동작/계약
 * - `assert` 기반으로 동작하며, JVM assertions가 활성화되어야 합니다.
 * - 실패 시 [AssertionError]가 발생합니다.
 *
 * ```kotlin
 * val value = 5
 * value.assertEquals(5, "value")
 * ```
 */
inline fun <T> T.assertEquals(expected: T, parameterName: String): T = apply {
    assert(this == expected) { "$parameterName[$this] must be equal to $expected" }
}

/**
 * 값이 지정한 값보다 큼을 보장합니다.
 *
 * ## 동작/계약
 * - `assert` 기반으로 동작하며, JVM assertions가 활성화되어야 합니다.
 * - 실패 시 [AssertionError]가 발생합니다.
 *
 * ```kotlin
 * val value = 10
 * value.assertGt(5, "value")
 * ```
 */
inline fun <T: Comparable<T>> T.assertGt(expected: T, parameterName: String): T = apply {
    assert(this > expected) { "$parameterName[$this] must be greater than $expected." }
}

/**
 * 값이 지정한 값보다 크거나 같음을 보장합니다.
 *
 * ## 동작/계약
 * - `assert` 기반으로 동작하며, JVM assertions가 활성화되어야 합니다.
 * - 실패 시 [AssertionError]가 발생합니다.
 *
 * ```kotlin
 * val value = 5
 * value.assertGe(5, "value")
 * ```
 */
inline fun <T: Comparable<T>> T.assertGe(expected: T, parameterName: String): T = apply {
    assert(this >= expected) { "$parameterName[$this] must be greater than or equal to $expected." }
}

/**
 * 값이 지정한 값보다 작음을 보장합니다.
 *
 * ## 동작/계약
 * - `assert` 기반으로 동작하며, JVM assertions가 활성화되어야 합니다.
 * - 실패 시 [AssertionError]가 발생합니다.
 *
 * ```kotlin
 * val value = 3
 * value.assertLt(5, "value")
 * ```
 */
inline fun <T: Comparable<T>> T.assertLt(expected: T, parameterName: String): T = apply {
    assert(this < expected) { "$parameterName[$this] must be less than $expected." }
}

/**
 * 값이 지정한 값보다 작거나 같음을 보장합니다.
 *
 * ## 동작/계약
 * - `assert` 기반으로 동작하며, JVM assertions가 활성화되어야 합니다.
 * - 실패 시 [AssertionError]가 발생합니다.
 *
 * ```kotlin
 * val value = 5
 * value.assertLe(5, "value")
 * ```
 */
inline fun <T: Comparable<T>> T.assertLe(expected: T, parameterName: String): T = apply {
    assert(this <= expected) { "$parameterName[$this] must be less than or equal to $expected." }
}

/**
 * 값이 지정한 닫힌 범위 내에 있음을 보장합니다.
 *
 * ## 동작/계약
 * - `assert` 기반으로 동작하며, JVM assertions가 활성화되어야 합니다.
 * - 실패 시 [AssertionError]가 발생합니다.
 * - 범위는 `start..endInclusive` 형태의 닫힌 범위입니다.
 *
 * ```kotlin
 * val value = 5
 * value.assertInRange(1, 10, "value")
 * ```
 */
inline fun <T: Comparable<T>> T.assertInRange(start: T, endInclusive: T, parameterName: String) = apply {
    assert(this in start..endInclusive) { "$parameterName[$this] must be in range ($start .. $endInclusive)" }
}

/**
 * 값이 지정한 열린 범위 내에 있음을 보장합니다.
 *
 * ## 동작/계약
 * - `assert` 기반으로 동작하며, JVM assertions가 활성화되어야 합니다.
 * - 실패 시 [AssertionError]가 발생합니다.
 * - 범위는 `start..<endExclusive` 형태의 열린 범위입니다.
 *
 * ```kotlin
 * val value = 5
 * value.assertInOpenRange(1, 10, "value")
 * ```
 */
inline fun <T: Comparable<T>> T.assertInOpenRange(start: T, endExclusive: T, name: String): T = apply {
    assert(this in start..<endExclusive) { "$start <= $name[$this] < $endExclusive" }
}

/**
 * 값이 0 이상임을 보장합니다. 내부적으로 `toDouble()`로 비교합니다.
 *
 * ## 동작/계약
 * - `assert` 기반으로 동작하며, JVM assertions가 활성화되어야 합니다.
 * - 실패 시 [AssertionError]가 발생합니다.
 * - NaN, Infinity 등의 특수 값에 주의해야 합니다.
 *
 * ```kotlin
 * val value: Int = 0
 * value.assertZeroOrPositiveNumber("value")
 * ```
 */
inline fun <T> T.assertZeroOrPositiveNumber(parameterName: String): T where T: Number, T: Comparable<T> = apply {
    toDouble().assertGe(0.0, parameterName)
}

/**
 * 값이 0 초과임을 보장합니다. 내부적으로 `toDouble()`로 비교합니다.
 *
 * ## 동작/계약
 * - `assert` 기반으로 동작하며, JVM assertions가 활성화되어야 합니다.
 * - 실패 시 [AssertionError]가 발생합니다.
 * - NaN, Infinity 등의 특수 값에 주의해야 합니다.
 *
 * ```kotlin
 * val value: Int = 1
 * value.assertPositiveNumber("value")
 * ```
 */
inline fun <T> T.assertPositiveNumber(parameterName: String): T where T: Number, T: Comparable<T> = apply {
    toDouble().assertGt(0.0, parameterName)
}

/**
 * 값이 0 이하임을 보장합니다. 내부적으로 `toDouble()`로 비교합니다.
 *
 * ## 동작/계약
 * - `assert` 기반으로 동작하며, JVM assertions가 활성화되어야 합니다.
 * - 실패 시 [AssertionError]가 발생합니다.
 * - NaN, Infinity 등의 특수 값에 주의해야 합니다.
 *
 * ```kotlin
 * val value: Int = 0
 * value.assertZeroOrNegativeNumber("value")
 * ```
 */
inline fun <T> T.assertZeroOrNegativeNumber(parameterName: String): T where T: Number, T: Comparable<T> = apply {
    toDouble().assertLe(0.0, parameterName)
}

/**
 * 값이 0 미만임을 보장합니다. 내부적으로 `toDouble()`로 비교합니다.
 *
 * ## 동작/계약
 * - `assert` 기반으로 동작하며, JVM assertions가 활성화되어야 합니다.
 * - 실패 시 [AssertionError]가 발생합니다.
 * - NaN, Infinity 등의 특수 값에 주의해야 합니다.
 *
 * ```kotlin
 * val value: Int = -1
 * value.assertNegativeNumber("value")
 * ```
 */
inline fun <T> T.assertNegativeNumber(parameterName: String): T where T: Number, T: Comparable<T> = apply {
    toDouble().assertLt(0.0, parameterName)
}

/**
 * 컬렉션이 null이 아니고 비어있지 않음을 보장합니다.
 *
 * ## 동작/계약
 * - `assert` 기반으로 동작하며, JVM assertions가 활성화되어야 합니다.
 * - null이거나 비어있으면 실패합니다.
 * - 실패 시 [AssertionError]가 발생합니다.
 *
 * ```kotlin
 * val list: List<Int>? = listOf(1, 2, 3)
 * list.assertNotEmpty("list")
 * ```
 */
inline fun <T> Collection<T>?.assertNotEmpty(parameterName: String) = apply {
    assert(!this.isNullOrEmpty()) { "$parameterName[$this] must not be null or empty." }
}

/**
 * 맵이 null이 아니고 비어있지 않음을 보장합니다.
 *
 * ## 동작/계약
 * - `assert` 기반으로 동작하며, JVM assertions가 활성화되어야 합니다.
 * - null이거나 비어있으면 실패합니다.
 * - 실패 시 [AssertionError]가 발생합니다.
 *
 * ```kotlin
 * val map: Map<String, Int>? = mapOf("a" to 1)
 * map.assertNotEmpty("map")
 * ```
 */
inline fun <K, V> Map<K, V>?.assertNotEmpty(parameterName: String) = apply {
    assert(!this.isNullOrEmpty()) { "$parameterName must not be null or empty." }
}

/**
 * 맵이 null이 아니고 비어있지 않으며, 지정한 키를 포함함을 보장합니다.
 *
 * ## 동작/계약
 * - `assert` 기반으로 동작하며, JVM assertions가 활성화되어야 합니다.
 * - null이거나 비어있거나 키가 없으면 실패합니다.
 * - 실패 시 [AssertionError]가 발생합니다.
 *
 * ```kotlin
 * val map: Map<String, Int>? = mapOf("key" to 1)
 * map.assertHasKey("key", "map")
 * ```
 */
inline fun <K, V> Map<K, V>?.assertHasKey(key: K, parameterName: String): Map<K, V> {
    assertNotEmpty(parameterName)
    assert(this!!.containsKey(key)) { "$parameterName must contain key $key" }
    return this
}

/**
 * 맵이 null이 아니고 비어있지 않으며, 지정한 값을 포함함을 보장합니다.
 *
 * ## 동작/계약
 * - `assert` 기반으로 동작하며, JVM assertions가 활성화되어야 합니다.
 * - null이거나 비어있거나 값이 없으면 실패합니다.
 * - 실패 시 [AssertionError]가 발생합니다.
 *
 * ```kotlin
 * val map: Map<String, Int>? = mapOf("key" to 1)
 * map.assertHasValue(1, "map")
 * ```
 */
inline fun <K, V> Map<K, V>?.assertHasValue(value: V, parameterName: String): Map<K, V> {
    assertNotEmpty(parameterName)
    assert(this!!.containsValue(value)) { "$parameterName must contain value $value" }
    return this
}

/**
 * 맵이 null이 아니고 비어있지 않으며, 지정한 키-값 쌍을 포함함을 보장합니다.
 *
 * ## 동작/계약
 * - `assert` 기반으로 동작하며, JVM assertions가 활성화되어야 합니다.
 * - null이거나 비어있거나 키-값 쌍이 없으면 실패합니다.
 * - 실패 시 [AssertionError]가 발생합니다.
 *
 * ```kotlin
 * val map: Map<String, Int>? = mapOf("key" to 1)
 * map.assertContains("key", 1, "map")
 * ```
 */
inline fun <K, V> Map<K, V>?.assertContains(key: K, value: V, parameterName: String): Map<K, V> {
    assertNotEmpty(parameterName)
    assert(this!![key] == value) { "$parameterName must contain ($key, $value)" }
    return this
}
