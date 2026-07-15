package io.bluetape4k.concurrent.virtualthread

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledForJreRange
import org.junit.jupiter.api.condition.JRE
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
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
    fun `timed virtualFutureAll interrupts running tasks after timeout`() {
        val started = CountDownLatch(1)
        val interrupted = CountDownLatch(1)
        val release = CountDownLatch(1)

        val result = virtualFutureAll(
            tasks = listOf {
                started.countDown()
                try {
                    release.await()
                    1
                } catch (e: InterruptedException) {
                    interrupted.countDown()
                    throw e
                }
            },
            timeout = 2.seconds.toJavaDuration(),
        )

        try {
            started.await(1, TimeUnit.SECONDS).shouldBeTrue()
            assertFailsWith<ExecutionException> {
                result.await()
            }.cause shouldBeInstanceOf TimeoutException::class
            interrupted.await(1, TimeUnit.SECONDS).shouldBeTrue()
        } finally {
            release.countDown()
        }
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

    @EnabledForJreRange(min = JRE.JAVA_21)
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
