@file:OptIn(ExperimentalContracts::class)
@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.support

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Provides the `checkNotNull` invariant check.
 *
 * ## Contract
 * - Throws [IllegalStateException] when the invariant is not satisfied.
 * - Returns the original receiver when the invariant is satisfied.
 * - Does not mutate the receiver.
 *
 * ```kotlin
 * val result = ("blue" as String?).checkNotNull("text")
 * // result == "blue"
 * ```
 */
inline fun <T: Any> T?.checkNotNull(parameterName: String): T =
    checkNotNull { "$parameterName[$this] must not be null." }

inline fun <T: Any> T?.checkNotNull(lazyMessage: () -> Any): T {
    contract {
        returns() implies (this@checkNotNull != null)
    }
    check(this != null) { lazyMessage() }
    return this
}

/**
 * Provides the `checkNull` invariant check.
 *
 * ## Contract
 * - Throws [IllegalStateException] when the invariant is not satisfied.
 * - Returns the original receiver when the invariant is satisfied.
 * - Does not mutate the receiver.
 *
 * ```kotlin
 * val result = (null as String?).checkNull("text")
 * // result == null
 * ```
 */
inline fun <T: Any> T?.checkNull(parameterName: String): T? =
    checkNull { "$parameterName[$this] must be null." }

inline fun <T: Any> T?.checkNull(lazyMessage: () -> Any): T? {
    contract {
        returns() implies (this@checkNull == null)
    }
    check(this == null) { lazyMessage() }
    return this
}

/**
 * Provides the `checkNotEmpty` invariant check.
 *
 * ## Contract
 * - Throws [IllegalStateException] when the invariant is not satisfied.
 * - Returns the original receiver when the invariant is satisfied.
 * - Does not mutate the receiver.
 *
 * ```kotlin
 * val result = ("blue" as String?).checkNotEmpty("text")
 * // result == "blue"
 * ```
 */
inline fun <T: CharSequence> T?.checkNotEmpty(parameterName: String): T =
    checkNotEmpty { "$parameterName must not be empty." }

inline fun <T: CharSequence> T?.checkNotEmpty(lazyMessage: () -> Any): T {
    contract {
        returnsNotNull() implies (this@checkNotEmpty != null)
    }
    val self = checkNotNull(lazyMessage)
    check(self.isNotEmpty()) { lazyMessage() }
    return self
}

/**
 * Provides the `checkNullOrEmpty` invariant check.
 *
 * ## Contract
 * - Throws [IllegalStateException] when the invariant is not satisfied.
 * - Returns the original receiver when the invariant is satisfied.
 * - Does not mutate the receiver.
 *
 * ```kotlin
 * val result = ("" as String?).checkNullOrEmpty("text")
 * // result == ""
 * ```
 */
inline fun <T: CharSequence> T?.checkNullOrEmpty(parameterName: String): T? =
    checkNullOrEmpty { "$parameterName must be null or empty." }

inline fun <T: CharSequence> T?.checkNullOrEmpty(lazyMessage: () -> Any): T? {
    check(this.isNullOrEmpty()) { lazyMessage() }
    return this
}

/**
 * Provides the `checkNotBlank` invariant check.
 *
 * ## Contract
 * - Throws [IllegalStateException] when the invariant is not satisfied.
 * - Returns the original receiver when the invariant is satisfied.
 * - Does not mutate the receiver.
 *
 * ```kotlin
 * val result = ("blue" as String?).checkNotBlank("text")
 * // result == "blue"
 * ```
 */
inline fun <T: CharSequence> T?.checkNotBlank(parameterName: String): T =
    checkNotBlank { "$parameterName must not be blank." }

inline fun <T: CharSequence> T?.checkNotBlank(lazyMessage: () -> Any): T {
    contract {
        returnsNotNull() implies (this@checkNotBlank != null)
    }
    val self = this.checkNotNull(lazyMessage)
    check(self.isNotBlank()) { lazyMessage() }
    return self
}

/**
 * Provides the `checkNullOrBlank` invariant check.
 *
 * ## Contract
 * - Throws [IllegalStateException] when the invariant is not satisfied.
 * - Returns the original receiver when the invariant is satisfied.
 * - Does not mutate the receiver.
 *
 * ```kotlin
 * val result = ("   " as String?).checkNullOrBlank("text")
 * // result == "   "
 * ```
 */
