package io.bluetape4k.jwt.keychain.repository.inmemory

import io.bluetape4k.jwt.keychain.KeyChain
import io.bluetape4k.jwt.keychain.repository.AbstractKeyChainRepository
import io.bluetape4k.jwt.keychain.repository.KeyChainRepository.Companion.DEFAULT_CAPACITY
import io.bluetape4k.jwt.keychain.repository.KeyChainRepository.Companion.MAX_CAPACITY
import io.bluetape4k.jwt.keychain.repository.KeyChainRepository.Companion.MIN_CAPACITY
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.bluetape4k.support.coerce
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * [KeyChain] 정보를 In memory에 저장하고, rotated key chain을 관리합니다.
 * 단 분산환경에서는 사용하지 못합니다.
 *
 * @property capacity  rotated key chain 의 최대 저장 갯수 (기본값은 DEFAULT_CAPACITY (10))
 */
class InMemoryKeyChainRepository private constructor(
    override val capacity: Int,
): AbstractKeyChainRepository() {

    companion object: KLogging() {
        @JvmStatic
        operator fun invoke(capacity: Int = DEFAULT_CAPACITY): InMemoryKeyChainRepository {
            return InMemoryKeyChainRepository(capacity.coerce(MIN_CAPACITY, MAX_CAPACITY))
        }
    }

    private val keyChainStore = ConcurrentLinkedDeque<KeyChain>()

    override fun doLoadCurrent(): KeyChain? {
        return keyChainStore.firstOrNull()
    }

    override fun doInsert(keyChain: KeyChain) {
        keyChainStore.addFirst(keyChain)
    }

    override fun findOrNull(kid: String): KeyChain? {
        return keyChainStore.find { it.id == kid }
    }

    override fun rotate(keyChain: KeyChain): Boolean {
        log.debug { "Rotate KeyChain. kid=${keyChain.id}" }
        if (keyChainStore.isEmpty()) {
            return changeCurrent(keyChain)
        }

        val currentKeyChain = current()

        // current KeyChain이 아직 유효하다면 굳이 revoke 하지 않는다
        if (!currentKeyChain.isExpired) {
            log.debug { "기존 KeyChain의 유효기간이 남아서 rotate 하지 않습니다." }
            return false
        }

        if (currentKeyChain.id != keyChain.id) {
            return changeCurrent(keyChain).apply {
                if (keyChainStore.size > capacity) {
                    log.debug { "Remove oldest keychain ..." }
                    keyChainStore.removeLast()
                }
            }
        }
        return false
    }

    override fun forcedRotate(keyChain: KeyChain): Boolean {
        if (keyChainStore.isEmpty()) {
            return changeCurrent(keyChain)
        }
        return changeCurrent(keyChain).apply {
            if (keyChainStore.size > capacity) {
                log.debug { "Remove oldest keychain ..." }
                keyChainStore.removeLast()
            }
        }
    }

    override fun deleteAll() {
        log.warn { "저장된 모든 KeyChain을 삭제합니다. NOTE: 테스트 시에만 사용하세요" }
        cachedCurrent = null
        keyChainStore.clear()
    }
}
