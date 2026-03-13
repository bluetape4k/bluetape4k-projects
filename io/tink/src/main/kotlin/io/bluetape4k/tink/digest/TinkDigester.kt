package io.bluetape4k.tink.digest

import java.security.MessageDigest
import java.util.Base64

/**
 * JDK [MessageDigest] 기반의 해시 다이제스트 클래스입니다.
 *
 * BouncyCastle 없이 순수 JDK만으로 MD5, SHA-1, SHA-256, SHA-384, SHA-512 등
 * 표준 해시 알고리즘을 지원합니다.
 *
 * String 입출력 시 내부적으로 UTF-8 인코딩과 Base64 변환을 처리합니다.
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
        val expectedBytes = Base64.getDecoder().decode(expected)
        return MessageDigest.isEqual(dataHashBytes, expectedBytes)
    }

    override fun toString(): String = "TinkDigester(algorithm=$algorithmName)"
}
