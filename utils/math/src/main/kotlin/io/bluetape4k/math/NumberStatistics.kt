package io.bluetape4k.math

import io.bluetape4k.collections.toDoubleArray
import org.apache.commons.math3.stat.StatUtils
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math3.stat.regression.SimpleRegression as ASR

//
// regression
//
/**
 * 시퀀스 요소로부터 단순 선형 회귀 모델을 만듭니다.
 *
 * ```kotlin
 * data class Point(val x: Double, val y: Double)
 * val points = sequenceOf(Point(1.0, 2.0), Point(2.0, 4.0), Point(3.0, 6.0))
 * val reg = points.simpleRegression({ it.x }, { it.y })
 * // reg.slope ≈ 2.0, reg.intercept ≈ 0.0
 * ```
 */
inline fun <T> Sequence<T>.simpleRegression(
    xSelector: (T) -> Number,
    ySelector: (T) -> Number,
): SimpleRegression {
    val r = ASR()
    forEach { r.addData(xSelector(it).toDouble(), ySelector(it).toDouble()) }
    return ApacheSimpleRegression(r)
}

/**
 * 컬렉션 요소로부터 단순 선형 회귀 모델을 만듭니다.
 *
 * ```kotlin
 * data class Point(val x: Double, val y: Double)
 * val points = listOf(Point(1.0, 2.0), Point(2.0, 4.0), Point(3.0, 6.0))
 * val reg = points.simpleRegression({ it.x }, { it.y })
 * // reg.slope ≈ 2.0, reg.intercept ≈ 0.0
 * ```
 */
inline fun <T> Iterable<T>.simpleRegression(
    xSelector: (T) -> Number,
    ySelector: (T) -> Number,
): SimpleRegression = asSequence().simpleRegression(xSelector, ySelector)

/**
 * (x, y) 쌍의 시퀀스로부터 단순 선형 회귀 모델을 만듭니다.
 *
 * ```kotlin
 * val reg = sequenceOf(1.0 to 2.0, 2.0 to 4.0, 3.0 to 6.0).simpleRegression()
 * // reg.slope ≈ 2.0
 * ```
 */
fun Sequence<Pair<Number, Number>>.simpleRegression(): SimpleRegression =
    simpleRegression({ it.first }, { it.second })

/**
 * (x, y) 쌍의 컬렉션으로부터 단순 선형 회귀 모델을 만듭니다.
 *
 * ```kotlin
 * val reg = listOf(1.0 to 2.0, 2.0 to 4.0, 3.0 to 6.0).simpleRegression()
 * // reg.slope ≈ 2.0
 * ```
 */
fun Iterable<Pair<Number, Number>>.simpleRegression(): SimpleRegression =
    simpleRegression({ it.first }, { it.second })

// Simple Number vector
/**
 * 시퀀스의 기술 통계 정보를 계산합니다.
 *
 * ```kotlin
 * val stats = sequenceOf(1.0, 2.0, 3.0, 4.0, 5.0).descriptiveStatistics()
 * // stats.mean == 3.0, stats.standardDeviation ≈ 1.58
 * ```
 */
fun <N: Number> Sequence<N>.descriptiveStatistics(): Descriptives =
    DescriptiveStatistics().apply { forEach { addValue(it.toDouble()) } }.let(::ApacheDescriptives)

/**
 * 컬렉션의 기술 통계 정보를 계산합니다.
 *
 * ```kotlin
 * val stats = listOf(1.0, 2.0, 3.0, 4.0, 5.0).descriptiveStatistics()
 * // stats.mean == 3.0
 * ```
 */
fun <N: Number> Iterable<N>.descriptiveStatistics(): Descriptives =
    asSequence().descriptiveStatistics()

/**
 * 배열의 기술 통계 정보를 계산합니다.
 *
 * ```kotlin
 * val stats = arrayOf(1.0, 2.0, 3.0).descriptiveStatistics()
 * // stats.mean == 2.0
 * ```
 */
fun <N: Number> Array<out N>.descriptiveStatistics(): Descriptives =
    asSequence().descriptiveStatistics()

// Geometric Mean
/**
 * 시퀀스의 기하 평균을 계산합니다.
 *
 * ```kotlin
 * val result = sequenceOf(2.0, 8.0).geometricMean()   // 4.0 (sqrt(2*8))
 * ```
 */
fun <N: Number> Sequence<N>.geometricMean(): Double =
    StatUtils.geometricMean(map { it.toDouble() }.toDoubleArray())

/**
 * 컬렉션의 기하 평균을 계산합니다.
 *
 * ```kotlin
 * val result = listOf(2.0, 8.0).geometricMean()   // 4.0
 * ```
 */
fun <N: Number> Iterable<N>.geometricMean(): Double = asSequence().geometricMean()

