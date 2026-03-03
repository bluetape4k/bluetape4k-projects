package io.bluetape4k.tink.aead

import io.bluetape4k.tink.EMPTY_BYTES

/**
 * 바이트 배열을 [TinkAead]로 암호화합니다.
 *
 * ```kotlin
 * val ciphertext = "Hello".toByteArray().tinkEncrypt(TinkAeads.AES256_GCM)
 * ```
 *
 * @param aead 사용할 [TinkAead] 인스턴스
 * @param associatedData 인증에 사용할 연관 데이터 (기본값: 빈 배열)
 * @return 암호화된 바이트 배열
 */
fun ByteArray.tinkEncrypt(aead: TinkAead, associatedData: ByteArray = EMPTY_BYTES): ByteArray =
    aead.encrypt(this, associatedData)

/**
 * 암호화된 바이트 배열을 [TinkAead]로 복호화합니다.
 *
 * ```kotlin
 * val plaintext = ciphertext.tinkDecrypt(TinkAeads.AES256_GCM)
 * ```
 *
 * @param aead 사용할 [TinkAead] 인스턴스
 * @param associatedData 암호화 시 사용한 연관 데이터 (기본값: 빈 배열)
 * @return 복호화된 평문 바이트 배열
 */
fun ByteArray.tinkDecrypt(aead: TinkAead, associatedData: ByteArray = EMPTY_BYTES): ByteArray =
    aead.decrypt(this, associatedData)

/**
 * 문자열을 [TinkAead]로 암호화합니다. 결과는 Base64 인코딩 문자열로 반환됩니다.
 *
 * ```kotlin
 * val encrypted = "비밀 메시지".tinkEncrypt(TinkAeads.AES256_GCM)
 * ```
 *
 * @param aead 사용할 [TinkAead] 인스턴스
 * @param associatedData 인증에 사용할 연관 데이터 (기본값: 빈 배열)
 * @return Base64 인코딩된 암호문 문자열
 */
fun String.tinkEncrypt(aead: TinkAead, associatedData: ByteArray = EMPTY_BYTES): String =
    aead.encrypt(this, associatedData)

/**
 * Base64 인코딩된 암호문 문자열을 [TinkAead]로 복호화합니다.
 *
 * ```kotlin
 * val decrypted = encrypted.tinkDecrypt(TinkAeads.AES256_GCM)
 * ```
 *
 * @param aead 사용할 [TinkAead] 인스턴스
 * @param associatedData 암호화 시 사용한 연관 데이터 (기본값: 빈 배열)
 * @return 복호화된 평문 문자열 (UTF-8)
 */
fun String.tinkDecrypt(aead: TinkAead, associatedData: ByteArray = EMPTY_BYTES): String =
    aead.decrypt(this, associatedData)
