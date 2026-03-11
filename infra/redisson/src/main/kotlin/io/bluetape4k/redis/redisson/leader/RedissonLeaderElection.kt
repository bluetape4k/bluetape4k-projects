package io.bluetape4k.redis.redisson.leader

import io.bluetape4k.concurrent.failedCompletableFutureOf
import io.bluetape4k.leader.LeaderElection
import io.bluetape4k.leader.LeaderElectionOptions
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.support.requireNotBlank
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import org.redisson.client.RedisException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit

/**
 * Redisson 분산 락을 이용하여 여러 프로세스/스레드 중 단 하나만 작업을 수행하도록 리더를 선출합니다.
 *
 * ## 동작
 * - [runIfLeader]: 동기 방식으로 락을 획득한 뒤 [LeaderElection.runIfLeader]를 실행하고, 완료 후 락을 해제합니다.
 * - [runAsyncIfLeader]: `tryLockAsync`로 비동기 락을 획득하고, `CompletableFuture` 완료 시 [RLock.unlockAsync]로 락을 해제합니다.
 * - 락 획득 실패 시 [org.redisson.client.RedisException]을 던집니다.
 *
 * ```kotlin
 * val election = RedissonLeaderElection(redissonClient)
 * val result = election.runIfLeader("my-job") {
 *     // 리더로 선출된 경우에만 실행
 *     processData()
 * }
 * ```
 *
 * @param redissonClient Redisson 클라이언트
 * @param options 리더 선출 옵션 (waitTime, leaseTime)
 * @see RedissonSuspendLeaderElection Coroutine 환경에서 사용할 suspend 버전
 */
class RedissonLeaderElection private constructor(
    private val redissonClient: RedissonClient,
    options: LeaderElectionOptions,
): LeaderElection {

    companion object: KLogging() {
        @JvmStatic
        operator fun invoke(
            redissonClient: RedissonClient,
            options: LeaderElectionOptions = LeaderElectionOptions.Default,
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

    /**
     * 락을 보유한 상태에서 비동기 [action]을 실행하고, 완료(성공/실패) 후 락을 해제합니다.
     *
     * ## 동작 상세
     * - [action] 호출 자체가 예외를 던지면 즉시 실패한 [CompletableFuture]를 반환합니다.
     * - [action]이 반환한 [CompletableFuture] 완료 시 `whenCompleteAsync`에서 락을 비동기 해제합니다.
     * - 락 해제는 [currentThreadId]로 식별합니다. 이는 Redisson이 락을 스레드 단위로 관리하기 때문이며,
     *   비동기 컨텍스트에서 락 획득 시 사용한 스레드 ID와 동일한 값으로 해제해야 합니다.
     * - 락 해제 실패 시 예외를 로그에만 기록하고 [action] 결과에는 영향을 주지 않습니다.
     *
     * @param lock 해제 대상 [RLock]
     * @param currentThreadId 락 획득 시 사용한 스레드 ID ([Thread.currentThread.threadId] 기준)
     * @param executor 완료 콜백을 실행할 [Executor]
     * @param action 실행할 비동기 작업
     * @return [action] 실행 결과를 담은 [CompletableFuture]
     */
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


/**
 * Redisson 분산 락을 이용하여 리더 선출을 통한 작업을 수행합니다.
 *
 * ```
 * val client: RedissonClient = ...
 * val result: Int = client.runIfLeader("jobName") {
 *    // 리더로 선출되었을 때 수행할 작업
 *    ...
 *    42
 * }
 * // result is 42
 * ```
 *
 * @param jobName 작업 이름
 * @param options 리더 선출 옵션
 * @param action 리더로 선출되었을 때 수행할 작업
 * @return 작업 결과
 */
inline fun <T> RedissonClient.runIfLeader(
    jobName: String,
    options: LeaderElectionOptions = LeaderElectionOptions.Default,
    crossinline action: () -> T,
): T {
    jobName.requireNotBlank("jobName")
    val leaderElection = RedissonLeaderElection(this, options)
    return leaderElection.runIfLeader(jobName) { action() }
}

/**
 * Redisson 분산 락을 이용하여 리더 선출을 통한 비동기 작업을 수행합니다.
 *
 * ```
 * val client: RedissonClient = ...
 * val result:CompletalbeFuture<Int> = client.runAsyncIfLeader("jobName") {
 *   // 리더로 선출되었을 때 수행할 작업
 *   futureOf {
 *      ...
 *      // 작업 결과
 *      42
 *   }
 * }
 * // result.get() is 42
 * ```
 *
 * @param jobName 작업 이름
 * @param executor 작업을 수행할 Executor
 * @param options 리더 선출 옵션
 * @param action 리더로 선출되었을 때 수행할 비동기 작업
 * @return 작업 결과를 담은 []CompletableFuture] 인스턴스
 */
inline fun <T> RedissonClient.runAsyncIfLeader(
    jobName: String,
    executor: Executor = ForkJoinPool.commonPool(),
    options: LeaderElectionOptions = LeaderElectionOptions.Default,
    crossinline action: () -> CompletableFuture<T>,
): CompletableFuture<T> {
    jobName.requireNotBlank("jobName")
    val leaderElection = RedissonLeaderElection(this, options)
    return leaderElection.runAsyncIfLeader(jobName, executor) { action() }
}
