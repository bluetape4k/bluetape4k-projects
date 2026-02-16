package io.bluetape4k.examples.redisson.coroutines.objects

import io.bluetape4k.coroutines.support.awaitSuspending
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
                counter.incrementAndGetAsync().awaitSuspending()
            }
        }
        jobs.joinAll()

        counter.async.awaitSuspending() shouldBeEqualTo TEST_COUNT.toLong()
        counter.deleteAsync().awaitSuspending().shouldBeTrue()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `AtomicLog operatiions`() = runSuspendIO {
        val counter = redisson.getAtomicLong(randomName())

        counter.setAsync(0).awaitSuspending()
        counter.addAndGetAsync(10L).awaitSuspending() shouldBeEqualTo 10L

        counter.compareAndSetAsync(-1L, 42L).awaitSuspending().shouldBeFalse()
        counter.compareAndSetAsync(10L, 42L).awaitSuspending().shouldBeTrue()

        counter.decrementAndGetAsync().awaitSuspending() shouldBeEqualTo 41L
        counter.incrementAndGetAsync().awaitSuspending() shouldBeEqualTo 42L

        counter.getAndAddAsync(3L).awaitSuspending() shouldBeEqualTo 42L

        counter.getAndDecrementAsync().awaitSuspending() shouldBeEqualTo 45L
        counter.getAndIncrementAsync().awaitSuspending() shouldBeEqualTo 44L

        counter.deleteAsync().awaitSuspending().shouldBeTrue()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `AtomicLong in Coroutines`() = runSuspendIO {
        val counter = redisson.getAtomicLong(randomName())

        SuspendedJobTester()
            .workers(Runtimex.availableProcessors)
            .rounds(32 * 8)
            .add {
                counter.incrementAndGetAsync().awaitSuspending()
            }
            .run()

        counter.async.awaitSuspending() shouldBeEqualTo 32 * 8L
        counter.deleteAsync().awaitSuspending().shouldBeTrue()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `AtomicLong in Multi threading`() {
        val counter = redisson.getAtomicLong(randomName())

        MultithreadingTester()
            .workers(32)
            .rounds(8)
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
            .rounds(32 * 8)
            .add {
                counter.incrementAndGet()
            }
            .run()

        counter.get() shouldBeEqualTo 32 * 8L
        counter.delete().shouldBeTrue()
    }
}
