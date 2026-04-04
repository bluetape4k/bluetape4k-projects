package io.bluetape4k.math.commons

import io.bluetape4k.math.MathConsts.BIGDECIMAL_EPSILON
import io.bluetape4k.math.MathConsts.EPSILON
import io.bluetape4k.math.MathConsts.FLOAT_EPSILON
import java.math.BigDecimal

/**
 * 현재 값이 `destValue`와 오차범위 내에 있다면 `destValue`로 대체한다
 *
 * ```kotlin
 * val result = 1.0000001.clamp(1.0, 1e-6)   // 1.0
 * val result2 = 2.0.clamp(1.0, 1e-6)        // 2.0
 * ```
 *
 * @param destValue 대체할 값
 * @param tolerance 오차범위
 * @return 오차범위 내에 있다면 대체할 값, 아니면 현재 값을 반환
 * @see approximateEqual
 */
fun Double.clamp(destValue: Double, tolerance: Double = EPSILON): Double =
    if (this.approximateEqual(destValue, tolerance)) destValue else this

/**
 * 현재 값이 `destValue`와 오차범위 내에 있다면 `destValue`로 대체한다
 *
 * ```kotlin
 * val result = 1.0000001f.clamp(1.0f, 1e-6f)   // 1.0
 * ```
 *
 * @param destValue 대체할 값
 * @param tolerance 오차범위
 * @return 오차범위 내에 있다면 대체할 값, 아니면 현재 값을 반환
 * @see approximateEqual
 */
fun Float.clamp(destValue: Float, tolerance: Float = FLOAT_EPSILON): Float =
    if (this.approximateEqual(destValue, tolerance)) destValue else this

/**
 * 현재 값이 `destValue`와 오차범위 내에 있다면 `destValue`로 대체한다
 *
 * ```kotlin
 * val result = BigDecimal("1.0000001").clamp(BigDecimal("1.0"), BigDecimal("1e-6"))
 * // BigDecimal("1.0")
 * ```
 *
 * @param destValue 대체할 값
 * @param tolerance 오차범위
 * @return 오차범위 내에 있다면 대체할 값, 아니면 현재 값을 반환
 * @see approximateEqual
 */
fun BigDecimal.clamp(destValue: BigDecimal, tolerance: BigDecimal = BIGDECIMAL_EPSILON): BigDecimal =
    if (this.approximateEqual(destValue, tolerance)) destValue else this

/**
 * 요소 값이 `destValue`와 오차범위 내에 있다면 `destValue`로 대체한다
 *
 * ```kotlin
 * val result = sequenceOf(1.0, 1.0000001, 2.0).clamp(1.0, 1e-6).toList()
 * // [1.0, 1.0, 2.0]
 * ```
 *
 * @param destValue 대체할 값
 * @param tolerance 오차범위
 * @return 오차범위 내에 있다면 대체할 값, 아니면 현재 값을 반환
 * @see approximateEqual
 */
fun Sequence<Double>.clamp(destValue: Double, tolerance: Double = EPSILON): Sequence<Double> =
    map { it.clamp(destValue, tolerance) }

/**
 * 요소 값이 `destValue`와 오차범위 내에 있다면 `destValue`로 대체한다
 *
 * ```kotlin
 * val result = listOf(1.0, 1.0000001, 2.0).clamp(1.0, 1e-6)
 * // [1.0, 1.0, 2.0]
 * ```
 *
 * @param destValue 대체할 값
 * @param tolerance 오차범위
 * @return 오차범위 내에 있다면 대체할 값, 아니면 현재 값을 반환
 * @see approximateEqual
 */
fun Iterable<Double>.clamp(destValue: Double, tolerance: Double = EPSILON): List<Double> =
    map { it.clamp(destValue, tolerance) }

/**
 * 요소 값이 `destValue`와 오차범위 내에 있다면 `destValue`로 대체한다
 *
 * ```kotlin
 * val result = sequenceOf(1.0f, 1.0000001f, 2.0f).clamp(1.0f, 1e-6f).toList()
 * // [1.0, 1.0, 2.0]
 * ```
 *
 * @param destValue 대체할 값
 * @param tolerance 오차범위
 * @return 오차범위 내에 있다면 대체할 값, 아니면 현재 값을 반환
 * @see approximateEqual
 */
