package io.bluetape4k.math.commons

import java.io.Serializable

/**
 * Moment 정보
 *
 * @property average  평균
 * @property avgDev   평균 편차
 * @property variance 분산
 * @property skew     기울기
 * @property kurtosis 첨도 (뾰족한 정도)
 */
data class Moment(
    val average: Double,
    val avgDev: Double,
    val variance: Double,
    val skew: Double,
    val kurtosis: Double,
): Serializable

/**
 * 변량의 다양한 통계 정보를 한번에 계산합니다. ([Moment]의 요소를 계산합니다)
 *
 * @return [Moment] 인스턴스
 */
fun Sequence<Double>.moment(): Moment {
    assert(this.count() > 1) { "변량의 수는 2개 이상이어야 합니다." }

    val n = this.count()
    val sum = this.sum()
    val avg = sum / n

    var avgDev = 0.0
    var variance = 0.0
    var skew = 0.0
    var kurtosis = 0.0

    forEach { x ->
        val dx = x - avg
        avgDev += dx.abs()

        var p = dx * dx
        variance += p

        p *= dx
        skew += p

        p *= dx
        kurtosis += p
    }

    avgDev /= n
    variance /= (n - 1)

    if (!variance.isNaN() && !variance.approximateEqual(0.0)) {
        skew /= (n * variance * variance)
        kurtosis = kurtosis / (n * variance * variance) - 3.0
    } else {
        skew = Double.NaN
        kurtosis = Double.NaN
    }

    return Moment(avg, avgDev, variance, skew, kurtosis)
}

/**
 * 변량의 다양한 통계 정보를 한번에 계산합니다. ([Moment]의 요소를 계산합니다)
 *
 * @return [Moment] 인스턴스
 */
fun Iterable<Double>.moment(): Moment = this.asSequence().moment()

/**
 * 변량의 다양한 통계 정보를 한번에 계산합니다. ([Moment]의 요소를 계산합니다)
 *
 * @return [Moment] 인스턴스
 */
fun DoubleArray.moment(): Moment = this.asSequence().moment()
