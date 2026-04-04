package io.bluetape4k.math

import org.apache.commons.math3.stat.StatUtils
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics

/**
 * DoubleArray에 대한 기술 통계 정보를 계산합니다.
 *
 * ```kotlin
 * val stats = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0).descriptiveStatistics
 * // stats.mean == 3.0
 * ```
 */
val DoubleArray.descriptiveStatistics: Descriptives
    get() = DescriptiveStatistics().apply { forEach { addValue(it) } }.let(::ApacheDescriptives)

/**
 * DoubleArray의 기하 평균을 계산합니다.
 *
 * ```kotlin
 * val result = doubleArrayOf(2.0, 8.0).geometricMean()   // 4.0
 * ```
 */
fun DoubleArray.geometricMean(
    begin: Int = 0,
    length: Int = this.size,
): Double = StatUtils.geometricMean(this, begin, length)

/**
 * DoubleArray의 p번째 백분위수를 계산합니다.
 *
 * ```kotlin
 * val result = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0).percentile(percentile = 50.0)
 * // 3.0 (중앙값)
 * ```
 */
fun DoubleArray.percentile(
    begin: Int = 0,
    length: Int = this.size,
    percentile: Double,
) = StatUtils.percentile(this, begin, length, percentile)

/**
 * DoubleArray의 중앙값을 계산합니다.
 *
 * ```kotlin
 * val result = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0).median()   // 3.0
 * ```
 */
fun DoubleArray.median(
    begin: Int = 0,
    length: Int = this.size,
): Double = percentile(begin, length, 50.0)

/**
 * DoubleArray의 분산을 계산합니다.
 *
 * ```kotlin
 * val result = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0).variance()   // 2.5
 * ```
 */
fun DoubleArray.variance(
    begin: Int = 0,
    length: Int = this.size,
): Double = StatUtils.variance(this, begin, length)

/**
 * DoubleArray 요소의 제곱 합을 계산합니다.
 *
 * ```kotlin
 * val result = doubleArrayOf(3.0, 4.0).sumOfSq()   // 25.0
 * ```
 */
fun DoubleArray.sumOfSq(
    begin: Int = 0,
    length: Int = this.size,
): Double = StatUtils.sumSq(this, begin, length)

/**
 * DoubleArray의 표준편차를 계산합니다.
 *
 * ```kotlin
 * val result = doubleArrayOf(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0).stDev   // 2.0
 * ```
 */
val DoubleArray.stDev: Double
    get() = descriptiveStatistics.standardDeviation

/**
 * DoubleArray를 정규화합니다.
 *
 * ```kotlin
 * val result = doubleArrayOf(1.0, 2.0, 3.0).normalize()   // 정규화된 배열
 * ```
 */
fun DoubleArray.normalize(): DoubleArray = StatUtils.normalize(this) ?: error("normalize() 결과가 null입니다.")

/**
 * DoubleArray에서 최빈값(가장 자주 나타나는 값)을 계산합니다.
 *
 * ```kotlin
 * val result = doubleArrayOf(1.0, 2.0, 2.0, 3.0).mode()   // [2.0]
 * ```
 */
fun DoubleArray.mode(
    begin: Int = 0,
    length: Int = this.size,
): DoubleArray = StatUtils.mode(this, begin, length) ?: error("mode() 결과가 null입니다.")

/**
 * DoubleArray의 첨도를 계산합니다.
 *
 * ```kotlin
 * val result = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0).kurtosis   // -1.3
 * ```
 */
val DoubleArray.kurtosis: Double
    get() = descriptiveStatistics.kurtosis

/**
 * DoubleArray의 왜도를 계산합니다.
 *
 * ```kotlin
 * val result = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0).skewness   // 0.0
 * ```
 */
val DoubleArray.skewness: Double
    get() = descriptiveStatistics.skewness

/**
 * Double 시퀀스의 최솟값..최댓값 범위를 계산합니다.
 *
 * ```kotlin
 * val result = sequenceOf(1.0, 3.0, 2.0).doubleRange()   // 1.0..3.0
 * ```
 */
fun Sequence<Double>.doubleRange() = asIterable().doubleRange()

/**
 * Double 컬렉션의 최솟값..최댓값 범위를 계산합니다.
 *
 * ```kotlin
 * val result = listOf(1.0, 5.0, 3.0).doubleRange()   // 1.0..5.0
 * ```
 */
fun Iterable<Double>.doubleRange() =
    (
        minOrNull() ?: error(
            "doubleRange()에는 최소 하나 이상의 요소가 필요합니다."
        )
    )..(maxOrNull() ?: error("doubleRange()에는 최소 하나 이상의 요소가 필요합니다."))

inline fun <T : Any, K : Any> Sequence<T>.rangeBy(
    keySelector: (T) -> K,
    doubleSelector: (T) -> Double,
): Map<K, ClosedRange<Double>> = aggregateBy(keySelector, doubleSelector) { it.range() }

inline fun <T : Any, K : Any> Iterable<T>.rangeBy(
    keySelector: (T) -> K,
    doubleSelector: (T) -> Double,
): Map<K, ClosedRange<Double>> = aggregateBy(keySelector, doubleSelector) { it.range() }