/**
 * 배열의 기하 평균을 계산합니다.
 *
 * ```kotlin
 * val result = arrayOf(2.0, 8.0).geometricMean()   // 4.0
 * ```
 */
fun <N: Number> Array<out N>.geometricMean(): Double = asSequence().geometricMean()

// Percentile
/**
 * 시퀀스의 p번째 백분위수를 계산합니다.
 *
 * ```kotlin
 * val result = sequenceOf(1.0, 2.0, 3.0, 4.0, 5.0).percentile(50.0)   // 3.0 (중앙값)
 * ```
 */
fun <N: Number> Sequence<N>.percentile(percentile: Double): Double =
    StatUtils.percentile(map { it.toDouble() }.toDoubleArray(), percentile)

/**
 * 컬렉션의 p번째 백분위수를 계산합니다.
 *
 * ```kotlin
 * val result = listOf(1.0, 2.0, 3.0, 4.0, 5.0).percentile(75.0)   // 4.0
 * ```
 */
fun <N: Number> Iterable<N>.percentile(percentile: Double): Double = asSequence().percentile(percentile)

/**
 * 배열의 p번째 백분위수를 계산합니다.
 *
 * ```kotlin
 * val result = arrayOf(1.0, 2.0, 3.0).percentile(50.0)   // 2.0
 * ```
 */
fun <N: Number> Array<out N>.percentile(percentile: Double): Double = asSequence().percentile(percentile)


// Median
/**
 * 시퀀스의 중앙값(50th percentile)을 계산합니다.
 *
 * ```kotlin
 * val result = sequenceOf(1.0, 2.0, 3.0, 4.0, 5.0).median()   // 3.0
 * ```
 */
fun <N: Number> Sequence<N>.median(): Double = map { it.toDouble() }.percentile(50.0)

/**
 * 컬렉션의 중앙값을 계산합니다.
 *
 * ```kotlin
 * val result = listOf(1.0, 2.0, 3.0).median()   // 2.0
 * ```
 */
fun <N: Number> Iterable<N>.median(): Double = asSequence().median()

/**
 * 배열의 중앙값을 계산합니다.
 *
 * ```kotlin
 * val result = arrayOf(1.0, 3.0, 5.0).median()   // 3.0
 * ```
 */
fun <N: Number> Array<out N>.median(): Double = asSequence().median()

// Veriance
/**
 * 시퀀스의 분산을 계산합니다.
 *
 * ```kotlin
 * val result = sequenceOf(1.0, 2.0, 3.0, 4.0, 5.0).variance()   // 2.5
 * ```
 */
fun <N: Number> Sequence<N>.variance(): Double =
    StatUtils.variance(map { it.toDouble() }.toDoubleArray())

/**
 * 컬렉션의 분산을 계산합니다.
 *
 * ```kotlin
 * val result = listOf(1.0, 2.0, 3.0, 4.0, 5.0).variance()   // 2.5
 * ```
 */
fun <N: Number> Iterable<N>.variance(): Double = asSequence().variance()

/**
 * 배열의 분산을 계산합니다.
 *
 * ```kotlin
 * val result = arrayOf(2.0, 4.0).variance()   // 2.0
 * ```
 */
fun <N: Number> Array<out N>.variance(): Double = asSequence().variance()

// Sum of squares
/**
 * 시퀀스 요소의 제곱 합을 계산합니다.
 *
 * ```kotlin
 * val result = sequenceOf(3.0, 4.0).sumOfSquares()   // 25.0
 * ```
 */
fun <N: Number> Sequence<N>.sumOfSquares(): Double =
    StatUtils.sumSq(map { it.toDouble() }.toDoubleArray())

/**
 * 컬렉션 요소의 제곱 합을 계산합니다.
 *
 * ```kotlin
 * val result = listOf(3.0, 4.0).sumOfSquares()   // 25.0
 * ```
 */
fun <N: Number> Iterable<N>.sumOfSquares(): Double = asSequence().sumOfSquares()

/**
 * 배열 요소의 제곱 합을 계산합니다.
 *
 * ```kotlin
 * val result = arrayOf(3.0, 4.0).sumOfSquares()   // 25.0
 * ```
 */
fun <N: Number> Array<out N>.sumOfSquares(): Double = asSequence().sumOfSquares()

// Standard Deviation
/**
 * 시퀀스의 표준편차를 계산합니다.
 *
 * ```kotlin
 * val result = sequenceOf(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0).stdev()   // 2.0
 * ```
 */
fun <N: Number> Sequence<N>.stdev(): Double = descriptiveStatistics().standardDeviation

/**
 * 컬렉션의 표준편차를 계산합니다.
 *
 * ```kotlin
 * val result = listOf(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0).stdev()   // 2.0
 * ```
 */
