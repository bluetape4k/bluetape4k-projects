package io.bluetape4k.math.commons

import kotlin.math.absoluteValue

/**
 * 컬렉션의 Min, Max Value 를 가져옵니다.
 *
 * ```
 * val (min, max) = sequenceOf(-1.0, 0.0, 1.0).minMax()  // min=-1.0, max=1.0
 * ```
 *
 * @return Min 값, Max 값의 Pair
 */
fun Sequence<Double>.minMax(): Pair<Double, Double> {
    var min = Double.MAX_VALUE
    var max = Double.MIN_VALUE

    this
        .filter { !it.isNaN() }
        .forEach { x ->
            if (x < min) min = x
            if (x > max) max = x
        }

    return min to max
}

/**
 * 컬렉션의 Min, Max Value 를 가져옵니다.
 *
 * ```
 * val (min, max) = listOf(-1.0, 0.0, 1.0).minMax()  // min=-1.0, max=1.0
 * ```
 *
 * @return Min 값, Max 값의 Pair
 */
fun Iterable<Double>.minMax(): Pair<Double, Double> = asSequence().minMax()

/**
 * 컬렉션의 Min, Max Value 를 가져옵니다.
 *
 * ```
 * val (min, max) = doubleArrayOf(-1.0, 0.0, 1.0).minMax()  // min=-1.0, max=1.0
 * ```
 *
 * @return Min 값, Max 값의 Pair
 */
fun DoubleArray.minMax(): Pair<Double, Double> = asSequence().minMax()

/**
 * 컬렉션 요소의 절대값의 Min, Max 에 해당하는 요소를 가져옵니다.
 *
 * ```
 * val (min, max) = sequenceOf(-2.0, -1.0, 4.0).absMinMax()  // min: 1.0, max: 4.0
 * ```
 *
 * @return Min 값, Max 값의 Pair
 */
fun Sequence<Double>.absMinMax(): Pair<Double, Double> {
    var min = Double.MAX_VALUE
    var max = Double.MIN_VALUE

    this
        .filter { !it.isNaN() }
        .map { it.absoluteValue }
        .forEach { x ->
            if (x < min) min = x
            if (x > max) max = x
        }

    return min to max
}

/**
 * 컬렉션 요소의 절대값의 Min, Max 에 해당하는 요소를 가져옵니다.
 *
 * ```
 * val (min, max) = listOf(-2.0, -1.0, 4.0).absMinMax()  // min: 1.0, max: 4.0
 * ```
 *
 * @return Min 값, Max 값의 Pair
 */
fun Iterable<Double>.absMinMax(): Pair<Double, Double> = asSequence().absMinMax()

/**
 * 컬렉션 요소의 절대값의 Min, Max 에 해당하는 요소를 가져옵니다.
 *
 * ```
 * val (min, max) = doubleArrayOf(-2.0, -1.0, 4.0).absMinMax()  // min: 1.0, max: 4.0
 * ```
 *
 * @return Min 값, Max 값의 Pair
 */
fun DoubleArray.absMinMax(): Pair<Double, Double> = asSequence().absMinMax()
