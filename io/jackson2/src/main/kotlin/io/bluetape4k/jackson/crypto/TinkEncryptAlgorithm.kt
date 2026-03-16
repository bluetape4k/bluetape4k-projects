package io.bluetape4k.jackson.crypto

import io.bluetape4k.tink.encrypt.TinkEncryptor
import io.bluetape4k.tink.encrypt.TinkEncryptors

/**
 * [JsonTinkEncrypt] 어노테이션에서 사용할 Tink 기반 암호화 알고리즘을 열거합니다.
 *
 * 각 값은 [TinkEncryptors] 싱글턴의 미리 구성된 [TinkEncryptor] 인스턴스에 매핑됩니다.
 *
 * ```kotlin
 * val encryptor = TinkEncryptAlgorithm.AES256_GCM.getEncryptor()
 * val encrypted = encryptor.encrypt("secret")
 * ```
 */
enum class TinkEncryptAlgorithm {

    /** AES256-GCM 비결정적 암호화 — 범용 AEAD, 권장 기본값 */
    AES256_GCM,

    /** AES128-GCM 비결정적 암호화 — 성능 우선 환경 */
    AES128_GCM,

    /** ChaCha20-Poly1305 비결정적 암호화 — 하드웨어 AES 가속이 없는 환경 */
    CHACHA20_POLY1305,

    /** XChaCha20-Poly1305 비결정적 암호화 — 큰 nonce(192bit)로 재사용 위험 감소 */
    XCHACHA20_POLY1305,

    /** AES256-SIV 결정적 암호화 — 동일 평문에 동일 암호문, DB 검색 가능 */
    DETERMINISTIC_AES256_SIV;

    /**
     * 이 알고리즘에 대응하는 [TinkEncryptor] 인스턴스를 반환합니다.
     *
     * @return 미리 구성된 [TinkEncryptor] 인스턴스
     */
    fun getEncryptor(): TinkEncryptor = when (this) {
        AES256_GCM -> TinkEncryptors.AES256_GCM
        AES128_GCM -> TinkEncryptors.AES128_GCM
        CHACHA20_POLY1305 -> TinkEncryptors.CHACHA20_POLY1305
        XCHACHA20_POLY1305 -> TinkEncryptors.XCHACHA20_POLY1305
        DETERMINISTIC_AES256_SIV -> TinkEncryptors.DETERMINISTIC_AES256_SIV
    }
}
