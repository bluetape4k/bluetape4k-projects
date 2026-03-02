package io.bluetape4k.crypto.cipher

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import io.bluetape4k.support.emptyByteArray
import io.bluetape4k.support.isNullOrEmpty
import javax.crypto.Cipher

private object CipherExtensionsHolder: KLogging()

/**
 * [Cipher]를 이용하여 평문 바이트 배열을 암호화합니다.
 *
 * ## 동작/계약
 * - [plain]이 null 또는 빈 배열이면 빈 배열을 반환합니다.
 * - 그 외에는 [Cipher.doFinal]로 지정 범위를 암호화한 새 배열을 반환합니다.
 * - [offset]/[length]가 잘못되면 JCA 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val key = ByteArray(16) { 1 }
 * val iv = ByteArray(16) { 2 }
 * val cipher = CipherBuilder().secretKey(key).ivBytes(iv).build(Cipher.ENCRYPT_MODE)
 * val encrypted = cipher.encrypt("Hello".toByteArray())
 * // encrypted.isNotEmpty() == true
 * ```
 * @param plain 암호화할 평문 바이트 배열
 * @param offset 시작 위치
 * @param length 처리 길이
 */
fun Cipher.encrypt(plain: ByteArray?, offset: Int = 0, length: Int = plain?.size ?: 0): ByteArray {
    if (plain.isNullOrEmpty()) {
        return emptyByteArray
    }
    return doFinal(plain, offset, length)
}

/**
 * [Cipher]를 이용하여 암호화된 바이트 배열을 복호화합니다.
 *
 * ## 동작/계약
 * - [encrypted]가 null 또는 빈 배열이면 빈 배열을 반환합니다.
 * - [update] 결과와 `doFinal()` 결과를 이어 붙인 새 배열을 반환합니다.
 * - `doFinal()` 실패는 경고 로그 후 빈 배열로 대체되어 반환됩니다.
 *
 * ```kotlin
 * val key = ByteArray(16) { 1 }
 * val iv = ByteArray(16) { 2 }
 * val cipher = CipherBuilder().secretKey(key).ivBytes(iv).build(Cipher.ENCRYPT_MODE)
 * val decipher = CipherBuilder().secretKey(key).ivBytes(iv).build(Cipher.DECRYPT_MODE)
 * val encrypted = cipher.encrypt("Hello".toByteArray())
 * val plain = decipher.decrypt(encrypted)
 * // plain.decodeToString() == "Hello"
 * ```
 * @param encrypted 복호화할 암호문 바이트 배열
 * @param offset 시작 위치
 * @param length 처리 길이
 */
fun Cipher.decrypt(encrypted: ByteArray?, offset: Int = 0, length: Int = encrypted?.size ?: 0): ByteArray {
    if (encrypted.isNullOrEmpty()) {
        return emptyByteArray
    }
    return update(encrypted, offset, length) +
            runCatching { doFinal() }
                .onFailure { e ->
                    CipherExtensionsHolder.log.warn(e) { "Cipher doFinal() 실행 중 오류가 발생했습니다." }
                }
                .getOrDefault(emptyByteArray)
}
