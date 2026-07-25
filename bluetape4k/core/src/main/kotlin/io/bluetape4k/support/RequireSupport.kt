@file:OptIn(ExperimentalContracts::class) @file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.support

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * requireNotNull 기능을 제공합니다.
 *
 * ## 동작/계약
 * - 조건을 만족하지 않으면 [IllegalArgumentException]이 발생합니다.
 * - 조건을 만족하면 수신 값을 그대로 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val result = ("blue" as String?).requireNotNull("text")
 * // result == "blue"
 * ```
 */
inline fun <T: Any> T?.requireNotNull(parameterName: String): T =
    requireNotNull { "$parameterName[$this] must not be null." }

inline fun <T: Any> T?.requireNotNull(lazyMessage: () -> Any): T {
    contract {
        returns() implies (this@requireNotNull != null)
    }
    require(this != null) { lazyMessage() }
    return this
}

/**
 * requireNull 기능을 제공합니다.
 *
 * ## 동작/계약
 * - 조건을 만족하지 않으면 [IllegalArgumentException]이 발생합니다.
 * - 조건을 만족하면 수신 값을 그대로 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val result = (null as String?).requireNull("text")
 * // result == null
 * ```
 */
inline fun <T: Any> T?.requireNull(parameterName: String): T? = requireNull { "$parameterName[$this] must be null." }

inline fun <T: Any> T?.requireNull(lazyMessage: () -> Any): T? {
    contract {
        returns() implies (this@requireNull == null)
    }
    require(this == null) { lazyMessage() }
    return this
}

/**
 * requireNotEmpty 기능을 제공합니다.
 *
 * ## 동작/계약
 * - 조건을 만족하지 않으면 [IllegalArgumentException]이 발생합니다.
 * - 조건을 만족하면 수신 값을 그대로 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val result = ("blue" as String?).requireNotEmpty("text")
 * // result == "blue"
 * ```
 */
inline fun <T: CharSequence> T?.requireNotEmpty(parameterName: String): T =
    requireNotEmpty { "$parameterName[$this] must not be empty." }

inline fun <T: CharSequence> T?.requireNotEmpty(lazyMessage: () -> Any): T {
    contract {
        returnsNotNull() implies (this@requireNotEmpty != null)
    }
    val self = this.requireNotNull(lazyMessage)
    require(self.isNotEmpty()) { lazyMessage() }
    return self
}

/**
 * requireNullOrEmpty 기능을 제공합니다.
 *
 * ## 동작/계약
 * - 조건을 만족하지 않으면 [IllegalArgumentException]이 발생합니다.
 * - 조건을 만족하면 수신 값을 그대로 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val result = ("" as String?).requireNullOrEmpty("text")
 * // result == ""
 * ```
 */
inline fun <T: CharSequence> T?.requireNullOrEmpty(parameterName: String): T? =
    requireNullOrEmpty { "$parameterName[$this] must be null or empty." }

inline fun <T: CharSequence> T?.requireNullOrEmpty(lazyMessage: () -> Any): T? {
    contract {
        returns() implies (this@requireNullOrEmpty == null)
    }
    require(this.isNullOrEmpty()) { lazyMessage() }
    return this
}

/**
 * requireNotBlank 기능을 제공합니다.
 *
 * ## 동작/계약
 * - 조건을 만족하지 않으면 [IllegalArgumentException]이 발생합니다.
 * - 조건을 만족하면 수신 값을 그대로 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val result = ("blue" as String?).requireNotBlank("text")
 * // result == "blue"
 * ```
 */
inline fun <T: CharSequence> T?.requireNotBlank(parameterName: String): T =
    requireNotBlank { "$parameterName[$this] must not be blank." }

inline fun <T: CharSequence> T?.requireNotBlank(lazyMessage: () -> Any): T {
    contract {
        returnsNotNull() implies (this@requireNotBlank != null)
    }
    val self = this.requireNotNull(lazyMessage)
    require(self.isNotBlank()) { lazyMessage() }
    return self
}

