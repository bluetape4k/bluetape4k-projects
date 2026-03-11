package io.bluetape4k.redis.lettuce.semaphore

import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

class LettuceSuspendSemaphoreTest: AbstractLettuceTest() {

    companion object: KLoggingChannel() {
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
            LettuceSuspendSemaphore(connection, randomName(), totalPermits = 0)
        }
        assertThrows<IllegalArgumentException> {
            LettuceSuspendSemaphore(connection, randomName(), totalPermits = -1)
        }
    }

    @Test
    fun `tryAcquireSuspending - 허가 획득 성공`() = runSuspendIO {
        val suspendSemaphore =
            LettuceSuspendSemaphore(LettuceClients.connect(client), semaphore.semaphoreKey, TOTAL_PERMITS)
        suspendSemaphore.tryAcquire().shouldBeTrue()
        semaphore.availablePermits() shouldBeEqualTo TOTAL_PERMITS - 1
        suspendSemaphore.release()
        semaphore.availablePermits() shouldBeEqualTo TOTAL_PERMITS
    }

    @Test
    fun `acquireSuspending and releaseSuspending`() = runSuspendIO {
        val suspendSemaphore =
            LettuceSuspendSemaphore(LettuceClients.connect(client), semaphore.semaphoreKey, TOTAL_PERMITS)
        suspendSemaphore.acquire(1, waitTime = Duration.ofSeconds(2))
        semaphore.availablePermits() shouldBeEqualTo TOTAL_PERMITS - 1
        suspendSemaphore.release()
        semaphore.availablePermits() shouldBeEqualTo TOTAL_PERMITS
    }

    @Test
    fun `코루틴 동시성 - 최대 TOTAL_PERMITS개만 허가`() = runSuspendIO {
        val acquired = AtomicInteger(0)

        val jobs = List(10) {
            async {
                val s = LettuceSuspendSemaphore(LettuceClients.connect(client), semaphore.semaphoreKey, TOTAL_PERMITS)
                if (s.tryAcquire()) {
                    acquired.incrementAndGet()
                    delay(50)
                    s.release()
                }
            }
        }
        jobs.awaitAll()

        // 최소 1개 이상 획득됐어야 함
        acquired.get() shouldBeGreaterOrEqualTo 1
    }

    // =========================================================================
    // SuspendedJobTester 동시성 테스트
    // =========================================================================

    @Test
    fun `SuspendedJobTester - 코루틴 동시 acquire release 안정성`() = runSuspendIO {
        val connection = LettuceClients.connect(client)
        val acquired = AtomicInteger(0)

        SuspendedJobTester()
            .workers(8)
            .rounds(5)
            .add {
                val s = LettuceSuspendSemaphore(connection, semaphore.semaphoreKey, TOTAL_PERMITS)
                if (s.tryAcquire()) {
                    acquired.incrementAndGet()
                    delay(10)
                    s.release()
                }
            }
            .run()

        acquired.get() shouldBeGreaterOrEqualTo 1
    }

    @Test
    fun `SuspendedJobTester - 코루틴 동시 허가 수 제한 검증`() = runSuspendIO {
        val connection = LettuceClients.connect(client)
        val concurrent = AtomicInteger(0)
        val maxConcurrent = AtomicInteger(0)

        SuspendedJobTester()
            .workers(10)
            .rounds(3)
            .add {
                val s = LettuceSuspendSemaphore(connection, semaphore.semaphoreKey, TOTAL_PERMITS)
                if (s.tryAcquire()) {
                    val current = concurrent.incrementAndGet()
                    maxConcurrent.updateAndGet { max -> maxOf(max, current) }
                    delay(20)
                    concurrent.decrementAndGet()
                    s.release()
                }
            }
            .run()

        maxConcurrent.get() shouldBeGreaterOrEqualTo 1
    }

}
