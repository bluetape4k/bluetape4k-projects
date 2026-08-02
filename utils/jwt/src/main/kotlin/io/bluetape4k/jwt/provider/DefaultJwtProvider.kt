package io.bluetape4k.jwt.provider

import io.bluetape4k.jwt.JwtConsts.DefaultKeyChainRepository
import io.bluetape4k.jwt.JwtConsts.DefaultSignatureAlgorithm
import io.bluetape4k.jwt.keychain.KeyChain
import io.bluetape4k.jwt.keychain.repository.KeyChainRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.logging.info
import io.bluetape4k.support.requirePositiveNumber
import io.jsonwebtoken.security.SignatureAlgorithm
import java.util.Timer
import kotlin.concurrent.timer
import kotlin.concurrent.withLock

/**
 * 키체인 로테이션을 자동 수행하는 기본 [JwtProvider] 구현체입니다.
 *
 * ## 동작/계약
 * - 생성 시 즉시 최초 키체인 로테이션을 수행합니다.
 * - 60초 간격으로 키체인 만료를 검사하고 필요 시 로테이션합니다.
 * - [repository]를 통해 키체인을 영속화합니다.
 * - 회전 타이머는 이 공급자가 소유하므로 [close]로 취소합니다.
 * - [repository]는 빌려서 사용하며 [close]에서 닫지 않습니다. 저장소 수명주기는 호출자가 관리합니다.
 *
 * @property signatureAlgorithm RSA 기반 서명 알고리즘
 */
class DefaultJwtProvider private constructor(
    override val signatureAlgorithm: SignatureAlgorithm,
    private val repository: KeyChainRepository,
    private val rotationIntervalMillis: Long = DEFAULT_ROTATION_TIME_MILLIS,
): AbstractJwtProvider() {

    companion object: KLogging() {
        private const val DEFAULT_ROTATION_TIME_MILLIS = 60_000L

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

        /** 테스트에서 짧은 주기의 rotation timer를 검증하기 위한 내부 생성 경로입니다. */
        internal fun forTesting(
            signatureAlgorithm: SignatureAlgorithm = DefaultSignatureAlgorithm,
            keyChainRepository: KeyChainRepository = DefaultKeyChainRepository,
            rotationIntervalMillis: Long,
        ): DefaultJwtProvider {
            return DefaultJwtProvider(signatureAlgorithm, keyChainRepository, rotationIntervalMillis)
        }
    }

    private var currentKeyChain: KeyChain? = null
    private val lifecycleLock = Any()
    @Volatile
    private var closed = false
    private var timer: Timer? = null

    init {
        rotationIntervalMillis.requirePositiveNumber("rotationIntervalMillis")
        rotate()
        timer = timer(this.javaClass.name, true, rotationIntervalMillis, rotationIntervalMillis) {
            synchronized(lifecycleLock) {
                if (!closed) rotate()
            }
        }
    }

    /**
     * 이 공급자가 소유한 key-chain rotation timer를 취소합니다.
     *
     * [repository]는 빌린 자원이므로 닫지 않습니다. 공급자와 저장소를 함께 생성한 호출자도
     * 각각 [close]를 호출해 두 자원의 수명을 명시해야 합니다.
     */
    override fun close() {
        synchronized(lifecycleLock) {
            if (closed) return

            closed = true
            timer?.cancel()
            timer = null
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
