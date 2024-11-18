package io.bluetape4k.netty.buffer

object SmallLong {
    const val SIZE_BYTES: Int = 6
    const val SIZE_BITS: Int = SIZE_BYTES * Byte.SIZE_BITS
    const val MAX_VALUE: Long = 140_737_488_355_327L
    const val MIN_VALUE: Long = -140_737_488_355_328L
}
