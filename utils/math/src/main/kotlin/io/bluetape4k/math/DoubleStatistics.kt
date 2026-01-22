package io.bluetape4k.math

import org.apache.commons.math3.stat.StatUtils
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics

val DoubleArray.descriptiveStatistics: Descriptives
    get() = DescriptiveStatistics().apply { forEach { addValue(it) } }.let(::ApacheDescriptives)

fun DoubleArray.geometricMean(begin: Int = 0, length: Int = this.size): Double =
    StatUtils.geometricMean(this, begin, length)

fun DoubleArray.percentile(begin: Int = 0, length: Int = this.size, percentile: Double) =
    StatUtils.percentile(this, begin, length, percentile)

fun DoubleArray.median(begin: Int = 0, length: Int = this.size): Double =
    percentile(begin, length, 50.0)

fun DoubleArray.variance(begin: Int = 0, length: Int = this.size): Double =
    StatUtils.variance(this, begin, length)

fun DoubleArray.sumOfSq(begin: Int = 0, length: Int = this.size): Double =
    StatUtils.sumSq(this, begin, length)

val DoubleArray.stDev: Double
    get() = descriptiveStatistics.standardDeviation

fun DoubleArray.normalize(): DoubleArray =
    StatUtils.normalize(this)!!

fun DoubleArray.mode(begin: Int = 0, length: Int = this.size): DoubleArray =
    StatUtils.mode(this, begin, length)!!

val DoubleArray.kurtosis: Double
    get() = descriptiveStatistics.kurtosis

val DoubleArray.skewness: Double
    get() = descriptiveStatistics.skewness

fun Sequence<Double>.doubleRange() = asIterable().doubleRange()

fun Iterable<Double>.doubleRange() =
    (minOrNull() ?: throw RuntimeException("At least one element must be present"))..
            (maxOrNull() ?: throw RuntimeException("At least one element must be present"))

inline fun <T: Any, K: Any> Sequence<T>.rangeBy(
    keySelector: (T) -> K,
    doubleSelector: (T) -> Double,
): Map<K, ClosedRange<Double>> =
    aggregateBy(keySelector, doubleSelector) { it.range() }

inline fun <T: Any, K: Any> Iterable<T>.rangeBy(
    keySelector: (T) -> K,
    doubleSelector: (T) -> Double,
): Map<K, ClosedRange<Double>> =
    aggregateBy(keySelector, doubleSelector) { it.range() }
