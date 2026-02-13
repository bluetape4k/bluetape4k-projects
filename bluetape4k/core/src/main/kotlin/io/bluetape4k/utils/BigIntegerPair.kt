package io.bluetape4k.utils

import io.bluetape4k.utils.BigIntegerPair.pair
import java.math.BigInteger

/**
 * 두 개의 64-bit 정수를 하나의 [BigInteger] 값으로 결합/복원하는 유틸리티입니다.
 */
@Suppress("NOTHING_TO_INLINE")
object BigIntegerPair {

    val HALF: BigInteger = BigInteger.ONE.shiftLeft(64)   // 2^64
    val MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE)

    /**
     * signed long 범위의 값을 unsigned 64-bit 표현으로 변환합니다.
     */
    inline fun BigInteger.unsigned(): BigInteger =
        if (this.signum() < 0) this + HALF else this

    /**
     * unsigned 64-bit 표현값을 signed long 범위 표현으로 복원합니다.
     */
    inline fun BigInteger.signed(): BigInteger =
        if (this > MAX_LONG) this - HALF else this

    /**
     * [hi], [lo] 값을 하나의 [BigInteger]로 결합합니다.
     */
    inline fun pair(hi: BigInteger, lo: BigInteger): BigInteger =
        lo.unsigned() + hi.unsigned() * HALF

    /**
     * [pair]로 결합한 값을 원래의 `[hi, lo]` 쌍으로 복원합니다.
     */
    inline fun unpair(value: BigInteger): Pair<BigInteger, BigInteger> {
        val parts = value.divideAndRemainder(HALF)
        return parts[0].signed() to parts[1].signed()
    }
}
