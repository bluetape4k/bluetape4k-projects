package io.bluetape4k.netty.buffer

/**
 * Netty 처리에서 사용하는 `Smart` 타입입니다.
 */
object Smart {
    const val MAX_BYTE_VALUE: Int = 63
    const val MIN_BYTE_VALUE: Int = -64
    const val BYTE_MOD: Int = MAX_BYTE_VALUE + 1
    const val MAX_SHORT_VALUE: Int = 16383
    const val MIN_SHORT_VALUE: Int = -16384
    const val SHORT_MOD: Int = MAX_SHORT_VALUE + 1
    const val MAX_INT_VALUE: Int = 1_073_741_823
    const val MIN_INT_VALUE: Int = -1_073_741_824
    const val INT_MOD: Int = MAX_INT_VALUE + 1
}
