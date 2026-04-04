package io.bluetape4k.math.fraction

import org.apache.commons.math3.fraction.Fraction

/**
 * 분수에 정수를 더합니다.
 *
 * ```kotlin
 * val f = Fraction(1, 2)
 * val result = f + 1   // Fraction(3, 2) == 3/2
 * ```
 */
operator fun Fraction.plus(scalar: Number): Fraction = this.add(scalar.toInt())

/**
 * 두 분수를 더합니다.
 *
 * ```kotlin
 * val f1 = Fraction(1, 2)
 * val f2 = Fraction(1, 3)
 * val result = f1 + f2   // Fraction(5, 6) == 5/6
 * ```
 */
operator fun Fraction.plus(that: Fraction): Fraction = this.add(that)

/**
 * 분수에서 정수를 뺍니다.
 *
 * ```kotlin
 * val f = Fraction(3, 2)
 * val result = f - 1   // Fraction(1, 2) == 1/2
 * ```
 */
operator fun Fraction.minus(scalar: Number): Fraction = this.subtract(scalar.toInt())

/**
 * 두 분수를 뺍니다.
 *
 * ```kotlin
 * val f1 = Fraction(1, 2)
 * val f2 = Fraction(1, 3)
 * val result = f1 - f2   // Fraction(1, 6) == 1/6
 * ```
 */
operator fun Fraction.minus(that: Fraction): Fraction = this.subtract(that)

/**
 * 분수에 정수를 곱합니다.
 *
 * ```kotlin
 * val f = Fraction(1, 2)
 * val result = f * 3   // Fraction(3, 2) == 3/2
 * ```
 */
operator fun Fraction.times(scalar: Number): Fraction = this.multiply(scalar.toInt())

/**
 * 두 분수를 곱합니다.
 *
 * ```kotlin
 * val f1 = Fraction(1, 2)
 * val f2 = Fraction(2, 3)
 * val result = f1 * f2   // Fraction(1, 3) == 1/3
 * ```
 */
operator fun Fraction.times(that: Fraction): Fraction = this.multiply(that)

/**
 * 분수를 정수로 나눕니다.
 *
 * ```kotlin
 * val f = Fraction(3, 2)
 * val result = f / 3   // Fraction(1, 2) == 1/2
 * ```
 */
operator fun Fraction.div(scalar: Number): Fraction = this.divide(scalar.toInt())

/**
 * 두 분수를 나눕니다.
 *
 * ```kotlin
 * val f1 = Fraction(1, 2)
 * val f2 = Fraction(1, 3)
 * val result = f1 / f2   // Fraction(3, 2) == 3/2
 * ```
 */
operator fun Fraction.div(that: Fraction): Fraction = this.divide(that)

/**
 * 정수로부터 [Fraction]을 생성합니다.
 *
 * ```kotlin
 * val f = fractionOf(3)   // Fraction(3) == 3/1
 * ```
 */
fun <N: Number> fractionOf(numerator: N): Fraction =
    Fraction(numerator.toInt())

/**
 * 분자와 분모로부터 [Fraction]을 생성합니다.
 *
 * ```kotlin
 * val f = fractionOf(1, 3)   // Fraction(1, 3) == 1/3
 * ```
 */
fun <N: Number> fractionOf(numerator: N, denominator: N): Fraction =
    Fraction(numerator.toInt(), denominator.toInt())

/**
 * 실수값을 분수로 근사하여 [Fraction]을 생성합니다.
 *
 * ```kotlin
 * val f = fractionOf(0.333)   // Fraction(1, 3) ≈ 1/3
 * ```
 */
fun fractionOf(value: Double, epsilon: Double = 0.0, maxIterations: Int = 100): Fraction =
    Fraction(value, epsilon, maxIterations)

/**
 * 분자와 분모를 기약분수로 약분하여 [Fraction]을 생성합니다.
 *
 * ```kotlin
 * val f = reducedFractionOf(4, 6)   // Fraction(2, 3) == 2/3
 * ```
 */
fun <N: Number> reducedFractionOf(numerator: N, denominator: N): Fraction =
    Fraction.getReducedFraction(numerator.toInt(), denominator.toInt())
