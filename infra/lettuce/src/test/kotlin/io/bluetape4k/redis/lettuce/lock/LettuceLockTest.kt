package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.codec.StringCodec
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterOrEqualTo
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

    companion object: KLogging()

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
    // MultithreadingTester 동시성 테스트
    // =========================================================================

    @Test
    fun `MultithreadingTester - 동시 락 상호 배제 검증`() {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        val concurrent = AtomicInteger(0)
        val maxConcurrent = AtomicInteger(0)
        val acquired = AtomicInteger(0)

        MultithreadingTester()
            .workers(8)
            .rounds(3)
            .add {
                val l = LettuceLock(connection, lock.lockKey, Duration.ofSeconds(10))
                if (l.tryLock(waitTime = Duration.ofSeconds(5))) {
                    val current = concurrent.incrementAndGet()
                    maxConcurrent.updateAndGet { max -> maxOf(max, current) }
                    Thread.sleep(10)
                    concurrent.decrementAndGet()
                    acquired.incrementAndGet()
                    l.unlock()
                }
            }
            .run()

        maxConcurrent.get() shouldBeEqualTo 1
        acquired.get() shouldBeGreaterOrEqualTo 1
    }

    @Test
    fun `MultithreadingTester - 락 획득 후 정상 해제 검증`() {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        val released = AtomicInteger(0)

        MultithreadingTester()
            .workers(4)
            .rounds(5)
            .add {
                val l = LettuceLock(connection, lock.lockKey, Duration.ofSeconds(10))
                if (l.tryLock(waitTime = Duration.ofSeconds(3))) {
                    Thread.sleep(5)
                    l.unlock()
                    released.incrementAndGet()
                }
            }
            .run()

        released.get() shouldBeGreaterOrEqualTo 1
    }

    // =========================================================================
    // StructuredTaskScopeTester 동시성 테스트
    // =========================================================================

    @Test
    fun `StructuredTaskScopeTester - 동시 락 상호 배제 검증`() {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        val concurrent = AtomicInteger(0)
        val maxConcurrent = AtomicInteger(0)

        StructuredTaskScopeTester()
            .rounds(10)
            .add {
                val l = LettuceLock(connection, lock.lockKey, Duration.ofSeconds(10))
                if (l.tryLock(waitTime = Duration.ofSeconds(5))) {
                    val current = concurrent.incrementAndGet()
                    maxConcurrent.updateAndGet { max -> maxOf(max, current) }
                    Thread.sleep(10)
                    concurrent.decrementAndGet()
                    l.unlock()
                }
            }
            .run()

        maxConcurrent.get() shouldBeEqualTo 1
    }
}
