package io.bluetape4k.examples.redisson.coroutines.locks

import io.bluetape4k.coroutines.support.awaitSuspending
import io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.utils.Runtimex
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration


/**
 * Semaphore examples
 *
 * 참고: [Semaphore](https://github.com/redisson/redisson/wiki/8.-distributed-locks-and-synchronizers/#86-semaphore)
 */
class SemaphoreExamples: AbstractRedissonCoroutineTest() {

    companion object: KLoggingChannel()

    @Test
    fun `semaphore example`() = runTest {
        val semaphoreName = randomName()
        val semaphore = redisson.getSemaphore(semaphoreName)

        // 5개 확보
        semaphore.trySetPermitsAsync(5).awaitSuspending().shouldBeTrue()

        // 3개 획득
        semaphore.acquireAsync(3).awaitSuspending()

        val redisson2 = newRedisson()

        val job = scope.launch {
            val s2 = redisson2.getSemaphore(semaphoreName)
            yield()
            // 2개 반납 (4개 남음)
            s2.releaseAsync(2).awaitSuspending()
            yield()
        }

        val redisson3 = newRedisson()
        val job2 = scope.launch {
            val s3 = redisson3.getSemaphore(semaphoreName)
            yield()
            // 4개 확보
            s3.tryAcquireAsync(4, 5.seconds.toJavaDuration()).awaitSuspending().shouldBeTrue()
            yield()
        }
        yield()

        job.join()
        job2.join()

        semaphore.availablePermitsAsync().awaitSuspending() shouldBeEqualTo 0

        // 4개 반납
        semaphore.releaseAsync(4).awaitSuspending()
        semaphore.availablePermitsAsync().awaitSuspending() shouldBeEqualTo 4

        // 여유분을 모두 획득합니다.
        semaphore.drainPermitsAsync().awaitSuspending() shouldBeEqualTo 4
        semaphore.availablePermitsAsync().awaitSuspending() shouldBeEqualTo 0

        semaphore.deleteAsync().awaitSuspending()

        redisson2.shutdown()
        redisson3.shutdown()
    }

    @Test
    fun `semaphore in multi threading`() {
        val semaphoreName = randomName()
        val semaphore = redisson.getSemaphore(semaphoreName)

        // 5개 확보
        semaphore.trySetPermits(5).shouldBeTrue()

        // 3개 획득
        semaphore.acquire(3)

        val redisson2 = newRedisson()
        val redisson3 = newRedisson()
        try {
            MultithreadingTester()
                .workers(8)
                .rounds(4)
                .add {
                    val s2 = redisson2.getSemaphore(semaphoreName)
                    Thread.sleep(1)
                    // 2개 반납 (4개 남음)
                    s2.release(2)
                    Thread.sleep(1)
                }
                .add {
                    val s3 = redisson3.getSemaphore(semaphoreName)
                    Thread.sleep(1)
                    // 4개 확보
                    s3.tryAcquire(2, 5.seconds.toJavaDuration()).shouldBeTrue()
                    Thread.sleep(1)
                }
                .run()

            semaphore.availablePermits() shouldBeEqualTo 2

            // 4개 반납
            semaphore.release(4)
            semaphore.availablePermits() shouldBeEqualTo 6

            // 여유분을 모두 획득합니다.
            semaphore.drainPermits() shouldBeEqualTo 6
            semaphore.availablePermits() shouldBeEqualTo 0
        } finally {
            semaphore.delete()

            redisson2.shutdown()
            redisson3.shutdown()
        }
    }

    @EnabledOnJre(JRE.JAVA_21)
    @Test
    fun `semaphore in virtual threads`() {
        val semaphoreName = randomName()
        val semaphore = redisson.getSemaphore(semaphoreName)

        // 5개 확보
        semaphore.trySetPermits(5).shouldBeTrue()

        // 3개 획득
        semaphore.acquire(3)

        val redisson2 = newRedisson()
        val redisson3 = newRedisson()

        try {
            StructuredTaskScopeTester()
                .rounds(16)
                .add {
                    val s2 = redisson2.getSemaphore(semaphoreName)
                    Thread.sleep(1)
                    // 2개 반납 (4개 남음)
                    s2.release(2)
                    Thread.sleep(1)
                }
                .add {
                    val s3 = redisson3.getSemaphore(semaphoreName)
                    Thread.sleep(1)
                    // 4개 확보
                    s3.tryAcquire(2, 5.seconds.toJavaDuration()).shouldBeTrue()
                    Thread.sleep(1)
                }
                .run()

            semaphore.availablePermits() shouldBeEqualTo 2

            // 4개 반납
            semaphore.release(4)
            semaphore.availablePermits() shouldBeEqualTo 6

            // 여유분을 모두 획득합니다.
            semaphore.drainPermits() shouldBeEqualTo 6
            semaphore.availablePermits() shouldBeEqualTo 0
        } finally {
            semaphore.delete()

            redisson2.shutdown()
            redisson3.shutdown()
        }
    }

    @Test
    fun `semaphore in coroutines`() = runSuspendIO {
        val semaphoreName = randomName()
        val semaphore = redisson.getSemaphore(semaphoreName)

        // 5개 확보
        semaphore.trySetPermits(5).shouldBeTrue()

        // 3개 획득
        semaphore.acquire(3)

        val redisson2 = newRedisson()
        val redisson3 = newRedisson()

        try {
            SuspendedJobTester()
                .workers(Runtimex.availableProcessors)
                .rounds(16)
                .add {
                    val s2 = redisson2.getSemaphore(semaphoreName)
                    Thread.sleep(1)
                    // 2개 반납 (4개 남음)
                    s2.release(2)
                    Thread.sleep(1)
                }
                .add {
                    val s3 = redisson3.getSemaphore(semaphoreName)
                    Thread.sleep(1)
                    // 4개 확보
                    s3.tryAcquire(2, 5.seconds.toJavaDuration()).shouldBeTrue()
                    Thread.sleep(1)
                }
                .run()

            semaphore.availablePermits() shouldBeEqualTo 2

            // 4개 반납
            semaphore.release(4)
            semaphore.availablePermits() shouldBeEqualTo 6

            // 여유분을 모두 획득합니다.
            semaphore.drainPermits() shouldBeEqualTo 6
            semaphore.availablePermits() shouldBeEqualTo 0
        } finally {
            semaphore.delete()

            redisson2.shutdown()
            redisson3.shutdown()
        }
    }
}
