package io.bluetape4k.coroutines

import io.bluetape4k.assertions.shouldBe
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBe
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.coroutines.withSingleThread
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.utils.Runtimex
import kotlinx.coroutines.async
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import kotlin.coroutines.CoroutineContext
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.nanoseconds

class SuspendLazyTest {

    companion object: KLoggingChannel() {
        private const val TEST_NUMBER = 42
    }

    @Test
    fun `get suspend lazy value in coroutine scope`() = runTest {
        val callCounter = AtomicInteger(0)

        val lazyValue = suspendLazy {
            delay(Random.nextLong(100).milliseconds)
            log.trace { "Calculate lazy value in non-blocking mode." }
            callCounter.incrementAndGet()
            TEST_NUMBER
        }
        callCounter.get() shouldBeEqualTo 0

        yield()

        lazyValue() shouldBeEqualTo TEST_NUMBER
        lazyValue() shouldBeEqualTo TEST_NUMBER

        callCounter.get() shouldBeEqualTo 1
    }

    @Test
    fun `get suspend lazy value in coroutine scope with Multijob`() = runTest {
        val callCounter = AtomicInteger(0)

        val lazyValue = suspendLazy {
            delay(Random.nextLong(100).milliseconds)
            log.trace { "Calculate lazy value in non-blocking mode." }
            callCounter.incrementAndGet()
            TEST_NUMBER
        }
        callCounter.get() shouldBeEqualTo 0

        SuspendedJobTester()
            .workers(Runtimex.availableProcessors)
            .rounds(16)
            .add {
                lazyValue() shouldBeEqualTo TEST_NUMBER
            }
            .add {
                lazyValue() shouldBeEqualTo TEST_NUMBER
            }
            .run()

        callCounter.get() shouldBeEqualTo 1
    }

    @Test
    fun `get lazy value in blocking mode`() = runSuspendIO {
        withSingleThread { callerDispatcher ->
            withContext(callerDispatcher) {
                val callerThread = Thread.currentThread()
                val initializerThread = AtomicReference<Thread?>()
                val callCounter = AtomicInteger(0)

                val lazyValue = suspendBlockingLazy {
                    // 의도적인 blocking 경계: suspendBlockingLazy는 caller context에서
                    // initializer를 실행하고 그 thread를 보존해야 한다.
                    initializerThread.set(Thread.currentThread())
                    Thread.sleep(Random.nextLong(100))
                    log.trace { "Calculate lazy value in blocking mode." }
                    callCounter.incrementAndGet()
                    TEST_NUMBER
                }
                callCounter.get() shouldBeEqualTo 0

                yield()

                lazyValue() shouldBeEqualTo TEST_NUMBER
                lazyValue() shouldBeEqualTo TEST_NUMBER

                callCounter.get() shouldBeEqualTo 1
                initializerThread.get().shouldBe(callerThread)
            }
        }
    }

    @Test
    fun `get lazy value in blocking mode with IO dispatchers`() = runSuspendIO {
        withSingleThread { callerDispatcher ->
            withContext(callerDispatcher) {
                val callerThread = Thread.currentThread()
                val initializerThread = AtomicReference<Thread?>()
                val callCounter = AtomicInteger(0)

                val lazyValue = suspendBlockingLazyIO {
                    // 의도적인 blocking 경계: suspendBlockingLazyIO가 caller와 다른
                    // Dispatchers.IO thread에서 initializer를 실행하는지 관찰한다.
                    initializerThread.set(Thread.currentThread())
                    Thread.sleep(Random.nextLong(100))
                    log.trace { "Calculate lazy value in blocking mode with IO dispatchers" }
                    callCounter.incrementAndGet()
                    TEST_NUMBER
                }
                callCounter.get() shouldBeEqualTo 0

                yield()

                val lazy1 = async { lazyValue() }
                val lazy2 = async { lazyValue() }

                yield()

                lazy1.await() shouldBeEqualTo TEST_NUMBER
                lazy2.await() shouldBeEqualTo TEST_NUMBER

                callCounter.get() shouldBeEqualTo 1
                initializerThread.get().shouldNotBeNull().shouldNotBe(callerThread)
            }
        }
    }

