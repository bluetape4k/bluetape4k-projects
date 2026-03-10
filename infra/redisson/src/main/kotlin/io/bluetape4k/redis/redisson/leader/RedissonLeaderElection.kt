package io.bluetape4k.redis.redisson.leader

import io.bluetape4k.concurrent.failedCompletableFutureOf
import io.bluetape4k.leader.LeaderElection
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.support.requireNotBlank
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import org.redisson.client.RedisException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

/**
 * 여러 Process, Thread에서 같은 작업이 동시, 무작위로 실행되는 것을 방지하기 위해
 * Redisson Lock을 이용하여 Leader를 선출되면 독점적으로 작업할 수 있도록 합니다.
 */
class RedissonLeaderElection private constructor(
    private val redissonClient: RedissonClient,
    options: RedissonLeaderElectionOptions,
): LeaderElection {

    companion object: KLogging() {
        @JvmStatic
        operator fun invoke(
            redissonClient: RedissonClient,
            options: RedissonLeaderElectionOptions = RedissonLeaderElectionOptions.Default,
        ): RedissonLeaderElection {
            return RedissonLeaderElection(redissonClient, options)
        }
    }

    private val waitTimeMills = options.waitTime.toMillis()
    private val leaseTimeMills = options.leaseTime.toMillis()

    /**
     * Redisson Lock을 이용하여, 리더로 선출되면 [action]을 수행하고, 그렇지 않다면 수행하지 않습니다.
     *
     * @param lockName lock name - lock 획득에 성공하면 leader로 승격되는 것이다.
     * @param action leader 로 승격되면 수행할 코드 블럭
     * @return 작업 결과
     */
    override fun <T> runIfLeader(lockName: String, action: () -> T): T {
        lockName.requireNotBlank("lockName")

        val lock: RLock = redissonClient.getLock(lockName)

        log.debug { "Leader 승격을 요청합니다 ..." }

        try {
            val acquired = lock.tryLock(waitTimeMills, leaseTimeMills, TimeUnit.MILLISECONDS)
            if (acquired) {
                log.debug { "Leader로 승격하여 작업을 수행합니다. lock=$lockName" }
                try {
                    return action()
                } finally {
                    if (lock.isHeldByCurrentThread) {
                        runCatching {
                            lock.unlock()
                            log.debug { "작업이 완료되어 Leader 권한을 반납했습니다. lock=$lockName" }
                        }
                    }
                }
            } else {
                throw RedisException("Fail to acquire lock. lock=$lockName")
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            log.error(e) { "Fail to run as leader" }
            throw RedisException("Interrupted while acquiring lock. lock=$lockName", e)
        }
    }

    /**
     * Redisson Lock을 이용하여, 리더로 선출되면 [action]을 수행하고, 그렇지 않다면 수행하지 않습니다.
     *
     * @param lockName lock name - lock 획득에 성공하면 leader로 승격되는 것이다.
     * @param executor 작업이 수행될 executor
     * @param action leader 로 승격되면 수행할 코드 블럭
     * @return 작업 결과
     */
    override fun <T> runAsyncIfLeader(
        lockName: String,
        executor: Executor,
        action: () -> CompletableFuture<T>,
    ): CompletableFuture<T> {
        lockName.requireNotBlank("lockName")

        val lock: RLock = redissonClient.getLock(lockName)

        try {
            val currentThreadId = Thread.currentThread().threadId()
            log.debug { "Leader 승격을 요청합니다 ... lock=$lockName, currentThreadId=$currentThreadId" }

            return lock
                .tryLockAsync(waitTimeMills, leaseTimeMills, TimeUnit.MILLISECONDS, currentThreadId)
                .thenComposeAsync({ acquired ->
                    if (acquired) {
                        executeActionAsync(lock, currentThreadId, executor, action)
                    } else {
                        failedCompletableFutureOf(RedisException("Fail to acquire lock. lock=$lockName"))
                    }
                }, executor)
                .toCompletableFuture()

        } catch (e: Throwable) {
            log.error(e) { "Fail to runAsync as Leader" }
            return failedCompletableFutureOf(e)
        }
    }

    private inline fun <T> executeActionAsync(
        lock: RLock,
        currentThreadId: Long,
        executor: Executor,
        action: () -> CompletableFuture<T>,
    ): CompletableFuture<T> {
        val lockName = lock.name
        log.debug { "Leader로 승격하여 비동기 작업을 수행합니다. lock=$lockName, threadId=$currentThreadId" }

        val actionFuture = runCatching { action() }
            .getOrElse { return failedCompletableFutureOf(it) }

        return actionFuture.whenCompleteAsync({ _, _ ->
            if (lock.isHeldByThread(currentThreadId)) {
                lock
                    .unlockAsync(currentThreadId)
                    .whenComplete { _, error ->
                        if (error != null) {
                            log.error(error) { "Fail to release lock. lock=$lockName, threadId=$currentThreadId" }
                        } else {
                            log.debug { "Leader 권한을 반납했습니다. lock=$lockName, threadId=$currentThreadId" }
                        }
                    }
            }
        }, executor)
    }
}
