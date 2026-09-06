package io.bluetape4k.tink.digest

import io.bluetape4k.logging.KLogging
import java.security.MessageDigest
import java.util.*

/**
 * JDK [MessageDigest] 기반의 해시 다이제스트 클래스입니다.
 *
 * BouncyCastle 없이 순수 JDK만으로 MD5, SHA-1, SHA-256, SHA-384, SHA-512 등
 * 표준 해시 알고리즘을 지원합니다.
 *
 * 문자열 입력은 UTF-8로 인코딩하며 Base64 또는 lowercase hex 결과를 제공합니다.
 *
 * ```kotlin
 * val digester = TinkDigester("SHA-256")
 * val hash = digester.digest("Hello, World!")
 * digester.matches("Hello, World!", hash) // true
 * ```
 *
 * @param algorithmName JDK [MessageDigest]가 지원하는 해시 알고리즘 이름
 */
class TinkDigester(
    val algorithmName: String,
) {

    companion object: KLogging()

    /**
     * 바이트 배열의 해시 다이제스트를 계산합니다.
     *
     * @param data 해시할 바이트 배열
     * @return 해시된 바이트 배열
     */
    fun digest(data: ByteArray): ByteArray = MessageDigest.getInstance(algorithmName).digest(data)

    /**
     * 문자열의 해시 다이제스트를 계산합니다.
     * 입력은 UTF-8로 인코딩되고, 결과는 Base64 문자열로 반환됩니다.
     *
     * @param data 해시할 문자열
     * @return Base64 인코딩된 해시 문자열
     */
    fun digest(data: String): String {
        val hashBytes = digest(data.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(hashBytes)
    }

    /**
     * 문자열의 해시 다이제스트를 lowercase hex 문자열로 반환합니다.
     *
     * 입력은 UTF-8로 인코딩하며 각 digest 바이트를 정확히 두 자리 hex로 변환합니다.
     * 따라서 SHA-256 결과 길이는 항상 64자입니다.
     *
     * @param data 해시할 문자열
     * @return lowercase hex로 인코딩한 해시 문자열
     */
    fun digestHex(data: String): String =
        HexFormat.of().formatHex(digest(data.toByteArray(Charsets.UTF_8)))

    /**
     * 바이트 배열의 해시가 기대값과 일치하는지 constant-time으로 비교합니다.
     *
     * @param data 원본 바이트 배열
     * @param expected 기대하는 해시 바이트 배열
     * @return 일치하면 `true`, 아니면 `false`
     */
    fun matches(
        data: ByteArray,
        expected: ByteArray,
    ): Boolean = MessageDigest.isEqual(digest(data), expected)

    /**
     * 문자열의 해시가 기대값(Base64)과 일치하는지 constant-time으로 비교합니다.
     *
     * Base64 문자열을 바이트 배열로 디코딩한 뒤 [MessageDigest.isEqual]로 비교하여
     * 타이밍 공격을 방지합니다.
     * [expected]가 Base64 형식이 아니면 예외를 던지지 않고 `false`를 반환합니다.
     *
     * @param data 원본 문자열
     * @param expected 기대하는 Base64 인코딩된 해시 문자열
     * @return 일치하면 `true`, 아니면 `false`
     */
    fun matches(
        data: String,
        expected: String,
    ): Boolean {
        val dataHashBytes = digest(data.toByteArray(Charsets.UTF_8))
        // Boolean verifier API는 malformed expected digest도 불일치로 취급한다.
        // 호출자가 단순 검증 경로에서 Base64 예외를 별도 처리하지 않아도 된다.
        val expectedBytes = runCatching {
            Base64.getDecoder().decode(expected)
        }.getOrElse {
            return false
        }
        return MessageDigest.isEqual(dataHashBytes, expectedBytes)
    }

    /**
     * 문자열의 해시가 lowercase hex 기대값과 일치하는지 검증합니다.
     *
     * [expected]는 현재 알고리즘의 digest 길이와 정확히 일치해야 하며 `0-9`, `a-f`만
     * 허용합니다. 길이가 다르거나 malformed 또는 uppercase 값이면 예외를 던지지 않고
     * `false`를 반환합니다. 형식 검사는 fail-fast이며 constant-time이 아닙니다. 길이와
     * 형식이 올바른 canonical digest byte 비교에만 [MessageDigest.isEqual]을 사용합니다.
     *
     * @param data 원본 문자열
     * @param expected 기대하는 lowercase hex 해시 문자열
     * @return 일치하면 `true`, 아니면 `false`
     */
    fun matchesHex(
        data: String,
        expected: String,
    ): Boolean {
        val dataHashBytes = digest(data.toByteArray(Charsets.UTF_8))
        if (expected.length != dataHashBytes.size * 2 || expected.any { it !in '0'..'9' && it !in 'a'..'f' }) {
            return false
        }
        val expectedBytes = HexFormat.of().parseHex(expected)
        return MessageDigest.isEqual(dataHashBytes, expectedBytes)
    }

    override fun toString(): String = "TinkDigester(algorithm=$algorithmName)"
}
