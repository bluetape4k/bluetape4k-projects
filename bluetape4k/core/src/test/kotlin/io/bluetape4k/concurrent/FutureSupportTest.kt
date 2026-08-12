package io.bluetape4k.concurrent

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.concurrent.virtualthread.VirtualFuture
import io.bluetape4k.concurrent.virtualthread.virtualFuture
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.utils.Runtimex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledForJreRange
import org.junit.jupiter.api.condition.JRE
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class FutureSupportTest {

    companion object: KLogging() {
        private const val ITEM_COUNT = 100
        private const val DELAY_TIME = 10L
    }

    @Test
    fun `Future as CompletableFuture`() {
        val future1 = FutureTask {
            Thread.sleep(DELAY_TIME)
            "value1"
        }
        future1.run()

        val future2 = FutureTask {
            Thread.sleep(DELAY_TIME)
            "value2"
        }
        future2.run()

        val result1 = future1.asCompletableFuture()
        val result2 = future2.asCompletableFuture()
        result1.join() shouldBeEqualTo "value1"
        result2.join() shouldBeEqualTo "value2"
    }

    @EnabledForJreRange(min = JRE.JAVA_21)
    @Test
    fun `Future wrapper waits on named virtual executor thread`() {
        val future = BlockingFuture<String>()
        val completableFuture = future.asCompletableFuture()

        future.awaitStarted()
        val watcher = future.getterThread.get()

        watcher.isVirtual.shouldBeTrue()
        watcher.name.startsWith("future-wrapper-").shouldBeTrue()
        (watcher.name == "future-wrapper").shouldBeFalse()

        future.complete("value")
        completableFuture.get(1, TimeUnit.SECONDS) shouldBeEqualTo "value"
    }

    @Test
    fun `cancel propagates to wrapped Future and cancels wrapper`() {
        val future = BlockingFuture<String>()
        val completableFuture = future.asCompletableFuture()

        future.awaitStarted()

        completableFuture.cancel(true).shouldBeTrue()

        future.isCancelled.shouldBeTrue()
        completableFuture.isCancelled.shouldBeTrue()
    }

    @Test
    fun `cancel returns true when wrapped Future cancellation races with watcher cancellation`() {
        val watcherStarted = CountDownLatch(1)
        val wrapperCompletionObserved = CountDownLatch(1)
        val future = object : Future<String> {
            private val cancelled = AtomicBoolean(false)
            private val getterThread = AtomicReference<Thread>()

            override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
                cancelled.set(true)
                getterThread.get().interrupt()
                wrapperCompletionObserved.await(1, TimeUnit.SECONDS).shouldBeTrue()
                return true
            }

            override fun isCancelled(): Boolean = cancelled.get()

            override fun isDone(): Boolean = cancelled.get()

            override fun get(): String {
                getterThread.set(Thread.currentThread())
                watcherStarted.countDown()
                CountDownLatch(1).await()
                error("unreachable")
            }

            override fun get(timeout: Long, unit: TimeUnit): String {
                getterThread.set(Thread.currentThread())
                watcherStarted.countDown()
                if (!CountDownLatch(1).await(timeout, unit)) {
                    throw TimeoutException()
                }
                error("unreachable")
            }
        }
        val completableFuture = future.asCompletableFuture()
        watcherStarted.await(1, TimeUnit.SECONDS).shouldBeTrue()
        completableFuture.whenComplete { _, _ -> wrapperCompletionObserved.countDown() }

        completableFuture.cancel(true).shouldBeTrue()

        future.isCancelled.shouldBeTrue()
        completableFuture.isCancelled.shouldBeTrue()
    }

    @Test
    fun `Massive Future as CompletableFuture`() {
        val futures = List(ITEM_COUNT) {
            FutureTask {
                Thread.sleep(Random.nextLong(DELAY_TIME))
                "value$it"
            }.apply { run() }
        }.map { it.asCompletableFuture() }

        val results = futures.sequence().get()
        results.size shouldBeEqualTo ITEM_COUNT
    }

    @Test
    fun `Massive Future as CompletaboeFuture in Multiple Thread`() {
        val counter = AtomicInteger(0)

        MultithreadingTester()
            .workers(Runtimex.availableProcessors * 2)
            .rounds(ITEM_COUNT / 4)
            .add {
                val task = CompletableFuture.supplyAsync {
                    Thread.sleep(Random.nextLong(DELAY_TIME))
                    counter.incrementAndGet()
                }
                val result = task.asCompletableFuture().get()
                log.trace { "result=$result" }
            }
            .run()

        counter.get() shouldBeEqualTo Runtimex.availableProcessors * 2 * ITEM_COUNT / 4
    }

    @EnabledForJreRange(min = JRE.JAVA_21)
    @Test
    fun `Massive Future as CompletaboeFuture in Virtual Threads`() {
        val counter = AtomicInteger(0)

        StructuredTaskScopeTester()
            .rounds(Runtimex.availableProcessors * 2 * ITEM_COUNT / 4)
            .add {
                val task: VirtualFuture<Int> = virtualFuture {
                    Thread.sleep(Random.nextLong(DELAY_TIME))
                    counter.incrementAndGet()
                }
                val result = task.await()
                log.trace { "result=$result" }
            }
            .run()

        counter.get() shouldBeEqualTo Runtimex.availableProcessors * 2 * ITEM_COUNT / 4
    }

    @Test
    fun `Massive Future as CompletaboeFuture in Coroutines`() = runSuspendDefault {
        val counter = AtomicInteger(0)

        SuspendedJobTester()
            .workers(Runtimex.availableProcessors * 2)
            .rounds(Runtimex.availableProcessors * 2 * ITEM_COUNT / 4)
            .add {
                val task = async(Dispatchers.Default) {
                    delay(Random.nextLong(DELAY_TIME).milliseconds)
                    counter.incrementAndGet()
                }
                val result = task.await()
                log.trace { "result=$result" }
            }
            .run()

        counter.get() shouldBeEqualTo Runtimex.availableProcessors * 2 * ITEM_COUNT / 4
    }

    private class BlockingFuture<T>: Future<T> {
        val getterThread: AtomicReference<Thread> = AtomicReference()

        private val started = CountDownLatch(1)
        private val finished = CountDownLatch(1)
        private val cancelled = AtomicBoolean(false)
        private val value = AtomicReference<T>()

        fun awaitStarted() {
            started.await(1, TimeUnit.SECONDS).shouldBeTrue()
        }

        fun complete(result: T) {
            value.set(result)
            finished.countDown()
        }

        override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
            cancelled.set(true)
            finished.countDown()
            return true
        }

        override fun isCancelled(): Boolean =
            cancelled.get()

        override fun isDone(): Boolean =
            finished.count == 0L

        override fun get(): T {
            getterThread.set(Thread.currentThread())
            started.countDown()
            finished.await()
            return completedValue()
        }

        override fun get(timeout: Long, unit: TimeUnit): T {
            getterThread.set(Thread.currentThread())
            started.countDown()
            if (!finished.await(timeout, unit)) {
                throw TimeoutException()
            }
            return completedValue()
        }

        private fun completedValue(): T {
            if (cancelled.get()) {
                throw CancellationException()
            }
            return value.get()
                ?: throw ExecutionException(IllegalStateException("BlockingFuture completed without a value"))
        }
    }
}