inline fun <T: CharSequence> T?.checkNullOrBlank(parameterName: String): T? =
    checkNullOrBlank { "$parameterName must be null or blank." }

inline fun <T: CharSequence> T?.checkNullOrBlank(lazyMessage: () -> Any): T? {
    check(this.isNullOrBlank()) { lazyMessage() }
    return this
}


/**
 * Provides the `checkContains` invariant check.
 *
 * ## Contract
 * - Throws [IllegalStateException] when the invariant is not satisfied.
 * - Returns the original receiver when the invariant is satisfied.
 * - Does not mutate the receiver.
 *
 * ```kotlin
 * val result = ("blue" as String?).checkContains("lu", "text")
 * // result == "blue"
 * ```
 */
inline fun <T: CharSequence> T?.checkContains(other: CharSequence, parameterName: String): T =
    checkContains(other) { "$parameterName must contain $other" }

inline fun <T: CharSequence> T?.checkContains(other: CharSequence, lazyMessage: () -> Any): T {
    val self = this.checkNotNull(lazyMessage)
    check(self.contains(other)) { lazyMessage() }
    return self
}


/**
 * Provides the `checkStartsWith` invariant check.
 *
 * ## Contract
 * - Throws [IllegalStateException] when the invariant is not satisfied.
 * - Returns the original receiver when the invariant is satisfied.
 * - Does not mutate the receiver.
 *
 * ```kotlin
 * val result = ("blue" as String?).checkStartsWith("bl", "text")
 * // result == "blue"
 * ```
 */
inline fun <T: CharSequence> T?.checkStartsWith(
    prefix: CharSequence,
    parameterName: String,
    ignoreCase: Boolean = false,
): T =
    checkStartsWith(prefix, ignoreCase) { "$parameterName[$this] must start with $prefix" }

inline fun <T: CharSequence> T?.checkStartsWith(
    prefix: CharSequence,
    ignoreCase: Boolean = false,
    lazyMessage: () -> Any,
): T {
    val self = this.checkNotNull(lazyMessage)
    check(self.startsWith(prefix, ignoreCase)) { lazyMessage() }
    return self
}

/**
 * Provides the `checkEndsWith` invariant check.
 *
 * ## Contract
 * - Throws [IllegalStateException] when the invariant is not satisfied.
 * - Returns the original receiver when the invariant is satisfied.
 * - Does not mutate the receiver.
 *
 * ```kotlin
 * val result = ("blue" as String?).checkEndsWith("ue", "text")
 * // result == "blue"
 * ```
 */
inline fun <T: CharSequence> T?.checkEndsWith(
    suffix: CharSequence,
    parameterName: String,
    ignoreCase: Boolean = false,
): T =
    checkEndsWith(suffix, ignoreCase) { "$parameterName[$this] must end with $suffix" }

inline fun <T: CharSequence> T?.checkEndsWith(
    suffix: CharSequence,
    ignoreCase: Boolean = false,
    lazyMessage: () -> Any,
): T {
    val self = checkNotNull(lazyMessage)
    check(self.endsWith(suffix, ignoreCase)) { lazyMessage() }
    return self
}

/**
 * Provides the `checkEquals` invariant check.
 *
 * ## Contract
 * - Throws [IllegalStateException] when the invariant is not satisfied.
 * - Returns the original receiver when the invariant is satisfied.
 * - Does not mutate the receiver.
 *
 * ```kotlin
 * val result = 10.checkEquals(10, "value")
 * // result == 10
 * ```
 */
inline fun <T> T.checkEquals(expected: T, parameterName: String): T =
    checkEquals(expected) { "$parameterName[$this] must be equal to $expected" }

inline fun <T> T.checkEquals(expected: T, lazyMessage: () -> Any): T {
    check(this == expected) { lazyMessage() }
    return this
}

/**
 * Provides the `checkGt` invariant check.
 *
 * ## Contract
 * - Throws [IllegalStateException] when the invariant is not satisfied.
 * - Returns the original receiver when the invariant is satisfied.
 * - Does not mutate the receiver.
 *
 * ```kotlin
 * val result = 10.checkGt(1, "value")
 * // result == 10
 * ```
 */
