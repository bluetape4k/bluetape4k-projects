package io.bluetape4k.tink.encrypt

/**
 * 바이트 배열을 [TinkEncryptor]로 암호화합니다.
 *
 * ```kotlin
 * val ciphertext = "Hello".toByteArray().tinkEncrypt(TinkEncryptors.AES256_GCM)
 * ```
 *
 * @param encryptor 사용할 [TinkEncryptor] 인스턴스
 * @return 암호화된 바이트 배열
 */
fun ByteArray.tinkEncrypt(encryptor: TinkEncryptor): ByteArray = encryptor.encrypt(this)

/**
 * 암호화된 바이트 배열을 [TinkEncryptor]로 복호화합니다.
 *
 * ```kotlin
 * val plaintext = ciphertext.tinkDecrypt(TinkEncryptors.AES256_GCM)
 * ```
 *
 * @param encryptor 사용할 [TinkEncryptor] 인스턴스
 * @return 복호화된 평문 바이트 배열
 */
fun ByteArray.tinkDecrypt(encryptor: TinkEncryptor): ByteArray = encryptor.decrypt(this)

/**
 * 문자열을 [TinkEncryptor]로 암호화합니다. 결과는 Base64 인코딩 문자열로 반환됩니다.
 *
 * ```kotlin
 * val encrypted = "비밀 메시지".tinkEncrypt(TinkEncryptors.AES256_GCM)
 * ```
 *
 * @param encryptor 사용할 [TinkEncryptor] 인스턴스
 * @return Base64 인코딩된 암호문 문자열
 */
fun String.tinkEncrypt(encryptor: TinkEncryptor): String = encryptor.encrypt(this)

/**
 * Base64 인코딩된 암호문 문자열을 [TinkEncryptor]로 복호화합니다.
 *
 * ```kotlin
 * val decrypted = encrypted.tinkDecrypt(TinkEncryptors.AES256_GCM)
 * ```
 *
 * @param encryptor 사용할 [TinkEncryptor] 인스턴스
 * @return 복호화된 평문 문자열 (UTF-8)
 */
fun String.tinkDecrypt(encryptor: TinkEncryptor): String = encryptor.decrypt(this)