/**
 * requireNullOrBlank 기능을 제공합니다.
 *
 * ## 동작/계약
 * - 조건을 만족하지 않으면 [IllegalArgumentException]이 발생합니다.
 * - 조건을 만족하면 수신 값을 그대로 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val result = ("   " as String?).requireNullOrBlank("text")
 * // result == "   "
 * ```
 */
inline fun <T: CharSequence> T?.requireNullOrBlank(parameterName: String): T? =
    requireNullOrBlank { "$parameterName[$this] must be null or blank." }

inline fun <T: CharSequence> T?.requireNullOrBlank(noinline lazyMessage: () -> Any): T? {
    contract {
        returns() implies (this@requireNullOrBlank == null)
    }
    require(this.isNullOrBlank()) { lazyMessage() }
    return this
}


/**
 * requireContains 기능을 제공합니다.
 *
 * ## 동작/계약
 * - 조건을 만족하지 않으면 [IllegalArgumentException]이 발생합니다.
 * - 조건을 만족하면 수신 값을 그대로 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val result = ("blue" as String?).requireContains("lu", "text")
 * // result == "blue"
 * ```
 */
inline fun <T: CharSequence> T?.requireContains(other: CharSequence, parameterName: String): T =
    requireContains(other) { "$parameterName[$this] must contain $other" }

inline fun <T: CharSequence> T?.requireContains(other: CharSequence, lazyMessage: () -> Any): T {
    val value = this.requireNotNull { lazyMessage() }
    require(value.contains(other)) { lazyMessage() }
    return value
}

/**
 * requireStartsWith 기능을 제공합니다.
 *
 * ## 동작/계약
 * - 조건을 만족하지 않으면 [IllegalArgumentException]이 발생합니다.
 * - 조건을 만족하면 수신 값을 그대로 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val result = ("blue" as String?).requireStartsWith("bl", "text")
 * // result == "blue"
 * ```
 */
inline fun <T: CharSequence> T?.requireStartsWith(
    prefix: CharSequence,
    parameterName: String,
    ignoreCase: Boolean = false,
): T = requireStartsWith(prefix, ignoreCase) { "$parameterName[$this] must start with $prefix" }

inline fun <T: CharSequence> T?.requireStartsWith(
    prefix: CharSequence, ignoreCase: Boolean = false, lazyMessage: () -> Any
): T {
    val value = requireNotNull(lazyMessage)
    require(value.startsWith(prefix, ignoreCase)) { lazyMessage() }
    return value
}

/**
 * requireEndsWith 기능을 제공합니다.
 *
 * ## 동작/계약
 * - 조건을 만족하지 않으면 [IllegalArgumentException]이 발생합니다.
 * - 조건을 만족하면 수신 값을 그대로 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val result = ("blue" as String?).requireEndsWith("ue", "text")
 * // result == "blue"
 * ```
 */
inline fun <T: CharSequence> T?.requireEndsWith(
    suffix: CharSequence,
    parameterName: String,
    ignoreCase: Boolean = false,
): T {
    val result = requireNotNull(parameterName)
    require(result.endsWith(suffix, ignoreCase)) { "$parameterName[$result] must end with $suffix" }
    return result
}

/**
 * requireEquals 기능을 제공합니다.
 *
 * ## 동작/계약
 * - 조건을 만족하지 않으면 [IllegalArgumentException]이 발생합니다.
 * - 조건을 만족하면 수신 값을 그대로 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val result = 10.requireEquals(10, "value")
 * // result == 10
 * ```
 */
inline fun <T> T.requireEquals(expected: T, parameterName: String): T =
    requireEquals(expected) { "$parameterName[$this] must be equal to $expected" }

inline fun <T> T.requireEquals(expected: T, lazyMessage: () -> Any): T {
    require(this == expected) { lazyMessage() }
    return this
}

/**
 * requireGt 기능을 제공합니다.
 *
 * ## 동작/계약
 * - 조건을 만족하지 않으면 [IllegalArgumentException]이 발생합니다.
 * - 조건을 만족하면 수신 값을 그대로 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val result = 10.requireGt(1, "value")
 * // result == 10
 * ```
 */
