@file:OptIn(ExperimentalContracts::class)
@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.support

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

inline fun <T: Any> T?.requireNotNull(parameterName: String): T {
    contract {
        returns() implies (this@requireNotNull != null)
    }
    require(this != null) { "$parameterName[$this] must not be null." }
    return this
}

inline fun <T: Any> T?.requireNull(parameterName: String): T? {
    contract {
        returns() implies (this@requireNull == null)
    }
    require(this == null) { "$parameterName[$this] must be null." }
    return this
}

inline fun <T: CharSequence> T?.requireNotEmpty(parameterName: String): T {
    contract {
        returnsNotNull() implies (this@requireNotEmpty != null)
    }
    val self = this.requireNotNull(parameterName)
    require(self.isNotEmpty()) { "$parameterName[$self] must not be empty." }
    return self
}

inline fun <T: CharSequence> T?.requireNullOrEmpty(parameterName: String): T? {
    contract {
        returns() implies (this@requireNullOrEmpty == null)
    }
    require(this.isNullOrEmpty()) { "$parameterName[$this] must be null or empty." }
    return this
}

inline fun <T: CharSequence> T?.requireNotBlank(parameterName: String): T {
    contract {
        returnsNotNull() implies (this@requireNotBlank != null)
    }
    val self = this.requireNotNull(parameterName)
    require(self.isNotBlank()) { "$parameterName[$self] must not be blank." }
    return self
}

inline fun <T: CharSequence> T?.requireNullOrBlank(parameterName: String): T? {
    contract {
        returns() implies (this@requireNullOrBlank == null)
    }
    require(this.isNullOrBlank()) { "$parameterName[$this] must be null or blank." }
    return this
}


inline fun <T: CharSequence> T?.requireContains(other: CharSequence, parameterName: String): T {
    this.requireNotNull(parameterName)
    require(this.contains(other)) { "$parameterName[$this] must contain $other" }
    return this
}

inline fun <T: CharSequence> T?.requireStartsWith(
    prefix: CharSequence,
    parameterName: String,
    ignoreCase: Boolean = false,
): T {
    this.requireNotNull(parameterName)
    require(this.startsWith(prefix, ignoreCase)) { "$parameterName[$this] must be starts with $prefix" }
    return this
}

inline fun <T: CharSequence> T?.requireEndsWith(
    prefix: CharSequence,
    parameterName: String,
    ignoreCase: Boolean = false,
): T {
    this.requireNotNull(parameterName)
    require(this.endsWith(prefix, ignoreCase)) { "$parameterName[$this] must be ends with $prefix" }
    return this
}

inline fun <T> T.requireEquals(expected: T, parameterName: String): T = apply {
    require(this == expected) { "$parameterName[$this] must be equal to $expected" }
}

inline fun <T: Comparable<T>> T.requireGt(expected: T, parameterName: String): T = apply {
    require(this > expected) { "$parameterName[$this] must be greater than $expected." }
}

inline fun <T: Comparable<T>> T.requireGe(expected: T, parameterName: String): T = apply {
    require(this >= expected) { "$parameterName[$this] must be greater than or equal to $expected." }
}

inline fun <T: Comparable<T>> T.requireLt(expected: T, parameterName: String): T = apply {
    require(this < expected) { "$parameterName[$this] must be less than $expected." }
}

inline fun <T: Comparable<T>> T.requireLe(expected: T, parameterName: String): T = apply {
    require(this <= expected) { "$parameterName[$this] must be less than or equal to $expected." }
}

inline fun <T: Comparable<T>> T.requireInRange(start: T, endInclusive: T, parameterName: String) = apply {
    require(this in start..endInclusive) { "$parameterName[$this] must be in range ($start .. $endInclusive)" }
}

inline fun <T: Comparable<T>> T.requireInOpenRange(start: T, endExclusive: T, parameterName: String): T = apply {
    require(this in start..<endExclusive) { "$start <= $parameterName[$this] < $endExclusive" }
}

inline fun <T> T.requireZeroOrPositiveNumber(parameterName: String): T where T: Number, T: Comparable<T> = apply {
    toDouble().requireGe(0.0, parameterName)
}

inline fun <T> T.requirePositiveNumber(parameterName: String): T where T: Number, T: Comparable<T> = apply {
    toDouble().requireGt(0.0, parameterName)
}

inline fun <T> T.requireZeroOrNegativeNumber(parameterName: String): T where T: Number, T: Comparable<T> = apply {
    toDouble().requireLe(0.0, parameterName)
}

inline fun <T> T.requireNegativeNumber(parameterName: String): T where T: Number, T: Comparable<T> = apply {
    toDouble().requireLt(0.0, parameterName)
}

inline fun <T> Array<T>?.requireNotEmpty(parameterName: String) = apply {
    require(!this.isNullOrEmpty()) { "$parameterName[$this] must not be null or empty." }
}

inline fun <T> Collection<T>?.requireNotEmpty(parameterName: String) = apply {
    require(!this.isNullOrEmpty()) { "$parameterName[$this] must not be null or empty." }
}

inline fun <K, V> Map<K, V>?.requireNotEmpty(parameterName: String) = apply {
    require(!this.isNullOrEmpty()) { "$parameterName must not be null or empty." }
}

fun <K, V> Map<K, V>?.requireHasKey(key: K, parameterName: String): Map<K, V> {
    requireNotEmpty(parameterName)
    require(this!!.containsKey(key)) { "$parameterName require contains key $key" }
    return this
}

fun <K, V> Map<K, V>?.requireHasValue(value: V, parameterName: String): Map<K, V> {
    requireNotEmpty(parameterName)
    require(this!!.containsValue(value)) { "$parameterName require contains value $value" }
    return this
}

fun <K, V> Map<K, V>?.requireContains(key: K, value: V, parameterName: String): Map<K, V> {
    requireNotEmpty(parameterName)
    require(this!![key] == value) { "$parameterName require contains ($key, $value)" }
    return this
}
