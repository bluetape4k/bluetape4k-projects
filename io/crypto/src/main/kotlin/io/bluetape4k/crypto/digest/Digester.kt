package io.bluetape4k.crypto.digest

import io.bluetape4k.codec.decodeBase64ByteArray
import io.bluetape4k.codec.encodeBase64ByteArray
import io.bluetape4k.codec.encodeBase64String
import org.jasypt.salt.SaltGenerator

/**
 * 해시 다이제스트 계산과 검증 계약을 정의하는 인터페이스입니다.
 *
 * ## 동작/계약
 * - 문자열 API는 내부적으로 Base64 변환 경로를 사용합니다.
 * - 입력 바이트/문자열은 변경하지 않고 새 다이제스트 결과를 반환합니다.
 * - 실제 알고리즘과 salt 적용 방식은 구현체에 따릅니다.
 *
 * ```kotlin
 * val digest = Digesters.SHA256.digest("hello")
 * val ok = Digesters.SHA256.matches("hello", digest)
 * // ok == true
 * ```
 */
interface Digester {

    /** 다이제스트 알고리즘 이름입니다. */
    val algorithm: String

    /** 다이제스트 계산 시 사용하는 salt 생성기입니다. */
    val saltGenerator: SaltGenerator

    /**
     * 바이트 배열의 다이제스트를 계산합니다.
     *
     * ## 동작/계약
     * - 입력을 변경하지 않고 새 바이트 배열을 반환합니다.
     * - 예외 정책은 구현체를 따릅니다.
     *
     * ```kotlin
     * val digest = Digesters.SHA256.digest("abc".toByteArray())
     * // digest.isNotEmpty() == true
     * ```
     *
     * @param message 다이제스트할 바이트 배열
     */
    fun digest(message: ByteArray): ByteArray

    /**
     * 문자열의 다이제스트를 Base64 문자열로 반환합니다.
     *
     * ## 동작/계약
     * - 문자열을 Base64 바이트로 인코딩한 뒤 [digest]를 호출합니다.
     * - 결과 바이트를 Base64 문자열로 변환해 반환합니다.
     *
     * ```kotlin
     * val digest = Digesters.SHA256.digest("abc")
     * // digest.isNotBlank() == true
     * ```
     *
     * @param message 다이제스트할 문자열
     */
    fun digest(message: String): String {
        return digest(message.encodeBase64ByteArray()).encodeBase64String()
    }

    /**
     * [CharArray]의 다이제스트를 계산합니다.
     *
     * ## 동작/계약
     * - 입력 문자를 문자열로 합친 뒤 [digest]를 호출합니다.
     * - 결과 문자열을 [CharArray]로 변환해 반환합니다.
     *
     * ```kotlin
     * val digest = Digesters.SHA256.digest(charArrayOf('a'))
     * // digest.isNotEmpty() == true
     * ```
     *
     * @param message 다이제스트할 문자 배열
     */
    fun digest(message: CharArray): CharArray {
        return digest(message.concatToString()).toCharArray()
    }

    /**
     * 원본 바이트 배열과 다이제스트 바이트 배열의 일치 여부를 검사합니다.
     *
     * ## 동작/계약
     * - 비교 방식은 구현체를 따릅니다.
     * - 입력 배열은 변경하지 않습니다.
     *
     * ```kotlin
     * val raw = "abc".toByteArray()
     * val digest = Digesters.SHA256.digest(raw)
     * // Digesters.SHA256.matches(raw, digest) == true
     * ```
     *
     * @param message 원본 바이트 배열
     * @param digest 다이제스트 바이트 배열
     */
    fun matches(message: ByteArray, digest: ByteArray): Boolean

    /**
     * 원본 문자열과 다이제스트 문자열의 일치 여부를 검사합니다.
     *
     * ## 동작/계약
     * - 문자열을 Base64 바이트/문자열 경로로 변환한 뒤 [matches]에 위임합니다.
     * - 형식이 잘못된 [digest]가 들어오면 디코딩 예외가 발생할 수 있습니다.
     *
     * ```kotlin
     * val digest = Digesters.SHA256.digest("abc")
     * // Digesters.SHA256.matches("abc", digest) == true
     * ```
     *
     * @param message 원본 문자열
     * @param digest Base64 문자열 다이제스트
     */
    fun matches(message: String, digest: String): Boolean {
        return matches(message.encodeBase64ByteArray(), digest.decodeBase64ByteArray())
    }

    /**
     * 원본 문자 배열과 다이제스트 문자 배열의 일치 여부를 검사합니다.
     *
     * ## 동작/계약
     * - 각 배열을 문자열로 변환해 [matches] 문자열 경로에 위임합니다.
     * - 입력 배열은 변경하지 않습니다.
     *
     * ```kotlin
     * val digest = Digesters.SHA256.digest(charArrayOf('a'))
     * // Digesters.SHA256.matches(charArrayOf('a'), digest) == true
     * ```
     *
     * @param message 원본 문자 배열
     * @param digest 다이제스트 문자 배열
     */
    fun matches(message: CharArray, digest: CharArray): Boolean {
        return matches(message.concatToString(), digest.concatToString())
    }

}
