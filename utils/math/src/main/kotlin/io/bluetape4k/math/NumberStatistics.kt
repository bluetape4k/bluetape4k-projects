package io.bluetape4k.math

import org.apache.commons.math3.stat.StatUtils
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.stat.regression.SimpleRegression as ASR

//
// regression
//
inline fun <T> Sequence<T>.simpleRegression(
    xSelector: (T) -> Number,
    ySelector: (T) -> Number,
): SimpleRegression {
    val r = ASR()
    forEach { r.addData(xSelector(it).toDouble(), ySelector(it).toDouble()) }
    return ApacheSimpleRegression(r)
}

inline fun <T> Iterable<T>.simpleRegression(
    xSelector: (T) -> Number,
    ySelector: (T) -> Number,
): SimpleRegression = asSequence().simpleRegression(xSelector, ySelector)


fun Sequence<Pair<Number, Number>>.simpleRegression(): SimpleRegression =
    simpleRegression({ it.first }, { it.second })

fun Iterable<Pair<Number, Number>>.simpleRegression(): SimpleRegression =
    simpleRegression({ it.first }, { it.second })

// Simple Number vector
fun <N: Number> Sequence<N>.descriptiveStatistics(): Descriptives =
    DescriptiveStatistics().apply { forEach { addValue(it.toDouble()) } }.let(::ApacheDescriptives)

fun <N: Number> Iterable<N>.descriptiveStatistics(): Descriptives =
    asSequence().descriptiveStatistics()

fun <N: Number> Array<out N>.descriptiveStatistics(): Descriptives =
    asSequence().descriptiveStatistics()

// Geometric Mean
fun <N: Number> Sequence<N>.geometricMean(): Double =
    StatUtils.geometricMean(map { it.toDouble() }.toList().toDoubleArray())

fun <N: Number> Iterable<N>.geometricMean(): Double = asSequence().geometricMean()
fun <N: Number> Array<out N>.geometricMean(): Double = asSequence().geometricMean()

// Percentile
fun <N: Number> Sequence<N>.percentile(percentile: Double): Double =
    StatUtils.percentile(map { it.toDouble() }.toList().toDoubleArray(), percentile)

fun <N: Number> Iterable<N>.percentile(percentile: Double): Double = asSequence().percentile(percentile)
fun <N: Number> Array<out N>.percentile(percentile: Double): Double = asSequence().percentile(percentile)


// Median
fun <N: Number> Sequence<N>.median(): Double = map { it.toDouble() }.percentile(50.0)
fun <N: Number> Iterable<N>.median(): Double = asSequence().median()
fun <N: Number> Array<out N>.median(): Double = asSequence().median()

// Veriance
fun <N: Number> Sequence<N>.variance(): Double =
    StatUtils.variance(map { it.toDouble() }.toList().toDoubleArray())

fun <N: Number> Iterable<N>.variance(): Double = asSequence().variance()
fun <N: Number> Array<out N>.variance(): Double = asSequence().variance()

// Sum of squares
fun <N: Number> Sequence<N>.sumOfSquares(): Double =
    StatUtils.sumSq(map { it.toDouble() }.toList().toDoubleArray())

fun <N: Number> Iterable<N>.sumOfSquares(): Double = asSequence().sumOfSquares()
fun <N: Number> Array<out N>.sumOfSquares(): Double = asSequence().sumOfSquares()

// Standard Deviation
fun <N: Number> Sequence<N>.stdev(): Double = descriptiveStatistics().standardDeviation
fun <N: Number> Iterable<N>.stdev(): Double = descriptiveStatistics().standardDeviation
fun <N: Number> Array<out N>.stdev(): Double = descriptiveStatistics().standardDeviation

// Normalize

fun <N: Number> Sequence<N>.normalize(): DoubleArray =
    StatUtils.normalize(map { it.toDouble() }.toList().toDoubleArray())

fun <N: Number> Iterable<N>.normalize(): DoubleArray = asSequence().normalize()
fun <N: Number> Array<out N>.normalize(): DoubleArray = asSequence().normalize()

// Kurtosis
fun <N: Number> Sequence<N>.kurtosis(): Double = descriptiveStatistics().kurtosis
fun <N: Number> Iterable<N>.kurtosis(): Double = descriptiveStatistics().kurtosis
fun <N: Number> Array<out N>.kurtosis(): Double = descriptiveStatistics().kurtosis

// Skewness
fun <N: Number> Sequence<N>.skewness(): Double = descriptiveStatistics().skewness
fun <N: Number> Iterable<N>.skewness(): Double = descriptiveStatistics().skewness
fun <N: Number> Array<out N>.skewness(): Double = descriptiveStatistics().skewness


