package io.bluetape4k.examples.redisson.coroutines.objects

import io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.coroutines.MultijobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.coroutines.coAwait
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.RepeatedTest

class AtomicLongExamples: AbstractRedissonCoroutineTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 3
        private const val TEST_COUNT = 1000
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `AtomicLog in coroutines`() = runSuspendIO {
        val counter = redisson.getAtomicLong(randomName())
        val jobs = List(TEST_COUNT) {
            scope.launch {
                counter.incrementAndGetAsync().coAwait()
            }
        }
        jobs.joinAll()

        counter.async.coAwait() shouldBeEqualTo TEST_COUNT.toLong()
        counter.deleteAsync().coAwait().shouldBeTrue()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `AtomicLog operatiions`() = runSuspendIO {
        val counter = redisson.getAtomicLong(randomName())

        counter.setAsync(0).coAwait()
        counter.addAndGetAsync(10L).coAwait() shouldBeEqualTo 10L

        counter.compareAndSetAsync(-1L, 42L).coAwait().shouldBeFalse()
        counter.compareAndSetAsync(10L, 42L).coAwait().shouldBeTrue()

        counter.decrementAndGetAsync().coAwait() shouldBeEqualTo 41L
        counter.incrementAndGetAsync().coAwait() shouldBeEqualTo 42L

        counter.getAndAddAsync(3L).coAwait() shouldBeEqualTo 42L

        counter.getAndDecrementAsync().coAwait() shouldBeEqualTo 45L
        counter.getAndIncrementAsync().coAwait() shouldBeEqualTo 44L

        counter.deleteAsync().coAwait().shouldBeTrue()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `AtomicLong in Multi job`() = runSuspendIO {
        val counter = redisson.getAtomicLong(randomName())

        MultijobTester()
            .numThreads(8)
            .roundsPerJob(32)
            .add {
                counter.incrementAndGetAsync().coAwait()
            }
            .run()

        counter.async.coAwait() shouldBeEqualTo 8 * 32L
        counter.deleteAsync().coAwait().shouldBeTrue()
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
}
