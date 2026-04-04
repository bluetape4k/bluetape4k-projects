package io.bluetape4k.netty.buffer

/**
 * Netty 처리에서 사용하는 `USmallLong` 타입입니다.
 *
 * 6바이트(48비트) 부호 없는 정수형 상수를 정의합니다.
 *
 * ```kotlin
 * val max = USmallLong.MAX_VALUE
 * // max == 281_474_976_710_655u
 * val min = USmallLong.MIN_VALUE
 * // min == 0u
 * ```
 */
object USmallLong {
    const val SIZE_BYTES: Int = SmallLong.SIZE_BYTES
    const val SIZE_BITS: Int = SmallLong.SIZE_BITS
    const val MAX_VALUE: ULong = 281_474_976_710_655u
    const val MIN_VALUE: ULong = 0u
}