inline fun <T: Comparable<T>> T.requireGt(expected: T, parameterName: String): T =
    requireGt(expected) { "$parameterName[$this] must be greater than $expected." }

inline fun <T: Comparable<T>> T.requireGt(expected: T, lazyMessage: () -> Any): T {
    require(this > expected) { lazyMessage() }
    return this
}

/**
 * requireGe 기능을 제공합니다.
 *
 * ## 동작/계약
 * - 조건을 만족하지 않으면 [IllegalArgumentException]이 발생합니다.
 * - 조건을 만족하면 수신 값을 그대로 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val result = 10.requireGe(10, "value")
 * // result == 10
 * ```
 */
inline fun <T: Comparable<T>> T.requireGe(expected: T, parameterName: String): T =
    requireGe(expected) { "$parameterName[$this] must be greater than or equal to $expected." }

inline fun <T: Comparable<T>> T.requireGe(expected: T, lazyMessage: () -> Any): T {
    require(this >= expected) { lazyMessage() }
    return this
}

/**
 * requireLt 기능을 제공합니다.
 *
 * ## 동작/계약
 * - 조건을 만족하지 않으면 [IllegalArgumentException]이 발생합니다.
 * - 조건을 만족하면 수신 값을 그대로 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val result = 1.requireLt(10, "value")
 * // result == 1
 * ```
 */
inline fun <T: Comparable<T>> T.requireLt(expected: T, parameterName: String): T =
    requireLt(expected) { "$parameterName[$this] must be less than $expected." }

inline fun <T: Comparable<T>> T.requireLt(expected: T, lazyMessage: () -> Any): T {
    require(this < expected) { lazyMessage() }
    return this
}


/**
 * requireLe 기능을 제공합니다.
 *
 * ## 동작/계약
 * - 조건을 만족하지 않으면 [IllegalArgumentException]이 발생합니다.
 * - 조건을 만족하면 수신 값을 그대로 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val result = 10.requireLe(10, "value")
 * // result == 10
 * ```
 */
inline fun <T: Comparable<T>> T.requireLe(expected: T, parameterName: String): T =
    requireLe(expected) { "$parameterName[$this] must be less than or equal to $expected." }

inline fun <T: Comparable<T>> T.requireLe(expected: T, lazyMessage: () -> Any): T {
    require(this <= expected) { lazyMessage() }
    return this
}

/**
 * requireInRange 기능을 제공합니다.
 *
 * ## 동작/계약
 * - 조건을 만족하지 않으면 [IllegalArgumentException]이 발생합니다.
 * - 조건을 만족하면 수신 값을 그대로 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val result = 5.requireInRange(1, 10, "value")
 * // result == 5
 * ```
 */
inline fun <T: Comparable<T>> T.requireInRange(start: T, endInclusive: T, parameterName: String) =
    requireInRange(start, endInclusive) { "$parameterName[$this] must be in range ($start .. $endInclusive)" }

inline fun <T: Comparable<T>> T.requireInRange(start: T, endInclusive: T, lazyMessage: () -> Any): T {
    require(this in start..endInclusive) { lazyMessage() }
    return this
}

/**
 * requireInOpenRange 기능을 제공합니다.
 *
 * ## 동작/계약
 * - 조건을 만족하지 않으면 [IllegalArgumentException]이 발생합니다.
 * - 조건을 만족하면 수신 값을 그대로 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val result = 5.requireInOpenRange(1, 10, "value")
 * // result == 5
 * ```
 */
inline fun <T: Comparable<T>> T.requireInOpenRange(start: T, endExclusive: T, parameterName: String): T =
    requireInOpenRange(start, endExclusive) { "$start <= $parameterName[$this] < $endExclusive" }

inline fun <T: Comparable<T>> T.requireInOpenRange(start: T, endExclusive: T, lazyMessage: () -> Any): T {
    require(this in start..<endExclusive) { lazyMessage() }
    return this
}

/**
 * requireZeroOrPositiveNumber 기능을 제공합니다.
 *
 * ## 동작/계약
 * - 조건을 만족하지 않으면 [IllegalArgumentException]이 발생합니다.
 * - 조건을 만족하면 수신 값을 그대로 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val result = 0.requireZeroOrPositiveNumber("value")
 * // result == 0
 * ```
 */