inline fun <T: Comparable<T>> T.checkGt(expected: T, parameterName: String): T =
    checkGt(expected) { "$parameterName[$this] must be greater than $expected." }

inline fun <T: Comparable<T>> T.checkGt(expected: T, lazyMessage: () -> Any): T = apply {
    check(this > expected) { lazyMessage() }
}

/**
 * Provides the `checkGe` invariant check.
 *
 * ## Contract
 * - Throws [IllegalStateException] when the invariant is not satisfied.
 * - Returns the original receiver when the invariant is satisfied.
 * - Does not mutate the receiver.
 *
 * ```kotlin
 * val result = 10.checkGe(10, "value")
 * // result == 10
 * ```
 */
inline fun <T: Comparable<T>> T.checkGe(expected: T, parameterName: String): T =
    checkGe(expected) { "$parameterName[$this] must be greater than or equal to $expected." }

inline fun <T: Comparable<T>> T.checkGe(expected: T, lazyMessage: () -> Any): T = apply {
    check(this >= expected) { lazyMessage() }
}

/**
 * Provides the `checkLt` invariant check.
 *
 * ## Contract
 * - Throws [IllegalStateException] when the invariant is not satisfied.
 * - Returns the original receiver when the invariant is satisfied.
 * - Does not mutate the receiver.
 *
 * ```kotlin
 * val result = 1.checkLt(10, "value")
 * // result == 1
 * ```
 */
inline fun <T: Comparable<T>> T.checkLt(expected: T, parameterName: String): T =
    checkLt(expected) { "$parameterName[$this] must be less than $expected." }

inline fun <T: Comparable<T>> T.checkLt(expected: T, lazyMessage: () -> Any): T = apply {
    check(this < expected) { lazyMessage() }
}

/**
 * Provides the `checkLe` invariant check.
 *
 * ## Contract
 * - Throws [IllegalStateException] when the invariant is not satisfied.
 * - Returns the original receiver when the invariant is satisfied.
 * - Does not mutate the receiver.
 *
 * ```kotlin
 * val result = 10.checkLe(10, "value")
 * // result == 10
 * ```
 */
inline fun <T: Comparable<T>> T.checkLe(expected: T, parameterName: String): T =
    checkLe(expected) { "$parameterName[$this] must be less than or equal to $expected." }

inline fun <T: Comparable<T>> T.checkLe(expected: T, lazyMessage: () -> Any): T = apply {
    check(this <= expected) { lazyMessage() }
}

/**
 * Provides the `checkInRange` invariant check.
 *
 * ## Contract
 * - Throws [IllegalStateException] when the invariant is not satisfied.
 * - Returns the original receiver when the invariant is satisfied.
 * - Does not mutate the receiver.
 *
 * ```kotlin
 * val result = 5.checkInRange(1, 10, "value")
 * // result == 5
 * ```
 */
inline fun <T: Comparable<T>> T.checkInRange(start: T, endInclusive: T, parameterName: String) =
    checkInRange(start, endInclusive) { "$parameterName[$this] must be in range ($start .. $endInclusive)" }

inline fun <T: Comparable<T>> T.checkInRange(start: T, endInclusive: T, lazyMessage: () -> Any) = apply {
    check(this in start..endInclusive) { lazyMessage() }
}

/**
 * Provides the `checkInOpenRange` invariant check.
 *
 * ## Contract
 * - Throws [IllegalStateException] when the invariant is not satisfied.
 * - Returns the original receiver when the invariant is satisfied.
 * - Does not mutate the receiver.
 *
 * ```kotlin
 * val result = 5.checkInOpenRange(1, 10, "value")
 * // result == 5
 * ```
 */
inline fun <T: Comparable<T>> T.checkInOpenRange(start: T, endExclusive: T, parameterName: String): T =
    checkInOpenRange(start, endExclusive) { "$start <= $parameterName[$this] < $endExclusive" }

inline fun <T: Comparable<T>> T.checkInOpenRange(start: T, endExclusive: T, lazyMessage: () -> Any): T = apply {
    check(this in start..<endExclusive) { lazyMessage() }
}

