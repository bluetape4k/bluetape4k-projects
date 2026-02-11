package io.bluetape4k.jwt.keychain.repository.redis

import io.bluetape4k.LibraryName
import io.bluetape4k.jwt.keychain.KeyChain
import io.bluetape4k.jwt.keychain.KeyChainDto
import io.bluetape4k.jwt.keychain.repository.AbstractKeyChainRepository
import io.bluetape4k.jwt.keychain.repository.KeyChainRepository
import io.bluetape4k.jwt.keychain.repository.KeyChainRepository.Companion.MAX_CAPACITY
import io.bluetape4k.jwt.keychain.repository.KeyChainRepository.Companion.MIN_CAPACITY
import io.bluetape4k.jwt.keychain.toDto
import io.bluetape4k.jwt.keychain.toKeyChain
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requireNotBlank
import org.redisson.api.RDeque
import org.redisson.api.RedissonClient

/**
 * JWT 토큰 발급에 사용된 [KeyChain]을 Redis에 저장하여, 분산환경에서 [KeyChain]을 공유하고, rotate 시에 전파되도록 합니다.
 * 또한 rotate로 인해 key chain이 변경된 경우에도 토큰 파싱이 가능하도록 저장합니다.
 *
 * @property keyChainStore  [KeyChain]을 저장하는 Redisson의 RDeque
 * @property capacity rotated key chain 의 최대 저장 갯수 (기본값은 [KeyChainRepository.DEFAULT_CAPACITY] (10))
 */
class RedisKeyChainRepository private constructor(
    private val keyChainStore: RDeque<KeyChainDto>,
    override val capacity: Int,
): AbstractKeyChainRepository() {

    companion object: KLogging() {
        const val DEFAULT_QUEUE_NAME = "$LibraryName:jwt:keychain"

        @JvmStatic
        operator fun invoke(
            redisson: RedissonClient,
            queueName: String = DEFAULT_QUEUE_NAME,
            capacity: Int = KeyChainRepository.DEFAULT_CAPACITY,
        ): RedisKeyChainRepository {
            val queue = redisson.getDeque<KeyChainDto>(queueName)
            return RedisKeyChainRepository(queue, capacity.coerceIn(MIN_CAPACITY, MAX_CAPACITY))
        }
    }

    override fun doLoadCurrent(): KeyChain? {
        return keyChainStore.firstOrNull()?.toKeyChain()
    }

    override fun doInsert(keyChain: KeyChain) {
        keyChainStore.addFirst(keyChain.toDto())
    }

    override fun findOrNull(kid: String): KeyChain? {
        kid.requireNotBlank("kid")
        return keyChainStore.firstOrNull { it.id == kid }?.toKeyChain()
    }

    override fun rotate(keyChain: KeyChain): Boolean {
        log.debug { "Rotate KeyChain. kid=${keyChain.id}" }
        if (keyChainStore.isEmpty()) {
            return changeCurrent(keyChain)
        }

        val currentKeyChain = current()

        // current KeyChain이 유효하다면 굳이 revoke 하지 않습니다 (다른 서버에서도 주기적으로 revoke하게 되면 ping-pong이 되어버립니다.)
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
        log.warn { "저장된 모든 KeyChain 정보를 삭제합니다. 테스트 시에만 사용하세요" }
        cachedCurrent = null
        keyChainStore.clear()
    }
}
