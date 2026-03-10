package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class RedisLockTest: AbstractLettuceTest() {

    companion object: KLoggingChannel()

    private lateinit var lock: RedisLock

    @BeforeEach
    fun setup() {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        lock = RedisLock(connection, randomName(), defaultLeaseTime = 10.seconds)
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
            val lock2 = RedisLock(LettuceClients.connect(client, StringCodec.UTF8), lock.lockKey)
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
            lock.lock(leaseTime = 5.seconds)
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
                val threadLock = RedisLock(connection, lock.lockKey, 5.seconds)
                if (threadLock.tryLock(waitTime = 100.milliseconds)) {
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
            val lock2 = RedisLock(LettuceClients.connect(client, StringCodec.UTF8), lock.lockKey)
            lock2.tryLockAsync().get().shouldBeFalse()
        } finally {
            lock.unlockAsync().get()
        }
    }

    // =========================================================================
    // 코루틴 테스트
    // =========================================================================

    @Test
    fun `tryLockSuspending - 락 획득 성공`() = runSuspendIO {
        lock.tryLockSuspending().shouldBeTrue()
        lock.isHeldByCurrentInstance().shouldBeTrue()
        lock.unlockSuspending()
        lock.isHeldByCurrentInstance().shouldBeFalse()
    }

    @Test
    fun `lockSuspending and unlockSuspending`() = runSuspendIO {
        lock.lockSuspending(leaseTime = 5.seconds)
        lock.isHeldByCurrentInstance().shouldBeTrue()
        lock.unlockSuspending()
        lock.isHeldByCurrentInstance().shouldBeFalse()
    }

    @Test
    fun `코루틴 동시성 - 여러 코루틴에서 하나만 락 획득`() = runSuspendIO {
        val acquiredCount = AtomicInteger(0)
        val connection = LettuceClients.connect(client, StringCodec.UTF8)

        val jobs = List(5) {
            async {
                val coLock = RedisLock(connection, lock.lockKey, 5.seconds)
                if (coLock.tryLockSuspending(waitTime = 100.milliseconds)) {
                    acquiredCount.incrementAndGet()
                    kotlinx.coroutines.delay(100)
                    coLock.unlockSuspending()
                }
            }
        }
        jobs.awaitAll()

        acquiredCount.get() shouldBeEqualTo 1
    }
}