/**
 * Provides the `checkZeroOrPositiveNumber` invariant check.
 *
 * ## Contract
 * - Throws [IllegalStateException] when the invariant is not satisfied.
 * - Returns the original receiver when the invariant is satisfied.
 * - Does not mutate the receiver.
 *
 * ```kotlin
 * val result = 0.checkZeroOrPositiveNumber("value")
 * // result == 0
 * ```
 */
inline fun <T> T.checkZeroOrPositiveNumber(parameterName: String): T where T: Number, T: Comparable<T> = apply {
    toDouble().checkGe(0.0, parameterName)
}

inline fun <T> T.checkZeroOrPositiveNumber(lazyMessage: () -> Any): T where T: Number, T: Comparable<T> = apply {
    toDouble().checkGe(0.0, lazyMessage)
}

/**
 * Provides the `checkPositiveNumber` invariant check.
 *
 * ## Contract
 * - Throws [IllegalStateException] when the invariant is not satisfied.
 * - Returns the original receiver when the invariant is satisfied.
 * - Does not mutate the receiver.
 *
 * ```kotlin
 * val result = 1.checkPositiveNumber("value")
 * // result == 1
 * ```
 */
inline fun <T> T.checkPositiveNumber(parameterName: String): T where T: Number, T: Comparable<T> = apply {
    toDouble().checkGt(0.0, parameterName)
}

inline fun <T> T.checkPositiveNumber(lazyMessage: () -> Any): T where T: Number, T: Comparable<T> = apply {
    toDouble().checkGt(0.0, lazyMessage)
}

/**
 * Provides the `checkZeroOrNegativeNumber` invariant check.
 *
 * ## Contract
 * - Throws [IllegalStateException] when the invariant is not satisfied.
 * - Returns the original receiver when the invariant is satisfied.
 * - Does not mutate the receiver.
 *
 * ```kotlin
 * val result = 0.checkZeroOrNegativeNumber("value")
 * // result == 0
 * ```
 */
inline fun <T> T.checkZeroOrNegativeNumber(parameterName: String): T where T: Number, T: Comparable<T> = apply {
    toDouble().checkLe(0.0, parameterName)
}

inline fun <T> T.checkZeroOrNegativeNumber(lazyMessage: () -> Any): T where T: Number, T: Comparable<T> = apply {
    toDouble().checkLe(0.0, lazyMessage)
}

/**
 * Provides the `checkNegativeNumber` invariant check.
 *
 * ## Contract
 * - Throws [IllegalStateException] when the invariant is not satisfied.
 * - Returns the original receiver when the invariant is satisfied.
 * - Does not mutate the receiver.
 *
 * ```kotlin
 * val result = (-1).checkNegativeNumber("value")
 * // result == -1
 * ```
 */
inline fun <T> T.checkNegativeNumber(parameterName: String): T where T: Number, T: Comparable<T> = apply {
    toDouble().checkLt(0.0, parameterName)
}

inline fun <T> T.checkNegativeNumber(lazyMessage: () -> Any): T where T: Number, T: Comparable<T> = apply {
    toDouble().checkLt(0.0, lazyMessage)
}

/**
 * Requires this array to be non-null and non-empty, then returns the same array.
 *
 * ## Contract
 * - Throws [IllegalStateException] when the invariant is not satisfied.
 * - Returns the original receiver when the invariant is satisfied.
 * - Does not mutate the receiver.
 *
 * ```kotlin
 * val result = arrayOf(1, 2).checkNotEmpty("items")
 * // result.size == 2
 * ```
 */
inline fun <T> Array<T>?.checkNotEmpty(parameterName: String): Array<T> =
    checkNotEmpty { "$parameterName[$this] must not be null or empty." }

inline fun <T> Array<T>?.checkNotEmpty(lazyMessage: () -> Any): Array<T> {
    val self = this ?: throw IllegalStateException(lazyMessage().toString())
    check(self.isNotEmpty()) { lazyMessage() }
    return self
}

/**
 * Requires this collection to be non-null and non-empty, then returns the same collection.
 *
 * ## Contract
 * - Throws [IllegalStateException] when the invariant is not satisfied.
 * - Returns the original receiver when the invariant is satisfied.
 * - Does not mutate the receiver.
 *
 * ```kotlin
 * val result = listOf(1, 2).checkNotEmpty("items")
 * // result.size == 2
 * ```
 */
