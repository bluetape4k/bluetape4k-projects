package io.bluetape4k.junit5.concurrency

import io.bluetape4k.logging.KLogging
import kotlinx.atomicfu.atomic
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis
import kotlin.test.assertFailsWith

class VirtualthreadTesterTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @Test
    fun `예외를 발생시키는 코드는 실패한다`() {
        val block = { throw RuntimeException("BAM!") }

        assertFailsWith<RuntimeException> {
            VirtualthreadTester()
                .numThreads(2)
                .roundsPerThread(1)
                .add(block)
                .run()
        }
    }

    @Test
    fun `thread 수가 복수이면 실행시간은 테스트 코드의 실행 시간의 총합보다 작아야 한다`() {
        val time = measureTimeMillis {
            VirtualthreadTester()
                .numThreads(2)
                .roundsPerThread(1)
                .add {
                    Thread.sleep(100)
                }
                .add {
                    Thread.sleep(100)
                }
                .run()
        }
        time shouldBeLessOrEqualTo 200
    }

    @Test
    fun `하나의 코드블럭을 여러번 수행 시 수행 횟수는 같아야 한다`() {
        val block = CountingTask()

        VirtualthreadTester()
            .numThreads(11)
            .roundsPerThread(13)
            .add(block)
            .run()

        block.count shouldBeEqualTo 11 * 13
    }

    @Test
    fun `두 개의 코드 블럭을 병렬로 실행`() {
        val block1 = CountingTask()
        val block2 = CountingTask()

        MultithreadingTester()
            .numThreads(3)
            .roundsPerThread(1)
            .addAll(block1, block2)
            .run()

        block1.count shouldBeEqualTo 2
        block2.count shouldBeEqualTo 1
    }

    @Test
    fun `실행할 코드블럭을 등록하지 않으면 예외가 발생한다`() {
        assertFailsWith<IllegalStateException> {
            VirtualthreadTester()
                .run()
        }

        assertFailsWith<IllegalStateException> {
            VirtualthreadTester()
                .numThreads(2)
                .roundsPerThread(1)
                .run()
        }
    }

    @Test
    fun `numThreads 보다 많은 코드블럭을 등록하면 예외가 발생한다`() {
        assertFailsWith<IllegalStateException> {
            VirtualthreadTester()
                .numThreads(2)
                .roundsPerThread(1)
                .add(CountingTask())
                .add(CountingTask())
                .add(CountingTask())
                .run()
        }
    }

    private class CountingTask: () -> Unit {
        private val counter = atomic(0)
        val count by counter

        override fun invoke() {
            Thread.sleep(1)
            counter.incrementAndGet()
            // log.trace { "Execution count: $count" }
        }
    }
}
