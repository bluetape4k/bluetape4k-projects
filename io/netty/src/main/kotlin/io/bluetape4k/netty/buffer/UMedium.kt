package io.bluetape4k.netty.buffer

/**
 * Netty 처리에서 사용하는 `UMedium` 타입입니다.
 */
object UMedium {
    const val SIZE_BYTES: Int = Medium.SIZE_BYTES
    const val SIZE_BITS: Int = Medium.SIZE_BITS
    const val MAX_VALUE: UInt = 16_777_215u
    const val MIN_VALUE: UInt = 0u
}
