package io.bluetape4k.netty.buffer

object Medium {
    const val SIZE_BYTES: Int = 3
    const val SIZE_BITS: Int = SIZE_BYTES * Byte.SIZE_BITS
    const val MAX_VALUE: Int = 8_388_607
    const val MIN_VALUE: Int = -8_388_608
}
