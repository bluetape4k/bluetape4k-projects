package io.bluetape4k.crypto.encrypt

/**
 * 문자열을 지정된 [Encryptor]로 암호화합니다.
 *
 * ## 동작/계약
 * - 수신 문자열을 [Encryptor.encrypt] 문자열 경로에 그대로 위임합니다.
 * - 수신 문자열은 변경하지 않고 새 암호문 문자열을 반환합니다.
 *
 * ```kotlin
 * val encrypted = "Hello, World!".encrypt(Encryptors.AES)
 * // encrypted.isNotBlank() == true
 * ```
 * @param encryptor 사용할 암호화기
 */
fun String.encrypt(encryptor: Encryptor): String =
    encryptor.encrypt(this)

/**
 * 암호화된 문자열을 지정된 [Encryptor]로 복호화합니다.
 *
 * ## 동작/계약
 * - 수신 문자열을 [Encryptor.decrypt] 문자열 경로에 그대로 위임합니다.
 * - 수신 문자열은 변경하지 않고 새 평문 문자열을 반환합니다.
 *
 * ```kotlin
 * val encrypted = "Hello, World!".encrypt(Encryptors.AES)
 * val decrypted = encrypted.decrypt(Encryptors.AES)
 * // decrypted == "Hello, World!"
 * ```
 * @param encryptor 사용할 암호화기
 */
fun String.decrypt(encryptor: Encryptor): String =
    encryptor.decrypt(this)

/**
 * 바이트 배열을 지정된 [Encryptor]로 암호화합니다.
 *
 * ## 동작/계약
 * - 수신 바이트 배열을 [Encryptor.encrypt] 바이트 경로에 위임합니다.
 * - 수신 배열은 변경하지 않고 새 암호문 바이트 배열을 반환합니다.
 *
 * ```kotlin
 * val encrypted = "Hello".toByteArray().encrypt(Encryptors.AES)
 * // encrypted.isNotEmpty() == true
 * ```
 * @param encryptor 사용할 암호화기
 */
fun ByteArray.encrypt(encryptor: Encryptor): ByteArray =
    encryptor.encrypt(this)

/**
 * 암호화된 바이트 배열을 지정된 [Encryptor]로 복호화합니다.
 *
 * ## 동작/계약
 * - 수신 바이트 배열을 [Encryptor.decrypt] 바이트 경로에 위임합니다.
 * - 수신 배열은 변경하지 않고 새 평문 바이트 배열을 반환합니다.
 *
 * ```kotlin
 * val encrypted = "Hello".toByteArray().encrypt(Encryptors.AES)
 * val decrypted = encrypted.decrypt(Encryptors.AES)
 * // decrypted.decodeToString() == "Hello"
 * ```
 * @param encryptor 사용할 암호화기
 */
fun ByteArray.decrypt(encryptor: Encryptor): ByteArray =
    encryptor.decrypt(this)
