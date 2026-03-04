package io.bluetape4k.tink.encrypt

/**
 * Tink 기반 대칭 암호화/복호화 인터페이스입니다.
 *
 * AEAD(비결정적)와 Deterministic AEAD(결정적) 구현체를 통합하는 공통 인터페이스로,
 * `io.bluetape4k.crypto.encrypt.Encryptor`를 대체합니다.
 *
 * ```kotlin
 * val encryptor: TinkEncryptor = TinkEncryptors.AES256_GCM
 * val encrypted = encryptor.encrypt("비밀 메시지")
 * val decrypted = encryptor.decrypt(encrypted)
 * ```
 */
interface TinkEncryptor {

    /**
     * 바이트 배열을 암호화합니다.
     *
     * @param plaintext 암호화할 평문 바이트 배열
     * @return 암호화된 바이트 배열
     */
    fun encrypt(plaintext: ByteArray): ByteArray

    /**
     * 암호화된 바이트 배열을 복호화합니다.
     *
     * @param ciphertext 복호화할 암호문 바이트 배열
     * @return 복호화된 평문 바이트 배열
     */
    fun decrypt(ciphertext: ByteArray): ByteArray

    /**
     * 문자열을 암호화합니다. 결과는 Base64 인코딩 문자열로 반환됩니다.
     *
     * @param plaintext 암호화할 평문 문자열 (UTF-8)
     * @return Base64 인코딩된 암호문 문자열
     */
    fun encrypt(plaintext: String): String

    /**
     * Base64 인코딩된 암호문 문자열을 복호화합니다.
     *
     * @param ciphertext Base64 인코딩된 암호문 문자열
     * @return 복호화된 평문 문자열 (UTF-8)
     */
    fun decrypt(ciphertext: String): String
}
