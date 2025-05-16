package io.bluetape4k.coroutines.support

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class JobSupportTest {

    companion object: KLoggingChannel()

    @Test
    fun `print job hierarchy`() = runSuspendIO {
        val job = launch {
            val childJobs = List(10) {
                launch {
                    delay(100)
                    log.debug { "Child job $it" }
                }
            }
            childJobs.joinAll()
        }

        job.printDebugTree()
    }

    @Test
    fun `jobs joinAny`() = runTest {
        val job1 = launch { delay(1000); log.debug { "Job 1" } }
        val job2 = launch { delay(2000); log.debug { "Job 2" } }
        val job3 = launch { delay(3000); log.debug { "Job 3" } }

        // 처음 완료된 값이 나오면 끝낸다.
        joinAny(job1, job2, job3)

        job1.isCompleted.shouldBeTrue()
        job2.isCompleted.shouldBeFalse()
        job3.isCompleted.shouldBeFalse()
    }

    @Test
    fun `collection of job joinAny`() = runTest {
        val job1 = launch { delay(1000); log.debug { "Job 1" } }
        val job2 = launch { delay(2000); log.debug { "Job 2" } }
        val job3 = launch { delay(3000); log.debug { "Job 3" } }

        val jobs = listOf(job1, job2, job3)

        // 처음 완료된 값이 나오면 끝낸다.
        jobs.joinAny()

        job1.isCompleted.shouldBeTrue()
        job2.isCompleted.shouldBeFalse()
        job3.isCompleted.shouldBeFalse()
    }

    @Test
    fun `collection of job joinAny and cancel others`() = runTest {
        val job1 = launch { delay(1000); log.debug { "Job 1" } }
        val job2 = launch { delay(2000); log.debug { "Job 2" } }
        val job3 = launch { delay(3000); log.debug { "Job 3" } }

        val jobs = listOf(job1, job2, job3)

        // 처음 완료된 값이 나오면 끝낸다.
        jobs.joinAnyAndCancelOthers()

        yield()

        job1.isCompleted.shouldBeTrue()
        job2.isCancelled.shouldBeTrue()
        job3.isCancelled.shouldBeTrue()
    }
}
