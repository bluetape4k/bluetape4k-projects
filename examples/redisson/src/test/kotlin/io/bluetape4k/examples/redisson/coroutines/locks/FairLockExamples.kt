package io.bluetape4k.examples.redisson.coroutines.locks

import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.coroutines.support.awaitSuspending
import io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.redis.redisson.coroutines.getLockId
import io.bluetape4k.utils.Runtimex
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

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

    companion object: KLoggingChannel()

    @Test
    fun `acquire fair lock`() = runSuspendIO {
        val lock = redisson.getFairLock(randomName())
        val lockCounter = AtomicInteger(0)
        val size = 10

        val jobs = fastList(size) {
            scope.launch {
                // 여러 Thread 가 lock을 요청하면, 요청 순서대로 lock 을 제공하는 것을 보장합니다.
                // 나머지 요청은 최대 5초간 대기하다가 요청 중단된다
                val lockId = redisson.getLockId(lock.name)
                log.trace { "lockId=$lockId" }
                val locked = lock.tryLockAsync(5, 10, TimeUnit.SECONDS, lockId).awaitSuspending()
                if (locked) {
                    lockCounter.incrementAndGet()
                }
                delay(10)
                lock.unlockAsync(lockId).awaitSuspending()
            }
        }
        jobs.joinAll()
        lockCounter.get() shouldBeEqualTo size
    }

    @Test
    fun `멀티 스레딩 환경에서 Fair Lock 을 제대로 획득합니다`() {
        val lock = redisson.getFairLock(randomName())
        val lockCounter = AtomicInteger(0)
        val lockIndex = AtomicInteger(0)

        MultithreadingTester()
            .workers(Runtimex.availableProcessors * 2)
            .rounds(2)
            .add {
                val index = lockIndex.incrementAndGet()
                log.debug { "FairLock[$index] 획득 시도 ..." }
                val locked = lock.tryLock(5, 10, TimeUnit.SECONDS)
                if (locked) {
                    log.debug { "FairLock[$index] 획득 성공 ..." }
                    lockCounter.incrementAndGet()
                    Thread.sleep(10)
                }
                // Thread.sleep(10)
                log.debug { "FairLock[$index] 해제 ..." }
                lock.unlock()
            }
            .run()

        lockCounter.get() shouldBeEqualTo Runtimex.availableProcessors * 2 * 2
    }

    @EnabledOnJre(JRE.JAVA_21)
    @Test
    fun `Virtual threads 에서 Fair Lock 을 제대로 획득합니다`() {
        val lock = redisson.getFairLock(randomName())
        val lockCounter = AtomicInteger(0)
        val lockIndex = AtomicInteger(0)

        StructuredTaskScopeTester()
            .rounds(Runtimex.availableProcessors * 4)
            .add {
                val index = lockIndex.incrementAndGet()
                log.debug { "FairLock[$index] 획득 시도 ..." }
                val locked = lock.tryLock(5, 10, TimeUnit.SECONDS)
                if (locked) {
                    log.debug { "FairLock[$index] 획득 성공 ..." }
                    lockCounter.incrementAndGet()
                    Thread.sleep(10)
                }
                // Thread.sleep(10)
                log.debug { "FairLock[$index] 해제 ..." }
                lock.unlock()
            }
            .run()

        lockCounter.get() shouldBeEqualTo Runtimex.availableProcessors * 4
    }

    @Test
    fun `코루틴 환경에서 Fair Lock 을 제대로 획득합니다`() = runSuspendIO {
        val lock = redisson.getFairLock(randomName())
        val lockCounter = AtomicInteger(0)
        val lockIndex = AtomicInteger(0)

        SuspendedJobTester()
            .workers(Runtimex.availableProcessors * 2)
            .rounds(Runtimex.availableProcessors * 4)
            .add {
                val lockId = redisson.getLockId(lock.name)
                val index = lockIndex.incrementAndGet()

                log.debug { "FairLock[$index] 획득 시도 ..." }
                val locked = lock.tryLockAsync(5, 10, TimeUnit.SECONDS, lockId).awaitSuspending()
                if (locked) {
                    log.debug { "FairLock[$index] 획득 성공 ..." }
                    lockCounter.incrementAndGet()
                    Thread.sleep(10)
                }
                // Thread.sleep(10)
                log.debug { "FairLock[$index] 해제 ..." }
                lock.unlockAsync(lockId).awaitSuspending()
            }
            .run()

        lockCounter.get() shouldBeEqualTo Runtimex.availableProcessors * 4
    }

    /**
     * FairLock은 요청 순서대로 락을 획득합니다.
     *
     * 여러 코루틴에서 동시에 FairLock 획득 시도
     * 코루틴 시작 시점 차이 주기
     * 락 획득한 순서 기록
     * 임의의 작업 수행
     * 락 해제
     */
    @Test
    fun `FairLock은 요청 순서대로 락을 획득합니다`() = runSuspendIO {
        val lockName = randomName()
        val counter = AtomicInteger(0)
        val executionOrder = CopyOnWriteArrayList<Int>()

        // 여러 코루틴에서 동시에 FairLock 획득 시도
        val jobs = fastList(5) { index ->
            scope.launch(exceptionHandler) {
                val fairLock = redisson.getFairLock(lockName)
                val lockId = redisson.getLockId("fair-$index")

                delay(10 * index.toLong()) // 코루틴 시작 시점 차이 주기
                log.debug { "코루틴 $index 에서 FairLock 획득 시도 시작" }
                fairLock.tryLockAsync(10, 60, TimeUnit.SECONDS, lockId).awaitSuspending().shouldBeTrue()

                try {
                    // 락 획득한 순서 기록
                    val order = counter.incrementAndGet()
                    executionOrder.add(index)
                    log.debug { "코루틴 $index 에서 FairLock 획득 성공 (순서: $order)" }

                    // 임의의 작업 수행
                    delay(100)
                } finally {
                    log.debug { "코루틴 $index 에서 FairLock 해제" }
                    fairLock.unlockAsync(lockId).awaitSuspending()
                }
            }
        }

        jobs.joinAll()
        log.debug { "실행 순서: $executionOrder" }
        // 실행 순서가 0, 1, 2, 3, 4 순서대로 처리되었는지 확인
        executionOrder shouldBeEqualTo listOf(0, 1, 2, 3, 4)
    }
}
