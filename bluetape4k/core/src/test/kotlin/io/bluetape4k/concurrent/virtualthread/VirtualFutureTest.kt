package io.bluetape4k.concurrent.virtualthread

import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
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
                Thread.sleep(100)
                it
            }
        }

        val virtualFutures = virtualFutureAll(tasks = tasks)
        virtualFutures.await() shouldBeEqualTo (0 until taskSize).toList()
    }

    @Test
    fun `virtualFutureAll with empty tasks should return empty list`() {
        val result = virtualFutureAll(tasks = emptyList<() -> Int>()).await()
        result shouldBeEqualTo emptyList()
    }

    @Test
    fun `awaitAll with empty virtual futures should return empty list`() {
        val result = emptyList<VirtualFuture<Int>>().awaitAll()
        result shouldBeEqualTo emptyList()
    }

    @EnabledOnJre(JRE.JAVA_21, JRE.JAVA_25)
    @Test
    fun `run many tasks with virtual thread tester`() {
        val taskCount = AtomicInteger(0)

        // 1초씩 대기하는 1000 개의 작업을 Virtual Thread를 이용하면, 2초내에 모든 작업이 완료됩니다.
        StructuredTaskScopeTester()
            .rounds(1)
            .add {
                Thread.sleep(100)
                taskCount.incrementAndGet()
                log.trace { "Run task ...${taskCount.get()}" }
            }
            .add {
                Thread.sleep(100)
                taskCount.incrementAndGet()
                log.trace { "Run task ...${taskCount.get()}" }
            }
            .run()

        taskCount.get() shouldBeEqualTo 2
    }
}