fun <N: Number> Iterable<N>.stdev(): Double = descriptiveStatistics().standardDeviation

/**
 * 배열의 표준편차를 계산합니다.
 *
 * ```kotlin
 * val result = arrayOf(1.0, 2.0, 3.0).stdev()   // 1.0
 * ```
 */
fun <N: Number> Array<out N>.stdev(): Double = descriptiveStatistics().standardDeviation

// Normalize
/**
 * 시퀀스를 정규화합니다. (각 요소에서 평균을 빼고 표준편차로 나눔)
 *
 * ```kotlin
 * val result = sequenceOf(1.0, 2.0, 3.0).normalize()   // 정규화된 DoubleArray
 * ```
 */
fun <N: Number> Sequence<N>.normalize(): DoubleArray =
    StatUtils.normalize(map { it.toDouble() }.toDoubleArray())

/**
 * 컬렉션을 정규화합니다.
 *
 * ```kotlin
 * val result = listOf(1.0, 2.0, 3.0).normalize()   // 정규화된 DoubleArray
 * ```
 */
fun <N: Number> Iterable<N>.normalize(): DoubleArray = asSequence().normalize()

/**
 * 배열을 정규화합니다.
 *
 * ```kotlin
 * val result = arrayOf(1.0, 2.0, 3.0).normalize()   // 정규화된 DoubleArray
 * ```
 */
fun <N: Number> Array<out N>.normalize(): DoubleArray = asSequence().normalize()

// Kurtosis
/**
 * 시퀀스의 첨도를 계산합니다.
 *
 * ```kotlin
 * val result = sequenceOf(1.0, 2.0, 3.0, 4.0, 5.0).kurtosis()   // -1.3
 * ```
 */
fun <N: Number> Sequence<N>.kurtosis(): Double = descriptiveStatistics().kurtosis

/**
 * 컬렉션의 첨도를 계산합니다.
 *
 * ```kotlin
 * val result = listOf(1.0, 2.0, 3.0, 4.0, 5.0).kurtosis()   // -1.3
 * ```
 */
fun <N: Number> Iterable<N>.kurtosis(): Double = descriptiveStatistics().kurtosis

/**
 * 배열의 첨도를 계산합니다.
 *
 * ```kotlin
 * val result = arrayOf(1.0, 2.0, 3.0, 4.0, 5.0).kurtosis()   // -1.3
 * ```
 */
fun <N: Number> Array<out N>.kurtosis(): Double = descriptiveStatistics().kurtosis

// Skewness
/**
 * 시퀀스의 왜도를 계산합니다.
 *
 * ```kotlin
 * val result = sequenceOf(1.0, 2.0, 3.0, 4.0, 5.0).skewness()   // 0.0 (대칭 분포)
 * ```
 */
fun <N: Number> Sequence<N>.skewness(): Double = descriptiveStatistics().skewness

/**
 * 컬렉션의 왜도를 계산합니다.
 *
 * ```kotlin
 * val result = listOf(1.0, 2.0, 3.0, 4.0, 5.0).skewness()   // 0.0
 * ```
 */
fun <N: Number> Iterable<N>.skewness(): Double = descriptiveStatistics().skewness

/**
 * 배열의 왜도를 계산합니다.
 *
 * ```kotlin
 * val result = arrayOf(1.0, 2.0, 3.0).skewness()   // 0.0
 * ```
 */
fun <N: Number> Array<out N>.skewness(): Double = descriptiveStatistics().skewness