inline fun <T> T.requireZeroOrPositiveNumber(parameterName: String): T where T: Number, T: Comparable<T> = apply {
    toDouble().requireGe(0.0, parameterName)
}

inline fun <T> T.requireZeroOrPositiveNumber(lazyMessage: () -> Any): T where T: Number, T: Comparable<T> {
    toDouble().requireGe(0.0, lazyMessage)
    return this
}

/**
 * requirePositiveNumber 기능을 제공합니다.
 *
 * ## 동작/계약
 * - 조건을 만족하지 않으면 [IllegalArgumentException]이 발생합니다.
 * - 조건을 만족하면 수신 값을 그대로 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val result = 1.requirePositiveNumber("value")
 * // result == 1
 * ```
 */
inline fun <T> T.requirePositiveNumber(parameterName: String): T where T: Number, T: Comparable<T> = apply {
    toDouble().requireGt(0.0, parameterName)
}

inline fun <T> T.requirePositiveNumber(lazyMessage: () -> Any): T where T: Number, T: Comparable<T> {
    toDouble().requireGt(0.0, lazyMessage)
    return this
}

/**
 * requireZeroOrNegativeNumber 기능을 제공합니다.
 *
 * ## 동작/계약
 * - 조건을 만족하지 않으면 [IllegalArgumentException]이 발생합니다.
 * - 조건을 만족하면 수신 값을 그대로 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val result = 0.requireZeroOrNegativeNumber("value")
 * // result == 0
 * ```
 */
inline fun <T> T.requireZeroOrNegativeNumber(parameterName: String): T where T: Number, T: Comparable<T> = apply {
    toDouble().requireLe(0.0, parameterName)
}

inline fun <T> T.requireZeroOrNegativeNumber(lazyMessage: () -> Any): T where T: Number, T: Comparable<T> {
    toDouble().requireLe(0.0, lazyMessage)
    return this
}

/**
 * requireNegativeNumber 기능을 제공합니다.
 *
 * ## 동작/계약
 * - 조건을 만족하지 않으면 [IllegalArgumentException]이 발생합니다.
 * - 조건을 만족하면 수신 값을 그대로 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val result = (-1).requireNegativeNumber("value")
 * // result == -1
 * ```
 */
inline fun <T> T.requireNegativeNumber(parameterName: String): T where T: Number, T: Comparable<T> = apply {
    toDouble().requireLt(0.0, parameterName)
}

inline fun <T> T.requireNegativeNumber(lazyMessage: () -> Any): T where T: Number, T: Comparable<T> {
    toDouble().requireLt(0.0, lazyMessage)
    return this
}

/**
 * Requires this array to be non-null and non-empty, then returns the same array.
 *
 * The returned array is the same instance as the receiver.
 *
 * - Throws [IllegalArgumentException] when the contract is not satisfied.
 * - Returns the original receiver when the contract is satisfied.
 * - Does not mutate the receiver.
 *
 * ```kotlin
 * val result = arrayOf(1, 2).requireNotEmpty("items")
 * // result.size == 2
 * ```
 */
inline fun <T> Array<T>?.requireNotEmpty(parameterName: String): Array<T> =
    requireNotEmpty { "$parameterName must not be null or empty." }

inline fun <T> Array<T>?.requireNotEmpty(lazyMessage: () -> Any): Array<T> {
    val self = this ?: throw IllegalArgumentException(lazyMessage().toString())
    require(self.isNotEmpty()) { lazyMessage() }
    return self
}

/**
 * Requires this collection to be non-null and non-empty, then returns the same collection.
 *
 * The returned collection is the same instance as the receiver.
 *
 * - Throws [IllegalArgumentException] when the contract is not satisfied.
 * - Returns the original receiver when the contract is satisfied.
 * - Does not mutate the receiver.
 *
 * ```kotlin
 * val result = listOf(1, 2).requireNotEmpty("items")
 * // result.size == 2
 * ```
 */
