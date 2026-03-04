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
 * 미리 구성된 [TinkEncryptor] 인스턴스를 제공하는 팩토리 싱글턴입니다.
 *
 * AEAD(비결정적)와 Deterministic AEAD(결정적) 암호화 구현체를 모두 제공합니다.
 * 각 인스턴스는 lazy 초기화되며, 서로 다른 키를 사용하므로
 * 같은 알고리즘이라도 교차 복호화가 불가능합니다.
 *
 * ```kotlin
 * // 비결정적 암호화 (범용)
 * val encrypted = TinkEncryptors.AES256_GCM.encrypt("Hello, World!")
 * val decrypted = TinkEncryptors.AES256_GCM.decrypt(encrypted)
 *
 * // 결정적 암호화 (DB 검색용)
 * val ct = TinkEncryptors.DETERMINISTIC_AES256_SIV.encrypt("검색 가능한 필드")
 * ```
 */
object TinkEncryptors: KLogging() {

    init {
        registerTink()
    }

    /** AES256-GCM 기반 비결정적 암호화. 범용 인증 암호화에 권장됩니다. */
    val AES256_GCM: TinkEncryptor by publicLazy {
        TinkAeadEncryptor(TinkAead(aeadKeysetHandle(AesGcmKeyManager.aes256GcmTemplate())))
    }

    /** AES128-GCM 기반 비결정적 암호화. 성능이 중요한 경우 사용합니다. */
    val AES128_GCM: TinkEncryptor by publicLazy {
        TinkAeadEncryptor(TinkAead(aeadKeysetHandle(AesGcmKeyManager.aes128GcmTemplate())))
    }

    /** ChaCha20-Poly1305 기반 비결정적 암호화. 하드웨어 AES 가속이 없는 환경에 적합합니다. */
    val CHACHA20_POLY1305: TinkEncryptor by publicLazy {
        TinkAeadEncryptor(TinkAead(aeadKeysetHandle(ChaCha20Poly1305KeyManager.chaCha20Poly1305Template())))
    }

    /** XChaCha20-Poly1305 기반 비결정적 암호화. 더 큰 nonce(192bit)로 nonce 재사용 위험을 줄입니다. */
    val XCHACHA20_POLY1305: TinkEncryptor by publicLazy {
        TinkAeadEncryptor(TinkAead(aeadKeysetHandle(XChaCha20Poly1305KeyManager.xChaCha20Poly1305Template())))
    }

    /** AES256-SIV 기반 결정적 암호화. 동일 평문에 동일 암호문을 생성하여 DB 검색이 가능합니다. */
    val DETERMINISTIC_AES256_SIV: TinkEncryptor by publicLazy {
        TinkDaeadEncryptor(TinkDeterministicAead(daeadKeysetHandle(AesSivKeyManager.aes256SivTemplate())))
    }
}
