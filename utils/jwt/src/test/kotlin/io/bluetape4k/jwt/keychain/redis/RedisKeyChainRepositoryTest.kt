package io.bluetape4k.jwt.keychain.redis

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.jwt.keychain.AbstractKeyChainRepositoryTest
import io.bluetape4k.jwt.keychain.KeyChain
import io.bluetape4k.jwt.keychain.KeyChainDto
import io.bluetape4k.jwt.keychain.repository.KeyChainRepository
import io.bluetape4k.jwt.keychain.repository.redis.REDIS_ROTATION_LOCK_WAIT_SECONDS
import io.bluetape4k.jwt.keychain.repository.redis.RedisKeyChainRepository
import io.bluetape4k.jwt.keychain.repository.redis.withRedisRotationLock
import io.bluetape4k.testcontainers.storage.RedisServer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.redisson.Redisson
import org.redisson.api.RDeque
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import java.time.Duration
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertNull
import kotlin.test.assertSame

class RedisKeyChainRepositoryTest: AbstractKeyChainRepositoryTest() {

    private val redisson by lazy {
        RedisServer.Launcher.RedissonLib.getRedisson()
    }

    override val repository: KeyChainRepository by lazy {
        RedisKeyChainRepository(redisson)
    }

    @Test
    fun `rotation uses watchdog-backed lock acquisition`() {
        val queue = mockk<RDeque<KeyChainDto>>(relaxed = true)
        val lock = mockk<RLock>(relaxed = true)
        val redisson = mockk<RedissonClient>(relaxed = true)
        every { redisson.getDeque<KeyChainDto>(any<String>()) } returns queue
        every { redisson.getLock(any<String>()) } returns lock
        every { lock.tryLock(REDIS_ROTATION_LOCK_WAIT_SECONDS, TimeUnit.SECONDS) } returns true
        every { lock.isHeldByCurrentThread } returns true

        RedisKeyChainRepository(redisson).forcedRotate(KeyChain()).shouldBeTrue()

        verify(exactly = 1) { lock.tryLock(REDIS_ROTATION_LOCK_WAIT_SECONDS, TimeUnit.SECONDS) }
    }

    @Test
    fun `rotation reports ownership loss before commit`() {
        val lock = mockk<RLock>()
        every { lock.tryLock(REDIS_ROTATION_LOCK_WAIT_SECONDS, TimeUnit.SECONDS) } returns true
        every { lock.isHeldByCurrentThread } returns false

        val failure = assertFailsWith<IllegalStateException> {
            withRedisRotationLock(lock) { Unit }
        }

        failure.message shouldContain "ownership was lost"
    }

    @Test
    fun `rotation keeps primary failure when unlock fails`() {
        val lock = mockk<RLock>()
        val primaryFailure = IllegalStateException("commit failed")
        val unlockFailure = IllegalStateException("unlock failed")
        every { lock.tryLock(REDIS_ROTATION_LOCK_WAIT_SECONDS, TimeUnit.SECONDS) } returns true
        every { lock.isHeldByCurrentThread } returns true
        every { lock.unlock() } throws unlockFailure

        val failure = assertFailsWith<IllegalStateException> {
            withRedisRotationLock(lock) { throw primaryFailure }
        }

        assertSame(primaryFailure, failure)
        assertSame(unlockFailure, failure.suppressed.single())
    }

    @Test
    fun `watchdog keeps rotation lock until a long commit completes`() {
        val firstClient = createWatchdogClient()
        val secondClient = createWatchdogClient()
        val lockName = "test:jwt:keychain:watchdog:${UUID.randomUUID()}"
        val ownerLock = LeaseCappingLock(firstClient.getLock(lockName), 250)
        val contenderLock = secondClient.getLock(lockName)
        val started = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()

        try {
            val result = executor.submit<Boolean> {
                withRedisRotationLock(ownerLock) {
                    started.countDown()
                    Thread.sleep(1_200)
                    true
                }
            }

            started.await(5, TimeUnit.SECONDS).shouldBeTrue()
            Thread.sleep(600)
            contenderLock.tryLock(100, TimeUnit.MILLISECONDS).shouldBeFalse()
            result.get(5, TimeUnit.SECONDS).shouldBeTrue()

            contenderLock.tryLock(1, TimeUnit.SECONDS).shouldBeTrue()
        } finally {
            if (contenderLock.isHeldByCurrentThread) {
                contenderLock.unlock()
            }
            executor.shutdownNow()
            firstClient.shutdown()
            secondClient.shutdown()
        }
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

    @Test
    fun `expired cached keychain rotates concurrently with single Redis winner`() {
        val queueName = "test:jwt:keychain:${UUID.randomUUID()}"
        val seedRepository = RedisKeyChainRepository(redisson, queueName = queueName)
        val firstNode = RedisKeyChainRepository(redisson, queueName = queueName)
        val secondNode = RedisKeyChainRepository(redisson, queueName = queueName)
        val executor = Executors.newFixedThreadPool(2)

        try {
            val expiredKeyChain = KeyChain(expiredTtl = Duration.ofMillis(1))
            seedRepository.forcedRotate(expiredKeyChain).shouldBeTrue()
            Thread.sleep(10)

            firstNode.current() shouldBeEqualTo expiredKeyChain
            secondNode.current() shouldBeEqualTo expiredKeyChain

            val firstCandidate = KeyChain()
            val secondCandidate = KeyChain()
            val start = CountDownLatch(1)
            val firstFuture = executor.submit<Boolean> {
                start.await()
                firstNode.rotate(firstCandidate)
            }
            val secondFuture = executor.submit<Boolean> {
                start.await()
                secondNode.rotate(secondCandidate)
            }

            start.countDown()
            val firstRotated = firstFuture.get(10, TimeUnit.SECONDS)
            val secondRotated = secondFuture.get(10, TimeUnit.SECONDS)
            listOf(firstRotated, secondRotated).count { it }.shouldBeEqualTo(1)

            val winner = if (firstRotated) firstCandidate else secondCandidate
            val loser = if (firstRotated) secondCandidate else firstCandidate
            firstNode.current() shouldBeEqualTo winner
            secondNode.current() shouldBeEqualTo winner
            seedRepository.findOrNull(winner.id) shouldBeEqualTo winner
            assertNull(seedRepository.findOrNull(loser.id))
        } finally {
            executor.shutdownNow()
            seedRepository.deleteAll()
        }
    }

    private fun createWatchdogClient(): RedissonClient =
        Redisson.create(
            RedisServer.Launcher.RedissonLib.getRedissonConfig().apply {
                setLockWatchdogTimeout(900)
            },
        )

    private class LeaseCappingLock(
        private val delegate: RLock,
        private val cappedLeaseMillis: Long,
    ): RLock by delegate {

        override fun tryLock(waitTime: Long, leaseTime: Long, unit: TimeUnit): Boolean =
            delegate.tryLock(waitTime, cappedLeaseMillis, TimeUnit.MILLISECONDS)
    }
}
