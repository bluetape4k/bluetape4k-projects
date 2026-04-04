package io.bluetape4k.codec

import io.bluetape4k.support.emptyByteArray
import java.util.*

/**
 * 문자열을 Url safe Base64 형태로 인코딩/디코딩 합니다.
 *
 * Java 표준 [java.util.Base64.getUrlEncoder]/[java.util.Base64.getUrlDecoder] 를 사용하여
 * URL-safe Base64 방식으로 인코딩/디코딩합니다.
 *
 * ```kotlin
 * val encoder = Base64StringEncoder()
 * val encoded = encoder.encode("Hello, World!".toUtf8Bytes())   // "SGVsbG8sIFdvcmxkIQ=="
 * val decoded = encoder.decode(encoded).toUtf8String()           // "Hello, World!"
 * ```
 */
class Base64StringEncoder: StringEncoder {

    companion object {
        private val encoder: Base64.Encoder by lazy { Base64.getUrlEncoder() }
        private val decoder: Base64.Decoder by lazy { Base64.getUrlDecoder() }
    }

    /**
     * [bytes]를 Base64 문자열로 인코딩합니다.
     *
     * ```
     * val encoded = Base64StringEncoder().encode("Hello, World!".toUtf8Bytes())   // "SGVsbG8sIFdvcmxkIQ"
     * ```
     *
     * @param bytes 인코딩할 바이트 배열
     * @return Base64로 인코딩된 문자열, [bytes] 가 null 인 경우 빈 문자열을 반환
     */
    override fun encode(bytes: ByteArray?): String = bytes?.let(encoder::encodeToString).orEmpty()

    /**
     * [encoded]를 디코딩하여 [ByteArray]로 만든다
     *
     * ```
     * val decoded = Base64StringEncoder().decode("SGVsbG8sIFdvcmxkIQ".toUtf8Bytes())   // "Hello, World!"
     * ```
     *
     * @param encoded 디코딩할 Base64 인코딩된 문자열
     * @return 디코딩된 바이트 배열, [encoded] 가 null 이거나 빈 문자열인 경우 빈 바이트 배열을 반환
     */
    override fun decode(encoded: String?): ByteArray =
        encoded?.takeIf(String::isNotBlank)?.let(decoder::decode) ?: emptyByteArray
}
