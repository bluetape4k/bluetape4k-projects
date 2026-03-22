package io.bluetape4k.redis.redisson.leader

import io.bluetape4k.coroutines.support.awaitSuspending
import io.bluetape4k.leader.LeaderElectionOptions
import io.bluetape4k.leader.coroutines.SuspendLeaderElection
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.bluetape4k.redis.redisson.coroutines.getLockId
import io.bluetape4k.support.requireNotBlank
import kotlinx.coroutines.future.await
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import org.redisson.client.RedisException
import java.util.concurrent.TimeUnit

/**
 * Redisson 분산 락을 이용하여 리더 선출을 통한 suspend 작업을 실행합니다.
 *
 * 락 획득에 성공하면 [action]을 실행하고, 완료 후 락을 해제합니다.
 * 락 획득 실패 시 [org.redisson.client.RedisException]을 던집니다.
 *
 * ```kotlin
 * val result = redissonClient.suspendRunIfLeader("my-job") {
 *     // 리더로 선출된 경우에만 실행되는 suspend 작업
 *     delay(100)
 *     42
 * }
 * ```
 *
 * @param jobName 작업 이름 (분산 락 키로 사용)
 * @param options 리더 선출 옵션 (waitTime, leaseTime)
 * @param action 리더로 선출되었을 때 실행할 suspend 작업
 * @return [action] 실행 결과
 * @see RedissonSuspendLeaderElection
 */
suspend inline fun <T> RedissonClient.suspendRunIfLeader(
    jobName: String,
    options: LeaderElectionOptions = LeaderElectionOptions.Default,
    crossinline action: suspend () -> T,
): T {
    jobName.requireNotBlank("jobName")

    val leaderElection = RedissonSuspendLeaderElection(this, options)
    return leaderElection.runIfLeader(jobName) { action() }
}


/**
 * Redisson 분산 락을 이용하여 여러 프로세스/스레드 중 단 하나만 작업을 수행하도록 리더를 선출합니다.
 * Coroutine 환경에서 사용할 수 있는 suspend 버전입니다.
 *
 * ## threadId 대신 getLockId()를 사용하는 이유
 * Redisson의 [RLock]은 락 소유자를 스레드 ID로 식별합니다.
 * 그러나 Coroutine은 여러 스레드를 오가며 실행되므로, 락 획득 시점의 스레드와
 * 락 해제 시점의 스레드가 달라질 수 있습니다.
 * 이를 해결하기 위해 [io.bluetape4k.redis.redisson.coroutines.getLockId]를 사용하여
 * Redis의 `RAtomicLong`으로 Coroutine 세션마다 고유한 ID를 발급하고,
 * 이 ID를 락 획득/해제 양쪽에 동일하게 사용합니다.
 *
 * ```kotlin
 * val election = RedissonSuspendLeaderElection(redissonClient)
 * val result = election.runIfLeader("my-job") {
 *     // 리더로 선출된 경우에만 실행되는 suspend 작업
 *     delay(100)
 *     processData()
 * }
 * ```
 *
 * @param redissonClient Redisson 클라이언트
 * @param options 리더 선출 옵션 (waitTime, leaseTime)
 * @see RedissonLeaderElection 동기/비동기(CompletableFuture) 버전
 */
class RedissonSuspendLeaderElection private constructor(
    private val redissonClient: RedissonClient,
    options: LeaderElectionOptions,
): SuspendLeaderElection {

    companion object: KLoggingChannel() {
        /**
         * [RedissonSuspendLeaderElection] 인스턴스를 생성합니다.
         *
         * @param redissonClient RedissonClient 인스턴스
         * @param options 리더 선출 옵션
         */
        operator fun invoke(
            redissonClient: RedissonClient,
            options: LeaderElectionOptions = LeaderElectionOptions.Default,
        ): RedissonSuspendLeaderElection {
            return RedissonSuspendLeaderElection(redissonClient, options)
        }
    }

    private val waitTimeMills = options.waitTime.toMillis()
    private val leaseTimeMills = options.leaseTime.toMillis()

    /**
     * Redisson Lock을 이용하여, 리더로 선출되면 [action]을 수행하고, 그렇지 않다면 수행하지 않습니다.
     *
     * Coroutine 환경에서 스레드 전환으로 인한 락 소유자 불일치를 방지하기 위해,
     * `Thread.currentThread().threadId()` 대신 Redis `RAtomicLong` 기반의
     * [io.bluetape4k.redis.redisson.coroutines.getLockId]로 발급한 고유 ID를 락 식별자로 사용합니다.
     *
     * @param lockName 락 이름 — 락 획득에 성공하면 리더로 승격됩니다.
     * @param action 리더로 승격되었을 때 수행할 suspend 코드 블록
     * @return [action] 실행 결과
     * @throws org.redisson.client.RedisException 락 획득 실패 또는 인터럽트 발생 시
     */
    override suspend fun <T> runIfLeader(
        lockName: String,
        action: suspend () -> T,
    ): T {
        lockName.requireNotBlank("lockName")

        val lock: RLock = redissonClient.getLock(lockName)

        try {
            log.debug { "Leader 승격을 요청합니다 ..." }

            // Thread Id 기반으로 Lock 을 걸게 되므로, Coroutines 환경에서는 사용할 수 없다.
            // 고유의 Id 값을 제공해야 하므로 [RAtomicLong] 을 사용한다.
            val lockId = redissonClient.getLockId(lockName)

            // Redis IO 를 줄이기 위해 Default Snowflake 를 사용합니다.
            // val lockId = Snowflakers.Default.nextId()

            val acquired = lock
                .tryLockAsync(
                    waitTimeMills,
                    leaseTimeMills,
                    TimeUnit.MILLISECONDS,
                    lockId
                )
                .awaitSuspending()

            if (acquired) {
                log.debug { "Leader로 승격되어 작업을 수행합니다. lock=$lockName, lockId=$lockId" }
                try {
                    return action()
                } finally {
                    if (lock.isHeldByThread(lockId)) {
                        runCatching { lock.unlockAsync(lockId).await() }
                        log.debug { "작업이 완료되어 Leader 권한을 반납했습니다. lock=$lockName, lockId=$lockId" }
                    }
                }
            } else {
                throw RedisException("Fail to acquire lock. lock=$lockName")
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            log.warn(e) { "Interrupt to run action as leader. lockName=$lockName" }
            throw RedisException("Interrupted while acquiring lock. lock=$lockName", e)
        }
    }
}
