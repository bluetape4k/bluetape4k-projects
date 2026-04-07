package io.bluetape4k.tink.daead

import com.google.crypto.tink.daead.AesSivKeyManager
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.publicLazy
import io.bluetape4k.tink.daeadKeysetHandle
import io.bluetape4k.tink.keyset.VersionedKeysetStore
import io.bluetape4k.tink.keyset.VersionedTinkDaead
import io.bluetape4k.tink.registerTink

/**
 * 미리 구성된 [TinkDeterministicAead] 인스턴스를 제공하는 팩토리 싱글턴입니다.
 *
 * 각 인스턴스는 lazy 초기화되며, 동일한 평문과 연관 데이터에 대해
 * 항상 동일한 암호문을 생성합니다 (결정적 암호화).
 *
 * ```kotlin
 * val ct = TinkDaeads.AES256_SIV.encryptDeterministically("검색 가능한 필드")
 * val pt = TinkDaeads.AES256_SIV.decryptDeterministically(ct)
 * // pt == "검색 가능한 필드"
 * ```
 */
object TinkDaeads: KLogging() {

    init {
        registerTink()
    }

    /** AES256-SIV 알고리즘 기반 Deterministic AEAD 인스턴스. 결정적 암호화 기본 권장 알고리즘입니다. */
    val AES256_SIV: TinkDeterministicAead by publicLazy {
        TinkDeterministicAead(daeadKeysetHandle(AesSivKeyManager.aes256SivTemplate()))
    }

    /**
     * versioned keyset 저장소를 사용하는 Deterministic AEAD 래퍼를 생성합니다.
     *
     * 주기적 키 로테이션과 이전 버전 복호화가 필요한 검색용 암호화 시나리오에 사용합니다.
     */
    fun versioned(keysetStore: VersionedKeysetStore): VersionedTinkDaead =
        VersionedTinkDaead(keysetStore)
}
