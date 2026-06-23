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
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import java.util.concurrent.TimeUnit

/**
 * JWT 토큰 발급에 사용된 [KeyChain]을 Redis에 저장하여, 분산환경에서 [KeyChain]을 공유하고, rotate 시에 전파되도록 합니다.
 * 또한 rotate로 인해 key chain이 변경된 경우에도 토큰 파싱이 가능하도록 저장합니다.
 *
 * ```kotlin
 * val redisson: RedissonClient = // ...
 * val repository = RedisKeyChainRepository(redisson)
 * val keyChain = KeyChain()
 * repository.forcedRotate(keyChain)
 * val current = repository.current()
 * // current.id == keyChain.id
 * ```
 *
 * @property keyChainStore  [KeyChain]을 저장하는 Redisson의 RDeque
 * @property capacity rotated key chain 의 최대 저장 갯수 (기본값은 [KeyChainRepository.DEFAULT_CAPACITY] (10))
 */
class RedisKeyChainRepository private constructor(
    private val keyChainStore: RDeque<KeyChainDto>,
    private val rotationLock: RLock,
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
            val lock = redisson.getLock("$queueName:rotation-lock")
            return RedisKeyChainRepository(queue, lock, capacity.coerceIn(MIN_CAPACITY, MAX_CAPACITY))
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
        return withRotationLock {
            val currentKeyChain = doLoadCurrent()
            cachedCurrent = currentKeyChain

            if (currentKeyChain == null) {
                return@withRotationLock changeCurrentAndTrim(keyChain)
            }

            // current KeyChain이 유효하다면 굳이 revoke 하지 않습니다 (다른 서버에서도 주기적으로 revoke하게 되면 ping-pong이 되어버립니다.)
            if (!currentKeyChain.isExpired) {
                log.debug { "기존 KeyChain의 유효기간이 남아서 rotate 하지 않습니다." }
                return@withRotationLock false
            }

            if (currentKeyChain.id != keyChain.id) {
                return@withRotationLock changeCurrentAndTrim(keyChain)
            }

            false
        }
    }

    override fun forcedRotate(keyChain: KeyChain): Boolean {
        return withRotationLock {
            changeCurrentAndTrim(keyChain)
        }
    }

    private fun changeCurrentAndTrim(keyChain: KeyChain): Boolean {
        val changed = changeCurrent(keyChain)
        if (changed) {
            while (keyChainStore.size > capacity) {
                log.debug { "Remove oldest keychain ..." }
                keyChainStore.removeLast()
            }
        }
        return changed
    }

    private inline fun withRotationLock(action: () -> Boolean): Boolean {
        check(rotationLock.tryLock(5, 30, TimeUnit.SECONDS)) {
            "Failed to acquire Redis keychain rotation lock."
        }
        return try {
            action()
        } finally {
            runCatching {
                if (rotationLock.isHeldByCurrentThread) {
                    rotationLock.unlock()
                }
            }
        }
    }

    override fun deleteAll() {
        log.warn { "저장된 모든 KeyChain 정보를 삭제합니다. 테스트 시에만 사용하세요" }
        cachedCurrent = null
        keyChainStore.clear()
    }
}
