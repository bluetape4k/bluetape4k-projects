package io.bluetape4k.jwt.keychain.redis

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.concurrent.await
import io.bluetape4k.concurrent.get
import io.bluetape4k.concurrent.tryLock
import io.bluetape4k.jwt.keychain.AbstractKeyChainRepositoryTest
import io.bluetape4k.jwt.keychain.KeyChain
import io.bluetape4k.jwt.keychain.KeyChainDto
import io.bluetape4k.jwt.keychain.repository.KeyChainRepository
import io.bluetape4k.jwt.keychain.repository.redis.REDIS_ROTATION_LOCK_WAIT_SECONDS
import io.bluetape4k.jwt.keychain.repository.redis.RedisKeyChainRepository
import io.bluetape4k.jwt.keychain.repository.redis.withRedisRotationLock
import io.bluetape4k.testcontainers.storage.RedisServer
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.awaitility.kotlin.await
import org.awaitility.kotlin.during
import org.awaitility.kotlin.until
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.redisson.Redisson
import org.redisson.api.RDeque
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class RedisKeyChainRepositoryTest: AbstractKeyChainRepositoryTest() {

    private val redisson by lazy {
        RedisServer.Launcher.RedissonLib.getRedisson()
    }

    override val repository: KeyChainRepository by lazy {
        RedisKeyChainRepository(redisson)
    }

    private val lock = mockk<RLock>(relaxed = true)

    @BeforeEach
    override fun beforeEach() {
        super.beforeEach()
        clearMocks(lock)
    }

    @Test
    fun `rotation uses watchdog-backed lock acquisition`() {
        val queue = mockk<RDeque<KeyChainDto>>(relaxed = true)
        val redisson = mockk<RedissonClient>(relaxed = true)

        every { redisson.getDeque<KeyChainDto>(any<String>()) } returns queue
        every { redisson.getLock(any<String>()) } returns lock
        every { lock.tryLock(REDIS_ROTATION_LOCK_WAIT_SECONDS.seconds) } returns true
        every { lock.isHeldByCurrentThread } returns true

        val repository = RedisKeyChainRepository(redisson)
        try {
            repository.forcedRotate(KeyChain()).shouldBeTrue()

            verify(exactly = 1) { lock.tryLock(REDIS_ROTATION_LOCK_WAIT_SECONDS.seconds) }
        } finally {
            repository.close()
        }
    }

    @Test
    fun `rotation reports ownership loss before commit`() {
        every { lock.tryLock(REDIS_ROTATION_LOCK_WAIT_SECONDS.seconds) } returns true
        every { lock.isHeldByCurrentThread } returns false

        val failure = assertFailsWith<IllegalStateException> {
            withRedisRotationLock(lock) { }
        }

        failure.message shouldContain "ownership was lost"
    }

    @Test
    fun `rotation keeps primary failure when unlock fails`() {
        val lock = mockk<RLock>()
        val primaryFailure = IllegalStateException("commit failed")
        val unlockFailure = IllegalStateException("unlock failed")
        every { lock.tryLock(REDIS_ROTATION_LOCK_WAIT_SECONDS.seconds) } returns true
        every { lock.isHeldByCurrentThread } returns true
        every { lock.unlock() } throws unlockFailure

        val failure = assertFailsWith<IllegalStateException> {
            withRedisRotationLock(lock) { throw primaryFailure }
        }

        failure shouldBeSameInstanceAs primaryFailure
        failure.suppressed.single() shouldBeSameInstanceAs unlockFailure
    }

    @Test
    fun `watchdog keeps rotation lock until a long commit completes`() {
        val firstClient = createWatchdogClient()
        val secondClient = createWatchdogClient()
        val lockName = "test:jwt:keychain:watchdog:${UUID.randomUUID()}"
        val ownerLock = firstClient.getLock(lockName)
        val contenderLock = secondClient.getLock(lockName)
        val started = CountDownLatch(1)
        val releaseCommit = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()

        try {
            val result = executor.submit<Boolean> {
                withRedisRotationLock(ownerLock) {
                    started.countDown()
                    releaseCommit.await(5.seconds).shouldBeTrue()
                    true
                }
            }

            started.await(5.seconds).shouldBeTrue()
            // watchdog timeout(900ms)을 넘는 동안 contender가 lock을 얻지 못해야 갱신이 증명됩니다.
            await during 1_200.milliseconds until {
                val acquired = contenderLock.tryLock(100.milliseconds)
                if (acquired) {
                    contenderLock.unlock()
                }
                !acquired
            }
            releaseCommit.countDown()
            result.get(5.seconds).shouldBeTrue()

            contenderLock.tryLock(1.seconds).shouldBeTrue()
        } finally {
            if (contenderLock.isHeldByCurrentThread) {
                contenderLock.unlock()
            }
            releaseCommit.countDown()
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
            val expiredKeyChain = alreadyExpiredKeyChain()
            seedRepository.forcedRotate(expiredKeyChain).shouldBeTrue()

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
            seedRepository.findOrNull(loser.id).shouldBeNull()
        } finally {
            try {
                seedRepository.deleteAll()
            } finally {
                firstNode.close()
                secondNode.close()
                seedRepository.close()
            }
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
            val expiredKeyChain = alreadyExpiredKeyChain()
            seedRepository.forcedRotate(expiredKeyChain).shouldBeTrue()

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
            val firstRotated = firstFuture.get(10.seconds)
            val secondRotated = secondFuture.get(10.seconds)
            listOf(firstRotated, secondRotated).count { it }.shouldBeEqualTo(1)

            val winner = if (firstRotated) firstCandidate else secondCandidate
            val loser = if (firstRotated) secondCandidate else firstCandidate
            firstNode.current() shouldBeEqualTo winner
            secondNode.current() shouldBeEqualTo winner
            seedRepository.findOrNull(winner.id) shouldBeEqualTo winner
            seedRepository.findOrNull(loser.id).shouldBeNull()
        } finally {
            executor.shutdownNow()
            try {
                seedRepository.deleteAll()
            } finally {
                firstNode.close()
                secondNode.close()
                seedRepository.close()
            }
        }
    }

    private fun createWatchdogClient(): RedissonClient =
        Redisson.create(
            RedisServer.Launcher.RedissonLib.getRedissonConfig().apply {
                setLockWatchdogTimeout(900)
            },
        )
}
