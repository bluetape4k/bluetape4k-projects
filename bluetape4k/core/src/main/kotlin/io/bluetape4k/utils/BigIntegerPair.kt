package io.bluetape4k.utils

import java.math.BigInteger

/**
 * Big integer pairing
 */
object BigIntegerPair {

    private val HALF: BigInteger = BigInteger.ONE.shiftLeft(64)   // 2^64
    private val MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE)

    fun BigInteger.unsinged(): BigInteger =
        if (this.signum() < 0) this + HALF else this

    fun BigInteger.signed(): BigInteger =
        if (this > MAX_LONG) this - HALF else this

    fun pair(hi: BigInteger, lo: BigInteger): BigInteger =
        lo.unsinged() + hi.unsinged() * HALF

    fun unpair(value: BigInteger): Pair<BigInteger, BigInteger> {
        val parts = value.divideAndRemainder(HALF)
        return parts[0].signed() to parts[1].signed()
    }
}