inline fun <T> Collection<T>?.requireNotEmpty(parameterName: String): Collection<T> =
    requireNotEmpty { "$parameterName must not be null or empty." }

inline fun <T> Collection<T>?.requireNotEmpty(lazyMessage: () -> Any): Collection<T> {
    val self = this ?: throw IllegalArgumentException(lazyMessage().toString())
    require(self.isNotEmpty()) { lazyMessage() }
    return self
}

/**
 * Requires this map to be non-null and non-empty, then returns the same map.
 *
 * The returned map is the same instance as the receiver.
 *
 * - Throws [IllegalArgumentException] when the contract is not satisfied.
 * - Returns the original receiver when the contract is satisfied.
 * - Does not mutate the receiver.
 *
 * ```kotlin
 * val result = mapOf("a" to 1).requireNotEmpty("map")
 * // result["a"] == 1
 * ```
 */
inline fun <K, V> Map<K, V>?.requireNotEmpty(parameterName: String): Map<K, V> =
    requireNotEmpty { "$parameterName must not be null or empty." }

inline fun <K, V> Map<K, V>?.requireNotEmpty(lazyMessage: () -> Any): Map<K, V> {
    val self = this ?: throw IllegalArgumentException(lazyMessage().toString())
    require(self.isNotEmpty()) { lazyMessage() }
    return self
}

/**
 * requireHasKey 기능을 제공합니다.
 *
 * ## 동작/계약
 * - 조건을 만족하지 않으면 [IllegalArgumentException]이 발생합니다.
 * - 조건을 만족하면 수신 값을 그대로 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val result = mapOf("a" to 1).requireHasKey("a", "map")
 * // result["a"] == 1
 * ```
 */
inline fun <K, V> Map<K, V>?.requireHasKey(key: K, parameterName: String): Map<K, V> {
    requireNotEmpty(parameterName)
    return requireHasKey(key) { "$parameterName must contain key $key" }
}

inline fun <K, V> Map<K, V>?.requireHasKey(key: K, lazyMessage: () -> Any): Map<K, V> {
    val self = this ?: throw IllegalArgumentException(lazyMessage().toString())
    require(self.containsKey(key)) { lazyMessage() }
    return self
}

/**
 * requireHasValue 기능을 제공합니다.
 *
 * ## 동작/계약
 * - 조건을 만족하지 않으면 [IllegalArgumentException]이 발생합니다.
 * - 조건을 만족하면 수신 값을 그대로 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val result = mapOf("a" to 1).requireHasValue(1, "map")
 * // result["a"] == 1
 * ```
 */
inline fun <K, V> Map<K, V>?.requireHasValue(value: V, parameterName: String): Map<K, V> {
    requireNotEmpty(parameterName)
    return requireHasValue(value) { "$parameterName must contain value $value" }
}

inline fun <K, V> Map<K, V>?.requireHasValue(value: V, lazyMessage: () -> Any): Map<K, V> {
    val self = this ?: throw IllegalArgumentException(lazyMessage().toString())
    require(self.containsValue(value)) { lazyMessage() }
    return self
}

/**
 * requireContains 기능을 제공합니다.
 *
 * ## 동작/계약
 * - 조건을 만족하지 않으면 [IllegalArgumentException]이 발생합니다.
 * - 조건을 만족하면 수신 값을 그대로 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val result = mapOf("a" to 1).requireContains("a", 1, "map")
 * // result["a"] == 1
 * ```
 */
inline fun <K, V> Map<K, V>?.requireContains(key: K, value: V, parameterName: String): Map<K, V> {
    requireNotEmpty(parameterName)
    return requireContains(key, value) { "$parameterName must contain ($key, $value)" }
}

inline fun <K, V> Map<K, V>?.requireContains(key: K, value: V, lazyMessage: () -> Any): Map<K, V> {
    val self = this ?: throw IllegalArgumentException(lazyMessage().toString())
    require(self[key] == value) { lazyMessage() }
    return self
}

/**
 * Requires a nullable character sequence to have a length within the inclusive range.
 *
 * ```kotlin
 * val id = ("order-42" as String?).requireLengthInRange(1, 80, "id")
 * ```
 */
