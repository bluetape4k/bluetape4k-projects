package io.bluetape4k.io.digest

import java.security.MessageDigest
import java.util.HexFormat

/**
 * 알고리즘별 digest 계산을 제공하는 불변 인터페이스입니다.
 *
 * 인스턴스는 여러 호출에서 공유할 수 있으며, 각 호출은 독립 세션을 사용합니다. 일반 digest는
 * 데이터의 무결성이나 발신자를 인증하지 않습니다. 공유 비밀 기반 인증에는 HMAC 같은 keyed MAC을,
 * 공개 검증에는 전자 서명을 사용해야 합니다.
 */
interface Digester {

    /** JDK provider에서 사용할 알고리즘 이름입니다. */
    val algorithm: String

    /** 독립적인 알고리즘 상태를 가진 새 세션을 생성합니다. */
    fun newSession(): DigestSession

    /** 입력 바이트의 digest를 계산해 호출자가 소유하는 배열로 반환합니다. */
    fun digest(data: ByteArray): ByteArray = newSession().update(data).digest()

    /** 입력 문자열을 UTF-8로 인코딩한 뒤 digest를 계산합니다. */
    fun digestUtf8(data: String): ByteArray = digest(data.toByteArray(Charsets.UTF_8))

    /** 입력 바이트의 digest를 기본 URL-safe padded Base64 형식으로 반환합니다. */
    fun digestBase64(data: ByteArray): String = digestBase64(data, Base64Format.URL_SAFE_PADDED)

    /** 입력 바이트의 digest를 지정한 Base64 형식으로 반환합니다. */
    fun digestBase64(data: ByteArray, format: Base64Format): String = Base64Codec.encode(digest(data), format)

    /** 입력 문자열을 UTF-8로 인코딩한 뒤 기본 URL-safe padded Base64 digest를 반환합니다. */
    fun digestBase64Utf8(data: String): String = digestBase64Utf8(data, Base64Format.URL_SAFE_PADDED)

    /** 입력 문자열을 UTF-8로 인코딩한 뒤 지정한 Base64 형식의 digest를 반환합니다. */
    fun digestBase64Utf8(data: String, format: Base64Format): String =
        digestBase64(data.toByteArray(Charsets.UTF_8), format)

    /** 입력 바이트의 digest를 lowercase hexadecimal 문자열로 반환합니다. */
    fun digestHex(data: ByteArray): String = HexFormat.of().formatHex(digest(data))

    /** 입력 문자열을 UTF-8로 인코딩한 뒤 lowercase hexadecimal digest를 반환합니다. */
    fun digestHexUtf8(data: String): String = digestHex(data.toByteArray(Charsets.UTF_8))

    /** 원시 digest 바이트를 constant-time 비교합니다. */
    fun matches(data: ByteArray, expectedDigest: ByteArray): Boolean =
        MessageDigest.isEqual(digest(data), expectedDigest)

    /** lowercase hexadecimal 형식인지 확인한 뒤 원시 digest 바이트를 constant-time 비교합니다. */
    fun matchesHex(data: ByteArray, expected: String): Boolean {
        val actualDigest = digest(data)
        if (expected.length != actualDigest.size * 2 || expected.any { it !in '0'..'9' && it !in 'a'..'f' }) {
            return false
        }
        val expectedDigest = HexFormat.of().parseHex(expected)
        return MessageDigest.isEqual(actualDigest, expectedDigest)
    }

    /** UTF-8 문자열의 lowercase hexadecimal digest를 검증합니다. */
    fun matchesHexUtf8(data: String, expected: String): Boolean =
        matchesHex(data.toByteArray(Charsets.UTF_8), expected)

    /** 기본 URL-safe padded Base64 형식의 expected digest를 검증합니다. */
    fun matchesBase64(data: ByteArray, expected: String): Boolean =
        matchesBase64(data, expected, Base64Format.URL_SAFE_PADDED)

    /** 지정 형식의 canonical Base64 expected digest를 검증합니다. */
    fun matchesBase64(data: ByteArray, expected: String, format: Base64Format): Boolean {
        val actualDigest = digest(data)
        val expectedDigest = try {
            Base64Codec.decode(expected, format)
        } catch (_: IllegalArgumentException) {
            return false
        }
        return MessageDigest.isEqual(actualDigest, expectedDigest)
    }
}
