/**
 * `AssertionError` 기반의 경량 assertion 유틸리티 모음입니다.
 *
 * ## 동작/계약
 * - JVM assertions(-ea) 플래그와 무관하게 **항상** 검증이 수행됩니다.
 * - 실패 시 [AssertionError]가 발생합니다.
 *
 * ## 마이그레이션 안내
 * 파라미터 검증(호출자 계약)에는 [RequireSupport]의 `requireXxx()` 함수를 사용하세요.
 * - `requireXxx()` → [IllegalArgumentException] 발생 (호출자 계약 위반)
 * - `assertXxx()` → [AssertionError] 발생 (불변식·내부 상태 검증)
 */
@file:OptIn(ExperimentalContracts::class)
@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.support

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * null이 아닌 값을 보장합니다.
 *
 * @deprecated 파라미터 검증에는 [requireNotNull]을 사용하세요.
 */
@Deprecated(
    "파라미터 검증에는 requireNotNull()을 사용하세요.",
    ReplaceWith("requireNotNull(parameterName)"),
    DeprecationLevel.WARNING,
)
inline fun <T: Any> T?.assertNotNull(parameterName: String): T {
    contract {
        returns() implies (this@assertNotNull != null)
    }
    if (this == null) throw AssertionError("$parameterName[$this] must not be null.")
    return this
}

/**
 * 값이 null임을 보장합니다.
 *
 * @deprecated 파라미터 검증에는 [requireNull]을 사용하세요.
 */
@Deprecated(
    "파라미터 검증에는 requireNull()을 사용하세요.",
    ReplaceWith("requireNull(parameterName)"),
    DeprecationLevel.WARNING,
)
inline fun <T: Any> T?.assertNull(parameterName: String): T? {
    contract {
        returns() implies (this@assertNull == null)
    }
    if (this != null) throw AssertionError("$parameterName[$this] must be null.")
    return this
}

/**
 * null이 아니고 빈 문자열이 아님을 보장합니다.
 *
 * @deprecated 파라미터 검증에는 [requireNotEmpty]를 사용하세요.
 */
@Deprecated(
    "파라미터 검증에는 requireNotEmpty()를 사용하세요.",
    ReplaceWith("requireNotEmpty(parameterName)"),
    DeprecationLevel.WARNING,
)
inline fun <T: CharSequence> T?.assertNotEmpty(parameterName: String): T {
    contract {
        returns() implies (this@assertNotEmpty != null)
    }
    @Suppress("DEPRECATION")
    val self = this.assertNotNull(parameterName)
    if (self.isEmpty()) throw AssertionError("$parameterName[$self] must not be empty.")
    return self
}

/**
 * null이거나 빈 문자열임을 보장합니다.
 *
 * @deprecated 파라미터 검증에는 [requireNullOrEmpty]를 사용하세요.
 */
@Deprecated(
    "파라미터 검증에는 requireNullOrEmpty()를 사용하세요.",
    ReplaceWith("requireNullOrEmpty(parameterName)"),
    DeprecationLevel.WARNING,
)
inline fun <T: CharSequence> T?.assertNullOrEmpty(parameterName: String): T? {
    contract {
        returns() implies (this@assertNullOrEmpty == null)
    }
    if (!this.isNullOrEmpty()) throw AssertionError("$parameterName[$this] must be null or empty.")
    return this
}

/**
 * null이 아니고 공백이 아닌 문자열임을 보장합니다.
 *
 * @deprecated 파라미터 검증에는 [requireNotBlank]를 사용하세요.
 */
@Deprecated(
    "파라미터 검증에는 requireNotBlank()를 사용하세요.",
    ReplaceWith("requireNotBlank(parameterName)"),
    DeprecationLevel.WARNING,
)
inline fun <T: CharSequence> T?.assertNotBlank(parameterName: String): T {
    contract {
        returns() implies (this@assertNotBlank != null)
    }
    @Suppress("DEPRECATION")
    val self = this.assertNotNull(parameterName)
    if (self.isBlank()) throw AssertionError("$parameterName[$self] must not be blank.")
    return self
}

/**
 * null이거나 공백 문자열임을 보장합니다.
 *
 * @deprecated 파라미터 검증에는 [requireNullOrBlank]를 사용하세요.
 */
