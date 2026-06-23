package io.bluetape4k.jwt.keychain.redis

import io.bluetape4k.jwt.keychain.KeyChain
import io.bluetape4k.jwt.keychain.AbstractKeyChainRepositoryTest
import io.bluetape4k.jwt.keychain.repository.KeyChainRepository
import io.bluetape4k.jwt.keychain.repository.redis.RedisKeyChainRepository
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.testcontainers.storage.RedisServer
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID
import kotlin.test.assertNull

class RedisKeyChainRepositoryTest: AbstractKeyChainRepositoryTest() {

    private val redisson by lazy {
        RedisServer.Launcher.RedissonLib.getRedisson()
    }

    override val repository: KeyChainRepository by lazy {
        RedisKeyChainRepository(redisson)
    }

    @Test
    fun `expired cached keychain rotates with single Redis winner`() {
        val queueName = "test:jwt:keychain:${UUID.randomUUID()}"
        val seedRepository = RedisKeyChainRepository(redisson, queueName = queueName)
        val firstNode = RedisKeyChainRepository(redisson, queueName = queueName)
        val secondNode = RedisKeyChainRepository(redisson, queueName = queueName)

        try {
            val expiredKeyChain = KeyChain(expiredTtl = Duration.ofMillis(1))
            seedRepository.forcedRotate(expiredKeyChain).shouldBeTrue()
            Thread.sleep(10)

            firstNode.current() shouldBeEqualTo expiredKeyChain
            secondNode.current() shouldBeEqualTo expiredKeyChain

            val firstCandidate = KeyChain()
            val secondCandidate = KeyChain()

            val firstRotated = firstNode.rotate(firstCandidate)
            val secondRotated = secondNode.rotate(secondCandidate)

            listOf(firstRotated, secondRotated).count { it }.shouldBeEqualTo(1)

            val winner = if (firstRotated) firstCandidate else secondCandidate
            val loser = if (firstRotated) secondCandidate else firstCandidate

            firstNode.current() shouldBeEqualTo winner
            secondNode.current() shouldBeEqualTo winner
            seedRepository.findOrNull(winner.id) shouldBeEqualTo winner
            assertNull(seedRepository.findOrNull(loser.id))
        } finally {
            seedRepository.deleteAll()
        }
    }
}
