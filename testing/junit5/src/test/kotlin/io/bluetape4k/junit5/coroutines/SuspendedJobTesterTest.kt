package io.bluetape4k.junit5.coroutines

import io.bluetape4k.logging.KLogging
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.test.assertFails
import kotlin.test.assertFailsWith

class SuspendedJobTesterTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @Test
    fun `항상 예외를 발생시키는 코드블럭은 실패한다`() = runTest {
        val block: suspend () -> Unit = { throw RuntimeException("BAM!") }

        assertFails {
            SuspendedJobTester()
                .add(block)
                .run()
        }
    }

    @Test
    fun `긴 실행 시간을 가진 코드블럭 실행`() = runTest {
        val job = CountingJob()

        SuspendedJobTester()
            .numThreads(3)
            .roundsPerJob(4)
            .add(job)
            .run()

        job.count shouldBeEqualTo 4
    }

    @Test
    fun `하나의 suspend 함수 실행하기`() = runTest {
        val block = CountingJob()

        SuspendedJobTester()
            .numThreads(11)
            .roundsPerJob(13)
            .add(block)
            .run()

        block.count shouldBeEqualTo 13
    }

    @Test
    fun `복수의 테스트 코드 실행하기`() = runTest {
        val block1 = CountingJob()
        val block2 = CountingJob()

        SuspendedJobTester()
            .numThreads(3)
            .roundsPerJob(4)
            .addAll(block1, block2)
            .run()

        block1.count shouldBeEqualTo 4
        block2.count shouldBeEqualTo 4
    }

    @Test
    fun `실행할 코드블럭을 등록하지 않으면 예외가 발생한다`() = runTest {
        assertFailsWith<IllegalStateException> {
            SuspendedJobTester()
                .run()
        }

        assertFailsWith<IllegalStateException> {
            SuspendedJobTester()
                .numThreads(2)
                .roundsPerJob(1)
                .run()
        }
    }

    private class CountingJob: suspend () -> Unit {
        val counter = atomic(0)
        val count: Int by counter

        override suspend fun invoke() {
            delay(10)
            counter.incrementAndGet()
            // log.trace { "execution count: $count" }
        }
    }
}
