package io.bluetape4k.jackson.crypto

import io.bluetape4k.tink.encrypt.TinkEncryptor
import io.bluetape4k.tink.encrypt.TinkEncryptors

/**
 * Tink-backed encryption algorithms used by [JsonTinkEncrypt].
 *
 * Each enum value resolves to a preconfigured [TinkEncryptors] singleton. Those
 * singleton encryptors use in-memory keysets generated for the current JVM
 * process, so ciphertext is not durable across restart, rollout, or
 * multi-instance access unless the caller uses a separate persisted keyset API.
 *
 * ```kotlin
 * val encryptor = TinkEncryptAlgorithm.AES256_GCM.getEncryptor()
 * val encrypted = encryptor.encrypt("secret")
 * ```
 */
enum class TinkEncryptAlgorithm {

    /** AES256-GCM non-deterministic encryption for general-purpose AEAD use. */
    AES256_GCM,

    /** AES128-GCM non-deterministic encryption for performance-focused use. */
    AES128_GCM,

    /** ChaCha20-Poly1305 non-deterministic encryption for environments without hardware AES acceleration. */
    CHACHA20_POLY1305,

    /** XChaCha20-Poly1305 non-deterministic encryption with a 192-bit nonce. */
    XCHACHA20_POLY1305,

    /** AES256-SIV deterministic encryption for process-local equality checks only. */
    DETERMINISTIC_AES256_SIV;

    /**
     * 이 알고리즘에 매핑된 singleton [TinkEncryptor]를 반환합니다.
     *
     * @return preconfigured singleton [TinkEncryptor].
     */
    fun getEncryptor(): TinkEncryptor = when (this) {
        AES256_GCM         -> TinkEncryptors.AES256_GCM
        AES128_GCM         -> TinkEncryptors.AES128_GCM
        CHACHA20_POLY1305  -> TinkEncryptors.CHACHA20_POLY1305
        XCHACHA20_POLY1305 -> TinkEncryptors.XCHACHA20_POLY1305
        DETERMINISTIC_AES256_SIV -> TinkEncryptors.DETERMINISTIC_AES256_SIV
    }
}
