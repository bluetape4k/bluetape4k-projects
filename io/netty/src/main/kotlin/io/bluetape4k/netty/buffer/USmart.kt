package io.bluetape4k.netty.buffer

/**
 * Netty 처리에서 사용하는 `USmart` 타입입니다.
 *
 * 부호 없는 Smart 인코딩 범위 상수를 정의합니다.
 *
 * ```kotlin
 * val maxByte = USmart.MAX_BYTE_VALUE
 * // maxByte == 127
 * val maxShort = USmart.MAX_SHORT_VALUE
 * // maxShort == 32767
 * ```
 */
object USmart {
    const val MAX_BYTE_VALUE: Int = Byte.MAX_VALUE.toInt()
    const val MIN_BYTE_VALUE: Int = 0
    const val MAX_SHORT_VALUE: Int = Short.MAX_VALUE.toInt()
    const val MIN_SHORT_VALUE: Int = 0
    const val MAX_INT_VALUE: Int = Int.MAX_VALUE
    const val MIN_INT_VALUE: Int = 0
}
