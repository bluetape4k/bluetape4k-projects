package io.bluetape4k.bloomfilter

import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.pow

internal const val DEFAULT_MAX_NUM: Long = Int.MAX_VALUE.toLong()
internal const val DEFAULT_ERROR_RATE: Double = 1.0e-8

internal fun optimalM(maxNum: Long, errorRate: Double): Int =
    ceil(-maxNum.toDouble() * ln(errorRate) / ln(2.0).pow(2)).toInt()

internal fun optimalK(maxNumber: Long, maxBitSize: Int): Int =
    ceil(ln(2.0) * maxBitSize.toDouble() / maxNumber.toDouble()).toInt()

internal fun optimalK(maxNum: Long, errorRate: Double): Int =
    optimalK(maxNum, optimalM(maxNum, errorRate))
