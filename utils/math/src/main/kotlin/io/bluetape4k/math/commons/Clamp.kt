package io.bluetape4k.math.commons

import io.bluetape4k.math.MathConsts.BIGDECIMAL_EPSILON
import io.bluetape4k.math.MathConsts.EPSILON
import io.bluetape4k.math.MathConsts.FLOAT_EPSILON
import java.math.BigDecimal

/**
 * 현재 값이 `destValue`와 오차범위 내에 있다면 `destValue`로 대체한다
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
 * @param destValue 대체할 값
 * @param tolerance 오차범위
 * @return 오차범위 내에 있다면 대체할 값, 아니면 현재 값을 반환
 * @see approximateEqual
 */
fun Iterable<BigDecimal>.clamp(destValue: BigDecimal, tolerance: BigDecimal = BIGDECIMAL_EPSILON): List<BigDecimal> =
    map { it.clamp(destValue, tolerance) }


/**
 * 요소 값이 `destValue`와 오차범위 내에 있다면 `destValue`로 대체한다
 *
 * @param range clamp 상하한 값
 * @return 오차범위 내에 있다면 대체할 값, 아니면 현재 값을 반환
 * @see approximateEqual
 */
fun <T: Comparable<T>> T.rangeClamp(range: ClosedRange<T>): T = coerceIn(range)

/**
 * 요소 값이 `destValue`와 오차범위 내에 있다면 `destValue`로 대체한다
 *
 * @param range clamp 상하한 값
 * @return 오차범위 내에 있다면 대체할 값, 아니면 현재 값을 반환
 * @see approximateEqual
 */
fun <T: Comparable<T>> Sequence<T>.rangeClamp(range: ClosedRange<T>): Sequence<T> = map { it.coerceIn(range) }

/**
 * 요소 값이 `destValue`와 오차범위 내에 있다면 `destValue`로 대체한다
 *
 * @param range clamp 상하한 값
 * @return 오차범위 내에 있다면 대체할 값, 아니면 현재 값을 반환
 * @see approximateEqual
 */
fun <T: Comparable<T>> Iterable<T>.rangeClamp(range: ClosedRange<T>): List<T> = map { it.coerceIn(range) }