@Deprecated(
    "파라미터 검증에는 requireNullOrBlank()를 사용하세요.",
    ReplaceWith("requireNullOrBlank(parameterName)"),
    DeprecationLevel.WARNING,
)
inline fun <T: CharSequence> T?.assertNullOrBlank(parameterName: String): T? {
    contract {
        returns() implies (this@assertNullOrBlank == null)
    }
    if (!this.isNullOrBlank()) throw AssertionError("$parameterName[$this] must be null or blank.")
    return this
}

/**
 * 지정한 문자열을 포함함을 보장합니다.
 *
 * @deprecated 파라미터 검증에는 [requireContains]를 사용하세요.
 */
@Deprecated(
    "파라미터 검증에는 requireContains()를 사용하세요.",
    ReplaceWith("requireContains(other, parameterName)"),
    DeprecationLevel.WARNING,
)
inline fun <T: CharSequence> T?.assertContains(other: CharSequence, parameterName: String): T {
    @Suppress("DEPRECATION")
    this.assertNotNull(parameterName)
    if (!this.contains(other)) throw AssertionError("$parameterName[$this] must contain $other")
    return this
}

/**
 * 지정한 접두사로 시작함을 보장합니다.
 *
 * @deprecated 파라미터 검증에는 [requireStartsWith]를 사용하세요.
 */
@Deprecated(
    "파라미터 검증에는 requireStartsWith()를 사용하세요.",
    ReplaceWith("requireStartsWith(prefix, parameterName, ignoreCase)"),
    DeprecationLevel.WARNING,
)
inline fun <T: CharSequence> T?.assertStartsWith(
    prefix: CharSequence,
    parameterName: String,
    ignoreCase: Boolean = false,
): T {
    @Suppress("DEPRECATION")
    this.assertNotNull(parameterName)
    if (!this.startsWith(prefix, ignoreCase)) throw AssertionError("$parameterName[$this] must start with $prefix")
    return this
}

/**
 * 지정한 접미사로 끝남을 보장합니다.
 *
 * @deprecated 파라미터 검증에는 [requireEndsWith]를 사용하세요.
 */
@Deprecated(
    "파라미터 검증에는 requireEndsWith()를 사용하세요.",
    ReplaceWith("requireEndsWith(suffix, parameterName, ignoreCase)"),
    DeprecationLevel.WARNING,
)
inline fun <T: CharSequence> T?.assertEndsWith(
    suffix: CharSequence,
    parameterName: String,
    ignoreCase: Boolean = false,
): T {
    @Suppress("DEPRECATION")
    this.assertNotNull(parameterName)
    if (!this.endsWith(suffix, ignoreCase)) throw AssertionError("$parameterName[$this] must end with $suffix")
    return this
}

/**
 * 값이 예상값과 같음을 보장합니다.
 *
 * @deprecated 파라미터 검증에는 [requireEquals]를 사용하세요.
 */
@Deprecated(
    "파라미터 검증에는 requireEquals()를 사용하세요.",
    ReplaceWith("requireEquals(expected, parameterName)"),
    DeprecationLevel.WARNING,
)
inline fun <T> T.assertEquals(expected: T, parameterName: String): T = apply {
    if (this != expected) throw AssertionError("$parameterName[$this] must be equal to $expected")
}

/**
 * 값이 지정한 값보다 큼을 보장합니다.
 *
 * @deprecated 파라미터 검증에는 [requireGt]를 사용하세요.
 */
@Deprecated(
    "파라미터 검증에는 requireGt()를 사용하세요.",
    ReplaceWith("requireGt(expected, parameterName)"),
    DeprecationLevel.WARNING,
)
inline fun <T: Comparable<T>> T.assertGt(expected: T, parameterName: String): T = apply {
    if (this <= expected) throw AssertionError("$parameterName[$this] must be greater than $expected.")
}

/**
 * 값이 지정한 값보다 크거나 같음을 보장합니다.
 *
 * @deprecated 파라미터 검증에는 [requireGe]를 사용하세요.
 */
@Deprecated(
    "파라미터 검증에는 requireGe()를 사용하세요.",
    ReplaceWith("requireGe(expected, parameterName)"),
    DeprecationLevel.WARNING,
)
inline fun <T: Comparable<T>> T.assertGe(expected: T, parameterName: String): T = apply {
    if (this < expected) throw AssertionError("$parameterName[$this] must be greater than or equal to $expected.")
}

