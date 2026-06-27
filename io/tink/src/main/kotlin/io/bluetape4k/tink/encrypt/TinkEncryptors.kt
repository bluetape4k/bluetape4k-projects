package io.bluetape4k.tink.encrypt

import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.aead.ChaCha20Poly1305KeyManager
import com.google.crypto.tink.aead.XChaCha20Poly1305KeyManager
import com.google.crypto.tink.daead.AesSivKeyManager
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.publicLazy
import io.bluetape4k.tink.aead.TinkAead
import io.bluetape4k.tink.aeadKeysetHandle
import io.bluetape4k.tink.daead.TinkDeterministicAead
import io.bluetape4k.tink.daeadKeysetHandle
import io.bluetape4k.tink.registerTink

/**
 * Factory singleton for preconfigured [TinkEncryptor] instances.
 *
 * The singleton encryptors are lazily initialized with process-local in-memory
 * keysets. They are useful for ephemeral JSON/document protection, examples,
 * and tests, but must not be used for durable ciphertext that must survive JVM
 * restart, rollout, or multi-instance access. Use versioned keyset APIs when
 * ciphertext is persisted.
 *
 * ```kotlin
 * // Non-deterministic encryption for ephemeral data.
 * val encrypted = TinkEncryptors.AES256_GCM.encrypt("Hello, World!")
 * val decrypted = TinkEncryptors.AES256_GCM.decrypt(encrypted)
 *
 * // Deterministic equality within this singleton keyset only.
 * val ct = TinkEncryptors.DETERMINISTIC_AES256_SIV.encrypt("searchable field")
 * ```
 */
object TinkEncryptors: KLogging() {

    init {
        registerTink()
    }

    /** AES256-GCM non-deterministic encryption for general authenticated encryption. */
    val AES256_GCM: TinkEncryptor by publicLazy {
        TinkAeadEncryptor(TinkAead(aeadKeysetHandle(AesGcmKeyManager.aes256GcmTemplate())))
    }

    /** AES128-GCM non-deterministic encryption for performance-focused use. */
    val AES128_GCM: TinkEncryptor by publicLazy {
        TinkAeadEncryptor(TinkAead(aeadKeysetHandle(AesGcmKeyManager.aes128GcmTemplate())))
    }

    /** ChaCha20-Poly1305 non-deterministic encryption for environments without hardware AES acceleration. */
    val CHACHA20_POLY1305: TinkEncryptor by publicLazy {
        TinkAeadEncryptor(TinkAead(aeadKeysetHandle(ChaCha20Poly1305KeyManager.chaCha20Poly1305Template())))
    }

    /** XChaCha20-Poly1305 non-deterministic encryption with a 192-bit nonce. */
    val XCHACHA20_POLY1305: TinkEncryptor by publicLazy {
        TinkAeadEncryptor(TinkAead(aeadKeysetHandle(XChaCha20Poly1305KeyManager.xChaCha20Poly1305Template())))
    }

    /** AES256-SIV deterministic encryption for process-local equality checks only. */
    val DETERMINISTIC_AES256_SIV: TinkEncryptor by publicLazy {
        TinkDaeadEncryptor(TinkDeterministicAead(daeadKeysetHandle(AesSivKeyManager.aes256SivTemplate())))
    }
}