    @Test
    fun `get lazy value in blocking mode with Multijob`() = runSuspendIO {
        val callerThreads = ConcurrentHashMap.newKeySet<Thread>()
        val initializerThread = AtomicReference<Thread?>()
        val callCounter = AtomicInteger(0)

        val lazyValue = suspendBlockingLazyIO {
            // 실제 blocking 경계: SuspendedJobTester의 고정 worker와
            // suspendBlockingLazyIO의 Dispatchers.IO initializer thread를 구분한다.
            initializerThread.set(Thread.currentThread())
            Thread.sleep(Random.nextLong(1000))
            log.debug { "Calculate lazy value in blocking mode with IO dispatchers" }
            callCounter.incrementAndGet()
            TEST_NUMBER
        }
        callCounter.get() shouldBeEqualTo 0

        SuspendedJobTester()
            .workers(Runtimex.availableProcessors)
            .rounds(16)
            .add {
                callerThreads += Thread.currentThread()
                lazyValue() shouldBeEqualTo TEST_NUMBER
            }
            .run()

        callCounter.get() shouldBeEqualTo 1
        val initializedOn = initializerThread.get().shouldNotBeNull()
        callerThreads.any { it === initializedOn }.shouldBeFalse()
    }

    @Test
    fun `interface defaults support implementations that only implement invoke`() = runTest {
        val lazyValue = object: SuspendLazy<Int> {
            override suspend fun invoke(): Int = TEST_NUMBER
        }

        lazyValue.getUntil(1.seconds) shouldBeEqualTo TEST_NUMBER
        lazyValue.getUntilOrNull(1.seconds) shouldBeEqualTo TEST_NUMBER
    }

    @Test
    fun `cancel suspending lazy cancels its child without cancelling owner scope`() = runTest {
        val ownerJob = SupervisorJob()
        val ownerScope = CoroutineScope(ownerJob + StandardTestDispatcher(testScheduler))
        val initializerStarted = CompletableDeferred<Unit>()
        val cleanupCount = AtomicInteger()
        val lazyValue = ownerScope.suspendLazy {
            try {
                initializerStarted.complete(Unit)
                CompletableDeferred<Int>().await()
            } finally {
                cleanupCount.incrementAndGet()
            }
        }
        val waiter = launch { lazyValue() }
        runCurrent()
        initializerStarted.isCompleted.shouldBeTrue()

        lazyValue.cancel()
        runCurrent()
        waiter.join()

        waiter.isCancelled.shouldBeTrue()
        cleanupCount.get() shouldBeEqualTo 1
        ownerJob.isActive.shouldBeTrue()
        ownerJob.cancelAndJoin()
    }

    @Test
    fun `cancel before first suspended lazy invocation is a no-op`() = runTest {
        val lazyValue = suspendLazy { TEST_NUMBER }

        lazyValue.cancel()

        lazyValue() shouldBeEqualTo TEST_NUMBER
    }

    @Test
    fun `timeout waiter does not cancel the lazy initializer`() = runTest {
        val lazyValue = suspendLazy {
            delay(1.seconds)
            TEST_NUMBER
        }

        assertFailsWith<kotlinx.coroutines.TimeoutCancellationException> {
            lazyValue.getUntil(100.milliseconds)
        }

        lazyValue() shouldBeEqualTo TEST_NUMBER
    }

    @Test
    fun `timeout or null distinguishes only by nullable result contract`() = runTest {
        val nullValue = suspendLazy<String?> { null }
        val source = CompletableDeferred<Int>()
        val neverCompletes = suspendLazy { source.await() }

        nullValue.getUntilOrNull(1.seconds) shouldBeEqualTo null
        neverCompletes.getUntilOrNull(100.milliseconds) shouldBeEqualTo null
        source.isCancelled.shouldBeFalse()
        source.complete(TEST_NUMBER).shouldBeTrue()
        neverCompletes() shouldBeEqualTo TEST_NUMBER
    }

    @Test
    fun `zero and negative timeouts expire immediately`() = runTest {
        val lazyValue = suspendLazy { TEST_NUMBER }

        assertFailsWith<kotlinx.coroutines.TimeoutCancellationException> {
            lazyValue.getUntil(0.nanoseconds)
        }
        assertFailsWith<kotlinx.coroutines.TimeoutCancellationException> {
            lazyValue.getUntil(-1.nanoseconds)
        }
    }

    @Test
    fun `blocking lazy cache hit observes caller cancellation`() = runTest {
        val lazyValue = suspendBlockingLazy { TEST_NUMBER }
        lazyValue() shouldBeEqualTo TEST_NUMBER

        val cancelled = Job().apply { cancel(CancellationException("caller stopped")) }
        assertFailsWith<CancellationException> {
            kotlinx.coroutines.withContext(cancelled) {
                lazyValue.getUntil(1.seconds)
            }
        }
    }

