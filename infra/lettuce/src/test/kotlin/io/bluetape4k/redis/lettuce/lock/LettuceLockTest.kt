package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.codec.StringCodec
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class LettuceLockTest: AbstractLettuceTest() {

    companion object: KLogging() {
        private val connection by lazy { LettuceClients.connect(LettuceTestUtils.client, StringCodec.UTF8) }
    }

    private lateinit var lock: LettuceLock

    @BeforeEach
    fun setup() {
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
            val lock2 = LettuceLock(connection, lock.lockKey)
            lock2.tryLock().shouldBeFalse()
        } finally {
            lock.unlock()
        }
    }

    @Test
    fun `unlock - 락을 보유하지 않으면 예외`() {
        assertFailsWith<IllegalStateException> {
            lock.unlock()
        }
    }

    @Test
    fun `tryLock - 잘못된 duration은 즉시 거부한다`() {
        assertFailsWith<IllegalArgumentException> {
            lock.tryLock(waitTime = Duration.ofMillis(-1))
        }
        assertFailsWith<IllegalArgumentException> {
            lock.tryLock(leaseTime = Duration.ZERO)
        }
        assertFailsWith<IllegalArgumentException> {
            lock.tryLock(leaseTime = Duration.ofNanos(1))
        }
    }

    @Test
    fun `lock - 잘못된 duration은 즉시 거부한다`() {
        assertFailsWith<IllegalArgumentException> {
            lock.lock(leaseTime = Duration.ZERO)
        }
        assertFailsWith<IllegalArgumentException> {
            lock.lock(maxWaitTime = Duration.ZERO)
        }
    }

    @Test
    fun `unlock - 만료된 lock 해제 실패 시 token을 보존해 재시도할 수 있다`() {
        lock.lock(leaseTime = Duration.ofSeconds(5))
        val token = connection.sync().get(lock.lockKey)
        connection.sync().del(lock.lockKey)

        assertFailsWith<IllegalStateException> {
            lock.unlock()
        }

        connection.sync().set(lock.lockKey, token)
        lock.unlock()
        lock.isHeldByCurrentInstance().shouldBeFalse()
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
        val acquiredCount = AtomicInteger(0)

        MultithreadingTester()
            .workers(5)
            .rounds(1)
            .add {
                val threadLock = LettuceLock(connection, lock.lockKey, Duration.ofSeconds(5))
                if (threadLock.tryLock(waitTime = Duration.ofMillis(100))) {
                    acquiredCount.incrementAndGet()
                    Thread.sleep(100)
                    threadLock.unlock()
                }
            }
            .run()

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
            val lock2 = LettuceLock(connection, lock.lockKey)
            lock2.tryLockAsync().get().shouldBeFalse()
        } finally {
            lock.unlockAsync().get()
        }
    }

    @Test
    fun `tryLockAsync - 잘못된 duration은 즉시 거부한다`() {
        assertFailsWith<IllegalArgumentException> {
            lock.tryLockAsync(waitTime = Duration.ofMillis(-1))
        }
        assertFailsWith<IllegalArgumentException> {
            lock.tryLockAsync(leaseTime = Duration.ZERO)
        }
        assertFailsWith<IllegalArgumentException> {
            lock.lockAsync(maxWaitTime = Duration.ZERO)
        }
    }

    @Test
    fun `unlockAsync - 만료된 lock 해제 실패 시 token을 보존해 재시도할 수 있다`() {
        lock.lockAsync(leaseTime = Duration.ofSeconds(5)).get()
        val token = connection.sync().get(lock.lockKey)
        connection.sync().del(lock.lockKey)

        val failure = assertFailsWith<ExecutionException> {
            lock.unlockAsync().get()
        }
        failure.cause.shouldBeInstanceOf<IllegalStateException>()

        connection.sync().set(lock.lockKey, token)
        lock.unlockAsync().get()
        lock.isHeldByCurrentInstance().shouldBeFalse()
    }

    // =========================================================================
    // MultithreadingTester 동시성 테스트
    // =========================================================================

    @Test
    fun `MultithreadingTester - 동시 락 상호 배제 검증`() {
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
