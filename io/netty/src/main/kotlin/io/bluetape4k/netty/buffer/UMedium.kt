package io.bluetape4k.netty.buffer

/**
 * Netty 처리에서 사용하는 `UMedium` 타입입니다.
 *
 * 3바이트(24비트) 부호 없는 정수형 상수를 정의합니다.
 *
 * ```kotlin
 * val max = UMedium.MAX_VALUE
 * // max == 16_777_215u
 * val min = UMedium.MIN_VALUE
 * // min == 0u
 * ```
 */
object UMedium {
    const val SIZE_BYTES: Int = Medium.SIZE_BYTES
    const val SIZE_BITS: Int = Medium.SIZE_BITS
    const val MAX_VALUE: UInt = 16_777_215u
    const val MIN_VALUE: UInt = 0u
}