inline fun <T: CharSequence> T?.requireLengthInRange(
    start: Int,
    endInclusive: Int,
    parameterName: String,
    noinline lazyMessage: (() -> Any)? = null,
): T {
    contract {
        returnsNotNull() implies (this@requireLengthInRange != null)
    }
    val self = this.requireNotNull(parameterName)
    require(self.length in start..endInclusive) {
        lazyMessage?.invoke() ?: "$parameterName length must be between $start and $endInclusive."
    }
    return self
}


/**
 * Requires a nullable collection to have a size within the inclusive range.
 *
 * ```kotlin
 * val items = (listOf(1, 2) as List<Int>?).requireSizeInRange(1, 50, "items")
 * ```
 */
inline fun <T> Collection<T>?.requireSizeInRange(
    start: Int,
    endInclusive: Int,
    parameterName: String,
    noinline lazyMessage: (() -> Any)? = null,
): Collection<T> {
    contract {
        returnsNotNull() implies (this@requireSizeInRange != null)
    }
    val self = this.requireNotNull(parameterName)
    require(self.size in start..endInclusive) {
        lazyMessage?.invoke() ?: "$parameterName size must be between $start and $endInclusive."
    }
    return self
}

/**
 * Requires a nullable map to have a size within the inclusive range.
 *
 * ```kotlin
 * val attributes = (mapOf("state" to "ready") as Map<String, String>?)
 *     .requireSizeInRange(1, 10, "attributes")
 * ```
 */
inline fun <K, V> Map<K, V>?.requireSizeInRange(
    start: Int,
    endInclusive: Int,
    parameterName: String,
    noinline lazyMessage: (() -> Any)? = null,
): Map<K, V> {
    contract {
        returnsNotNull() implies (this@requireSizeInRange != null)
    }
    val self = this.requireNotNull(parameterName)
    require(self.size in start..endInclusive) {
        lazyMessage?.invoke() ?: "$parameterName size must be between $start and $endInclusive."
    }
    return self
}

/**
 * Requires a nullable array to have a size within the inclusive range.
 *
 * ```kotlin
 * val values = (arrayOf(1, 2) as Array<Int>?).requireSizeInRange(1, 10, "values")
 * ```
 */
inline fun <T> Array<T>?.requireSizeInRange(
    start: Int,
    endInclusive: Int,
    parameterName: String,
    noinline lazyMessage: (() -> Any)? = null,
): Array<T> {
    contract {
        returnsNotNull() implies (this@requireSizeInRange != null)
    }
    val self = this.requireNotNull(parameterName)
    require(self.size in start..endInclusive) {
        lazyMessage?.invoke() ?: "$parameterName size must be between $start and $endInclusive."
    }
    return self
}

/**
 * Requires a nullable character sequence to match [regex].
 *
 * ```kotlin
 * val sku = ("SKU-42" as String?).requireMatches(Regex("SKU-\\d+"), "sku")
 * ```
 */
inline fun <T: CharSequence> T?.requireMatches(
    regex: Regex,
    parameterName: String,
    noinline lazyMessage: (() -> Any)? = null,
): T {
    contract {
        returnsNotNull() implies (this@requireMatches != null)
    }
    val self = this.requireNotNull(parameterName)
    require(regex.matches(self)) {
        lazyMessage?.invoke() ?: "$parameterName must match the expected pattern."
    }
    return self
}

/**
 * Requires a finite `Float` value.
 *
 * ```kotlin
 * val ratio = 0.5f.requireFinite("ratio")
 * ```
 */
inline fun Float.requireFinite(parameterName: String, noinline lazyMessage: (() -> Any)? = null): Float = apply {
    require(isFinite()) {
        lazyMessage?.invoke() ?: "$parameterName must be finite."
    }
}

/**
 * Requires a finite `Double` value.
 *
 * ```kotlin
 * val ratio = 0.5.requireFinite("ratio")
 * ```
 */
inline fun Double.requireFinite(parameterName: String, noinline lazyMessage: (() -> Any)? = null): Double = apply {
    require(isFinite()) { lazyMessage?.invoke() ?: "$parameterName must be finite." }
}
