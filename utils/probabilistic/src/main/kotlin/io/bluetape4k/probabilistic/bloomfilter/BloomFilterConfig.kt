package io.bluetape4k.probabilistic.bloomfilter

import io.bluetape4k.support.requireGt
import io.bluetape4k.support.requireLt
import io.bluetape4k.support.requirePositiveNumber
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

private const val DEFAULT_EXPECTED_INSERTIONS = 1_000_000L
private const val DEFAULT_FALSE_POSITIVE_PROBABILITY = 0.03
private const val MAX_SUPPORTED_BIT_SIZE = Int.MAX_VALUE.toLong() * Long.SIZE_BITS
private val LN_2 = ln(2.0)
private val LN_2_SQUARED = LN_2 * LN_2

/**
 * Bloom Filter 생성 설정입니다.
 *
 * @property expectedInsertions 예상 삽입 원소 수. 0보다 커야 합니다.
 * @property falsePositiveProbability 목표 오탐률. `(0, 1)` 배타 구간만 허용합니다.
 */
data class BloomFilterConfig(
    val expectedInsertions: Long = DEFAULT_EXPECTED_INSERTIONS,
    val falsePositiveProbability: Double = DEFAULT_FALSE_POSITIVE_PROBABILITY,
) {

    /** 계산된 bitset 크기입니다. */
    val bitSize: Long

    /** 계산된 해시 함수 개수입니다. */
    val hashFunctionCount: Int

    init {
        expectedInsertions.requirePositiveNumber("expectedInsertions")
        falsePositiveProbability.requireGt(0.0, "falsePositiveProbability")
        falsePositiveProbability.requireLt(1.0, "falsePositiveProbability")

        val calculatedBitSize = optimalBitSize(expectedInsertions, falsePositiveProbability)
        require(calculatedBitSize <= MAX_SUPPORTED_BIT_SIZE) {
            "bitSize must be less than or equal to $MAX_SUPPORTED_BIT_SIZE"
        }

        bitSize = calculatedBitSize
        hashFunctionCount = optimalHashFunctionCount(expectedInsertions, calculatedBitSize)
    }
}

internal fun optimalBitSize(expectedInsertions: Long, fpp: Double): Long =
    ceil(-expectedInsertions.toDouble() * ln(fpp) / LN_2_SQUARED).toLong()

internal fun optimalHashFunctionCount(expectedInsertions: Long, bitSize: Long): Int =
    max(1, ((bitSize.toDouble() / expectedInsertions.toDouble()) * LN_2).roundToInt())

internal fun expectedFpp(bitCount: Long, bitSize: Long, hashFunctionCount: Int): Double {
    if (bitCount <= 0L) {
        return 0.0
    }
    val fillRatio = bitCount.toDouble() / bitSize.toDouble()
    return fillRatio.pow(hashFunctionCount.toDouble())
}

internal fun approximateElementCount(bitCount: Long, bitSize: Long, hashFunctionCount: Int): Long {
    if (bitCount <= 0L) {
        return 0L
    }
    if (bitCount >= bitSize) {
        return Long.MAX_VALUE
    }
    val fraction = 1.0 - bitCount.toDouble() / bitSize.toDouble()
    return ceil(-(bitSize.toDouble() / hashFunctionCount.toDouble()) * ln(fraction)).toLong()
}
