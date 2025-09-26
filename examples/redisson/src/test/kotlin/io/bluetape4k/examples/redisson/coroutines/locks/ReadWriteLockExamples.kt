package io.bluetape4k.examples.redisson.coroutines.locks

import io.bluetape4k.coroutines.support.suspendAwait
import io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.redisson.coroutines.getLockId
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit


/**
 * Redisson ReadWriteLock example
 *
 * Redis based distributed reentrant ReadWriteLock object for Java implements ReadWriteLock interface.
 * Both Read and Write locks implement RLock interface.
 *
 * Multiple ReadLock owners and only one WriteLock owner are allowed.
 *
 * 참고: [ReadWriteLock](https://github.com/redisson/redisson/wiki/8.-Distributed-locks-and-synchronizers#85-readwritelock)
 *
 */
class ReadWriteLockExamples: AbstractRedissonCoroutineTest() {

    companion object: KLoggingChannel()

    @Test
    fun `하나의 WriteLock을 잡고, 여러개의 ReadLock을 잡을 수 있습니다`() = runSuspendIO {
        val lockName = randomName()
        val lock = redisson.getReadWriteLock(lockName)

        // Write Lock 획득
        log.debug { "WriteLock을 획득합니다... threadId=${Thread.currentThread().threadId()}" }
        val writeLock = lock.writeLock()
        val writeLockId = redisson.getLockId(lockName)
        writeLock.tryLockAsync(1, 60, TimeUnit.SECONDS, writeLockId).suspendAwait().shouldBeTrue()

        delay(1000)

        scope.launch(exceptionHandler) {
            log.debug { "WriteLock이 걸린 상태에서 ReadLock 을 획득 시도합니다... -> 실패해야 합니다. ${Thread.currentThread().threadId()}" }
            val readLock = lock.readLock()
            val readLockId = redisson.getLockId(lockName)
            readLock.tryLockAsync(1, 60, TimeUnit.SECONDS, readLockId).suspendAwait().shouldBeFalse()
        }.join()

        log.debug { "WriteLock을 반납합니다... threadId=${Thread.currentThread().threadId()}" }
        writeLock.unlockAsync(writeLockId).suspendAwait()

        // read lock 을 여러 개를 동시에 획득, 작업을 수행
        val jobs = List(3) {
            scope.launch(exceptionHandler) {
                // 기본적으로 current thread id 기준으로 lock을 잡는데, suspend 후에는 변경될 수 있습니다.
                // currentCoroutineId 는 같은 CoroutineScope에서는 같기 때문에 CorouineScope 안에서는 currentCoroutineId 를 사용해야 합니다.
                val readLockId = redisson.getLockId(lockName)
                log.debug { "ReadLock을 획득합니다... $it:${Thread.currentThread().threadId()}, readLockId=$readLockId" }
                val readLock = lock.readLock()

                readLock.tryLockAsync(1, 60, TimeUnit.SECONDS, readLockId).suspendAwait().shouldBeTrue()

                try {
                    log.debug { "Some suspending job" }
                    delay(it * 500L)

                    log.debug { "ReadLock을 반납합니다... $it:${Thread.currentThread().threadId()}, readLockId=$readLockId" }
                    readLock.isLocked.shouldBeTrue()

                } finally {
                    // 같은 coroutine context id 에서만 unlock 이 가능하다
                    readLock.unlockAsync(readLockId).suspendAwait()
                }
            }
        }
        jobs.joinAll()
    }
}
