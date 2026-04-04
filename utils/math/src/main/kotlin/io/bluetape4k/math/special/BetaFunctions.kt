package io.bluetape4k.math.special

import io.bluetape4k.support.assertPositiveNumber
import org.apache.commons.math3.special.Gamma.logGamma
import kotlin.math.exp

/**
 * 베타 함수의 자연로그를 계산합니다. `ln(B(x, y))`
 *
 * ```kotlin
 * val result = betaLn(1.0, 1.0)   // 0.0 (B(1,1) = 1)
 * ```
 */
fun betaLn(x: Double, y: Double): Double {
    x.assertPositiveNumber("x")
    y.assertPositiveNumber("y")

    return logGamma(x) + logGamma(y) - logGamma(x + y)
}

/**
 * 두 시퀀스의 대응하는 요소 쌍에 대해 베타 함수의 자연로그를 계산합니다.
 *
 * ```kotlin
 * val result = betaLn(sequenceOf(1.0, 2.0), sequenceOf(3.0, 4.0)).toList()
 * // [betaLn(1,3), betaLn(2,4)]
 * ```
 */
fun betaLn(xs: Sequence<Double>, ys: Sequence<Double>): Sequence<Double> =
    xs.zip(ys).map { (x, y) -> betaLn(x, y) }

/**
 * 두 Iterable의 대응하는 요소 쌍에 대해 베타 함수의 자연로그를 계산합니다.
 *
 * ```kotlin
 * val result = betaLn(listOf(1.0, 2.0), listOf(3.0, 4.0))
 * // DoubleArray of [betaLn(1,3), betaLn(2,4)]
 * ```
 */
fun betaLn(xs: Iterable<Double>, ys: Iterable<Double>): DoubleArray {
    val results = mutableListOf<Double>()
    val xe = xs.iterator()
    val ye = ys.iterator()
    while (xe.hasNext() && ye.hasNext()) {
        results.add(betaLn(xe.next(), ye.next()))
    }
    return results.toDoubleArray()
}

/**
 * 두 DoubleArray의 대응하는 요소 쌍에 대해 베타 함수의 자연로그를 계산합니다.
 *
 * ```kotlin
 * val result = betaLn(doubleArrayOf(1.0, 2.0), doubleArrayOf(3.0, 4.0))
 * // DoubleArray of [betaLn(1,3), betaLn(2,4)]
 * ```
 */
fun betaLn(xs: DoubleArray, ys: DoubleArray): DoubleArray {
    val minSize = minOf(xs.size, ys.size)
    return DoubleArray(minSize) {
        betaLn(xs[it], ys[it])
    }
}

/**
 * 두 List의 대응하는 요소 쌍에 대해 베타 함수의 자연로그를 계산합니다.
 *
 * ```kotlin
 * val result = betaLn(listOf(1.0, 2.0), listOf(3.0, 4.0))
 * // [betaLn(1,3), betaLn(2,4)]
 * ```
 */
fun betaLn(xs: List<Double>, ys: List<Double>): List<Double> {
    val minSize = minOf(xs.size, ys.size)
    return List(minSize) {
        betaLn(xs[it], ys[it])
    }
}

/**
 * 베타 함수 `B(x, y)`를 계산합니다.
 *
 * ```kotlin
 * val result = beta(1.0, 1.0)   // 1.0
 * val result2 = beta(2.0, 2.0)  // 0.1666... (B(2,2) = 1/6)
 * ```
 */
fun beta(x: Double, y: Double): Double = exp(betaLn(x, y))

/**
 * 두 시퀀스의 대응하는 요소 쌍에 대해 베타 함수를 계산합니다.
 *
 * ```kotlin
 * val result = beta(sequenceOf(1.0, 2.0), sequenceOf(1.0, 2.0)).toList()
 * // [beta(1,1), beta(2,2)] == [1.0, 0.1666...]
 * ```
 */
fun beta(xs: Sequence<Double>, ys: Sequence<Double>): Sequence<Double> {
    return betaLn(xs, ys).map { exp(it) }
}

/**
 * 두 Iterable의 대응하는 요소 쌍에 대해 베타 함수를 계산합니다.
 *
 * ```kotlin
 * val result = beta(listOf(1.0, 2.0), listOf(1.0, 2.0))
 * // DoubleArray of [1.0, 0.1666...]
 * ```
 */
fun beta(xs: Iterable<Double>, ys: Iterable<Double>): DoubleArray {
    return betaLn(xs, ys).map { exp(it) }.toDoubleArray()
}

/**
 * 두 DoubleArray의 대응하는 요소 쌍에 대해 베타 함수를 계산합니다.
 *
 * ```kotlin
 * val result = beta(doubleArrayOf(1.0, 2.0), doubleArrayOf(1.0, 2.0))
 * // DoubleArray of [1.0, 0.1666...]
 * ```
 */
fun beta(xs: DoubleArray, ys: DoubleArray): DoubleArray {
    val minSize = minOf(xs.size, ys.size)
    return DoubleArray(minSize) {
        beta(xs[it], ys[it])
    }
}

/**
 * 두 List의 대응하는 요소 쌍에 대해 베타 함수를 계산합니다.
 *
 * ```kotlin
 * val result = beta(listOf(1.0, 2.0), listOf(1.0, 2.0))
 * // [1.0, 0.1666...]
 * ```
 */
fun beta(xs: List<Double>, ys: List<Double>): List<Double> {
    val minSize = minOf(xs.size, ys.size)
    return List(minSize) { beta(xs[it], ys[it]) }
}