fun Sequence<Float>.clamp(destValue: Float, tolerance: Float = FLOAT_EPSILON): Sequence<Float> =
    map { it.clamp(destValue, tolerance) }

/**
 * 요소 값이 `destValue`와 오차범위 내에 있다면 `destValue`로 대체한다
 *
 * ```kotlin
 * val result = listOf(1.0f, 1.0000001f, 2.0f).clamp(1.0f, 1e-6f)
 * // [1.0, 1.0, 2.0]
 * ```
 *
 * @param destValue 대체할 값
 * @param tolerance 오차범위
 * @return 오차범위 내에 있다면 대체할 값, 아니면 현재 값을 반환
 * @see approximateEqual
 */
fun Iterable<Float>.clamp(destValue: Float, tolerance: Float = FLOAT_EPSILON): List<Float> =
    map { it.clamp(destValue, tolerance) }

/**
 * 요소 값이 `destValue`와 오차범위 내에 있다면 `destValue`로 대체한다
 *
 * ```kotlin
 * val result = sequenceOf(BigDecimal("1.0"), BigDecimal("1.0000001"))
 *     .clamp(BigDecimal("1.0"), BigDecimal("1e-6")).toList()
 * // [BigDecimal("1.0"), BigDecimal("1.0")]
 * ```
 *
 * @param destValue 대체할 값
 * @param tolerance 오차범위
 * @return 오차범위 내에 있다면 대체할 값, 아니면 현재 값을 반환
 * @see approximateEqual
 */
fun Sequence<BigDecimal>.clamp(
    destValue: BigDecimal,
    tolerance: BigDecimal = BIGDECIMAL_EPSILON,
): Sequence<BigDecimal> =
    map { it.clamp(destValue, tolerance) }

/**
 * 요소 값이 `destValue`와 오차범위 내에 있다면 `destValue`로 대체한다
 *
 * ```kotlin
 * val result = listOf(BigDecimal("1.0"), BigDecimal("1.0000001"))
 *     .clamp(BigDecimal("1.0"), BigDecimal("1e-6"))
 * // [BigDecimal("1.0"), BigDecimal("1.0")]
 * ```
 *
 * @param destValue 대체할 값
 * @param tolerance 오차범위
 * @return 오차범위 내에 있다면 대체할 값, 아니면 현재 값을 반환
 * @see approximateEqual
 */
fun Iterable<BigDecimal>.clamp(destValue: BigDecimal, tolerance: BigDecimal = BIGDECIMAL_EPSILON): List<BigDecimal> =
    map { it.clamp(destValue, tolerance) }


/**
 * 값을 `range` 범위 내로 제한합니다. 범위를 벗어나면 경계값으로 대체됩니다.
 *
 * ```kotlin
 * val result = 5.rangeClamp(1..3)   // 3
 * val result2 = 2.rangeClamp(1..3)  // 2
 * ```
 *
 * @param range clamp 상하한 범위
 * @return 범위 내 값
 * @see approximateEqual
 */
fun <T: Comparable<T>> T.rangeClamp(range: ClosedRange<T>): T = coerceIn(range)

/**
 * 시퀀스의 각 요소를 `range` 범위 내로 제한합니다.
 *
 * ```kotlin
 * val result = sequenceOf(0, 2, 5).rangeClamp(1..3).toList()
 * // [1, 2, 3]
 * ```
 *
 * @param range clamp 상하한 범위
 * @return 범위 내로 제한된 시퀀스
 * @see approximateEqual
 */
fun <T: Comparable<T>> Sequence<T>.rangeClamp(range: ClosedRange<T>): Sequence<T> = map { it.coerceIn(range) }

/**
 * 컬렉션의 각 요소를 `range` 범위 내로 제한합니다.
 *
 * ```kotlin
 * val result = listOf(0, 2, 5).rangeClamp(1..3)
 * // [1, 2, 3]
 * ```
 *
 * @param range clamp 상하한 범위
 * @return 범위 내로 제한된 리스트
 * @see approximateEqual
 */
fun <T: Comparable<T>> Iterable<T>.rangeClamp(range: ClosedRange<T>): List<T> = map { it.coerceIn(range) }
