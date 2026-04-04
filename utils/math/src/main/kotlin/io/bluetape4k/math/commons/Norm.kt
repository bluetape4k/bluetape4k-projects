package io.bluetape4k.math.commons

import io.bluetape4k.math.sumOfSq
import io.bluetape4k.math.sumOfSquares

/**
 * 변량들의 제곱의 합을 구합니다.
 *
 * ```kotlin
 * val result = doubleArrayOf(3.0, 4.0).norm()   // 25.0 (3^2 + 4^2)
 * ```
 */
fun DoubleArray.norm(): Double = sumOfSq()

/**
 * 변량들의 제곱의 합을 구합니다.
 *
 * ```kotlin
 * val result = sequenceOf(3.0, 4.0).norm()   // 25.0 (3^2 + 4^2)
 * ```
 */
fun <N: Number> Sequence<N>.norm(): Double = sumOfSquares()

/**
 * 변량들의 제곱의 합을 구합니다.
 *
 * ```kotlin
 * val result = listOf(3.0, 4.0).norm()   // 25.0 (3^2 + 4^2)
 * ```
 */
fun <N: Number> Iterable<N>.norm(): Double = sumOfSquares()
