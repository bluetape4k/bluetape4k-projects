package io.bluetape4k.concurrent.virtualthread

import io.bluetape4k.junit5.concurrency.VirtualthreadTester
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import kotlinx.atomicfu.atomic
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeoutException
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class VirtualFutureTest {

    companion object: KLogging()

    @Test
    fun `run task with virtual thread`() {
        val vfuture = virtualFuture {
            log.debug { "Run VirtualFuture ..." }
            Thread.sleep(1000)
            42
        }

        vfuture.await() shouldBeEqualTo 42
    }

    @Test
    fun `run task with virtual thread and await timeout`() {
        val vfuture = virtualFuture {
            log.debug { "Run VirtualFuture ..." }
            Thread.sleep(1000)
            42
        }

        // 1초 작업에 대해 500ms 대기 후 TimeoutException 발생
        assertFailsWith<TimeoutException> {
            vfuture.await(500.milliseconds.toJavaDuration()) shouldBeEqualTo 42
        }

        vfuture.await(2.seconds.toJavaDuration()) shouldBeEqualTo 42
    }

    @Test
    fun `run many tasks with virtual threads`() {
        val taskSize = 100

        val tasks: List<() -> Int> = List(taskSize) {
            {
                log.debug { "Run task[$it]" }
                Thread.sleep(1000)
                it
            }
        }

        val virtualFutures = virtualFutureAll(tasks = tasks)
        virtualFutures.await() shouldBeEqualTo (0 until taskSize).toList()
    }

    @Test
    fun `run many tasks with virtual thread tester`() {
        val taskCount = atomic(0)

        // 1초씩 대기하는 1000 개의 작업을 Virtual Thread를 이용하면, 2초내에 모든 작업이 완료됩니다.
        VirtualthreadTester()
            .numThreads(1000)
            .roundsPerThread(1)
            .add {
                Thread.sleep(1000)
                log.trace { "Run task ...${taskCount.incrementAndGet()}" }
            }
            .add {
                Thread.sleep(1000)
                log.trace { "Run task ...${taskCount.incrementAndGet()}" }
            }
            .run()
    }
}
