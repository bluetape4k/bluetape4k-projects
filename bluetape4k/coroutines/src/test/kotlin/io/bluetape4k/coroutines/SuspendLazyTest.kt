package io.bluetape4k.coroutines

import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import io.bluetape4k.utils.Runtimex
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.random.Random

class SuspendLazyTest {

    companion object: KLoggingChannel() {
        private const val TEST_NUMBER = 42
    }

    @Test
    fun `get suspend lazy value in coroutine scope`() = runTest {
        val callCounter = atomic(0)

        val lazyValue = suspendLazy {
            delay(Random.nextLong(100))
            log.trace { "Calculate lazy value in non-blocking mode." }
            callCounter.incrementAndGet()
            TEST_NUMBER
        }
        callCounter.value shouldBeEqualTo 0

        yield()

        lazyValue() shouldBeEqualTo TEST_NUMBER
        lazyValue() shouldBeEqualTo TEST_NUMBER

        callCounter.value shouldBeEqualTo 1
    }

    @Test
    fun `get suspend lazy value in coroutine scope with Multijob`() = runTest {
        val callCounter = atomic(0)

        val lazyValue = suspendLazy {
            delay(Random.nextLong(100))
            log.trace { "Calculate lazy value in non-blocking mode." }
            callCounter.incrementAndGet()
            TEST_NUMBER
        }
        callCounter.value shouldBeEqualTo 0

        SuspendedJobTester()
            .numThreads(Runtimex.availableProcessors)
            .roundsPerJob(16)
            .add {
                lazyValue() shouldBeEqualTo TEST_NUMBER
            }
            .add {
                lazyValue() shouldBeEqualTo TEST_NUMBER
            }
            .run()

        callCounter.value shouldBeEqualTo 1
    }

    @Test
    fun `get lazy value in blocking mode`() = runTest {
        val callCounter = atomic(0)

        val lazyValue = suspendBlockingLazy {
            Thread.sleep(Random.nextLong(100))
            log.trace { "Calculate lazy value in blocking mode." }
            callCounter.incrementAndGet()
            TEST_NUMBER
        }
        callCounter.value shouldBeEqualTo 0

        yield()

        lazyValue() shouldBeEqualTo TEST_NUMBER
        lazyValue() shouldBeEqualTo TEST_NUMBER

        callCounter.value shouldBeEqualTo 1
    }

    @Test
    fun `get lazy value in blocking mode with IO dispatchers`() = runTest {
        val callCounter = atomic(0)

        val lazyValue = suspendBlockingLazyIO {
            Thread.sleep(Random.nextLong(100))
            log.trace { "Calculate lazy value in blocking mode with IO dispatchers" }
            callCounter.incrementAndGet()
            TEST_NUMBER
        }
        callCounter.value shouldBeEqualTo 0

        yield()

        val lazy1 = async { lazyValue() }
        val lazy2 = async { lazyValue() }

        yield()

        lazy1.await() shouldBeEqualTo TEST_NUMBER
        lazy2.await() shouldBeEqualTo TEST_NUMBER

        callCounter.value shouldBeEqualTo 1
    }

    @Test
    fun `get lazy value in blocking mode with Multijob`() {
        val callCounter = atomic(0)

        val lazyValue = suspendBlockingLazyIO {
            Thread.sleep(Random.nextLong(1000))
            log.trace { "Calculate lazy value in blocking mode with IO dispatchers" }
            callCounter.incrementAndGet()
            TEST_NUMBER
        }
        callCounter.value shouldBeEqualTo 0

        runTest {
            SuspendedJobTester()
                .numThreads(Runtimex.availableProcessors)
                .roundsPerJob(16)
                .add {
                    lazyValue() shouldBeEqualTo TEST_NUMBER
                }
                .run()
        }
        callCounter.value shouldBeEqualTo 1
    }
}
