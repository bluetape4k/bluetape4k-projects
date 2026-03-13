package io.bluetape4k.tink

import com.google.crypto.tink.KeyTemplate
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.daead.AesSivKeyManager
import com.google.crypto.tink.daead.DeterministicAeadConfig
import com.google.crypto.tink.mac.HmacKeyManager
import com.google.crypto.tink.mac.MacConfig
import kotlinx.atomicfu.atomic

/** 빈 바이트 배열 상수 — associatedData 기본값으로 사용됩니다. */
internal val EMPTY_BYTES = ByteArray(0)

private val tinkRegistered = atomic(false)

/**
 * Google Tink 암호화 라이브러리를 초기화합니다.
 *
 * AEAD, DeterministicAEAD, MAC 설정을 모두 등록하며,
 * 멀티스레드 환경에서 안전하게 1회만 초기화됩니다.
 *
 * ```kotlin
 * registerTink()
 * // 이후 TinkAeads, TinkDaeads, TinkMacs 사용 가능
 * ```
 */
fun registerTink() {
    if (tinkRegistered.compareAndSet(false, true)) {
        AeadConfig.register()
        DeterministicAeadConfig.register()
        MacConfig.register()
    }
}

/**
 * 지정된 [KeyTemplate]로 AEAD용 [KeysetHandle]을 생성합니다.
 *
 * @param keyTemplate 사용할 키 템플릿 (기본값: AES256-GCM)
 * @return 생성된 [KeysetHandle]
 */
fun aeadKeysetHandle(keyTemplate: KeyTemplate = AesGcmKeyManager.aes256GcmTemplate()): KeysetHandle {
    registerTink()
    return KeysetHandle.generateNew(keyTemplate)
}

/**
 * 지정된 [KeyTemplate]로 Deterministic AEAD용 [KeysetHandle]을 생성합니다.
 *
 * @param keyTemplate 사용할 키 템플릿 (기본값: AES256-SIV)
 * @return 생성된 [KeysetHandle]
 */
fun daeadKeysetHandle(keyTemplate: KeyTemplate = AesSivKeyManager.aes256SivTemplate()): KeysetHandle {
    registerTink()
    return KeysetHandle.generateNew(keyTemplate)
}

/**
 * 지정된 [KeyTemplate]으로 MAC용 [KeysetHandle]을 생성합니다.
 *
 * @param keyTemplate 사용할 키 템플릿 (기본값: HMAC-SHA256 256비트 태그)
 * @return 생성된 [KeysetHandle]
 */
fun macKeysetHandle(keyTemplate: KeyTemplate = HmacKeyManager.hmacSha256Template()): KeysetHandle {
    registerTink()
    return KeysetHandle.generateNew(keyTemplate)
}
