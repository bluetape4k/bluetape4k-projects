package io.bluetape4k.examples.redisson.coroutines.objects

import io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.future.await
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
    fun `AtomicLog operatiions`() = runSuspendIO {
        val counter = redisson.getAtomicLong(randomName())

        counter.setAsync(0).await()
        counter.addAndGetAsync(10L).await() shouldBeEqualTo 10L

        counter.compareAndSetAsync(-1L, 42L).await().shouldBeFalse()
        counter.compareAndSetAsync(10L, 42L).await().shouldBeTrue()

        counter.decrementAndGetAsync().await() shouldBeEqualTo 41L
        counter.incrementAndGetAsync().await() shouldBeEqualTo 42L

        counter.getAndAddAsync(3L).await() shouldBeEqualTo 42L

        counter.getAndDecrementAsync().await() shouldBeEqualTo 45L
        counter.getAndIncrementAsync().await() shouldBeEqualTo 44L

        counter.deleteAsync().await().shouldBeTrue()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `AtomicLong in Coroutines`() = runSuspendIO {
        val counter = redisson.getAtomicLong(randomName())

        SuspendedJobTester()
            .workers(4)
            .rounds(32 * 8)
            .add {
                counter.incrementAndGetAsync().await()
            }
            .run()

        counter.async.await() shouldBeEqualTo 32 * 8L
        counter.deleteAsync().await().shouldBeTrue()
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

    @EnabledOnJre(JRE.JAVA_21, JRE.JAVA_25)
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
