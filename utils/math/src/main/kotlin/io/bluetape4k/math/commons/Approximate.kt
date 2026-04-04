package io.bluetape4k.math.commons

import io.bluetape4k.math.MathConsts.EPSILON
import io.bluetape4k.math.MathConsts.FLOAT_EPSILON
import java.math.BigDecimal
import kotlin.math.abs
import kotlin.math.absoluteValue

/**
 * 두 Double 값이 주어진 오차(epsilon) 범위 내에 있는지 비교합니다.
 *
 * ```kotlin
 * val result = 1.0000001.approximateEqual(1.0, 1e-6)   // true
 * val result2 = 1.1.approximateEqual(1.0, 1e-6)        // false
 * ```
 *
 * @param that 비교할 값
 * @param epsilon 허용 오차 (기본값: [EPSILON])
 * @return 오차 범위 내이면 `true`
 */
fun Double.approximateEqual(that: Double, epsilon: Double = EPSILON): Boolean =
    abs(this - that) < epsilon.absoluteValue

/**
 * 두 Float 값이 주어진 오차(epsilon) 범위 내에 있는지 비교합니다.
 *
 * ```kotlin
 * val result = 1.0000001f.approximateEqual(1.0f, 1e-6f)   // true
 * ```
 *
 * @param that 비교할 값
 * @param epsilon 허용 오차 (기본값: [FLOAT_EPSILON])
 * @return 오차 범위 내이면 `true`
 */
fun Float.approximateEqual(that: Float, epsilon: Float = FLOAT_EPSILON): Boolean =
    abs(this - that) < abs(epsilon)

/**
 * 두 BigDecimal 값이 주어진 오차(epsilon) 범위 내에 있는지 비교합니다.
 *
 * ```kotlin
 * val a = BigDecimal("1.0000001")
 * val b = BigDecimal("1.0")
 * val result = a.approximateEqual(b, BigDecimal("1e-6"))   // true
 * ```
 *
 * @param that 비교할 값
 * @param epsilon 허용 오차
 * @return 오차 범위 내이면 `true`
 */
fun BigDecimal.approximateEqual(that: BigDecimal, epsilon: BigDecimal = EPSILON.toBigDecimal()): Boolean =
    (this - that).abs() < epsilon.abs()

/**
 * Double 컬렉션에서 `that`과 오차 범위 내에 있는 요소만 필터링합니다.
 *
 * ```kotlin
 * val result = listOf(1.0, 1.0000001, 2.0).filterApproximate(1.0, 1e-6)
 * // [1.0, 1.0000001]
 * ```
 */
fun Iterable<Double>.filterApproximate(
    that: Double,
    epsilon: Double = EPSILON,
    destination: MutableList<Double> = mutableListOf(),
): List<Double> {
    filterTo(destination) { it.approximateEqual(that, epsilon) }
    return destination
}

/**
 * Float 컬렉션에서 `that`과 오차 범위 내에 있는 요소만 필터링합니다.
 *
 * ```kotlin
 * val result = listOf(1.0f, 1.0000001f, 2.0f).filterApproximate(1.0f, 1e-6f)
 * // [1.0, 1.0000001]
 * ```
 */
fun Iterable<Float>.filterApproximate(
    that: Float,
    epsilon: Float = FLOAT_EPSILON,
    destination: MutableList<Float> = mutableListOf(),
): List<Float> {
    filterTo(destination) { it.approximateEqual(that, epsilon) }
    return destination
}

/**
 * BigDecimal 컬렉션에서 `that`과 오차 범위 내에 있는 요소만 필터링합니다.
 *
 * ```kotlin
 * val result = listOf(BigDecimal("1.0"), BigDecimal("1.0000001"), BigDecimal("2.0"))
 *     .filterApproximate(BigDecimal("1.0"), BigDecimal("1e-6"))
 * // [BigDecimal("1.0"), BigDecimal("1.0000001")]
 * ```
 */
fun Iterable<BigDecimal>.filterApproximate(
    that: BigDecimal,
    epsilon: BigDecimal = EPSILON.toBigDecimal(),
    destination: MutableList<BigDecimal> = mutableListOf(),
): List<BigDecimal> {
    filterTo(destination) { it.approximateEqual(that, epsilon) }
    return destination
}

/**
 * Double 시퀀스에서 `that`과 오차 범위 내에 있는 요소만 필터링합니다.
 *
 * ```kotlin
 * val result = sequenceOf(1.0, 1.0000001, 2.0).filterApproximate(1.0, 1e-6).toList()
 * // [1.0, 1.0000001]
 * ```
 */
fun Sequence<Double>.filterApproximate(
    that: Double,
    epsilon: Double = EPSILON,
): Sequence<Double> {
    return filter { it.approximateEqual(that, epsilon) }
}

/**
 * Float 시퀀스에서 `that`과 오차 범위 내에 있는 요소만 필터링합니다.
 *
 * ```kotlin
 * val result = sequenceOf(1.0f, 1.0000001f, 2.0f).filterApproximate(1.0f, 1e-6f).toList()
 * // [1.0, 1.0000001]
 * ```
 */
fun Sequence<Float>.filterApproximate(
    that: Float,
    epsilon: Float = FLOAT_EPSILON,
): Sequence<Float> {
    return filter { it.approximateEqual(that, epsilon) }
}

/**
 * BigDecimal 시퀀스에서 `that`과 오차 범위 내에 있는 요소만 필터링합니다.
 *
 * ```kotlin
 * val result = sequenceOf(BigDecimal("1.0"), BigDecimal("1.0000001"), BigDecimal("2.0"))
 *     .filterApproximate(BigDecimal("1.0"), BigDecimal("1e-6")).toList()
 * // [BigDecimal("1.0"), BigDecimal("1.0000001")]
 * ```
 */
fun Sequence<BigDecimal>.filterApproximate(
    that: BigDecimal,
    epsilon: BigDecimal = EPSILON.toBigDecimal(),
): Sequence<BigDecimal> {
    return filter { it.approximateEqual(that, epsilon) }
}