/**
 * 값이 지정한 값보다 작음을 보장합니다.
 *
 * @deprecated 파라미터 검증에는 [requireLt]를 사용하세요.
 */
@Deprecated(
    "파라미터 검증에는 requireLt()를 사용하세요.",
    ReplaceWith("requireLt(expected, parameterName)"),
    DeprecationLevel.WARNING,
)
inline fun <T: Comparable<T>> T.assertLt(expected: T, parameterName: String): T = apply {
    if (this >= expected) throw AssertionError("$parameterName[$this] must be less than $expected.")
}

/**
 * 값이 지정한 값보다 작거나 같음을 보장합니다.
 *
 * @deprecated 파라미터 검증에는 [requireLe]를 사용하세요.
 */
@Deprecated(
    "파라미터 검증에는 requireLe()를 사용하세요.",
    ReplaceWith("requireLe(expected, parameterName)"),
    DeprecationLevel.WARNING,
)
inline fun <T: Comparable<T>> T.assertLe(expected: T, parameterName: String): T = apply {
    if (this > expected) throw AssertionError("$parameterName[$this] must be less than or equal to $expected.")
}

/**
 * 값이 지정한 닫힌 범위 내에 있음을 보장합니다.
 *
 * @deprecated 파라미터 검증에는 [requireInRange]를 사용하세요.
 */
@Deprecated(
    "파라미터 검증에는 requireInRange()를 사용하세요.",
    ReplaceWith("requireInRange(start, endInclusive, parameterName)"),
    DeprecationLevel.WARNING,
)
inline fun <T: Comparable<T>> T.assertInRange(start: T, endInclusive: T, parameterName: String) = apply {
    if (this !in start..endInclusive) throw AssertionError("$parameterName[$this] must be in range ($start .. $endInclusive)")
}

/**
 * 값이 지정한 열린 범위 내에 있음을 보장합니다.
 *
 * @deprecated 파라미터 검증에는 [requireInOpenRange]를 사용하세요.
 */
@Deprecated(
    "파라미터 검증에는 requireInOpenRange()를 사용하세요.",
    ReplaceWith("requireInOpenRange(start, endExclusive, name)"),
    DeprecationLevel.WARNING,
)
inline fun <T: Comparable<T>> T.assertInOpenRange(start: T, endExclusive: T, name: String): T = apply {
    if (this !in start..<endExclusive) throw AssertionError("$start <= $name[$this] < $endExclusive")
}

/**
 * 값이 0 이상임을 보장합니다.
 *
 * @deprecated 파라미터 검증에는 [requireZeroOrPositiveNumber]를 사용하세요.
 */
@Deprecated(
    "파라미터 검증에는 requireZeroOrPositiveNumber()를 사용하세요.",
    ReplaceWith("requireZeroOrPositiveNumber(parameterName)"),
    DeprecationLevel.WARNING,
)
inline fun <T> T.assertZeroOrPositiveNumber(parameterName: String): T where T: Number, T: Comparable<T> = apply {
    @Suppress("DEPRECATION")
    toDouble().assertGe(0.0, parameterName)
}

/**
 * 값이 0 초과임을 보장합니다.
 *
 * @deprecated 파라미터 검증에는 [requirePositiveNumber]를 사용하세요.
 */
@Deprecated(
    "파라미터 검증에는 requirePositiveNumber()를 사용하세요.",
    ReplaceWith("requirePositiveNumber(parameterName)"),
    DeprecationLevel.WARNING,
)
inline fun <T> T.assertPositiveNumber(parameterName: String): T where T: Number, T: Comparable<T> = apply {
    @Suppress("DEPRECATION")
    toDouble().assertGt(0.0, parameterName)
}

/**
 * 값이 0 이하임을 보장합니다.
 *
 * @deprecated 파라미터 검증에는 [requireZeroOrNegativeNumber]를 사용하세요.
 */
