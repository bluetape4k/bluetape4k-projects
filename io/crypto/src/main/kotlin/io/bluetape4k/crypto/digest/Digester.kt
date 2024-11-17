package io.bluetape4k.crypto.digest

import io.bluetape4k.codec.decodeBase64ByteArray
import io.bluetape4k.codec.encodeBase64ByteArray
import io.bluetape4k.codec.encodeBase64String
import org.jasypt.salt.SaltGenerator

/**
 * Hash 알고리즘을 활용한 Digester
 *
 * ```
 * val digester = SHA256Digester()
 * val message = "Hello, World!"
 * val digest = digester.digest(message)
 * val matches = digester.matches("Hello, World!", digest)  // true
 * ```
 */
interface Digester {

    /**
     * Digest 암호화를 위한 알고리즘 명
     */
    val algorithm: String

    /**
     * 암호화 시에 사용하는 Salt 값 생성기를 반환합니다.
     */
    val saltGenerator: SaltGenerator

    /**
     * 바이트 배열 정보를 암호화 합니다.
     * @param message 바이트 배열
     * @return 암호화된 바이트 배열
     */
    fun digest(message: ByteArray): ByteArray

    /**
     * 문자열을 암호화 합니다.
     * @param message 암호화할 문자열
     * @return 암호화된 문자열
     */
    fun digest(message: String): String {
        return digest(message.encodeBase64ByteArray()).encodeBase64String()
    }

    /**
     * [CharArray]를 암호화 합니다.
     * @param message 암호화할 CharArray
     * @return 암호화된 CharArray
     */
    fun digest(message: CharArray): CharArray {
        return digest(message.concatToString()).toCharArray()
    }

    /**
     * Message 를 암호화하면, digest 와 같은 값이 되는지 확인한다.
     * @param message 암호화된 바이트 배열과 비교할 message
     * @param digest  암호화된 바이트 배열
     * @return 같은 값이 되는지 여부
     */
    fun matches(message: ByteArray, digest: ByteArray): Boolean

    /**
     * 해당 메시지가 암호화된 내용과 일치하는지 확인합니다.
     * @param message 일반 메시지
     * @param digest  암호화된 메시지
     * @return 메시지 일치 여부
     */
    fun matches(message: String, digest: String): Boolean {
        return matches(message.encodeBase64ByteArray(), digest.decodeBase64ByteArray())
    }

    /**
     * 해당 메시지가 암호화된 내용과 일치하는지 확인합니다.
     * @param message 일반 메시지
     * @param digest  암호화된 메시지
     * @return 메시지 일치 여부
     */
    fun matches(message: CharArray, digest: CharArray): Boolean {
        return matches(message.concatToString(), digest.concatToString())
    }

}
