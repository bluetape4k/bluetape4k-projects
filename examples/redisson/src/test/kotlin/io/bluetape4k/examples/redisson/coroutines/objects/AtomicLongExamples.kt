package io.bluetape4k.examples.redisson.coroutines.objects

import io.bluetape4k.coroutines.support.suspendAwait
import io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.utils.Runtimex
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE

class AtomicLongExamples: AbstractRedissonCoroutineTest() {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 3
        private const val TEST_COUNT = 1000
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `AtomicLog in coroutines`() = runSuspendIO {
        val counter = redisson.getAtomicLong(randomName())
        val jobs = List(TEST_COUNT) {
            scope.launch {
                counter.incrementAndGetAsync().suspendAwait()
            }
        }
        jobs.joinAll()

        counter.async.suspendAwait() shouldBeEqualTo TEST_COUNT.toLong()
        counter.deleteAsync().suspendAwait().shouldBeTrue()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `AtomicLog operatiions`() = runSuspendIO {
        val counter = redisson.getAtomicLong(randomName())

        counter.setAsync(0).suspendAwait()
        counter.addAndGetAsync(10L).suspendAwait() shouldBeEqualTo 10L

        counter.compareAndSetAsync(-1L, 42L).suspendAwait().shouldBeFalse()
        counter.compareAndSetAsync(10L, 42L).suspendAwait().shouldBeTrue()

        counter.decrementAndGetAsync().suspendAwait() shouldBeEqualTo 41L
        counter.incrementAndGetAsync().suspendAwait() shouldBeEqualTo 42L

        counter.getAndAddAsync(3L).suspendAwait() shouldBeEqualTo 42L

        counter.getAndDecrementAsync().suspendAwait() shouldBeEqualTo 45L
        counter.getAndIncrementAsync().suspendAwait() shouldBeEqualTo 44L

        counter.deleteAsync().suspendAwait().shouldBeTrue()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `AtomicLong in Coroutines`() = runSuspendIO {
        val counter = redisson.getAtomicLong(randomName())

        SuspendedJobTester()
            .numThreads(Runtimex.availableProcessors)
            .roundsPerJob(32 * 8)
            .add {
                counter.incrementAndGetAsync().suspendAwait()
            }
            .run()

        counter.async.suspendAwait() shouldBeEqualTo 32 * 8L
        counter.deleteAsync().suspendAwait().shouldBeTrue()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `AtomicLong in Multi threading`() {
        val counter = redisson.getAtomicLong(randomName())

        MultithreadingTester()
            .numThreads(32)
            .roundsPerThread(8)
            .add {
                counter.incrementAndGet()
            }
            .run()

        counter.get() shouldBeEqualTo 32 * 8L
        counter.delete().shouldBeTrue()
    }

    @EnabledOnJre(JRE.JAVA_21)
    @RepeatedTest(REPEAT_SIZE)
    fun `AtomicLong in Virtual threads`() {
        val counter = redisson.getAtomicLong(randomName())

        StructuredTaskScopeTester()
            .roundsPerTask(32 * 8)
            .add {
                counter.incrementAndGet()
            }
            .run()

        counter.get() shouldBeEqualTo 32 * 8L
        counter.delete().shouldBeTrue()
    }
}