@Deprecated(
    "파라미터 검증에는 requireZeroOrNegativeNumber()를 사용하세요.",
    ReplaceWith("requireZeroOrNegativeNumber(parameterName)"),
    DeprecationLevel.WARNING,
)
inline fun <T> T.assertZeroOrNegativeNumber(parameterName: String): T where T: Number, T: Comparable<T> = apply {
    @Suppress("DEPRECATION")
    toDouble().assertLe(0.0, parameterName)
}

/**
 * 값이 0 미만임을 보장합니다.
 *
 * @deprecated 파라미터 검증에는 [requireNegativeNumber]를 사용하세요.
 */
@Deprecated(
    "파라미터 검증에는 requireNegativeNumber()를 사용하세요.",
    ReplaceWith("requireNegativeNumber(parameterName)"),
    DeprecationLevel.WARNING,
)
inline fun <T> T.assertNegativeNumber(parameterName: String): T where T: Number, T: Comparable<T> = apply {
    @Suppress("DEPRECATION")
    toDouble().assertLt(0.0, parameterName)
}

/**
 * 컬렉션이 null이 아니고 비어있지 않음을 보장합니다.
 *
 * @deprecated 파라미터 검증에는 [requireNotEmpty]를 사용하세요.
 */
@Deprecated(
    "파라미터 검증에는 requireNotEmpty()를 사용하세요.",
    ReplaceWith("requireNotEmpty(parameterName)"),
    DeprecationLevel.WARNING,
)
inline fun <T> Collection<T>?.assertNotEmpty(parameterName: String) = apply {
    if (this.isNullOrEmpty()) throw AssertionError("$parameterName[$this] must not be null or empty.")
}

/**
 * 맵이 null이 아니고 비어있지 않음을 보장합니다.
 *
 * @deprecated 파라미터 검증에는 [requireNotEmpty]를 사용하세요.
 */
@Deprecated(
    "파라미터 검증에는 requireNotEmpty()를 사용하세요.",
    ReplaceWith("requireNotEmpty(parameterName)"),
    DeprecationLevel.WARNING,
)
inline fun <K, V> Map<K, V>?.assertNotEmpty(parameterName: String) = apply {
    if (this.isNullOrEmpty()) throw AssertionError("$parameterName must not be null or empty.")
}

/**
 * 맵이 null이 아니고 비어있지 않으며, 지정한 키를 포함함을 보장합니다.
 *
 * @deprecated 파라미터 검증에는 [requireHasKey]를 사용하세요.
 */
@Deprecated(
    "파라미터 검증에는 requireHasKey()를 사용하세요.",
    ReplaceWith("requireHasKey(key, parameterName)"),
    DeprecationLevel.WARNING,
)
inline fun <K, V> Map<K, V>?.assertHasKey(key: K, parameterName: String): Map<K, V> {
    @Suppress("DEPRECATION")
    assertNotEmpty(parameterName)
    if (!this!!.containsKey(key)) throw AssertionError("$parameterName must contain key $key")
    return this
}

/**
 * 맵이 null이 아니고 비어있지 않으며, 지정한 값을 포함함을 보장합니다.
 *
 * @deprecated 파라미터 검증에는 [requireHasValue]를 사용하세요.
 */
@Deprecated(
    "파라미터 검증에는 requireHasValue()를 사용하세요.",
    ReplaceWith("requireHasValue(value, parameterName)"),
    DeprecationLevel.WARNING,
)
inline fun <K, V> Map<K, V>?.assertHasValue(value: V, parameterName: String): Map<K, V> {
    @Suppress("DEPRECATION")
    assertNotEmpty(parameterName)
    if (!this!!.containsValue(value)) throw AssertionError("$parameterName must contain value $value")
    return this
}

/**
 * 맵이 null이 아니고 비어있지 않으며, 지정한 키-값 쌍을 포함함을 보장합니다.
 *
 * @deprecated 파라미터 검증에는 [requireContains]를 사용하세요.
 */
@Deprecated(
    "파라미터 검증에는 requireContains()를 사용하세요.",
    ReplaceWith("requireContains(key, value, parameterName)"),
    DeprecationLevel.WARNING,
)
inline fun <K, V> Map<K, V>?.assertContains(key: K, value: V, parameterName: String): Map<K, V> {
    @Suppress("DEPRECATION")
    assertNotEmpty(parameterName)
    if (this!![key] != value) throw AssertionError("$parameterName must contain ($key, $value)")
    return this
}
