package io.bluetape4k.jwt.keychain.repository

import io.bluetape4k.jwt.keychain.KeyChain
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import java.util.*
import kotlin.concurrent.timer

abstract class AbstractKeyChainRepository: KeyChainRepository {

    companion object: KLogging() {
        /**
         * 기본 Refresh time (1분)
         */
        private const val DEFAULT_REFRESH_TIME_MILLIS = 60_000L
    }

    protected var cachedCurrent: KeyChain? = null
    private var timer: Timer? = null

    init {
        timer = timer(this::class.java.name, true, DEFAULT_REFRESH_TIME_MILLIS, DEFAULT_REFRESH_TIME_MILLIS) {
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
        runCatching {
            cachedCurrent = doLoadCurrent()
        }.onFailure {
            log.warn(it) { "Fail to refresh current keyChain" }
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
