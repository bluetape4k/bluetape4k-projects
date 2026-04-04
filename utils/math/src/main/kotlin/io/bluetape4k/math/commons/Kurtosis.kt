package io.bluetape4k.math.commons

import io.bluetape4k.collections.toDoubleArray
import io.bluetape4k.math.kurtosis

/**
 * 변량 분포의 첨예도를 나타냅니다. 값이 작을 수록 뽀족한 분포이고, 값이 클수록 언덕 분포입니다.
 *
 * ```kotlin
 * val result = listOf(1.0, 2.0, 3.0, 4.0, 5.0).kurtosis()
 * // -1.3 (정규분포보다 평탄한 분포)
 * ```
 */
fun <N: Number> Iterable<N>.kurtosis(): Double =
    map { it.toDouble() }.toDoubleArray().kurtosis

/**
 * 변량 분포의 첨예도를 나타냅니다. 값이 작을 수록 뽀족한 분포이고, 값이 클수록 언덕 분포입니다.
 *
 * ```kotlin
 * val result = sequenceOf(1.0, 2.0, 3.0, 4.0, 5.0).kurtosis()
 * // -1.3 (정규분포보다 평탄한 분포)
 * ```
 */
fun <N: Number> Sequence<N>.kurtosis(): Double =
    map { it.toDouble() }.toDoubleArray().kurtosis
