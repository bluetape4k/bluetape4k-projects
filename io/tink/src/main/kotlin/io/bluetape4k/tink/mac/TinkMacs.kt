package io.bluetape4k.tink.mac

import com.google.crypto.tink.mac.HmacKeyManager
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.publicLazy
import io.bluetape4k.tink.macKeysetHandle
import io.bluetape4k.tink.registerTink

/**
 * 미리 구성된 [TinkMac] 인스턴스를 제공하는 팩토리 싱글턴입니다.
 *
 * 각 인스턴스는 lazy 초기화되며 독립적인 키를 사용합니다.
 *
 * ```kotlin
 * val tag = TinkMacs.HMAC_SHA256.computeMac("데이터")
 * val valid = TinkMacs.HMAC_SHA256.verifyMac(tag, "데이터")
 * // valid == true
 * ```
 */
object TinkMacs: KLogging() {

    init {
        registerTink()
    }

    /** HMAC-SHA256 256비트 태그 기반 MAC 인스턴스. 범용 데이터 무결성 검증에 권장됩니다. */
    val HMAC_SHA256: TinkMac by publicLazy {
        TinkMac(macKeysetHandle(HmacKeyManager.hmacSha256Template()))
    }

    /** HMAC-SHA512 256비트 태그 기반 MAC 인스턴스. SHA512 해시 함수를 사용합니다. */
    val HMAC_SHA512: TinkMac by publicLazy {
        TinkMac(macKeysetHandle(HmacKeyManager.hmacSha512HalfDigestTemplate()))
    }

    /** HMAC-SHA512 512비트 태그 기반 MAC 인스턴스. 최대 보안 강도가 필요한 경우 사용합니다. */
    val HMAC_SHA512_512BITTAG: TinkMac by publicLazy {
        TinkMac(macKeysetHandle(HmacKeyManager.hmacSha512Template()))
    }
}
