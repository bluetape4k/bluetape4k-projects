package io.bluetape4k.crypto.cipher

import io.bluetape4k.support.emptyByteArray
import io.bluetape4k.support.isNullOrEmpty
import javax.crypto.Cipher

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
@Deprecated(message = "io.bluetape4k.tink.aead.TinkAead 또는 JCA Cipher를 직접 사용하세요.")
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
 * - 복호화 성공 시 복호화된 새 바이트 배열을 반환합니다.
 * - 잘못된 키, 손상된 데이터, 패딩 오류 등 복호화 실패 시 JCA 예외가 호출자에게 전파됩니다.
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
@Deprecated(message = "io.bluetape4k.tink.aead.TinkAead 또는 JCA Cipher를 직접 사용하세요.")
fun Cipher.decrypt(encrypted: ByteArray?, offset: Int = 0, length: Int = encrypted?.size ?: 0): ByteArray {
    if (encrypted.isNullOrEmpty()) {
        return emptyByteArray
    }
    return doFinal(encrypted, offset, length)
}
