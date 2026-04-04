package io.bluetape4k.jwt.provider

import io.bluetape4k.jwt.composer.JwtComposer
import io.bluetape4k.jwt.composer.JwtComposerDsl
import io.bluetape4k.jwt.composer.composeJwt
import io.bluetape4k.jwt.keychain.KeyChain
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * [JwtProvider]의 공통 로직을 구현하는 추상 기반 클래스입니다.
 *
 * ## 동작/계약
 * - [composer] 및 [compose] 호출은 내부 [ReentrantLock]으로 스레드 안전하게 보호됩니다.
 * - [keyChain]이 `null`이면 [currentKeyChain]을 사용합니다.
 *
 * ```kotlin
 * class MyProvider : AbstractJwtProvider() { ... }
 * val jwt = provider.compose { claim("key", "value") }
 * ```
 */
abstract class AbstractJwtProvider: JwtProvider {

    /** 스레드 안전한 키체인 접근을 위한 락입니다. */
    protected val lock = ReentrantLock()

    /**
     * [JwtComposer]를 생성합니다.
     *
     * ```kotlin
     * val provider = JwtProviderFactory.default()
     * val composer = provider.composer()
     * composer.claim("userId", "alice")
     * composer.expirationAfterMinutes(60L)
     * val jwt = composer.compose()
     * // jwt.isNotBlank() == true
     * ```
     *
     * @param keyChain 사용할 키체인. `null`이면 [currentKeyChain]을 사용합니다.
     * @return [JwtComposer] 인스턴스
     */
    override fun composer(keyChain: KeyChain?): JwtComposer {
        return lock.withLock {
            JwtComposer(keyChain ?: currentKeyChain())
        }
    }

    /**
     * DSL 블록으로 JWT 문자열을 생성합니다.
     *
     * ```kotlin
     * val provider = JwtProviderFactory.default()
     * val jwt = provider.compose {
     *     subject = "alice"
     *     issuer = "bluetape4k"
     *     claim("role", "admin")
     *     expirationAfterMinutes = 60
     * }
     * // jwt.count { it == '.' } == 2
     * ```
     *
     * @param keyChain 사용할 키체인. `null`이면 [currentKeyChain]을 사용합니다.
     * @param builder JWT 구성 DSL 블록
     * @return 생성된 JWT 문자열
     */
    override fun compose(
        keyChain: KeyChain?,
        builder: JwtComposerDsl.() -> Unit,
    ): String {
        return lock.withLock {
            composeJwt(keyChain ?: currentKeyChain(), builder)
        }
    }
}
