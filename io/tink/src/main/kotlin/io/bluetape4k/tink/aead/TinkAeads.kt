package io.bluetape4k.tink.aead

import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.aead.ChaCha20Poly1305KeyManager
import com.google.crypto.tink.aead.XChaCha20Poly1305KeyManager
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.publicLazy
import io.bluetape4k.tink.aeadKeysetHandle
import io.bluetape4k.tink.registerTink

/**
 * 미리 구성된 [TinkAead] 인스턴스를 제공하는 팩토리 싱글턴입니다.
 *
 * 각 인스턴스는 lazy 초기화되며, 서로 다른 키를 사용하므로
 * 같은 알고리즘이라도 교차 복호화가 불가능합니다.
 *
 * ```kotlin
 * val encrypted = TinkAeads.AES256_GCM.encrypt("Hello, World!")
 * val decrypted = TinkAeads.AES256_GCM.decrypt(encrypted)
 * // decrypted == "Hello, World!"
 * ```
 */
object TinkAeads : KLogging() {

    init {
        registerTink()
    }

    /** AES256-GCM 알고리즘 기반 AEAD 인스턴스. 범용 인증 암호화에 권장됩니다. */
    val AES256_GCM: TinkAead by publicLazy {
        TinkAead(aeadKeysetHandle(AesGcmKeyManager.aes256GcmTemplate()))
    }

    /** AES128-GCM 알고리즘 기반 AEAD 인스턴스. 성능이 중요한 경우 사용합니다. */
    val AES128_GCM: TinkAead by publicLazy {
        TinkAead(aeadKeysetHandle(AesGcmKeyManager.aes128GcmTemplate()))
    }

    /** ChaCha20-Poly1305 알고리즘 기반 AEAD 인스턴스. 하드웨어 AES 가속이 없는 환경에 적합합니다. */
    val CHACHA20_POLY1305: TinkAead by publicLazy {
        TinkAead(aeadKeysetHandle(ChaCha20Poly1305KeyManager.chaCha20Poly1305Template()))
    }

    /** XChaCha20-Poly1305 알고리즘 기반 AEAD 인스턴스. 더 큰 nonce(192bit)로 nonce 재사용 위험을 줄입니다. */
    val XCHACHA20_POLY1305: TinkAead by publicLazy {
        TinkAead(aeadKeysetHandle(XChaCha20Poly1305KeyManager.xChaCha20Poly1305Template()))
    }
}
