package io.bluetape4k.junit5.concurrency

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import kotlinx.atomicfu.atomic
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeLessOrEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledForJreRange
import org.junit.jupiter.api.condition.JRE
import io.bluetape4k.assertions.assertFailsWith
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.milliseconds

@EnabledForJreRange(min = JRE.JAVA_21)
class StructuredTaskScopeTesterTest {
    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @Test
    fun `예외를 발생시키는 코드는 실패한다`() {
        val block = { throw RuntimeException("BAM!") }

        assertFailsWith<RuntimeException> {
            StructuredTaskScopeTester()
                .rounds(Runtime.getRuntime().availableProcessors())
                .add(block)
                .run()
        }
    }

    @Test
    fun `thread 수가 복수이면 실행시간은 테스트 코드의 실행 시간의 총합보다 작아야 한다`() {
        val time = measureTimeMillis {
            StructuredTaskScopeTester()
                .rounds(4)
                .add { Thread.sleep(100) }
                .add { Thread.sleep(100) }
                .run()
        }
        time shouldBeLessOrEqualTo 200
    }

    @Test
    fun `하나의 코드블럭을 여러번 수행 시 수행 횟수는 같아야 한다`() {
        val block = CountingTask()

        StructuredTaskScopeTester()
            .rounds(10)
            .add(block)
            .run()

        block.count shouldBeEqualTo 10
    }

    @Test
    fun `공통 설정명 rounds를 사용할 수 있다`() {
        val block = CountingTask()

        StructuredTaskScopeTester()
            .rounds(7)
            .add(block)
            .run()

        block.count shouldBeEqualTo 7
    }

    @Test
    fun `두 개의 코드 블럭을 병렬로 실행`() {
        val block1 = CountingTask()
        val block2 = CountingTask()

        StructuredTaskScopeTester()
            .rounds(4)
            .addAll(block1, block2)
            .run()

        block1.count shouldBeEqualTo 4
        block2.count shouldBeEqualTo 4
    }

    @Test
    fun `실행할 코드블럭을 등록하지 않으면 예외가 발생한다`() {
        assertFailsWith<IllegalStateException> {
            StructuredTaskScopeTester().run()
        }
    }

    @Test
    fun `withTimeout - 데드라인 내 완료 시 정상 반환`() {
        val block = CountingTask()

        StructuredTaskScopeTester()
            .rounds(3)
            .withTimeout(2_000.milliseconds)
            .add(block)
            .run()

        block.count shouldBeEqualTo 3
    }

    @Test
    fun `withTimeout - 데드라인 초과 시 TimeoutException 발생`() {
        assertFailsWith<TimeoutException> {
            StructuredTaskScopeTester()
                .rounds(1)
                .withTimeout(50.milliseconds)
                .add { Thread.sleep(5_000) }
                .run()
        }
    }

    @Test
    fun `workers - 기본 worker 수는 availableProcessors 의 2배이다`() {
        StructuredTaskScopeTester.DEFAULT_WORKER_COUNT shouldBeEqualTo Runtime.getRuntime().availableProcessors() * 2
    }

    @Test
    fun `workers - 지정한 수 이상으로 동시 실행되지 않는다`() {
        val workerLimit = 2
        // Semaphore(workerLimit).tryAcquire() fails atomically if more than workerLimit
        // threads enter concurrently — avoids the non-atomic increment+peak-update race.
        val guard = Semaphore(workerLimit)
        val limitExceeded = AtomicBoolean(false)

        StructuredTaskScopeTester()
            .workers(workerLimit)
            .rounds(10)
            .add {
                if (!guard.tryAcquire()) {
                    limitExceeded.set(true)
                } else {
                    try {
                        Thread.sleep(10)
                    } finally {
                        guard.release()
                    }
                }
            }
            .run()

        limitExceeded.get() shouldBeEqualTo false
    }

    private class CountingTask: () -> Unit {
        private val counter = atomic(0)
        val count by counter

        override fun invoke() {
            Thread.sleep(1)
            counter.incrementAndGet()
            log.trace { "Execution count: $count" }
        }
    }
}
