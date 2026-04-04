package io.bluetape4k.codec

/**
 * 바이트 배열을 문자열로 인코딩/디코딩 하는 Interface
 *
 * 구현체([Base64StringEncoder], [HexStringEncoder])를 사용하여 encode → decode 왕복이 가능합니다.
 *
 * ```kotlin
 * val encoder: StringEncoder = Base64StringEncoder()
 * val bytes = "Hello, bluetape4k!".toByteArray()
 * val encoded = encoder.encode(bytes)   // "SGVsbG8sIGJsdWV0YXBlNGsh"
 * val decoded = encoder.decode(encoded) // "Hello, bluetape4k!" 와 동일한 바이트 배열
 * ```
 */
interface StringEncoder {
    /**
     * 바이트 배열을 인코딩하여 문자열로 만든다.
     *
     * ```kotlin
     * val encoder: StringEncoder = Base64StringEncoder()
     * val encoded = encoder.encode("Hello, bluetape4k!".toByteArray()) // "SGVsbG8sIGJsdWV0YXBlNGsh"
     * ```
     *
     * @param bytes 인코딩할 바이트 배열
     * @return 인코딩된 문자열
     */
    fun encode(bytes: ByteArray?): String

    /**
     * 인코딩된 문자열을 분해하여 바이트 배열로 만든다.
     *
     * ```kotlin
     * val encoder: StringEncoder = Base64StringEncoder()
     * val decoded = encoder.decode("SGVsbG8sIGJsdWV0YXBlNGsh") // "Hello, bluetape4k!" 바이트 배열
     * ```
     *
     * @param encoded 인코딩된 문자열
     * @return 디코딩된 바이트 배열
     */
    fun decode(encoded: String?): ByteArray
}
