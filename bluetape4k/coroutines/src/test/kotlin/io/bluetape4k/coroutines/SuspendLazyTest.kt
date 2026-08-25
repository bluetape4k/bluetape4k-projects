package io.bluetape4k.coroutines

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBe
import io.bluetape4k.assertions.shouldNotBe
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.coroutines.withSingleThread
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import io.bluetape4k.utils.Runtimex
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

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
            log.trace { "Calculate lazy value in blocking mode with IO dispatchers" }
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
}