// Slicing operations
inline fun <T, K> Sequence<T>.descriptiveStatisticsBy(
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, Descriptives> =
    aggregateBy(keySelector, valueMapper) { it.descriptiveStatistics() }

inline fun <T, K> Iterable<T>.descriptiveStatisticsBy(
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, Descriptives> =
    asSequence().descriptiveStatisticsBy(keySelector, valueMapper)

fun <K, N: Number> Sequence<Pair<K, N>>.descriptiveStatisticsBy(): Map<K, Descriptives> =
    aggregateBy({ it.first }, { it.second }) { it.descriptiveStatistics() }

fun <K, N: Number> Iterable<Pair<K, N>>.descriptiveStatisticsBy(): Map<K, Descriptives> =
    asSequence().descriptiveStatisticsBy()

inline fun <T, K> Sequence<T>.medianBy(
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, Double> =
    aggregateBy(keySelector, valueMapper) { it.median() }

inline fun <T, K> Iterable<T>.medianBy(
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, Double> =
    asSequence().medianBy(keySelector, valueMapper)

fun <K, N: Number> Sequence<Pair<K, N>>.medianBy(): Map<K, Double> =
    aggregateBy({ it.first }, { it.second }) { it.median() }

fun <K, N: Number> Iterable<Pair<K, N>>.medianBy(): Map<K, Double> =
    asSequence().medianBy()

inline fun <T, K> Sequence<T>.percentileBy(
    percentile: Double,
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, Double> =
    aggregateBy(keySelector, valueMapper) { it.percentile(percentile) }

inline fun <T, K> Iterable<T>.percentileBy(
    percentile: Double,
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, Double> =
    asSequence().percentileBy(percentile, keySelector, valueMapper)

fun <K, N: Number> Sequence<Pair<K, N>>.percentileBy(percentile: Double): Map<K, Double> =
    aggregateBy({ it.first }, { it.second }) { it.percentile(percentile) }

fun <K, N: Number> Iterable<Pair<K, N>>.percentileBy(percentile: Double): Map<K, Double> =
    asSequence().percentileBy(percentile)

inline fun <T, K> Sequence<T>.sumBy(
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, Double> =
    aggregateBy(keySelector, valueMapper) { it.sumOf { x -> x.toDouble() } }

inline fun <T, K> Iterable<T>.sumBy(
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, Double> =
    asSequence().sumBy(keySelector, valueMapper)

fun <K, N: Number> Sequence<Pair<K, N>>.sumBy(): Map<K, Double> = sumBy({ it.first }, { it.second })
fun <K, N: Number> Iterable<Pair<K, N>>.sumBy(): Map<K, Double> = asSequence().sumBy()

inline fun <T, K> Sequence<T>.averageBy(
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, Double> =
    aggregateBy(keySelector, valueMapper) { it.map { it.toDouble() }.average() }

inline fun <T, K> Iterable<T>.averageBy(
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, Double> =
    asSequence().averageBy(keySelector, valueMapper)

fun <K, N: Number> Sequence<Pair<K, N>>.averageBy(): Map<K, Double> = averageBy({ it.first }, { it.second })
fun <K, N: Number> Iterable<Pair<K, N>>.averageBy(): Map<K, Double> = asSequence().averageBy()


inline fun <T, K> Sequence<T>.varianceBy(
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, Double> =
    aggregateBy(keySelector, valueMapper) { it.variance() }

inline fun <T, K> Iterable<T>.varianceBy(
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, Double> =
    asSequence().varianceBy(keySelector, valueMapper)

fun <K, N: Number> Sequence<Pair<K, N>>.varianceBy(): Map<K, Double> = varianceBy({ it.first }, { it.second })
fun <K, N: Number> Iterable<Pair<K, N>>.varianceBy(): Map<K, Double> = asSequence().varianceBy()

inline fun <T, K> Sequence<T>.stdevBy(
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, Double> =
    aggregateBy(keySelector, valueMapper) { it.stdev() }

inline fun <T, K> Iterable<T>.stdevBy(
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, Double> =
    asSequence().stdevBy(keySelector, valueMapper)

fun <K, N: Number> Sequence<Pair<K, N>>.stdevBy(): Map<K, Double> = stdevBy({ it.first }, { it.second })
fun <K, N: Number> Iterable<Pair<K, N>>.stdevBy(): Map<K, Double> = asSequence().stdevBy()

inline fun <T, K> Sequence<T>.normalizeBy(
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, DoubleArray> =
    aggregateBy(keySelector, valueMapper) { it.normalize() }

inline fun <T, K> Iterable<T>.normalizeBy(
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, DoubleArray> =
    asSequence().normalizeBy(keySelector, valueMapper)

fun <K, N: Number> Sequence<Pair<K, N>>.normalizeBy(): Map<K, DoubleArray> = normalizeBy({ it.first }, { it.second })
fun <K, N: Number> Iterable<Pair<K, N>>.normalizeBy(): Map<K, DoubleArray> = asSequence().normalizeBy()

inline fun <T, K> Sequence<T>.geometricMeanBy(
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, Double> =
    aggregateBy(keySelector, valueMapper) { it.geometricMean() }

inline fun <T, K> Iterable<T>.geometricMeanBy(
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, Double> =
    asSequence().geometricMeanBy(keySelector, valueMapper)

fun <K, N: Number> Sequence<Pair<K, N>>.geometricMeanBy(): Map<K, Double> =
    geometricMeanBy({ it.first }, { it.second })

fun <K, N: Number> Iterable<Pair<K, N>>.geometricMeanBy(): Map<K, Double> =
    asSequence().geometricMeanBy()
