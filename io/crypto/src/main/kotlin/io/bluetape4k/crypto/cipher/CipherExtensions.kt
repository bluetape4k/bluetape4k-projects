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
 * ```
 * val cipher = CipherBuilder()
 *     .secretKeySize(16)
 *     .ivBytesSize(16)
 *     .build(Cipher.ENCRYPT_MODE)
 *
 * val encrypted = cipher.encrypt("Hello".toByteArray())
 * ```
 *
 * @receiver 암호화 모드([Cipher.ENCRYPT_MODE])로 초기화된 [Cipher] 인스턴스
 * @param plain 암호화할 평문 바이트 배열 (null 또는 빈 배열이면 빈 배열 반환)
 * @param offset 바이트 배열에서 시작 위치 (기본값: 0)
 * @param length 암호화할 바이트 수 (기본값: 배열 전체 크기)
 * @return 암호화된 바이트 배열
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
 * ```
 * val decipher = CipherBuilder()
 *     .secretKeySize(16)
 *     .ivBytesSize(16)
 *     .build(Cipher.DECRYPT_MODE)
 *
 * val decrypted = decipher.decrypt(encryptedBytes)
 * ```
 *
 * @receiver 복호화 모드([Cipher.DECRYPT_MODE])로 초기화된 [Cipher] 인스턴스
 * @param encrypted 복호화할 암호문 바이트 배열 (null 또는 빈 배열이면 빈 배열 반환)
 * @param offset 바이트 배열에서 시작 위치 (기본값: 0)
 * @param length 복호화할 바이트 수 (기본값: 배열 전체 크기)
 * @return 복호화된 평문 바이트 배열
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
