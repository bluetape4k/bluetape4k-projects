package io.bluetape4k.concurrent

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ExecutorServiceExtensionsTest {

    @Test
    fun `invokeAll calls each task and returns results in input order`() = withExecutor(2) { executor ->
        val invoked = AtomicInteger()
        val tasks: List<() -> Int> = listOf(
            { invoked.incrementAndGet(); 10 },
            { invoked.incrementAndGet(); 20 },
        )

        val futures: List<Future<Int>> = executor.invokeAll(tasks, 1.seconds)
        futures.map { it.get() } shouldBeEqualTo listOf(10, 20)
        invoked.get() shouldBeEqualTo 2
    }

    @Test
    fun `invokeAny returns first successful result and cancels remaining task`() = withExecutor(2) { executor ->
        val loserStarted = CountDownLatch(1)
        val loserInterrupted = CountDownLatch(1)
        val releaseLoser = CountDownLatch(1)
        val tasks: List<() -> Int> = listOf(
            {
                loserStarted.await(5, TimeUnit.SECONDS).shouldBeTrue()
                42
            },
            {
                loserStarted.countDown()
                try {
                    releaseLoser.await()
                } catch (error: InterruptedException) {
                    loserInterrupted.countDown()
                    throw error
                }
                -1
            },
        )

        try {
            executor.invokeAny(tasks, 1.seconds) shouldBeEqualTo 42
            loserInterrupted.await(5, TimeUnit.SECONDS).shouldBeTrue()
        } finally {
            releaseLoser.countDown()
        }
    }

    @Test
    fun `invokeAll preserves task failures and handles empty input`() = withExecutor(1) { executor ->
        val futures = executor.invokeAll(
            listOf<() -> Int>({ throw IllegalStateException("task failed") }, { 7 }),
            1.seconds,
        )

        assertFailsWith<ExecutionException> { futures[0].get() }
            .cause?.message shouldBeEqualTo "task failed"
        futures[1].get() shouldBeEqualTo 7
        executor.invokeAll(emptyList<() -> Int>(), 1.seconds).size shouldBeEqualTo 0
    }

    @Test
    fun `invokeAny propagates task failures and rejects empty input`() = withExecutor(2) { executor ->
        assertFailsWith<ExecutionException> {
            executor.invokeAny(
                listOf<() -> Int>(
                    { throw IllegalStateException("first failed") },
                    { throw IllegalArgumentException("second failed") },
                ),
                1.seconds,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            executor.invokeAny(emptyList<() -> Int>(), 1.seconds)
        }
    }

    @Test
    fun `invokeAll timeout cancels unfinished tasks without cancelling the executor`() = withExecutor(1) { executor ->
        val started = CountDownLatch(1)
        val interrupted = CountDownLatch(1)
        val release = CountDownLatch(1)
        val task: () -> Int = {
            started.countDown()
            try {
                release.await()
            } catch (error: InterruptedException) {
                interrupted.countDown()
                throw error
            }
            42
        }

        try {
            val futures = executor.invokeAll(listOf(task), 100.milliseconds)
            started.await(5, TimeUnit.SECONDS).shouldBeTrue()
            futures.single().isCancelled.shouldBeTrue()
            interrupted.await(5, TimeUnit.SECONDS).shouldBeTrue()
            executor.isShutdown.shouldBeFalse()
        } finally {
            release.countDown()
        }
    }

    @Test
    fun `invokeAny timeout cancels running task`() = withExecutor(1) { executor ->
        val started = CountDownLatch(1)
        val interrupted = CountDownLatch(1)
        val release = CountDownLatch(1)
        val task: () -> Int = {
            started.countDown()
            try {
                release.await()
            } catch (error: InterruptedException) {
                interrupted.countDown()
                throw error
            }
            42
        }

        try {
            assertFailsWith<TimeoutException> { executor.invokeAny(listOf(task), 100.milliseconds) }
            started.await(5, TimeUnit.SECONDS).shouldBeTrue()
            interrupted.await(5, TimeUnit.SECONDS).shouldBeTrue()
        } finally {
            release.countDown()
        }
    }

    @Test
    fun `zero and negative timeouts cancel tasks queued behind a busy worker`() = withExecutor(1) { executor ->
        val blockerStarted = CountDownLatch(1)
        val releaseBlocker = CountDownLatch(1)
        executor.execute {
            blockerStarted.countDown()
            releaseBlocker.await()
        }
        blockerStarted.await(5, TimeUnit.SECONDS).shouldBeTrue()

        val invoked = AtomicInteger()
        try {
            val all = executor.invokeAll(
                listOf<() -> Int>({ invoked.incrementAndGet() }, { invoked.incrementAndGet() }),
                Duration.ZERO,
            )
            all.all { it.isCancelled }.shouldBeTrue()

            assertFailsWith<TimeoutException> {
                executor.invokeAny(listOf({ invoked.incrementAndGet() }), -1.milliseconds)
            }
        } finally {
            releaseBlocker.countDown()
        }

        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS).shouldBeTrue()
        invoked.get() shouldBeEqualTo 0
    }

    @Test
    fun `invokeAll and invokeAny propagate waiter interruption and interrupt running work`() {
        assertInterruptedWaiter { executor, taskStarted, taskInterrupted, release ->
            executor.invokeAll(
                listOf<() -> Int>({ interruptibleTask(taskStarted, taskInterrupted, release) }, { 2 }),
                5.seconds,
            )
        }
        assertInterruptedWaiter { executor, taskStarted, taskInterrupted, release ->
            executor.invokeAny(
                listOf<() -> Int>({ interruptibleTask(taskStarted, taskInterrupted, release) }),
                5.seconds,
            )
        }
    }

    @Test
    fun `partial submission rejection cancels previously started tasks`() {
        assertPartialRejectionCancelsStartedTask { executor, firstTask ->
            executor.invokeAll(listOf(firstTask, { 2 }), 5.seconds)
        }
        assertPartialRejectionCancelsStartedTask { executor, firstTask ->
            executor.invokeAny(listOf(firstTask, { 2 }), 5.seconds)
        }
    }

    @Test
    fun `terminated executor rejects both operations`() {
        val executor = Executors.newSingleThreadExecutor()
        executor.shutdown()

        assertFailsWith<RejectedExecutionException> {
            executor.invokeAll(listOf<() -> Int>({ 1 }), 1.seconds)
        }
        assertFailsWith<RejectedExecutionException> {
            executor.invokeAny(listOf<() -> Int>({ 1 }), 1.seconds)
        }
    }

    @Test
    fun `infinite duration is passed to both timed JDK operations as saturated nanoseconds`() {
        val executor = RecordingExecutorService()
        val tasks: List<() -> Int> = listOf({ 42 })

        val futures = executor.invokeAll(tasks, Duration.INFINITE)
        futures.single().get() shouldBeEqualTo 42
        executor.lastTimeout shouldBeEqualTo Long.MAX_VALUE
        executor.lastUnit shouldBeEqualTo TimeUnit.NANOSECONDS

        executor.invokeAny(tasks, Duration.INFINITE) shouldBeEqualTo 42
        executor.lastTimeout shouldBeEqualTo Long.MAX_VALUE
        executor.lastUnit shouldBeEqualTo TimeUnit.NANOSECONDS
        executor.shutdown()
    }

    private fun interruptibleTask(
        started: CountDownLatch,
        interrupted: CountDownLatch,
        release: CountDownLatch,
    ): Int {
        started.countDown()
        return try {
            release.await()
            1
        } catch (error: InterruptedException) {
            interrupted.countDown()
            throw error
        }
    }

    private fun assertInterruptedWaiter(
        invoke: (ExecutorService, CountDownLatch, CountDownLatch, CountDownLatch) -> Any?,
    ) {
        withExecutor(1) { delegate ->
            val started = CountDownLatch(1)
            val interrupted = CountDownLatch(1)
            val release = CountDownLatch(1)
            val entered = CountDownLatch(1)
            val failure = AtomicReference<Throwable?>()
            val waiter = Thread {
                entered.countDown()
                try {
                    invoke(delegate, started, interrupted, release)
                } catch (error: Throwable) {
                    failure.set(error)
                }
            }

            try {
                waiter.start()
                entered.await(5, TimeUnit.SECONDS).shouldBeTrue()
                started.await(5, TimeUnit.SECONDS).shouldBeTrue()
                waiter.interrupt()
                waiter.join(TimeUnit.SECONDS.toMillis(5))
                waiter.isAlive.shouldBeFalse()
                failure.get().shouldNotBeNull().shouldBeInstanceOf(InterruptedException::class)
                interrupted.await(5, TimeUnit.SECONDS).shouldBeTrue()
            } finally {
                release.countDown()
            }
        }
    }

    private fun assertPartialRejectionCancelsStartedTask(
        invoke: (RejectSecondExecutor, () -> Int) -> Any?,
    ) {
        val executor = RejectSecondExecutor()
        val release = CountDownLatch(1)
        val firstTask: () -> Int = {
            executor.firstStarted.countDown()
            try {
                release.await()
            } catch (error: InterruptedException) {
                executor.firstInterrupted.countDown()
                throw error
            }
            1
        }

        try {
            assertFailsWith<RejectedExecutionException> { invoke(executor, firstTask) }
            executor.firstInterrupted.await(5, TimeUnit.SECONDS).shouldBeTrue()
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
    }

    private inline fun <T> withExecutor(
        threads: Int,
        block: (java.util.concurrent.ExecutorService) -> T,
    ): T {
        val executor = Executors.newFixedThreadPool(threads)
        try {
            return block(executor)
        } finally {
            executor.shutdownNow()
            executor.awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    private class RejectSecondExecutor: AbstractExecutorService() {
        private val delegate = Executors.newSingleThreadExecutor()
        private val executeCount = AtomicInteger()
        val firstStarted = CountDownLatch(1)
        val firstInterrupted = CountDownLatch(1)

        override fun execute(command: Runnable) {
            if (executeCount.incrementAndGet() == 1) {
                delegate.execute(command)
            } else {
                firstStarted.await(5, TimeUnit.SECONDS).shouldBeTrue()
                throw RejectedExecutionException("second submission rejected")
            }
        }

        override fun shutdown() = delegate.shutdown()
        override fun shutdownNow(): MutableList<Runnable> = delegate.shutdownNow()
        override fun isShutdown(): Boolean = delegate.isShutdown
        override fun isTerminated(): Boolean = delegate.isTerminated
        override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean =
            delegate.awaitTermination(timeout, unit)
    }

    private class RecordingExecutorService: AbstractExecutorService() {
        var lastTimeout: Long = 0
        var lastUnit: TimeUnit? = null

        override fun execute(command: Runnable) = error("recording executor does not execute commands")

        override fun <T> invokeAll(
            tasks: MutableCollection<out Callable<T>>,
            timeout: Long,
            unit: TimeUnit,
        ): MutableList<Future<T>> {
            lastTimeout = timeout
            lastUnit = unit
            return tasks.map { CompletableFuture.completedFuture(it.call()) }.toMutableList()
        }

        override fun <T> invokeAny(
            tasks: MutableCollection<out Callable<T>>,
            timeout: Long,
            unit: TimeUnit,
        ): T {
            lastTimeout = timeout
            lastUnit = unit
            return tasks.first().call()
        }

        override fun shutdown() = Unit
        override fun shutdownNow(): MutableList<Runnable> = mutableListOf()
        override fun isShutdown(): Boolean = false
        override fun isTerminated(): Boolean = false
        override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean = true
    }
}