// Slicing operations
inline fun <T: Any, K: Any> Sequence<T>.descriptiveStatisticsBy(
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, Descriptives> =
    aggregateBy(keySelector, valueMapper) { it.descriptiveStatistics() }

inline fun <T: Any, K: Any> Iterable<T>.descriptiveStatisticsBy(
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, Descriptives> =
    asSequence().descriptiveStatisticsBy(keySelector, valueMapper)

fun <K: Any, N: Number> Sequence<Pair<K, N>>.descriptiveStatisticsBy(): Map<K, Descriptives> =
    aggregateBy({ it.first }, { it.second }) { it.descriptiveStatistics() }

fun <K: Any, N: Number> Iterable<Pair<K, N>>.descriptiveStatisticsBy(): Map<K, Descriptives> =
    asSequence().descriptiveStatisticsBy()

inline fun <T: Any, K: Any> Sequence<T>.medianBy(
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, Double> =
    aggregateBy(keySelector, valueMapper) { it.median() }

inline fun <T: Any, K: Any> Iterable<T>.medianBy(
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, Double> =
    asSequence().medianBy(keySelector, valueMapper)

fun <K: Any, N: Number> Sequence<Pair<K, N>>.medianBy(): Map<K, Double> =
    aggregateBy({ it.first }, { it.second }) { it.median() }

fun <K: Any, N: Number> Iterable<Pair<K, N>>.medianBy(): Map<K, Double> =
    asSequence().medianBy()

inline fun <T: Any, K: Any> Sequence<T>.percentileBy(
    percentile: Double,
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, Double> =
    aggregateBy(keySelector, valueMapper) { it.percentile(percentile) }

inline fun <T: Any, K: Any> Iterable<T>.percentileBy(
    percentile: Double,
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, Double> =
    asSequence().percentileBy(percentile, keySelector, valueMapper)

fun <K: Any, N: Number> Sequence<Pair<K, N>>.percentileBy(percentile: Double): Map<K, Double> =
    aggregateBy({ it.first }, { it.second }) { it.percentile(percentile) }

fun <K: Any, N: Number> Iterable<Pair<K, N>>.percentileBy(percentile: Double): Map<K, Double> =
    asSequence().percentileBy(percentile)

inline fun <T: Any, K: Any> Sequence<T>.sumBy(
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, Double> =
    aggregateBy(keySelector, valueMapper) { it.sumOf { x -> x.toDouble() } }

inline fun <T: Any, K: Any> Iterable<T>.sumBy(
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, Double> =
    asSequence().sumBy(keySelector, valueMapper)

fun <K: Any, N: Number> Sequence<Pair<K, N>>.sumBy(): Map<K, Double> = sumBy({ it.first }, { it.second })
fun <K: Any, N: Number> Iterable<Pair<K, N>>.sumBy(): Map<K, Double> = asSequence().sumBy()

inline fun <T: Any, K: Any> Sequence<T>.averageBy(
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, Double> =
    aggregateBy(keySelector, valueMapper) { it.map { n -> n.toDouble() }.average() }

inline fun <T: Any, K: Any> Iterable<T>.averageBy(
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, Double> =
    asSequence().averageBy(keySelector, valueMapper)

fun <K: Any, N: Number> Sequence<Pair<K, N>>.averageBy(): Map<K, Double> = averageBy({ it.first }, { it.second })
fun <K: Any, N: Number> Iterable<Pair<K, N>>.averageBy(): Map<K, Double> = asSequence().averageBy()


inline fun <T: Any, K: Any> Sequence<T>.varianceBy(
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, Double> =
    aggregateBy(keySelector, valueMapper) { it.variance() }

inline fun <T: Any, K: Any> Iterable<T>.varianceBy(
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, Double> =
    asSequence().varianceBy(keySelector, valueMapper)

fun <K: Any, N: Number> Sequence<Pair<K, N>>.varianceBy(): Map<K, Double> = varianceBy({ it.first }, { it.second })
fun <K: Any, N: Number> Iterable<Pair<K, N>>.varianceBy(): Map<K, Double> = asSequence().varianceBy()

inline fun <T: Any, K: Any> Sequence<T>.stdevBy(
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, Double> =
    aggregateBy(keySelector, valueMapper) { it.stdev() }

inline fun <T: Any, K: Any> Iterable<T>.stdevBy(
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, Double> =
    asSequence().stdevBy(keySelector, valueMapper)

fun <K: Any, N: Number> Sequence<Pair<K, N>>.stdevBy(): Map<K, Double> = stdevBy({ it.first }, { it.second })
fun <K: Any, N: Number> Iterable<Pair<K, N>>.stdevBy(): Map<K, Double> = asSequence().stdevBy()

inline fun <T: Any, K: Any> Sequence<T>.normalizeBy(
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, DoubleArray> =
    aggregateBy(keySelector, valueMapper) { it.normalize() }

inline fun <T: Any, K: Any> Iterable<T>.normalizeBy(
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, DoubleArray> =
    asSequence().normalizeBy(keySelector, valueMapper)

fun <K: Any, N: Number> Sequence<Pair<K, N>>.normalizeBy(): Map<K, DoubleArray> =
    normalizeBy({ it.first }, { it.second })

fun <K: Any, N: Number> Iterable<Pair<K, N>>.normalizeBy(): Map<K, DoubleArray> = asSequence().normalizeBy()

inline fun <T: Any, K: Any> Sequence<T>.geometricMeanBy(
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, Double> =
    aggregateBy(keySelector, valueMapper) { it.geometricMean() }

inline fun <T: Any, K: Any> Iterable<T>.geometricMeanBy(
    keySelector: (T) -> K,
    valueMapper: (T) -> Number,
): Map<K, Double> =
    asSequence().geometricMeanBy(keySelector, valueMapper)

fun <K: Any, N: Number> Sequence<Pair<K, N>>.geometricMeanBy(): Map<K, Double> =
    geometricMeanBy({ it.first }, { it.second })

fun <K: Any, N: Number> Iterable<Pair<K, N>>.geometricMeanBy(): Map<K, Double> =
    asSequence().geometricMeanBy()
