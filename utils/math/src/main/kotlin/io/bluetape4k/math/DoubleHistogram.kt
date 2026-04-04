package io.bluetape4k.math

/**
 * Double 값을 기준으로 히스토그램(bin)을 만듭니다.
 *
 * ```kotlin
 * data class Score(val value: Double)
 * val scores = sequenceOf(Score(1.5), Score(2.5), Score(3.5))
 * val bins = scores.binByDouble(1.0) { it.value }
 * // Double 범위로 구분된 BinModel
 * ```
 */
inline fun <T: Any> Sequence<T>.binByDouble(
    binSize: Double,
    valueMapper: (T) -> Double,
    rangeStart: Double? = null,
): BinModel<List<T>, Double> =
    asIterable().binByDouble(binSize, valueMapper, rangeStart)

/**
 * Double 값을 기준으로 히스토그램(bin)을 만듭니다.
 *
 * ```kotlin
 * data class Score(val value: Double)
 * val scores = listOf(Score(1.5), Score(2.5), Score(3.5))
 * val bins = scores.binByDouble(1.0) { it.value }
 * // Double 범위로 구분된 BinModel
 * ```
 */
inline fun <T: Any> Iterable<T>.binByDouble(
    binSize: Double,
    valueMapper: (T) -> Double,
    rangeStart: Double? = null,
): BinModel<List<T>, Double> =
    binByDouble(binSize, valueMapper, { it }, rangeStart)

/**
 * Double 값을 기준으로 그룹화된 히스토그램(bin)을 만듭니다.
 *
 * ```kotlin
 * data class Score(val value: Double)
 * val scores = listOf(Score(1.5), Score(2.5), Score(3.5))
 * val bins = scores.binByDouble(1.0, { it.value }, { it.size })
 * // 각 bin에 포함된 요소 수를 집계한 BinModel
 * ```
 */
inline fun <T: Any, G: Any> Iterable<T>.binByDouble(
    binSize: Double,
    valueMapper: (T) -> Double,
    crossinline groupOp: (List<T>) -> G,
    rangeStart: Double? = null,
): BinModel<G, Double> =
    binByComparable({ it + binSize }, valueMapper, groupOp, rangeStart)
