package io.bluetape4k.crypto.encrypt

import io.bluetape4k.crypto.urlBase64Decoder
import io.bluetape4k.crypto.urlBase64Encoder
import io.bluetape4k.support.EMPTY_STRING
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import org.jasypt.salt.SaltGenerator

/**
 * 대칭키 기반 암복호화 계약을 정의하는 인터페이스입니다.
 *
 * ## 동작/계약
 * - 문자열 API는 내부적으로 URL-safe Base64 문자열 경로를 사용합니다.
 * - 문자열 입력이 `null` 또는 빈 문자열이면 빈 문자열을 반환합니다.
 * - `ByteArray` 경로의 null 처리 규칙은 구현체([encrypt], [decrypt]) 계약을 따릅니다.
 *
 * ```kotlin
 * val encrypted = Encryptors.AES.encrypt("hello")
 * val plain = Encryptors.AES.decrypt(encrypted)
 * // plain == "hello"
 * ```
 */
@Deprecated(
    message = "io.bluetape4k.tink.encrypt.TinkEncryptor를 사용하세요.",
    replaceWith = ReplaceWith("TinkEncryptor", "io.bluetape4k.tink.encrypt.TinkEncryptor"),
)
interface Encryptor {

    /** 대칭형 암호화를 위한 알고리즘 이름입니다. */
    val algorithm: String

    /** 암호화 시 사용할 salt 생성기입니다. */
    val saltGenerator: SaltGenerator

    /**
     * 바이트 배열을 암호화합니다.
     *
     * ## 동작/계약
     * - 입력 null 처리와 예외 정책은 구현체를 따릅니다.
     * - 수신 객체 상태를 변경하지 않고 새 바이트 배열을 반환합니다.
     *
     * ```kotlin
     * val encrypted = Encryptors.AES.encrypt("abc".toByteArray())
     * // encrypted.isNotEmpty() == true
     * ```
     *
     * @param message 암호화할 평문 바이트 배열
     */
    fun encrypt(message: ByteArray?): ByteArray

    /**
     * 문자열을 암호화해 URL-safe Base64 문자열로 반환합니다.
     *
     * ## 동작/계약
     * - [message]가 null 또는 빈 문자열이면 빈 문자열을 반환합니다.
     * - 그 외 입력은 UTF-8 바이트로 변환해 [encrypt]에 위임합니다.
     *
     * ```kotlin
     * val encrypted = Encryptors.AES.encrypt("hello")
     * // encrypted.isNotBlank() == true
     * ```
     *
     * @param message 암호화할 문자열
     */
    fun encrypt(message: String?): String {
        if (message.isNullOrEmpty())
            return EMPTY_STRING

        return urlBase64Encoder.encodeToString(encrypt(message.toUtf8Bytes()))
    }

    /**
     * 암호화된 바이트 배열을 복호화합니다.
     *
     * ## 동작/계약
     * - 입력 null 처리와 예외 정책은 구현체를 따릅니다.
     * - 수신 객체 상태를 변경하지 않고 새 바이트 배열을 반환합니다.
     *
     * ```kotlin
     * val plain = Encryptors.AES.decrypt(Encryptors.AES.encrypt("abc".toByteArray()))
     * // plain.decodeToString() == "abc"
     * ```
     *
     * @param encrypted 복호화할 암호문 바이트 배열
     */
    fun decrypt(encrypted: ByteArray?): ByteArray

    /**
     * URL-safe Base64 문자열을 복호화해 일반 문자열로 반환합니다.
     *
     * ## 동작/계약
     * - [encrypted]가 null 또는 빈 문자열이면 빈 문자열을 반환합니다.
     * - 그 외 입력은 Base64 디코딩 후 [decrypt]에 위임합니다.
     *
     * ```kotlin
     * val plain = Encryptors.AES.decrypt(Encryptors.AES.encrypt("hello"))
     * // plain == "hello"
     * ```
     *
     * @param encrypted 복호화할 암호문 문자열(URL-safe Base64)
     */
    fun decrypt(encrypted: String?): String {
        if (encrypted.isNullOrEmpty())
            return EMPTY_STRING

        return decrypt(urlBase64Decoder.decode(encrypted.toUtf8Bytes())).toUtf8String()
    }

    /**
     * [CharArray]를 암호화합니다.
     *
     * ## 동작/계약
     * - [CharArray]를 문자열로 합친 뒤 [encrypt]를 호출합니다.
     * - 결과 문자열을 [CharArray]로 변환해 반환합니다.
     *
     * ```kotlin
     * val encrypted = Encryptors.AES.encrypt(charArrayOf('a', 'b'))
     * // encrypted.isNotEmpty() == true
     * ```
     *
     * @param message 암호화할 문자 배열
     */
    fun encrypt(message: CharArray): CharArray =
        encrypt(message.concatToString()).toCharArray()

    /**
     * 암호화된 [CharArray]를 복호화합니다.
     *
     * ## 동작/계약
     * - [CharArray]를 문자열로 합친 뒤 [decrypt]를 호출합니다.
     * - 결과 문자열을 [CharArray]로 변환해 반환합니다.
     *
     * ```kotlin
     * val plain = Encryptors.AES.decrypt(Encryptors.AES.encrypt(charArrayOf('x')))
     * // plain.concatToString() == "x"
     * ```
     *
     * @param encrypted 복호화할 암호문 문자 배열
     */
    fun decrypt(encrypted: CharArray): CharArray =
        decrypt(encrypted.concatToString()).toCharArray()
}
