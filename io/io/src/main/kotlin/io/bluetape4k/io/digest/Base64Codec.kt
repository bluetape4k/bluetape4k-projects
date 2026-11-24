package io.bluetape4k.io.digest

import java.util.Base64

/** JDK Base64 codec에 strict alphabet, padding, canonical 입력 검사를 적용합니다. */
object Base64Codec {

    /** 바이트 배열을 URL-safe padded Base64 문자열로 인코딩합니다. */
    @JvmStatic
    fun encode(data: ByteArray): String = encode(data, Base64Format.URL_SAFE_PADDED)

    /** 바이트 배열을 지정한 alphabet과 padding 형식으로 인코딩합니다. */
    @JvmStatic
    fun encode(data: ByteArray, format: Base64Format): String = encoder(format).encodeToString(data)

    /** URL-safe padded Base64 문자열을 canonical 형식만 허용해 디코딩합니다. */
    @JvmStatic
    fun decode(data: String): ByteArray = decode(data, Base64Format.URL_SAFE_PADDED)

    /** 지정한 형식으로 디코딩하고 canonical 표현이 아니면 `IllegalArgumentException`을 던집니다. */
    @JvmStatic
    fun decode(data: String, format: Base64Format): ByteArray {
        val decoded = decoder(format).decode(data)
        require(encode(decoded, format) == data) { "Base64 input is not canonical for $format" }
        return decoded
    }

    private fun encoder(format: Base64Format): Base64.Encoder = when (format) {
        Base64Format.STANDARD_PADDED -> Base64.getEncoder()
        Base64Format.STANDARD_UNPADDED -> Base64.getEncoder().withoutPadding()
        Base64Format.URL_SAFE_PADDED -> Base64.getUrlEncoder()
        Base64Format.URL_SAFE_UNPADDED -> Base64.getUrlEncoder().withoutPadding()
    }

    private fun decoder(format: Base64Format): Base64.Decoder = when (format) {
        Base64Format.STANDARD_PADDED, Base64Format.STANDARD_UNPADDED -> Base64.getDecoder()
        Base64Format.URL_SAFE_PADDED, Base64Format.URL_SAFE_UNPADDED -> Base64.getUrlDecoder()
    }
}