inline fun <T> Collection<T>?.checkNotEmpty(parameterName: String): Collection<T> =
    checkNotEmpty { "$parameterName must not be null or empty." }

inline fun <T> Collection<T>?.checkNotEmpty(lazyMessage: () -> Any): Collection<T> {
    val self = this ?: throw IllegalStateException(lazyMessage().toString())
    check(self.isNotEmpty()) { lazyMessage() }
    return self
}

/**
 * Requires this map to be non-null and non-empty, then returns the same map.
 *
 * ## Contract
 * - Throws [IllegalStateException] when the invariant is not satisfied.
 * - Returns the original receiver when the invariant is satisfied.
 * - Does not mutate the receiver.
 *
 * ```kotlin
 * val result = mapOf("a" to 1).checkNotEmpty("map")
 * // result["a"] == 1
 * ```
 */
inline fun <K, V> Map<K, V>?.checkNotEmpty(parameterName: String): Map<K, V> =
    checkNotEmpty { "$parameterName must not be null or empty." }

inline fun <K, V> Map<K, V>?.checkNotEmpty(lazyMessage: () -> Any): Map<K, V> {
    val self = this ?: throw IllegalStateException(lazyMessage().toString())
    check(self.isNotEmpty()) { lazyMessage() }
    return self
}

/**
 * Provides the `checkHasKey` invariant check.
 *
 * ## Contract
 * - Throws [IllegalStateException] when the invariant is not satisfied.
 * - Returns the original receiver when the invariant is satisfied.
 * - Does not mutate the receiver.
 *
 * ```kotlin
 * val result = mapOf("a" to 1).checkHasKey("a", "map")
 * // result["a"] == 1
 * ```
 */
inline fun <K, V> Map<K, V>?.checkHasKey(key: K, parameterName: String): Map<K, V> =
    checkHasKey(key) { "$parameterName must contain key $key" }

inline fun <K, V> Map<K, V>?.checkHasKey(key: K, lazyMessage: () -> Any): Map<K, V> {
    checkNotEmpty(lazyMessage)
    val self = this ?: throw IllegalStateException(lazyMessage().toString())
    check(self.containsKey(key)) { lazyMessage() }
    return self
}

/**
 * Provides the `checkHasValue` invariant check.
 *
 * ## Contract
 * - Throws [IllegalStateException] when the invariant is not satisfied.
 * - Returns the original receiver when the invariant is satisfied.
 * - Does not mutate the receiver.
 *
 * ```kotlin
 * val result = mapOf("a" to 1).checkHasValue(1, "map")
 * // result["a"] == 1
 * ```
 */
inline fun <K, V> Map<K, V>?.checkHasValue(value: V, parameterName: String): Map<K, V> =
    checkHasValue(value) { "$parameterName must contain value $value" }

inline fun <K, V> Map<K, V>?.checkHasValue(value: V, lazyMessage: () -> Any): Map<K, V> {
    checkNotEmpty(lazyMessage)
    val self = this ?: throw IllegalStateException(lazyMessage().toString())
    check(self.containsValue(value)) { lazyMessage() }
    return self
}

/**
 * Provides the `checkContains` invariant check.
 *
 * ## Contract
 * - Throws [IllegalStateException] when the invariant is not satisfied.
 * - Returns the original receiver when the invariant is satisfied.
 * - Does not mutate the receiver.
 *
 * ```kotlin
 * val result = mapOf("a" to 1).checkContains("a", 1, "map")
 * // result["a"] == 1
 * ```
 */
inline fun <K, V> Map<K, V>?.checkContains(key: K, value: V, parameterName: String): Map<K, V> =
    checkContains(key, value) { "$parameterName must contain ($key, $value)" }

inline fun <K, V> Map<K, V>?.checkContains(key: K, value: V, lazyMessage: () -> Any): Map<K, V> {
    checkNotEmpty(lazyMessage)
    val self = this ?: throw IllegalStateException(lazyMessage().toString())
    check(self[key] == value) { lazyMessage() }
    return self
}

/**
 * Checks that a nullable character sequence has a length within the inclusive range.
 *
 * ```kotlin
 * val id = ("order-42" as String?).checkLengthInRange(1, 80, "id")
 * ```
 */
