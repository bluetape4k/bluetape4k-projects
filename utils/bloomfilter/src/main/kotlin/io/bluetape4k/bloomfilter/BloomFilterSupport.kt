package io.bluetape4k.bloomfilter

import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.pow

/**
 * 기본 최대 요소 개수 (Int.MAX_VALUE)
 */
internal const val DEFAULT_MAX_NUM: Long = Int.MAX_VALUE.toLong()

/**
 * 기본 오류율 (0.00000001 = 0.000001%)
 */
internal const val DEFAULT_ERROR_RATE: Double = 1.0e-8

/**
 * 주어진 최대 요소 개수와 오류율에 대해 최적의 Bloom Filter 크기(m)을 계산합니다.
 *
 * 공식: m = -n * ln(errorRate) / (ln(2))^2
 *
 * @param maxNum 최대 요소 개수
 * @param errorRate 오류율 (0 < errorRate < 1)
 * @return 최적의 Bloom Filter 크기 (bit 수)
 */
internal fun optimalM(
    maxNum: Long,
    errorRate: Double,
): Int = ceil(-maxNum.toDouble() * ln(errorRate) / ln(2.0).pow(2)).toInt()

/**
 * 주어진 최대 요소 개수와 Bloom Filter 크기에 대해 최적의 해시 함수 개수(k)를 계산합니다.
 *
 * 공식: k = ln(2) * m / n
 *
 * @param maxNumber 최대 요소 개수
 * @param maxBitSize Bloom Filter 크기 (bit 수)
 * @return 최적의 해시 함수 개수
 */
internal fun optimalK(
    maxNumber: Long,
    maxBitSize: Int,
): Int = ceil(ln(2.0) * maxBitSize.toDouble() / maxNumber.toDouble()).toInt()

/**
 * 주어진 최대 요소 개수와 오류율에 대해 최적의 해시 함수 개수(k)를 계산합니다.
 *
 * @param maxNum 최대 요소 개수
 * @param errorRate 오류율 (0 < errorRate < 1)
 * @return 최적의 해시 함수 개수
 */
internal fun optimalK(
    maxNum: Long,
    errorRate: Double,
): Int = optimalK(maxNum, optimalM(maxNum, errorRate))
