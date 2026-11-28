package io.bluetape4k.io.digest

import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Domain-separated digest와 HMAC fingerprint를 위한 최소 공용 인코더입니다. */
object FingerprintEncoder {

    private const val NUL = '\u0000'
    private const val HMAC_SHA256 = "HmacSHA256"

    /** UTF-8 문자열을 SHA-256 hex로 인코딩합니다. */
    fun sha256Hex(value: String): String = Digesters.SHA256.digestHexUtf8(value)

    /** UTF-8 문자열을 URL-safe, unpadded SHA-256 base64로 인코딩합니다. */
    fun sha256Base64Url(value: String): String =
        Digesters.SHA256.digestBase64Utf8(value, Base64Format.URL_SAFE_UNPADDED)

    /** domain과 field를 NUL로 구분해 SHA-256 hex를 계산합니다. */
    fun domainSeparatedSha256Hex(domain: String, vararg fields: String): String =
        sha256Hex(frame(domain, fields))

    /** domain과 field를 NUL로 구분해 URL-safe, unpadded HMAC-SHA256을 계산합니다. */
    fun hmacSha256Base64Url(secret: ByteArray, domain: String, vararg fields: String): String {
        require(secret.isNotEmpty()) { "secret must not be empty" }

        val mac = Mac.getInstance(HMAC_SHA256)
        mac.init(SecretKeySpec(secret.copyOf(), HMAC_SHA256))
        val digest = mac.doFinal(frame(domain, fields).toByteArray(StandardCharsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    private fun frame(domain: String, fields: Array<out String>): String =
        (sequenceOf(domain) + fields.asSequence()).joinToString(NUL.toString())
}
