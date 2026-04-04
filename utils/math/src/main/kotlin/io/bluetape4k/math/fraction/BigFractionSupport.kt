package io.bluetape4k.math.fraction

import io.bluetape4k.support.toBigInt
import org.apache.commons.math3.fraction.BigFraction

/**
 * [BigFraction]에 정수를 더합니다.
 *
 * ```kotlin
 * val f = BigFraction(1, 2)
 * val result = f + 1L   // BigFraction(3, 2) == 3/2
 * ```
 */
operator fun BigFraction.plus(scalar: Number): BigFraction = this.add(scalar.toLong())

/**
 * 두 [BigFraction]을 더합니다.
 *
 * ```kotlin
 * val f1 = BigFraction(1, 2)
 * val f2 = BigFraction(1, 3)
 * val result = f1 + f2   // BigFraction(5, 6) == 5/6
 * ```
 */
operator fun BigFraction.plus(that: BigFraction): BigFraction = this.add(that)

/**
 * [BigFraction]에서 정수를 뺍니다.
 *
 * ```kotlin
 * val f = BigFraction(3, 2)
 * val result = f - 1L   // BigFraction(1, 2) == 1/2
 * ```
 */
operator fun BigFraction.minus(scalar: Number): BigFraction = this.subtract(scalar.toLong())

/**
 * 두 [BigFraction]을 뺍니다.
 *
 * ```kotlin
 * val f1 = BigFraction(1, 2)
 * val f2 = BigFraction(1, 3)
 * val result = f1 - f2   // BigFraction(1, 6) == 1/6
 * ```
 */
operator fun BigFraction.minus(that: BigFraction): BigFraction = this.subtract(that)

/**
 * [BigFraction]에 정수를 곱합니다.
 *
 * ```kotlin
 * val f = BigFraction(1, 2)
 * val result = f * 3L   // BigFraction(3, 2) == 3/2
 * ```
 */
operator fun BigFraction.times(scalar: Number): BigFraction = this.multiply(scalar.toLong())

/**
 * 두 [BigFraction]을 곱합니다.
 *
 * ```kotlin
 * val f1 = BigFraction(1, 2)
 * val f2 = BigFraction(2, 3)
 * val result = f1 * f2   // BigFraction(1, 3) == 1/3
 * ```
 */
operator fun BigFraction.times(that: BigFraction): BigFraction = this.multiply(that)

/**
 * [BigFraction]을 정수로 나눕니다.
 *
 * ```kotlin
 * val f = BigFraction(3, 2)
 * val result = f / 3L   // BigFraction(1, 2) == 1/2
 * ```
 */
operator fun BigFraction.div(scalar: Number): BigFraction = this.divide(scalar.toLong())

/**
 * 두 [BigFraction]을 나눕니다.
 *
 * ```kotlin
 * val f1 = BigFraction(1, 2)
 * val f2 = BigFraction(1, 3)
 * val result = f1 / f2   // BigFraction(3, 2) == 3/2
 * ```
 */
operator fun BigFraction.div(that: BigFraction): BigFraction = this.divide(that)

/**
 * 정수로부터 [BigFraction]을 생성합니다.
 *
 * ```kotlin
 * val f = bigFractionOf(3)   // BigFraction(3) == 3/1
 * ```
 */
fun <N: Number> bigFractionOf(numerator: N): BigFraction =
    BigFraction(numerator.toBigInt())

/**
 * 분자와 분모로부터 [BigFraction]을 생성합니다.
 *
 * ```kotlin
 * val f = bigFractionOf(1, 3)   // BigFraction(1, 3) == 1/3
 * ```
 */
fun <N: Number> bigFractionOf(numerator: N, denominator: N): BigFraction =
    BigFraction(numerator.toBigInt(), denominator.toBigInt())

/**
 * 실수값을 분수로 근사하여 [BigFraction]을 생성합니다.
 *
 * ```kotlin
 * val f = bigFractionOf(0.333)   // BigFraction(1, 3) ≈ 1/3
 * ```
 */
fun bigFractionOf(value: Double, epsilon: Double = 0.0, maxIterations: Int = 100): BigFraction =
    BigFraction(value, epsilon, maxIterations)

/**
 * 분자와 분모를 기약분수로 약분하여 [BigFraction]을 생성합니다.
 *
 * ```kotlin
 * val f = reducedBigFractionOf(4, 6)   // BigFraction(2, 3) == 2/3
 * ```
 */
fun <N: Number> reducedBigFractionOf(numerator: N, denominator: N): BigFraction =
    BigFraction.getReducedFraction(numerator.toInt(), denominator.toInt())
