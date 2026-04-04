package io.bluetape4k.netty.buffer

/**
 * Netty 처리에서 사용하는 `SmallLong` 타입입니다.
 *
 * 6바이트(48비트) 정수형 상수를 정의합니다.
 *
 * ```kotlin
 * val sizeBytes = SmallLong.SIZE_BYTES
 * // sizeBytes == 6
 * val sizeBits = SmallLong.SIZE_BITS
 * // sizeBits == 48
 * val max = SmallLong.MAX_VALUE
 * // max == 140_737_488_355_327L
 * ```
 */
object SmallLong {
    const val SIZE_BYTES: Int = 6
    const val SIZE_BITS: Int = SIZE_BYTES * Byte.SIZE_BITS
    const val MAX_VALUE: Long = 140_737_488_355_327L
    const val MIN_VALUE: Long = -140_737_488_355_328L
}
