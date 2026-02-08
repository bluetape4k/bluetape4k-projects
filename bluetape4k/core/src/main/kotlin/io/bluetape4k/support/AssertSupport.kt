@file:OptIn(ExperimentalContracts::class)
@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.support

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

inline fun <T: Any> T?.assertNotNull(parameterName: String): T {
    contract {
        returns() implies (this@assertNotNull != null)
    }
    assert(this != null) { "$parameterName[$this] must not be null." }
    return this!!
}

inline fun <T: Any> T?.assertNull(parameterName: String): T? {
    contract {
        returns() implies (this@assertNull == null)
    }
    assert(this == null) { "$parameterName[$this] must be null." }
    return this
}

inline fun <T: CharSequence> T?.assertNotEmpty(parameterName: String): T {
    contract {
        returns() implies (this@assertNotEmpty != null)
    }
    val self = this.assertNotNull(parameterName)
    assert(self.isNotEmpty()) { "$parameterName[$self] must not be empty." }
    return self
}


inline fun <T: CharSequence> T?.assertNullOrEmpty(parameterName: String): T? {
    contract {
        returns() implies (this@assertNullOrEmpty == null)
    }
    assert(this.isNullOrEmpty()) { "$parameterName[$this] must be null or empty." }
    return this
}


inline fun <T: CharSequence> T?.assertNotBlank(parameterName: String): T {
    contract {
        returns() implies (this@assertNotBlank != null)
    }
    val self = this.assertNotNull(parameterName)
    assert(self.isNotBlank()) { "$parameterName[$self] must not be blank." }
    return self
}


inline fun <T: CharSequence> T?.assertNullOrBlank(parameterName: String): T? {
    contract {
        returns() implies (this@assertNullOrBlank == null)
    }
    assert(this.isNullOrBlank()) { "$parameterName[$this] must be null or blank." }
    return this
}


inline fun <T: CharSequence> T?.assertContains(other: CharSequence, parameterName: String): T {
    this.assertNotNull(parameterName)
    assert(this.contains(other)) { "$parameterName[$this] must contain $other" }
    return this
}

inline fun <T: CharSequence> T?.assertStartsWith(
    prefix: CharSequence,
    parameterName: String,
    ignoreCase: Boolean = false,
): T {
    this.assertNotNull(parameterName)
    assert(this.startsWith(prefix, ignoreCase)) { "$parameterName[$this] must be starts with $prefix" }
    return this
}

inline fun <T: CharSequence> T?.assertEndsWith(
    prefix: CharSequence,
    parameterName: String,
    ignoreCase: Boolean = false,
): T {
    this.assertNotNull(parameterName)
    assert(this.endsWith(prefix, ignoreCase)) { "$parameterName[$this] must be ends with $prefix" }
    return this
}

inline fun <T> T.assertEquals(expected: T, parameterName: String): T = apply {
    assert(this == expected) { "$parameterName[$this] must be equal to $expected" }
}

inline fun <T: Comparable<T>> T.assertGt(expected: T, parameterName: String): T = apply {
    assert(this > expected) { "$parameterName[$this] must be greater than $expected." }
}

inline fun <T: Comparable<T>> T.assertGe(expected: T, parameterName: String): T = apply {
    assert(this >= expected) { "$parameterName[$this] must be greater than or equal to $expected." }
}

inline fun <T: Comparable<T>> T.assertLt(expected: T, parameterName: String): T = apply {
    assert(this < expected) { "$parameterName[$this] must be less than $expected." }
}

inline fun <T: Comparable<T>> T.assertLe(expected: T, parameterName: String): T = apply {
    assert(this <= expected) { "$parameterName[$this] must be less than or equal to $expected." }
}

inline fun <T: Comparable<T>> T.assertInRange(start: T, endInclusive: T, parameterName: String) = apply {
    assert(this in start..endInclusive) { "$parameterName[$this] must be in range ($start .. $endInclusive)" }
}

inline fun <T: Comparable<T>> T.assertInOpenRange(start: T, endExclusive: T, name: String): T = apply {
    assert(this in start..<endExclusive) { "$start <= $name[$this] < $endExclusive" }
}

inline fun <T> T.assertZeroOrPositiveNumber(parameterName: String): T where T: Number, T: Comparable<T> = apply {
    toDouble().assertGe(0.0, parameterName)
}

inline fun <T> T.assertPositiveNumber(parameterName: String): T where T: Number, T: Comparable<T> = apply {
    toDouble().assertGt(0.0, parameterName)
}

inline fun <T> T.assertZeroOrNegativeNumber(parameterName: String): T where T: Number, T: Comparable<T> = apply {
    toDouble().assertLe(0.0, parameterName)
}

inline fun <T> T.assertNegativeNumber(parameterName: String): T where T: Number, T: Comparable<T> = apply {
    toDouble().assertLt(0.0, parameterName)
}

inline fun <T> Collection<T>?.assertNotEmpty(parameterName: String) = apply {
    assert(!this.isNullOrEmpty()) { "$parameterName[$this] must not be null or empty." }
}


inline fun <K, V> Map<K, V>?.assertNotEmpty(parameterName: String) = apply {
    assert(!this.isNullOrEmpty()) { "$parameterName must not be null or empty." }
}

fun <K, V> Map<K, V>?.assertHasKey(key: K, parameterName: String): Map<K, V> {
    assertNotEmpty(parameterName)
    assert(this!!.containsKey(key)) { "$parameterName require contains key $key" }
    return this
}

fun <K, V> Map<K, V>?.assertHasValue(value: V, parameterName: String): Map<K, V> {
    assertNotEmpty(parameterName)
    assert(this!!.containsValue(value)) { "$parameterName require contains value $value" }
    return this
}

fun <K, V> Map<K, V>?.assertContains(key: K, value: V, parameterName: String): Map<K, V> {
    assertNotEmpty(parameterName)
    assert(this!![key] == value) { "$parameterName require contains ($key, $value)" }
    return this
}
