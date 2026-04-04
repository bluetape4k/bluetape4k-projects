package io.bluetape4k.jwt.provider

import io.bluetape4k.jwt.JwtConsts.DefaultKeyChainRepository
import io.bluetape4k.jwt.JwtConsts.DefaultSignatureAlgorithm
import io.bluetape4k.jwt.keychain.KeyChain
import io.bluetape4k.jwt.keychain.repository.KeyChainRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.logging.info
import io.jsonwebtoken.security.SignatureAlgorithm
import java.util.*
import kotlin.concurrent.timer
import kotlin.concurrent.withLock

/**
 * 키체인 로테이션을 자동 수행하는 기본 [JwtProvider] 구현체입니다.
 *
 * ## 동작/계약
 * - 생성 시 즉시 최초 키체인 로테이션을 수행합니다.
 * - 60초 간격으로 키체인 만료를 검사하고 필요 시 로테이션합니다.
 * - [repository]를 통해 키체인을 영속화합니다.
 *
 * @property signatureAlgorithm RSA 기반 서명 알고리즘
 */
class DefaultJwtProvider private constructor(
    override val signatureAlgorithm: SignatureAlgorithm,
    private val repository: KeyChainRepository,
): AbstractJwtProvider() {

    companion object: KLogging() {
        /**
         * [DefaultJwtProvider] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val provider = DefaultJwtProvider()
         * val jwt = provider.compose { claim("userId", "alice"); expirationAfterMinutes = 60 }
         * val reader = provider.parse(jwt)
         * // reader.claim<String>("userId") == "alice"
         * ```
         *
         * @param signatureAlgorithm 서명 알고리즘 (기본: RS256)
         * @param keyChainRepository 키체인 저장소 (기본: 인메모리)
         * @return [DefaultJwtProvider] 인스턴스
         */
        @JvmStatic
        operator fun invoke(
            signatureAlgorithm: SignatureAlgorithm = DefaultSignatureAlgorithm,
            keyChainRepository: KeyChainRepository = DefaultKeyChainRepository,
        ): DefaultJwtProvider {
            log.info { "Create DefaultJwtProvider" }
            return DefaultJwtProvider(signatureAlgorithm, keyChainRepository)
        }
    }

    private var currentKeyChain: KeyChain? = null
    private var timer: Timer? = null

    init {
        rotate()
        timer = timer(this.javaClass.name, true, 60_000, 60_000) {
            rotate()
        }
    }

    override fun currentKeyChain(): KeyChain {
        return currentKeyChain ?: repository.current().apply { currentKeyChain = this }
    }

    override fun rotate(): Boolean {
        log.info { "try rotate current KeyChain ..." }

        lock.withLock {
            var rotated = false
            runCatching {
                val newKeyChain = createKeyChain()
                if (repository.rotate(newKeyChain)) {
                    log.info { "Rotate to new KeyChain. kid=${newKeyChain.id}" }
                    currentKeyChain = newKeyChain
                    rotated = true
                }
            }.onFailure { error ->
                log.error(error) { "Fail to rotate." }
            }
            return rotated
        }
    }

    override fun forcedRotate(): Boolean {
        log.info { "forced rotate current KeyChain ..." }

        lock.withLock {
            var rotated = false

            runCatching {
                val newKeyChain = createKeyChain()
                if (repository.forcedRotate(newKeyChain)) {
                    log.info { "Rotate to new KeyChain. kid=${newKeyChain.id}" }
                    currentKeyChain = newKeyChain
                    rotated = true
                }
            }.onFailure { error ->
                log.error(error) { "Fail to rotate." }
            }

            return rotated
        }
    }

    override fun findKeyChain(kid: String): KeyChain? {
        log.debug { "find KeyChain. kid=$kid" }
        return if (currentKeyChain?.id == kid) currentKeyChain else repository.findOrNull(kid)
    }
}
