package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.codec.StringCodec
import io.bluetape4k.assertions.assertFailsWith
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class LettuceSuspendLockTest: AbstractLettuceTest() {

    companion object: KLoggingChannel() {
        private val connection by lazy { LettuceClients.connect(LettuceTestUtils.client, StringCodec.UTF8) }
    }

    private lateinit var lockKey: String

    @BeforeEach
    fun setup() {
        lockKey = randomName()
    }

    private fun suspendLock(leaseTime: Duration = Duration.ofSeconds(10)): LettuceSuspendLock =
        LettuceSuspendLock(connection, lockKey, leaseTime)

    // =========================================================================
    // 코루틴 기본 테스트
    // =========================================================================

    @Test
    fun `tryLock (suspend) - 락 획득 성공`() = runSuspendIO {
        val lock = suspendLock()
        lock.tryLock().shouldBeTrue()
        lock.isHeldByCurrentInstance().shouldBeTrue()
        lock.unlock()
        lock.isHeldByCurrentInstance()
    }

    @Test
    fun `lock and unlock (suspend)`() = runSuspendIO {
        val lock = suspendLock()
        lock.lock(leaseTime = Duration.ofSeconds(5))
        lock.isHeldByCurrentInstance().shouldBeTrue()
        lock.unlock()
        lock.isHeldByCurrentInstance()
    }

    @Test
    fun `tryLock and lock - 잘못된 duration은 즉시 거부한다`() = runSuspendIO {
        val lock = suspendLock()

        assertFailsWith<IllegalArgumentException> {
            lock.tryLock(waitTime = Duration.ofMillis(-1))
        }
        assertFailsWith<IllegalArgumentException> {
            lock.tryLock(leaseTime = Duration.ZERO)
        }
        assertFailsWith<IllegalArgumentException> {
            lock.tryLock(leaseTime = Duration.ofNanos(1))
        }
        assertFailsWith<IllegalArgumentException> {
            lock.lock(maxWaitTime = Duration.ZERO)
        }
    }

    @Test
    fun `unlock - 만료된 lock 해제 실패 시 token을 보존해 재시도할 수 있다`() = runSuspendIO {
        val lock = suspendLock()
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
    fun `코루틴 동시성 - 여러 코루틴에서 하나만 락 획득`() = runSuspendIO {
        val acquiredCount = AtomicInteger(0)

        val jobs = List(5) {
            async {
                val lock = LettuceSuspendLock(connection, lockKey, Duration.ofSeconds(5))
                if (lock.tryLock(waitTime = Duration.ofMillis(100))) {
                    acquiredCount.incrementAndGet()
                    delay(100.milliseconds)
                    lock.unlock()
                }
            }
        }
        jobs.awaitAll()

        acquiredCount.get() shouldBeEqualTo 1
    }

    // =========================================================================
    // SuspendedJobTester 동시성 테스트
    // =========================================================================

    @Test
    fun `SuspendedJobTester - 코루틴 동시 락 상호 배제 검증`() = runSuspendIO {
        val concurrent = AtomicInteger(0)
        val maxConcurrent = AtomicInteger(0)
        val acquired = AtomicInteger(0)

        SuspendedJobTester()
            .workers(8)
            .rounds(3)
            .add {
                val l = LettuceSuspendLock(connection, lockKey, Duration.ofSeconds(10))
                if (l.tryLock(waitTime = Duration.ofSeconds(5))) {
                    val current = concurrent.incrementAndGet()
                    maxConcurrent.updateAndGet { max -> maxOf(max, current) }
                    delay(10.milliseconds)
                    concurrent.decrementAndGet()
                    acquired.incrementAndGet()
                    l.unlock()
                }
            }
            .run()

        maxConcurrent.get() shouldBeEqualTo 1
        acquired.get() shouldBeGreaterOrEqualTo 1
    }

}
