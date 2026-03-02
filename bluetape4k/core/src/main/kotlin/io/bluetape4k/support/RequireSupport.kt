@file:OptIn(ExperimentalContracts::class)
@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.support

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * requireNotNull 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::requireNotNull
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T: Any> T?.requireNotNull(parameterName: String): T {
    contract {
        returns() implies (this@requireNotNull != null)
    }
    require(this != null) { "$parameterName[$this] must not be null." }
    return this
}

/**
 * requireNull 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::requireNull
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T: Any> T?.requireNull(parameterName: String): T? {
    contract {
        returns() implies (this@requireNull == null)
    }
    require(this == null) { "$parameterName[$this] must be null." }
    return this
}

/**
 * requireNotEmpty 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::requireNotEmpty
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T: CharSequence> T?.requireNotEmpty(parameterName: String): T {
    contract {
        returnsNotNull() implies (this@requireNotEmpty != null)
    }
    val self = this.requireNotNull(parameterName)
    require(self.isNotEmpty()) { "$parameterName[$self] must not be empty." }
    return self
}

/**
 * requireNullOrEmpty 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::requireNullOrEmpty
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T: CharSequence> T?.requireNullOrEmpty(parameterName: String): T? {
    contract {
        returns() implies (this@requireNullOrEmpty == null)
    }
    require(this.isNullOrEmpty()) { "$parameterName[$this] must be null or empty." }
    return this
}

/**
 * requireNotBlank 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::requireNotBlank
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T: CharSequence> T?.requireNotBlank(parameterName: String): T {
    contract {
        returnsNotNull() implies (this@requireNotBlank != null)
    }
    val self = this.requireNotNull(parameterName)
    require(self.isNotBlank()) { "$parameterName[$self] must not be blank." }
    return self
}

/**
 * requireNullOrBlank 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::requireNullOrBlank
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T: CharSequence> T?.requireNullOrBlank(parameterName: String): T? {
    contract {
        returns() implies (this@requireNullOrBlank == null)
    }
    require(this.isNullOrBlank()) { "$parameterName[$this] must be null or blank." }
    return this
}


/**
 * requireContains 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::requireContains
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T: CharSequence> T?.requireContains(other: CharSequence, parameterName: String): T {
    this.requireNotNull(parameterName)
    require(this.contains(other)) { "$parameterName[$this] must contain $other" }
    return this
}

/**
 * requireStartsWith 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::requireStartsWith
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T: CharSequence> T?.requireStartsWith(
    prefix: CharSequence,
    parameterName: String,
    ignoreCase: Boolean = false,
): T {
    this.requireNotNull(parameterName)
    require(this.startsWith(prefix, ignoreCase)) { "$parameterName[$this] must start with $prefix" }
    return this
}

/**
 * requireEndsWith 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::requireEndsWith
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T: CharSequence> T?.requireEndsWith(
    suffix: CharSequence,
    parameterName: String,
    ignoreCase: Boolean = false,
): T {
    this.requireNotNull(parameterName)
    require(this.endsWith(suffix, ignoreCase)) { "$parameterName[$this] must end with $suffix" }
    return this
}

/**
 * requireEquals 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::requireEquals
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T> T.requireEquals(expected: T, parameterName: String): T = apply {
    require(this == expected) { "$parameterName[$this] must be equal to $expected" }
}

/**
 * requireGt 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::requireGt
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T: Comparable<T>> T.requireGt(expected: T, parameterName: String): T = apply {
    require(this > expected) { "$parameterName[$this] must be greater than $expected." }
}

/**
 * requireGe 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::requireGe
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T: Comparable<T>> T.requireGe(expected: T, parameterName: String): T = apply {
    require(this >= expected) { "$parameterName[$this] must be greater than or equal to $expected." }
}

/**
 * requireLt 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::requireLt
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T: Comparable<T>> T.requireLt(expected: T, parameterName: String): T = apply {
    require(this < expected) { "$parameterName[$this] must be less than $expected." }
}

/**
 * requireLe 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::requireLe
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T: Comparable<T>> T.requireLe(expected: T, parameterName: String): T = apply {
    require(this <= expected) { "$parameterName[$this] must be less than or equal to $expected." }
}

/**
 * requireInRange 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::requireInRange
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T: Comparable<T>> T.requireInRange(start: T, endInclusive: T, parameterName: String) = apply {
    require(this in start..endInclusive) { "$parameterName[$this] must be in range ($start .. $endInclusive)" }
}

/**
 * requireInOpenRange 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::requireInOpenRange
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T: Comparable<T>> T.requireInOpenRange(start: T, endExclusive: T, parameterName: String): T = apply {
    require(this in start..<endExclusive) { "$start <= $parameterName[$this] < $endExclusive" }
}

/**
 * requireZeroOrPositiveNumber 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::requireZeroOrPositiveNumber
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T> T.requireZeroOrPositiveNumber(parameterName: String): T where T: Number, T: Comparable<T> = apply {
    toDouble().requireGe(0.0, parameterName)
}

/**
 * requirePositiveNumber 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::requirePositiveNumber
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T> T.requirePositiveNumber(parameterName: String): T where T: Number, T: Comparable<T> = apply {
    toDouble().requireGt(0.0, parameterName)
}

/**
 * requireZeroOrNegativeNumber 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::requireZeroOrNegativeNumber
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T> T.requireZeroOrNegativeNumber(parameterName: String): T where T: Number, T: Comparable<T> = apply {
    toDouble().requireLe(0.0, parameterName)
}

/**
 * requireNegativeNumber 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::requireNegativeNumber
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T> T.requireNegativeNumber(parameterName: String): T where T: Number, T: Comparable<T> = apply {
    toDouble().requireLt(0.0, parameterName)
}

/**
 * requireNotEmpty 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::requireNotEmpty
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T> Array<T>?.requireNotEmpty(parameterName: String) = apply {
    require(!this.isNullOrEmpty()) { "$parameterName[$this] must not be null or empty." }
}

/**
 * requireNotEmpty 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::requireNotEmpty
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <T> Collection<T>?.requireNotEmpty(parameterName: String) = apply {
    require(!this.isNullOrEmpty()) { "$parameterName[$this] must not be null or empty." }
}

/**
 * requireNotEmpty 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::requireNotEmpty
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <K, V> Map<K, V>?.requireNotEmpty(parameterName: String) = apply {
    require(!this.isNullOrEmpty()) { "$parameterName must not be null or empty." }
}

/**
 * requireHasKey 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::requireHasKey
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <K, V> Map<K, V>?.requireHasKey(key: K, parameterName: String): Map<K, V> {
    requireNotEmpty(parameterName)
    require(this!!.containsKey(key)) { "$parameterName must contain key $key" }
    return this
}

/**
 * requireHasValue 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::requireHasValue
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <K, V> Map<K, V>?.requireHasValue(value: V, parameterName: String): Map<K, V> {
    requireNotEmpty(parameterName)
    require(this!!.containsValue(value)) { "$parameterName must contain value $value" }
    return this
}

/**
 * requireContains 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ref = ::requireContains
 * println(ref.name)
 * check(ref.name.isNotEmpty())
 * ```
 */
inline fun <K, V> Map<K, V>?.requireContains(key: K, value: V, parameterName: String): Map<K, V> {
    requireNotEmpty(parameterName)
    require(this!![key] == value) { "$parameterName must contain ($key, $value)" }
    return this
}
