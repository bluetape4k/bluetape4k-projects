package io.bluetape4k.crypto.encrypt

/**
 * 문자열을 지정된 [Encryptor]로 암호화합니다.
 *
 * ```
 * val encrypted = "Hello, World!".encrypt(Encryptors.AES)
 * ```
 *
 * @receiver 암호화할 문자열
 * @param encryptor 사용할 [Encryptor] 인스턴스
 * @return 암호화된 문자열 (URL-safe Base64 인코딩)
 */
fun String.encrypt(encryptor: Encryptor): String =
    encryptor.encrypt(this)

/**
 * 암호화된 문자열을 지정된 [Encryptor]로 복호화합니다.
 *
 * ```
 * val decrypted = encrypted.decrypt(Encryptors.AES) // "Hello, World!"
 * ```
 *
 * @receiver 복호화할 암호화된 문자열 (URL-safe Base64 인코딩)
 * @param encryptor 사용할 [Encryptor] 인스턴스
 * @return 복호화된 원문 문자열
 */
fun String.decrypt(encryptor: Encryptor): String =
    encryptor.decrypt(this)

/**
 * 바이트 배열을 지정된 [Encryptor]로 암호화합니다.
 *
 * ```
 * val encrypted = "Hello".toByteArray().encrypt(Encryptors.AES)
 * ```
 *
 * @receiver 암호화할 바이트 배열
 * @param encryptor 사용할 [Encryptor] 인스턴스
 * @return 암호화된 바이트 배열
 */
fun ByteArray.encrypt(encryptor: Encryptor): ByteArray =
    encryptor.encrypt(this)

/**
 * 암호화된 바이트 배열을 지정된 [Encryptor]로 복호화합니다.
 *
 * ```
 * val decrypted = encryptedBytes.decrypt(Encryptors.AES)
 * ```
 *
 * @receiver 복호화할 암호화된 바이트 배열
 * @param encryptor 사용할 [Encryptor] 인스턴스
 * @return 복호화된 원문 바이트 배열
 */
fun ByteArray.decrypt(encryptor: Encryptor): ByteArray =
    encryptor.decrypt(this)
