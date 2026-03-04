package io.bluetape4k.tink.encrypt

import io.bluetape4k.tink.daead.TinkDeterministicAead

/**
 * [TinkDeterministicAead] 기반의 결정적(deterministic) [TinkEncryptor] 구현체입니다.
 *
 * 동일한 평문과 키에 대해 항상 동일한 암호문을 생성합니다.
 * 데이터베이스 필드 암호화 후 검색이 필요한 경우에 사용합니다.
 *
 * **주의**: 동일 평문이 동일 암호문을 생성하므로 패턴 유출 가능성이 있습니다.
 * 이 특성이 필요하지 않다면 [TinkAeadEncryptor]를 사용하세요.
 *
 * ```kotlin
 * val encryptor = TinkDaeadEncryptor(TinkDaeads.AES256_SIV)
 * val ct1 = encryptor.encrypt("Hello")
 * val ct2 = encryptor.encrypt("Hello")
 * // ct1 == ct2 (결정적)
 * ```
 *
 * @param daead 사용할 [TinkDeterministicAead] 인스턴스
 */
class TinkDaeadEncryptor(private val daead: TinkDeterministicAead): TinkEncryptor {

    override fun encrypt(plaintext: ByteArray): ByteArray = daead.encryptDeterministically(plaintext)

    override fun decrypt(ciphertext: ByteArray): ByteArray = daead.decryptDeterministically(ciphertext)

    override fun encrypt(plaintext: String): String = daead.encryptDeterministically(plaintext)

    override fun decrypt(ciphertext: String): String = daead.decryptDeterministically(ciphertext)

    override fun toString(): String = "TinkDaeadEncryptor"
}
