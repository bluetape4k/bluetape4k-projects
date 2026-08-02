package io.bluetape4k.jwt.keychain.repository

import io.bluetape4k.jwt.keychain.KeyChain
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requirePositiveNumber
import java.util.Timer
import kotlin.concurrent.timer

/**
 * [KeyChainRepository]의 공통 로직을 구현하는 추상 기반 클래스입니다.
 *
 * ## 동작/계약
 * - 생성 시 기본 1분(60초) 간격으로 현재 키체인을 자동 갱신하는 타이머가 시작됩니다.
 * - [current]는 캐시된 키체인을 우선 반환하며, 없으면 [doLoadCurrent]를 호출합니다.
 * - 저장소가 소유한 타이머는 [close]로 취소하며, [close]는 여러 번 호출해도 안전합니다.
 *
 * ```kotlin
 * class MyRepository : AbstractKeyChainRepository() {
 *     override val capacity = 10
 *     override fun doLoadCurrent(): KeyChain? = null
 *     override fun doInsert(keyChain: KeyChain) {}
 *     override fun findOrNull(kid: String): KeyChain? = null
 *     override fun rotate(keyChain: KeyChain): Boolean = changeCurrent(keyChain)
 *     override fun forcedRotate(keyChain: KeyChain): Boolean = changeCurrent(keyChain)
 *     override fun deleteAll() {}
 * }
 * ```
 */
abstract class AbstractKeyChainRepository(
    private val refreshIntervalMillis: Long = DEFAULT_REFRESH_TIME_MILLIS,
): KeyChainRepository {

    companion object: KLogging() {
        /**
         * 기본 Refresh time (1분)
         */
        private const val DEFAULT_REFRESH_TIME_MILLIS = 60_000L
    }

    protected var cachedCurrent: KeyChain? = null
    private val lifecycleLock = Any()
    @Volatile
    private var closed = false
    private var timer: Timer? = null

    init {
        refreshIntervalMillis.requirePositiveNumber("refreshIntervalMillis")
        timer = timer(this::class.java.name, true, refreshIntervalMillis, refreshIntervalMillis) {
            refreshCurrent()
        }
    }

    protected abstract fun doLoadCurrent(): KeyChain?
    protected abstract fun doInsert(keyChain: KeyChain)

    override fun current(): KeyChain {
        if (cachedCurrent == null) {
            cachedCurrent = doLoadCurrent()
        }
        return cachedCurrent ?: error("Current keyChain을 가져올 수 없습니다. rotate를 먼저 수행해주세요")
    }

    protected fun refreshCurrent() {
        synchronized(lifecycleLock) {
            if (closed) return

            runCatching {
                cachedCurrent = doLoadCurrent()
            }.onFailure {
                log.warn(it) { "Fail to refresh current keyChain" }
            }
        }
    }

    /**
     * 이 저장소가 소유한 refresh timer를 취소합니다.
     *
     * 주입받은 저장소나 클라이언트는 이 클래스가 소유하지 않으므로 닫지 않습니다.
     */
    override fun close() {
        synchronized(lifecycleLock) {
            if (closed) return

            closed = true
            timer?.cancel()
            timer = null
        }
    }

    protected fun changeCurrent(keyChain: KeyChain): Boolean {
        log.debug { "Change new keyChain. kid=${keyChain.id}" }
        var changed = false
        runCatching {
            doInsert(keyChain)
        }.onSuccess {
            cachedCurrent = keyChain
            changed = true
        }.onFailure {
            log.warn(it) { "Fail to change current keyChain. kid=${keyChain.id}" }
        }
        return changed
    }
}
