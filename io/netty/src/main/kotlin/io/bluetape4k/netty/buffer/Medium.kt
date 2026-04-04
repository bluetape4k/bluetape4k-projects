package io.bluetape4k.netty.buffer

/**
 * Netty 처리에서 사용하는 `Medium` 타입입니다.
 *
 * 3바이트(24비트) 정수형 상수를 정의합니다.
 *
 * ```kotlin
 * val sizeBytes = Medium.SIZE_BYTES
 * // sizeBytes == 3
 * val sizeBits = Medium.SIZE_BITS
 * // sizeBits == 24
 * val max = Medium.MAX_VALUE
 * // max == 8_388_607
 * ```
 */
object Medium {
    const val SIZE_BYTES: Int = 3
    const val SIZE_BITS: Int = SIZE_BYTES * Byte.SIZE_BITS
    const val MAX_VALUE: Int = 8_388_607
    const val MIN_VALUE: Int = -8_388_608
}
