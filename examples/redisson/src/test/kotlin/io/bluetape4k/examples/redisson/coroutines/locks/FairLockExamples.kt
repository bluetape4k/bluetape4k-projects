package io.bluetape4k.examples.redisson.coroutines.locks

import io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.redis.redisson.coroutines.coAwait
import io.bluetape4k.redis.redisson.coroutines.getLockId
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit


/**
 * Fair Lock
 *
 * Redis based distributed reentrant fair Lock object for Java implements Lock interface.
 * Fair lock guarantees that threads will acquire it in is same order they requested it.
 * All waiting threads are queued and if some thread has died then Redisson waits its return for 5 seconds.
 * For example, if 5 threads are died for some reason then delay will be 25 seconds.
 *
 * 참고: [FairLock](https://github.com/redisson/redisson/wiki/8.-distributed-locks-and-synchronizers#82-fair-lock)
 */
class FairLockExamples: AbstractRedissonCoroutineTest() {

    companion object: KLogging()

    @Test
    fun `acquire fair lock`() = runSuspendIO {
        val lock = redisson.getFairLock(randomName())
        val lockCounter = atomic(0)

        val size = 10

        val jobs = List(size) {
            scope.launch {
                // 여러 Thread 가 lock을 요청하면, 요청 순서대로 lock 을 제공하는 것을 보장합니다.
                // 나머지 요청은 최대 5초간 대기하다가 요청 중단된다
                val lockId = redisson.getLockId(lock.name)
                log.trace { "lockId=$lockId" }
                val locked = lock.tryLockAsync(5, 10, TimeUnit.SECONDS, lockId).coAwait()
                if (locked) {
                    lockCounter.incrementAndGet()
                }
                delay(10)
                lock.unlockAsync(lockId).coAwait()
            }
        }
        jobs.joinAll()
        lockCounter.value shouldBeEqualTo size
    }

    @Test
    fun `acquire fair lock in multi threading`() {
        val lock = redisson.getFairLock(randomName())
        val lockCounter = atomic(0)

        MultithreadingTester()
            .numThreads(16)
            .roundsPerThread(2)
            .add {
                val locked = lock.tryLock(5, 10, TimeUnit.SECONDS)
                if (locked) {
                    lockCounter.incrementAndGet()
                }
                Thread.sleep(10)
                lock.unlock()
            }
            .run()

        lockCounter.value shouldBe 32
    }
}
