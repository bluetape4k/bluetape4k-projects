package io.bluetape4k.coroutines

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBe
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.coroutines.withSingleThread
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import kotlin.coroutines.CoroutineContext
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class SuspendBlockingLazyTimeoutTest {
    private companion object {
        const val TEST_NUMBER = 42
        const val JOB_CHILD_COMPLETION_CHECK_ATTEMPTS = 100
    }

    @Test
    fun `default blocking initializer timeout progresses on a single thread caller`() =
        runSuspendIO(timeout = 5.seconds) {
        withSingleThread { callerDispatcher ->
            val started = CountDownLatch(1)
            val release = CountDownLatch(1)
            val finished = CountDownLatch(1)
            val waiterThread = AtomicReference<Thread?>()
            val initializerThread = AtomicReference<Thread?>()
            val lazyValue = suspendBlockingLazy {
                initializerThread.set(Thread.currentThread())
                started.countDown()
                try {
                    release.await()
                    TEST_NUMBER
                } finally {
                    finished.countDown()
                }
            }
            val waiter = async(callerDispatcher) {
                waiterThread.set(Thread.currentThread())
                runCatching { lazyValue.getUntil(100.milliseconds) }
            }

            val timedOutBeforeRelease = try {
                withContext(Dispatchers.IO) {
                    started.await(5, TimeUnit.SECONDS).shouldBeTrue()
                    val result = withTimeoutOrNull(500.milliseconds) { waiter.await() }
                    if (result != null) {
                        finished.count shouldBeEqualTo 1L
                        initializerThread.get().shouldNotBe(waiterThread.get())
                    }
                    result?.exceptionOrNull() is kotlinx.coroutines.TimeoutCancellationException
                }
            } finally {
                release.countDown()
            }

            val finalResult = withTimeout(5.seconds) { waiter.await() }
            (finalResult.exceptionOrNull() is kotlinx.coroutines.TimeoutCancellationException).shouldBeTrue()
            finished.await(5, TimeUnit.SECONDS).shouldBeTrue()
            timedOutBeforeRelease.shouldBeTrue()
        }
    }

    @Test
    fun `matching configured dispatcher does not block timeout waiter`() = runSuspendIO(timeout = 5.seconds) {
        withSingleThread { callerDispatcher ->
            val started = CountDownLatch(1)
            val release = CountDownLatch(1)
            val finished = CountDownLatch(1)
            val waiterThread = AtomicReference<Thread?>()
            val initializerThread = AtomicReference<Thread?>()
            val lazyValue = suspendBlockingLazy(callerDispatcher) {
                initializerThread.set(Thread.currentThread())
                started.countDown()
                try {
                    release.await()
                    TEST_NUMBER
                } finally {
                    finished.countDown()
                }
            }
            val waiter = async(callerDispatcher) {
                waiterThread.set(Thread.currentThread())
                runCatching { lazyValue.getUntil(100.milliseconds) }
            }

            val timedOutBeforeRelease = try {
                withContext(Dispatchers.IO) {
                    started.await(5, TimeUnit.SECONDS).shouldBeTrue()
                    val result = withTimeoutOrNull(500.milliseconds) { waiter.await() }
                    if (result != null) {
                        finished.count shouldBeEqualTo 1L
                        initializerThread.get().shouldNotBe(waiterThread.get())
                    }
                    result?.exceptionOrNull() is kotlinx.coroutines.TimeoutCancellationException
                }
            } finally {
                release.countDown()
            }

            val finalResult = withTimeout(5.seconds) { waiter.await() }
            (finalResult.exceptionOrNull() is kotlinx.coroutines.TimeoutCancellationException).shouldBeTrue()
            finished.await(5, TimeUnit.SECONDS).shouldBeTrue()
            timedOutBeforeRelease.shouldBeTrue()
        }
    }

    @Test
    fun `blocking timeout isolates initializer while direct invocation uses configured dispatcher`() =
        runSuspendIO(timeout = 5.seconds) {
        withSingleThread { initializerDispatcher ->
            val configuredThread = withContext(initializerDispatcher) { Thread.currentThread() }
            val directThread = AtomicReference<Thread?>()
            val directLazy = suspendBlockingLazy(initializerDispatcher) {
                directThread.set(Thread.currentThread())
                TEST_NUMBER
            }
            directLazy() shouldBeEqualTo TEST_NUMBER
            directThread.get() shouldBeEqualTo configuredThread

            val timeoutThread = AtomicReference<Thread?>()
            val timeoutLazy = suspendBlockingLazy(initializerDispatcher) {
                timeoutThread.set(Thread.currentThread())
                TEST_NUMBER
            }
            timeoutLazy.getUntil(1.seconds) shouldBeEqualTo TEST_NUMBER
            timeoutThread.get().shouldNotBe(configuredThread)
        }
    }

    @Test
    fun `cancel without an active blocking timeout initializer is a no-op`() = runSuspendIO(timeout = 5.seconds) {
        val callCounter = AtomicInteger()
        val lazyValue = suspendBlockingLazy {
            callCounter.incrementAndGet()
            TEST_NUMBER
        }

        lazyValue.cancel()

        lazyValue.getUntil(1.seconds) shouldBeEqualTo TEST_NUMBER
        callCounter.get() shouldBeEqualTo 1
    }

    @Test
    fun `cancel does not interrupt caller-owned blocking invocation`() = runSuspendIO(timeout = 5.seconds) {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val interrupted = CountDownLatch(1)
        val finished = CountDownLatch(1)
        val lazyValue = suspendBlockingLazy {
            started.countDown()
            try {
                release.await()
                TEST_NUMBER
            } catch (error: InterruptedException) {
                interrupted.countDown()
                throw error
            } finally {
                finished.countDown()
            }
        }
        val invocation = async(Dispatchers.Default) { lazyValue() }

        try {
            withContext(Dispatchers.IO) { started.await(5, TimeUnit.SECONDS).shouldBeTrue() }
            lazyValue.cancel()
            withContext(Dispatchers.IO) {
                interrupted.await(200, TimeUnit.MILLISECONDS).shouldBeFalse()
            }
        } finally {
            release.countDown()
        }

        withTimeout(5.seconds) { invocation.await() } shouldBeEqualTo TEST_NUMBER
        finished.await(5, TimeUnit.SECONDS).shouldBeTrue()
        interrupted.count shouldBeEqualTo 1L
    }

    @Test
    fun `cancel explicitly stops blocking timeout initializer and allows retry`() = runSuspendIO(timeout = 5.seconds) {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val interrupted = CountDownLatch(1)
        val finished = CountDownLatch(1)
        val callCounter = AtomicInteger()
        val lazyValue = suspendBlockingLazy {
            if (callCounter.incrementAndGet() == 1) {
                started.countDown()
                try {
                    release.await()
                } catch (error: InterruptedException) {
                    interrupted.countDown()
                    throw error
                } finally {
                    finished.countDown()
                }
            }
            TEST_NUMBER
        }
        val waiter = async(Dispatchers.Default) {
            runCatching { lazyValue.getUntil(Duration.INFINITE) }
        }

        try {
            withContext(Dispatchers.IO) { started.await(5, TimeUnit.SECONDS).shouldBeTrue() }
            lazyValue.cancel()
            withContext(Dispatchers.IO) { interrupted.await(5, TimeUnit.SECONDS).shouldBeTrue() }

            val result = withTimeout(5.seconds) { waiter.await() }
            (result.exceptionOrNull() is kotlinx.coroutines.CancellationException).shouldBeTrue()
        } finally {
            release.countDown()
        }
        finished.await(5, TimeUnit.SECONDS).shouldBeTrue()
        lazyValue.getUntil(1.seconds) shouldBeEqualTo TEST_NUMBER
        callCounter.get() shouldBeEqualTo 2
    }

    @Test
    fun `blocking timeout initializer that ignores interrupt continues until released`() =
        runSuspendIO(timeout = 5.seconds) {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val interrupted = CountDownLatch(1)
        val finished = CountDownLatch(1)
        val lazyValue = suspendBlockingLazy {
            started.countDown()
            try {
                try {
                    release.await()
                } catch (error: InterruptedException) {
                    interrupted.countDown()
                    release.await()
                }
                TEST_NUMBER
            } finally {
                finished.countDown()
            }
        }
        val waiter = async(Dispatchers.Default) {
            runCatching { lazyValue.getUntil(Duration.INFINITE) }
        }

        try {
            withContext(Dispatchers.IO) { started.await(5, TimeUnit.SECONDS).shouldBeTrue() }
            lazyValue.cancel()
            withContext(Dispatchers.IO) {
                interrupted.await(5, TimeUnit.SECONDS).shouldBeTrue()
                finished.await(200, TimeUnit.MILLISECONDS).shouldBeFalse()
            }
        } finally {
            release.countDown()
        }

        val result = withTimeout(5.seconds) { waiter.await() }
        (result.exceptionOrNull() is kotlinx.coroutines.CancellationException).shouldBeTrue()
        finished.await(5, TimeUnit.SECONDS).shouldBeTrue()
    }

    @Test
    fun `creating blocking lazy with inherited job does not add a child`() = runSuspendIO(timeout = 5.seconds) {
        withTimeout(1.seconds) {
            coroutineScope {
                val parentJob = currentCoroutineContext()[Job]!!
                suspendBlockingLazy(currentCoroutineContext()) { TEST_NUMBER }

                parentJob.children.count() shouldBeEqualTo 0
            }
        }
    }

    @Test
    fun `completed timeout initialization releases inherited job child`() = runSuspendIO(timeout = 5.seconds) {
        withTimeout(4.seconds) {
            coroutineScope {
                val context = currentCoroutineContext()
                val parentJob = context[Job]!!
                val lazyValue = suspendBlockingLazy(context) { TEST_NUMBER }

                lazyValue.getUntil(1.seconds) shouldBeEqualTo TEST_NUMBER
                awaitNoChildren(parentJob)
            }
        }
    }

    @Test
    fun `failed timeout initialization retries with the configured context`() =
        runSuspendIO(timeout = 5.seconds) {
            val callerRequestContext = ThreadLocal<String>()
            val configuredRequestContext = ThreadLocal<String>()
            val firstCallerObserved = AtomicReference<String?>()
            val firstConfiguredObserved = AtomicReference<String?>()
            val retryCallerObserved = AtomicReference<String?>()
            val retryConfiguredObserved = AtomicReference<String?>()
            val attempts = AtomicInteger()

            withTimeout(4.seconds) {
                coroutineScope {
                    val parentContext = currentCoroutineContext()
                    val parentJob = parentContext[Job].shouldNotBeNull()
                    val configuredContext = parentContext + configuredRequestContext.asContextElement("configured")
                    val lazyValue = suspendBlockingLazy(configuredContext) {
                        val attempt = attempts.incrementAndGet()
                        if (attempt == 1) {
                            firstCallerObserved.set(callerRequestContext.get())
                            firstConfiguredObserved.set(configuredRequestContext.get())
                            error("first attempt")
                        }
                        retryCallerObserved.set(callerRequestContext.get())
                        retryConfiguredObserved.set(configuredRequestContext.get())
                        TEST_NUMBER
                    }

                    withContext(callerRequestContext.asContextElement("first")) {
                        assertFailsWith<IllegalStateException> { lazyValue.getUntil(1.seconds) }
                    }
                    firstCallerObserved.get().shouldBeNull()
                    firstConfiguredObserved.get() shouldBeEqualTo "configured"

                    withContext(callerRequestContext.asContextElement("retry")) {
                        lazyValue.getUntil(1.seconds) shouldBeEqualTo TEST_NUMBER
                    }
                    retryCallerObserved.get().shouldBeNull()
                    retryConfiguredObserved.get() shouldBeEqualTo "configured"
                    awaitNoChildren(parentJob)
                }
            }
        }

    @Test
    fun `timeout worker keeps configured context when the first waiter times out`() =
        runSuspendIO(timeout = 5.seconds) {
            val callerRequestContext = ThreadLocal<String>()
            val configuredRequestContext = ThreadLocal<String>()
            val callerContextObserved = AtomicReference<String?>()
            val configuredContextObserved = AtomicReference<String?>()
            val started = CountDownLatch(1)
            val release = CountDownLatch(1)
            val finished = CountDownLatch(1)
            val callCounter = AtomicInteger()
            val parentJob = currentCoroutineContext()[Job].shouldNotBeNull()
            val configuredContext = currentCoroutineContext() + configuredRequestContext.asContextElement("configured")
            val lazyValue = suspendBlockingLazy(configuredContext) {
                callCounter.incrementAndGet()
                callerContextObserved.set(callerRequestContext.get())
                configuredContextObserved.set(configuredRequestContext.get())
                started.countDown()
                try {
                    release.await()
                    TEST_NUMBER
                } finally {
                    finished.countDown()
                }
            }
            val firstWaiter = async(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
                withContext(callerRequestContext.asContextElement("first")) {
                    lazyValue.getUntilOrNull(100.milliseconds)
                }
            }

            try {
                withContext(Dispatchers.IO) { started.await(5, TimeUnit.SECONDS).shouldBeTrue() }
                firstWaiter.await().shouldBeNull()
                callerContextObserved.get().shouldBeNull()
                configuredContextObserved.get() shouldBeEqualTo "configured"

                val secondWaiter = async(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
                    withContext(callerRequestContext.asContextElement("second")) {
                        lazyValue.getUntil(4.seconds)
                    }
                }
                secondWaiter.isCompleted.shouldBeFalse()

                release.countDown()
                secondWaiter.await() shouldBeEqualTo TEST_NUMBER
                callCounter.get() shouldBeEqualTo 1
            } finally {
                release.countDown()
                withContext(Dispatchers.IO) { finished.await(5, TimeUnit.SECONDS).shouldBeTrue() }
            }

            awaitNoChildren(parentJob)
        }

    @Test
    fun `cached blocking lazy invocation skips configured dispatcher`() = runSuspendIO(timeout = 5.seconds) {
        val dispatcher = CountingThreadDispatcher()
        val lazyValue = suspendBlockingLazy(dispatcher) { TEST_NUMBER }

        lazyValue() shouldBeEqualTo TEST_NUMBER
        val dispatchesAfterInitialization = dispatcher.dispatchCount.get()

        withContext(Dispatchers.Default) {
            lazyValue() shouldBeEqualTo TEST_NUMBER
        }

        dispatcher.dispatchCount.get() shouldBeEqualTo dispatchesAfterInitialization
    }

    @Test
    fun `direct invocation waiting for timeout initialization is cancellable`() = runSuspendIO(timeout = 5.seconds) {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val lazyValue = suspendBlockingLazy(Dispatchers.IO) {
            started.countDown()
            release.await()
            TEST_NUMBER
        }
        val timeoutWaiter = async(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
            runCatching { lazyValue.getUntil(Duration.INFINITE) }
        }

        try {
            withContext(Dispatchers.IO) { started.await(5, TimeUnit.SECONDS).shouldBeTrue() }
            val directInvocation = async(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
                lazyValue()
            }
            yield()
            directInvocation.cancel()
            val cancelledBeforeInitializerRelease = withTimeoutOrNull(250.milliseconds) {
                directInvocation.join()
                true
            } ?: false

            release.countDown()
            timeoutWaiter.await().getOrThrow() shouldBeEqualTo TEST_NUMBER
            directInvocation.join()
            cancelledBeforeInitializerRelease.shouldBeTrue()
        } finally {
            release.countDown()
        }
    }

    @Test
    fun `publication lazy mode allows concurrent initializers`() = runSuspendIO(timeout = 5.seconds) {
        val initializersEntered = CountDownLatch(2)
        val release = CountDownLatch(1)
        val initializerCalls = AtomicInteger()
        val lazyValue = suspendBlockingLazy(mode = LazyThreadSafetyMode.PUBLICATION) {
            initializerCalls.incrementAndGet()
            initializersEntered.countDown()
            release.await()
            TEST_NUMBER
        }
        val first = async(Dispatchers.Default) { lazyValue() }
        val second = async(Dispatchers.Default) { lazyValue() }

        val bothInitializersEntered = try {
            withContext(Dispatchers.IO) { initializersEntered.await(1, TimeUnit.SECONDS) }
        } finally {
            release.countDown()
        }
        first.await() shouldBeEqualTo TEST_NUMBER
        second.await() shouldBeEqualTo TEST_NUMBER

        bothInitializersEntered.shouldBeTrue()
        initializerCalls.get() shouldBeEqualTo 2
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `timeout initialization avoids aliased single thread dispatchers`() = runSuspendIO(timeout = 5.seconds) {
        val executor = Executors.newSingleThreadExecutor()
        val initializerDispatcher = executor.asCoroutineDispatcher()
        val waiterDispatcher = initializerDispatcher.limitedParallelism(1)
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val lazyValue = suspendBlockingLazy(initializerDispatcher) {
            started.countDown()
            release.await()
            TEST_NUMBER
        }

        try {
            val waiter = async(waiterDispatcher) {
                runCatching { lazyValue.getUntil(100.milliseconds) }
            }
            withContext(Dispatchers.IO) { started.await(5, TimeUnit.SECONDS).shouldBeTrue() }
            val timedOutBeforeRelease = withContext(Dispatchers.IO) {
                withTimeoutOrNull(1.seconds) { waiter.await() }
            }

            release.countDown()
            val finalResult = withTimeout(5.seconds) { waiter.await() }
            (timedOutBeforeRelease?.exceptionOrNull() is kotlinx.coroutines.TimeoutCancellationException)
                .shouldBeTrue()
            (finalResult.exceptionOrNull() is kotlinx.coroutines.TimeoutCancellationException).shouldBeTrue()
        } finally {
            release.countDown()
            lazyValue.cancel()
            initializerDispatcher.close()
        }
    }

    @Test
    fun `timeout waiters share initialization and keep independent deadlines`() = runSuspendIO(timeout = 5.seconds) {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val callCounter = AtomicInteger()
        val lazyValue = suspendBlockingLazy {
            callCounter.incrementAndGet()
            started.countDown()
            release.await()
            TEST_NUMBER
        }
        val shortWaiter = async(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
            runCatching { lazyValue.getUntil(100.milliseconds) }
        }
        val longWaiter = async(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
            runCatching { lazyValue.getUntil(5.seconds) }
        }

        try {
            withContext(Dispatchers.IO) { started.await(5, TimeUnit.SECONDS).shouldBeTrue() }
            val shortResult = withTimeout(1.seconds) { shortWaiter.await() }
            (shortResult.exceptionOrNull() is kotlinx.coroutines.TimeoutCancellationException).shouldBeTrue()
            longWaiter.isCompleted.shouldBeFalse()
        } finally {
            release.countDown()
        }

        longWaiter.await().getOrThrow() shouldBeEqualTo TEST_NUMBER
        callCounter.get() shouldBeEqualTo 1
    }

    private class CountingThreadDispatcher: CoroutineDispatcher() {
        val dispatchCount = AtomicInteger()

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            dispatchCount.incrementAndGet()
            Dispatchers.IO.dispatch(context, block)
        }
    }


    private suspend fun awaitNoChildren(parentJob: Job) {
        repeat(JOB_CHILD_COMPLETION_CHECK_ATTEMPTS) {
            if (parentJob.children.none()) return
            yield()
        }
        parentJob.children.count() shouldBeEqualTo 0
    }
}
