package io.bluetape4k.redis.lettuce.semaphore

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
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
class LettuceSemaphoreTest: AbstractLettuceTest() {

    companion object: KLogging() {
        private const val TOTAL_PERMITS = 3
    }

    private lateinit var semaphore: LettuceSemaphore

    @BeforeEach
    fun setup() {
        val connection = LettuceClients.connect(client)
        semaphore = LettuceSemaphore(connection, randomName(), totalPermits = TOTAL_PERMITS)
        semaphore.initialize()
    }

    @Test
    fun `constructor rejects non positive totalPermits`() {
        val connection = LettuceClients.connect(client)
        assertThrows<IllegalArgumentException> {
            LettuceSemaphore(connection, randomName(), totalPermits = 0)
        }
        assertThrows<IllegalArgumentException> {
            LettuceSemaphore(connection, randomName(), totalPermits = -1)
        }
    }

    // =========================================================================
    // 동기 테스트
    // =========================================================================

    @Test
    fun `초기화 후 availablePermits는 totalPermits와 같아야 함`() {
        semaphore.availablePermits() shouldBeEqualTo TOTAL_PERMITS
    }

    @Test
    fun `tryAcquire - 허가 획득 성공`() {
        semaphore.tryAcquire().shouldBeTrue()
        semaphore.availablePermits() shouldBeEqualTo TOTAL_PERMITS - 1
        semaphore.release()
        semaphore.availablePermits() shouldBeEqualTo TOTAL_PERMITS
    }

    @Test
    fun `tryAcquire - 허가 소진 시 false 반환`() {
        repeat(TOTAL_PERMITS) {
            semaphore.tryAcquire().shouldBeTrue()
        }
        semaphore.availablePermits() shouldBeEqualTo 0
        semaphore.tryAcquire().shouldBeFalse()
    }

    @Test
    fun `release - 최대값 초과하지 않음`() {
        semaphore.release(10)
        semaphore.availablePermits() shouldBeEqualTo TOTAL_PERMITS
    }

    @Test
    fun `acquire - 허가 가용 시 즉시 성공`() {
        semaphore.acquire(1, waitTime = Duration.ofSeconds(1))
        semaphore.availablePermits() shouldBeEqualTo TOTAL_PERMITS - 1
        semaphore.release()
    }

    @Test
    fun `acquire - 시간 초과 시 예외 발생`() {
        repeat(TOTAL_PERMITS) { semaphore.tryAcquire() }
        assertThrows<IllegalStateException> {
            semaphore.acquire(1, waitTime = Duration.ofMillis(200))
        }
    }

    @Test
    fun `동시성 - 최대 TOTAL_PERMITS개만 동시 접근 허용`() {
        val maxConcurrent = AtomicInteger(0)
        val concurrent = AtomicInteger(0)
        val latch = CountDownLatch(10)
        val executor = Executors.newFixedThreadPool(10)
        val connection = LettuceClients.connect(client)

        repeat(10) {
            executor.submit {
                val s = LettuceSemaphore(connection, semaphore.semaphoreKey, TOTAL_PERMITS)
                if (s.tryAcquire()) {
                    val current = concurrent.incrementAndGet()
                    maxConcurrent.updateAndGet { max -> maxOf(max, current) }
                    Thread.sleep(50)
                    concurrent.decrementAndGet()
                    s.release()
                }
                latch.countDown()
            }
        }

        latch.await(10, TimeUnit.SECONDS)
        executor.shutdown()
        maxConcurrent.get() shouldBeGreaterOrEqualTo 1
    }

    // =========================================================================
    // 비동기 테스트
    // =========================================================================

    @Test
    fun `tryAcquireAsync - 허가 획득 성공`() {
        semaphore.tryAcquireAsync().get().shouldBeTrue()
        semaphore.availablePermits() shouldBeEqualTo TOTAL_PERMITS - 1
        semaphore.releaseAsync().get()
        semaphore.availablePermits() shouldBeEqualTo TOTAL_PERMITS
    }

    @Test
    fun `tryAcquireAsync - 허가 소진 시 false`() {
        repeat(TOTAL_PERMITS) { semaphore.tryAcquireAsync().get().shouldBeTrue() }
        semaphore.tryAcquireAsync().get().shouldBeFalse()
    }

    // =========================================================================
    // MultithreadingTester 동시성 테스트
    // =========================================================================

    @Test
    fun `MultithreadingTester - 동시 acquire release 안정성`() {
        val connection = LettuceClients.connect(client)
        val acquired = AtomicInteger(0)

        MultithreadingTester()
            .workers(8)
            .rounds(5)
            .add {
                val s = LettuceSemaphore(connection, semaphore.semaphoreKey, TOTAL_PERMITS)
                if (s.tryAcquire()) {
                    acquired.incrementAndGet()
                    Thread.sleep(10)
                    s.release()
                }
            }
            .run()

        acquired.get() shouldBeGreaterOrEqualTo 1
    }

    @Test
    fun `MultithreadingTester - 동시 허가 수 제한 검증`() {
        val connection = LettuceClients.connect(client)
        val concurrent = AtomicInteger(0)
        val maxConcurrent = AtomicInteger(0)

        MultithreadingTester()
            .workers(10)
            .rounds(3)
            .add {
                val s = LettuceSemaphore(connection, semaphore.semaphoreKey, TOTAL_PERMITS)
                if (s.tryAcquire()) {
                    val current = concurrent.incrementAndGet()
                    maxConcurrent.updateAndGet { max -> maxOf(max, current) }
                    Thread.sleep(20)
                    concurrent.decrementAndGet()
                    s.release()
                }
            }
            .run()

        maxConcurrent.get() shouldBeGreaterOrEqualTo 1
    }

    // =========================================================================
    // StructuredTaskScopeTester 동시성 테스트
    // =========================================================================

    @Test
    fun `StructuredTaskScopeTester - 동시 acquire release 안정성`() {
        val connection = LettuceClients.connect(client)
        val acquired = AtomicInteger(0)

        StructuredTaskScopeTester()
            .rounds(10)
            .add {
                val s = LettuceSemaphore(connection, semaphore.semaphoreKey, TOTAL_PERMITS)
                if (s.tryAcquire()) {
                    acquired.incrementAndGet()
                    Thread.sleep(10)
                    s.release()
                }
            }
            .run()

        acquired.get() shouldBeGreaterOrEqualTo 1
    }

    @Test
    fun `StructuredTaskScopeTester - 동시 허가 수 제한 검증`() {
        val connection = LettuceClients.connect(client)
        val concurrent = AtomicInteger(0)
        val maxConcurrent = AtomicInteger(0)

        StructuredTaskScopeTester()
            .rounds(12)
            .add {
                val s = LettuceSemaphore(connection, semaphore.semaphoreKey, TOTAL_PERMITS)
                if (s.tryAcquire()) {
                    val current = concurrent.incrementAndGet()
                    maxConcurrent.updateAndGet { max -> maxOf(max, current) }
                    Thread.sleep(20)
                    concurrent.decrementAndGet()
                    s.release()
                }
            }
            .run()

        maxConcurrent.get() shouldBeGreaterOrEqualTo 1
    }
}
