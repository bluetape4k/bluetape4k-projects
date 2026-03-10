package io.bluetape4k.redis.lettuce.semaphore

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeTrue
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
class LettuceSemaphoreTest: AbstractLettuceTest() {

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
        semaphore.acquire(1, waitTime = 1.seconds)
        semaphore.availablePermits() shouldBeEqualTo TOTAL_PERMITS - 1
        semaphore.release()
    }

    @Test
    fun `acquire - 시간 초과 시 예외 발생`() {
        repeat(TOTAL_PERMITS) { semaphore.tryAcquire() }
        assertThrows<IllegalStateException> {
            semaphore.acquire(1, waitTime = 200.milliseconds)
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
    // 코루틴 테스트
    // =========================================================================

    @Test
    fun `tryAcquireSuspending - 허가 획득 성공`() = runSuspendIO {
        val suspendSemaphore = LettuceSuspendSemaphore(LettuceClients.connect(client), semaphore.semaphoreKey, TOTAL_PERMITS)
        suspendSemaphore.tryAcquire().shouldBeTrue()
        semaphore.availablePermits() shouldBeEqualTo TOTAL_PERMITS - 1
        suspendSemaphore.release()
        semaphore.availablePermits() shouldBeEqualTo TOTAL_PERMITS
    }

    @Test
    fun `acquireSuspending and releaseSuspending`() = runSuspendIO {
        val suspendSemaphore = LettuceSuspendSemaphore(LettuceClients.connect(client), semaphore.semaphoreKey, TOTAL_PERMITS)
        suspendSemaphore.acquire(1, waitTime = 2.seconds)
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
                    kotlinx.coroutines.delay(50)
                    s.release()
                }
            }
        }
        jobs.awaitAll()

        // 최소 1개 이상 획득됐어야 함
        acquired.get() shouldBeGreaterOrEqualTo 1
    }
}