inline fun <T: CharSequence> T?.checkLengthInRange(
    start: Int,
    endInclusive: Int,
    parameterName: String,
    noinline lazyMessage: (() -> Any)? = null,
): T {
    contract {
        returnsNotNull() implies (this@checkLengthInRange != null)
    }
    val self = this.checkNotNull(parameterName)
    check(self.length in start..endInclusive) {
        lazyMessage?.invoke() ?: "$parameterName length must be between $start and $endInclusive."
    }
    return self
}

/**
 * Checks that a nullable collection has a size within the inclusive range.
 *
 * ```kotlin
 * val items = (listOf(1, 2) as List<Int>?).checkSizeInRange(1, 50, "items")
 * ```
 */
inline fun <T> Collection<T>?.checkSizeInRange(
    start: Int,
    endInclusive: Int,
    parameterName: String,
    noinline lazyMessage: (() -> Any)? = null,
): Collection<T> {
    contract {
        returnsNotNull() implies (this@checkSizeInRange != null)
    }
    val self = this.checkNotNull(parameterName)
    check(self.size in start..endInclusive) {
        lazyMessage?.invoke() ?: "$parameterName size must be between $start and $endInclusive."
    }
    return self
}

/**
 * Checks that a nullable map has a size within the inclusive range.
 *
 * ```kotlin
 * val attributes = (mapOf("state" to "ready") as Map<String, String>?)
 *     .checkSizeInRange(1, 10, "attributes")
 * ```
 */
inline fun <K, V> Map<K, V>?.checkSizeInRange(
    start: Int,
    endInclusive: Int,
    parameterName: String,
    noinline lazyMessage: (() -> Any)? = null,
): Map<K, V> {
    contract {
        returnsNotNull() implies (this@checkSizeInRange != null)
    }
    val self = this.checkNotNull(parameterName)
    check(self.size in start..endInclusive) {
        lazyMessage?.invoke() ?: "$parameterName size must be between $start and $endInclusive."
    }
    return self
}

/**
 * Checks that a nullable array has a size within the inclusive range.
 *
 * ```kotlin
 * val values = (arrayOf(1, 2) as Array<Int>?).checkSizeInRange(1, 10, "values")
 * ```
 */
inline fun <T> Array<T>?.checkSizeInRange(
    start: Int,
    endInclusive: Int,
    parameterName: String,
    noinline lazyMessage: (() -> Any)? = null,
): Array<T> {
    contract {
        returnsNotNull() implies (this@checkSizeInRange != null)
    }
    val self = this.checkNotNull(parameterName)
    check(self.size in start..endInclusive) {
        lazyMessage?.invoke() ?: "$parameterName size must be between $start and $endInclusive."
    }
    return self
}

/**
 * Checks that a nullable character sequence matches [regex].
 *
 * ```kotlin
 * val sku = ("SKU-42" as String?).checkMatches(Regex("SKU-\\d+"), "sku")
 * ```
 */
inline fun <T: CharSequence> T?.checkMatches(
    regex: Regex,
    parameterName: String,
    noinline lazyMessage: (() -> Any)? = null,
): T {
    contract {
        returnsNotNull() implies (this@checkMatches != null)
    }
    val self = this.checkNotNull(parameterName)
    check(regex.matches(self)) {
        lazyMessage?.invoke() ?: "$parameterName must match the expected pattern."
    }
    return self
}

/**
 * Checks that a `Float` value is finite.
 *
 * ```kotlin
 * val ratio = 0.5f.checkFinite("ratio")
 * ```
 */
inline fun Float.checkFinite(parameterName: String, noinline lazyMessage: (() -> Any)? = null): Float = apply {
    check(isFinite()) {
        lazyMessage?.invoke() ?: "$parameterName must be finite."
    }
}

/**
 * Checks that a `Double` value is finite.
 *
 * ```kotlin
 * val ratio = 0.5.checkFinite("ratio")
 * ```
 */
inline fun Double.checkFinite(parameterName: String, noinline lazyMessage: (() -> Any)? = null): Double = apply {
    check(isFinite()) {
        lazyMessage?.invoke() ?: "$parameterName must be finite."
    }
}