    @Test
    fun `blocking lazy cache ignores expired timeout without creating a timer`() = runTest {
        val callCounter = AtomicInteger()
        val lazyValue = suspendBlockingLazy { callCounter.incrementAndGet() }
        lazyValue() shouldBeEqualTo 1

        lazyValue.getUntil(Duration.ZERO) shouldBeEqualTo 1
        lazyValue.getUntil(-1.nanoseconds) shouldBeEqualTo 1
        lazyValue.getUntilOrNull(Duration.ZERO) shouldBeEqualTo 1

        val dispatcher = CountingDelayDispatcher()
        withContext(dispatcher) {
            lazyValue.getUntil(1.seconds) shouldBeEqualTo 1
            lazyValue.getUntilOrNull(1.seconds) shouldBeEqualTo 1
        }
        dispatcher.timeoutSchedules.get() shouldBeEqualTo 0
        callCounter.get() shouldBeEqualTo 1
    }

    @Test
    fun `failed timeout initializer can be retried`() = runSuspendIO(timeout = 5.seconds) {
        val callCounter = AtomicInteger()
        val lazyValue = suspendBlockingLazy {
            if (callCounter.incrementAndGet() == 1) throw IllegalStateException("first attempt")
            TEST_NUMBER
        }

        assertFailsWith<IllegalStateException> { lazyValue.getUntil(1.seconds) }
            .message shouldBeEqualTo "first attempt"
        lazyValue.getUntil(1.seconds) shouldBeEqualTo TEST_NUMBER
        callCounter.get() shouldBeEqualTo 2
    }

    @Test
    fun `timeout and caller cancellation leave lazy source work alive`() = runTest {
        val timeoutSource = CompletableDeferred<Int>()
        val timeoutLazy = suspendLazy { timeoutSource.await() }
        assertFailsWith<kotlinx.coroutines.TimeoutCancellationException> {
            withTimeout(100.milliseconds) { timeoutLazy.getUntil(Duration.INFINITE) }
        }
        timeoutSource.isCancelled.shouldBeFalse()
        timeoutSource.complete(TEST_NUMBER).shouldBeTrue()

        val cancellationSource = CompletableDeferred<Int>()
        val cancellationLazy = suspendLazy { cancellationSource.await() }
        val waiter = launch { cancellationLazy.getUntilOrNull(Duration.INFINITE) }
        runCurrent()
        waiter.cancelAndJoin()

        cancellationSource.isCancelled.shouldBeFalse()
        cancellationSource.complete(TEST_NUMBER).shouldBeTrue()
        cancellationLazy() shouldBeEqualTo TEST_NUMBER
    }

    @Test
    fun `blocking IO initializer keeps running after waiter timeout`() = runSuspendDefault(timeout = 5.seconds) {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val finished = CountDownLatch(1)
        val lazyValue = suspendBlockingLazyIO {
            started.countDown()
            try {
                release.await()
            } finally {
                finished.countDown()
            }
            TEST_NUMBER
        }
        val waiter = async { runCatching { lazyValue.getUntil(50.milliseconds) } }

        try {
            withContext(Dispatchers.IO) {
                started.await(5, TimeUnit.SECONDS).shouldBeTrue()
            }
            val returnedBeforeInitializer = withTimeoutOrNull(500.milliseconds) { waiter.await() }
            (returnedBeforeInitializer?.exceptionOrNull() is kotlinx.coroutines.TimeoutCancellationException)
                .shouldBeTrue()
            finished.count shouldBeEqualTo 1L
        } finally {
            release.countDown()
        }

        val result = waiter.await()
        (result.exceptionOrNull() is kotlinx.coroutines.TimeoutCancellationException).shouldBeTrue()
        finished.await(5, TimeUnit.SECONDS).shouldBeTrue()
    }

    @OptIn(InternalCoroutinesApi::class)
    private class CountingDelayDispatcher: CoroutineDispatcher(), Delay {
        val timeoutSchedules = AtomicInteger()

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            block.run()
        }

        override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
            error("Cached timeout paths must not schedule delays.")
        }

        override fun invokeOnTimeout(
            timeMillis: Long,
            block: Runnable,
            context: CoroutineContext,
        ): DisposableHandle {
            timeoutSchedules.incrementAndGet()
            return DisposableHandle { }
        }
    }




}
