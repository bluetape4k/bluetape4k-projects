package io.bluetape4k.codec

import io.bluetape4k.support.EMPTY_STRING
import io.bluetape4k.support.emptyByteArray
import java.util.*


/**
 * 문자열을 16진법 (Hex Decimal) 문자로 인코딩/디코딩 합니다
 */
class HexStringEncoder: StringEncoder {

    companion object {
        private val hex by lazy { HexFormat.of() }
    }

    /**
     * [bytes]를 16진법 (Hex Decimal) 문자열로 인코딩합니다.
     *
     * ```
     * val encoded = HexStringEncoder().encode("Hello, World!".toUtf8Bytes())   // "48656c6c6f2c20576f726c6421"
     * ```
     *
     * @param bytes 인코딩할 데이터
     * @return 16진법으로 인코딩된 문자열, [bytes] 가 null 인 경우 빈 문자열을 반환
     */
    override fun encode(bytes: ByteArray?): String {
        return bytes?.run { hex.formatHex(this) } ?: EMPTY_STRING
    }

    /**
     * 16진법 (Hex Decimmal) 문자열로 인코딩된 [encoded]를 [ByteArray]로 디코딩합니다.
     *
     * ```
     * val decoded = HexStringEncoder().decode("48656c6c6f2c20576f726c6421")   // "Hello, World!"
     * ```
     *
     * @param encoded Hex Decimal로 인코딩된 문자열
     * @return 디코딩된 [ByteArray], [encoded] 가 null 이거나 빈 문자열인 경우 빈 바이트 배열을 반환
     */
    override fun decode(encoded: String?): ByteArray {
        return encoded?.takeIf { it.isNotBlank() }?.run { hex.parseHex(this) } ?: emptyByteArray
    }
}
