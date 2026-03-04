package io.bluetape4k.tink.encrypt

import io.bluetape4k.tink.aead.TinkAead

/**
 * [TinkAead] 기반의 비결정적(randomized) [TinkEncryptor] 구현체입니다.
 *
 * 동일한 평문을 암호화하더라도 매번 다른 암호문을 생성합니다 (nonce 랜덤화).
 * 범용 데이터 암호화에 권장됩니다.
 *
 * ```kotlin
 * val encryptor = TinkAeadEncryptor(TinkAeads.AES256_GCM)
 * val ct1 = encryptor.encrypt("Hello")
 * val ct2 = encryptor.encrypt("Hello")
 * // ct1 != ct2 (비결정적)
 * ```
 *
 * @param aead 사용할 [TinkAead] 인스턴스
 */
class TinkAeadEncryptor(private val aead: TinkAead): TinkEncryptor {

    override fun encrypt(plaintext: ByteArray): ByteArray = aead.encrypt(plaintext)

    override fun decrypt(ciphertext: ByteArray): ByteArray = aead.decrypt(ciphertext)

    override fun encrypt(plaintext: String): String = aead.encrypt(plaintext)

    override fun decrypt(ciphertext: String): String = aead.decrypt(ciphertext)

    override fun toString(): String = "TinkAeadEncryptor"
}
