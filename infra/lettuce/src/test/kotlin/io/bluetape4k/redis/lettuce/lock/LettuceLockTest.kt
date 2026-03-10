package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class LettuceLockTest: AbstractLettuceTest() {

    companion object: KLoggingChannel()

    private lateinit var lock: LettuceLock

    @BeforeEach
    fun setup() {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        lock = LettuceLock(connection, randomName(), defaultLeaseTime = Duration.ofSeconds(10))
    }

    // =========================================================================
    // 동기 테스트
    // =========================================================================

    @Test
    fun `tryLock - 락 획득 성공`() {
        lock.tryLock().shouldBeTrue()
        lock.isHeldByCurrentInstance().shouldBeTrue()
        lock.isLocked().shouldBeTrue()
        lock.unlock()
    }

    @Test
    fun `tryLock - 이미 잠긴 경우 즉시 false 반환`() {
        lock.tryLock().shouldBeTrue()
        try {
            // 같은 키를 가진 다른 락 인스턴스는 획득 실패
            val lock2 = LettuceLock(LettuceClients.connect(client, StringCodec.UTF8), lock.lockKey)
            lock2.tryLock().shouldBeFalse()
        } finally {
            lock.unlock()
        }
    }

    @Test
    fun `unlock - 락을 보유하지 않으면 예외`() {
        assertThrows<IllegalStateException> {
            lock.unlock()
        }
    }

    @Test
    fun `lock and unlock - 순차 실행`() {
        repeat(3) {
            lock.lock(leaseTime = Duration.ofSeconds(5))
            lock.isHeldByCurrentInstance().shouldBeTrue()
            lock.unlock()
            lock.isHeldByCurrentInstance().shouldBeFalse()
        }
    }

    @Test
    fun `동시성 - 여러 스레드에서 하나만 락 획득`() {
        val threadCount = 5
        val acquiredCount = AtomicInteger(0)
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)

        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        repeat(threadCount) {
            executor.submit {
                val threadLock = LettuceLock(connection, lock.lockKey, Duration.ofSeconds(5))
                if (threadLock.tryLock(waitTime = Duration.ofMillis(100))) {
                    acquiredCount.incrementAndGet()
                    Thread.sleep(100)
                    threadLock.unlock()
                }
                latch.countDown()
            }
        }

        latch.await(5, TimeUnit.SECONDS)
        executor.shutdown()
        // 동시에 하나만 획득할 수 있어야 함
        acquiredCount.get() shouldBeEqualTo 1
    }

    // =========================================================================
    // 비동기 테스트
    // =========================================================================

    @Test
    fun `tryLockAsync - 락 획득 성공`() {
        val acquired = lock.tryLockAsync().get()
        acquired.shouldBeTrue()
        lock.isLocked().shouldBeTrue()
        lock.unlockAsync().get()
    }

    @Test
    fun `tryLockAsync - 이미 잠긴 경우 false`() {
        lock.tryLockAsync().get().shouldBeTrue()
        try {
            val lock2 = LettuceLock(LettuceClients.connect(client, StringCodec.UTF8), lock.lockKey)
            lock2.tryLockAsync().get().shouldBeFalse()
        } finally {
            lock.unlockAsync().get()
        }
    }

    // =========================================================================
    // 코루틴 테스트
    // =========================================================================

    @Test
    fun `tryLock (suspend) - 락 획득 성공`() = runSuspendIO {
        val suspendLock = LettuceSuspendLock(LettuceClients.connect(client, StringCodec.UTF8), lock.lockKey)
        suspendLock.tryLock().shouldBeTrue()
        suspendLock.isHeldByCurrentInstance().shouldBeTrue()
        suspendLock.unlock()
        suspendLock.isHeldByCurrentInstance().shouldBeFalse()
    }

    @Test
    fun `lock and unlock (suspend)`() = runSuspendIO {
        val suspendLock = LettuceSuspendLock(LettuceClients.connect(client, StringCodec.UTF8), lock.lockKey)
        suspendLock.lock(leaseTime = Duration.ofSeconds(5))
        suspendLock.isHeldByCurrentInstance().shouldBeTrue()
        suspendLock.unlock()
        suspendLock.isHeldByCurrentInstance().shouldBeFalse()
    }

    @Test
    fun `코루틴 동시성 - 여러 코루틴에서 하나만 락 획득`() = runSuspendIO {
        val acquiredCount = AtomicInteger(0)
        val connection = LettuceClients.connect(client, StringCodec.UTF8)

        val jobs = List(5) {
            async {
                val coLock = LettuceSuspendLock(connection, lock.lockKey, Duration.ofSeconds(5))
                if (coLock.tryLock(waitTime = Duration.ofMillis(100))) {
                    acquiredCount.incrementAndGet()
                    kotlinx.coroutines.delay(100)
                    coLock.unlock()
                }
            }
        }
        jobs.awaitAll()

        acquiredCount.get() shouldBeEqualTo 1
    }
}
