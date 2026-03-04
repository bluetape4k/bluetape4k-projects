package io.bluetape4k.tink.digest

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.publicLazy

/**
 * 미리 구성된 [TinkDigester] 인스턴스를 제공하는 팩토리 싱글턴입니다.
 *
 * 각 인스턴스는 lazy 초기화되며, JDK [java.security.MessageDigest]가 지원하는
 * 표준 해시 알고리즘을 사용합니다.
 *
 * ```kotlin
 * val hash = TinkDigesters.SHA256.digest("Hello, World!")
 * TinkDigesters.SHA256.matches("Hello, World!", hash) // true
 * ```
 */
object TinkDigesters: KLogging() {

    /** MD5 해시 알고리즘 (128-bit). 보안 용도에는 권장하지 않습니다. */
    val MD5: TinkDigester by publicLazy { TinkDigester("MD5") }

    /** SHA-1 해시 알고리즘 (160-bit). 보안 용도에는 권장하지 않습니다. */
    val SHA1: TinkDigester by publicLazy { TinkDigester("SHA-1") }

    /** SHA-256 해시 알고리즘 (256-bit). 범용 보안 해시에 권장됩니다. */
    val SHA256: TinkDigester by publicLazy { TinkDigester("SHA-256") }

    /** SHA-384 해시 알고리즘 (384-bit). SHA-256보다 높은 보안이 필요할 때 사용합니다. */
    val SHA384: TinkDigester by publicLazy { TinkDigester("SHA-384") }

    /** SHA-512 해시 알고리즘 (512-bit). 최고 수준의 SHA-2 해시입니다. */
    val SHA512: TinkDigester by publicLazy { TinkDigester("SHA-512") }
}
