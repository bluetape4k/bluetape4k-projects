package io.bluetape4k.coroutines.support

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class JobSupportTest {

    companion object: KLoggingChannel()

    @Test
    fun `print job hierarchy`() = runSuspendIO {
        val job = launch {
            val childJobs = List(10) {
                launch {
                    delay(100.milliseconds)
                    log.debug { "Child job $it" }
                }
            }
            childJobs.joinAll()
        }

        job.printDebugTree()
    }

    @Test
    fun `jobs joinAny`() = runTest {
        val job1 = launch { delay(1000.milliseconds); log.debug { "Job 1" } }
        val job2 = launch { delay(2000.milliseconds); log.debug { "Job 2" } }
        val job3 = launch { delay(3000.milliseconds); log.debug { "Job 3" } }

        // 처음 완료된 값이 나오면 끝낸다.
        joinAny(job1, job2, job3)

        job1.isCompleted.shouldBeTrue()
        job2.isCompleted.shouldBeFalse()
        job3.isCompleted.shouldBeFalse()
    }

    @Test
    fun `collection of job joinAny`() = runTest {
        val job1 = launch { delay(1000.milliseconds); log.debug { "Job 1" } }
        val job2 = launch { delay(2000.milliseconds); log.debug { "Job 2" } }
        val job3 = launch { delay(3000.milliseconds); log.debug { "Job 3" } }

        val jobs = listOf(job1, job2, job3)

        // 처음 완료된 값이 나오면 끝낸다.
        jobs.joinAny()

        job1.isCompleted.shouldBeTrue()
        job2.isCompleted.shouldBeFalse()
        job3.isCompleted.shouldBeFalse()
    }

    @Test
    fun `collection of job joinAny and cancel others`() = runTest {
        val job1 = launch { delay(1000.milliseconds); log.debug { "Job 1" } }
        val job2 = launch { delay(2000.milliseconds); log.debug { "Job 2" } }
        val job3 = launch { delay(3000.milliseconds); log.debug { "Job 3" } }

        val jobs = listOf(job1, job2, job3)

        // 처음 완료된 값이 나오면 끝낸다.
        jobs.joinAnyAndCancelOthers()

        yield()

        job1.isCompleted.shouldBeTrue()
        job2.isCancelled.shouldBeTrue()
        job3.isCancelled.shouldBeTrue()
    }

    @Test
    fun `joinUntil returns for successful failed and cancelled jobs`() = runTest {
        val ownerJob = SupervisorJob()
        val owner = CoroutineScope(ownerJob + StandardTestDispatcher(testScheduler))
        try {
            val succeeded = owner.launch { }
            succeeded.joinUntil(1.seconds)
            succeeded.isCompleted.shouldBeTrue()

            val failure = IllegalStateException("target failed")
            val failed = Job(ownerJob).apply { completeExceptionally(failure) }
            failed.joinUntil(1.seconds)
            failed.isCancelled.shouldBeTrue()

            val cancelled = Job(ownerJob).apply { cancel(CancellationException("target cancelled")) }
            cancelled.joinUntil(1.seconds)
            cancelled.isCancelled.shouldBeTrue()
        } finally {
            ownerJob.cancelAndJoin()
        }
    }

    @Test
    fun `joinUntil timeout and caller cancellation do not cancel target jobs`() = runTest {
        val ownerJob = SupervisorJob()
        val owner = CoroutineScope(ownerJob + StandardTestDispatcher(testScheduler))
        try {
            val timedTarget = Job(ownerJob)
            assertFailsWith<kotlinx.coroutines.TimeoutCancellationException> {
                timedTarget.joinUntil(100.milliseconds)
            }
            timedTarget.isActive.shouldBeTrue()

            assertFailsWith<kotlinx.coroutines.TimeoutCancellationException> {
                timedTarget.joinUntil(Duration.ZERO)
            }
            assertFailsWith<kotlinx.coroutines.TimeoutCancellationException> {
                timedTarget.joinUntil(-1.nanoseconds)
            }
            timedTarget.isActive.shouldBeTrue()

            val callerCancelledTarget = owner.launch { awaitCancellation() }
            val waiter = launch { callerCancelledTarget.joinUntil(Duration.INFINITE) }
            runCurrent()
            waiter.cancelAndJoin()
            callerCancelledTarget.isActive.shouldBeTrue()

            val infiniteWaitTarget = Job(ownerJob)
            val infiniteWaiter = launch { infiniteWaitTarget.joinUntil(Duration.INFINITE) }
            runCurrent()
            infiniteWaitTarget.complete()
            infiniteWaiter.join()
            infiniteWaitTarget.isCompleted.shouldBeTrue()
        } finally {
            ownerJob.cancelAndJoin()
        }
    }
}
