package io.bluetape4k.redis.redisson.leader.coroutines

import io.bluetape4k.coroutines.support.suspendAwait
import io.bluetape4k.leader.coroutines.SuspendLeaderElection
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.bluetape4k.redis.redisson.coroutines.getLockId
import io.bluetape4k.redis.redisson.leader.RedissonLeaderElectionOptions
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.uninitialized
import kotlinx.coroutines.coroutineScope
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import java.util.concurrent.TimeUnit

/**
 * 여러 Process, Thread에서 같은 작업이 동시, 무작위로 실행되는 것을 방지하기 위해
 * Redisson Lock을 이용하여 Leader를 선출되면 독점적으로 작업할 수 있도록 합니다.
 */
class RedissonSuspendLeaderElection private constructor(
    private val redissonClient: RedissonClient,
    options: RedissonLeaderElectionOptions,
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
            options: RedissonLeaderElectionOptions = RedissonLeaderElectionOptions.Default,
        ): RedissonSuspendLeaderElection {
            return RedissonSuspendLeaderElection(redissonClient, options)
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
    override suspend fun <T> runIfLeader(
        lockName: String,
        action: suspend () -> T,
    ): T = coroutineScope {
        lockName.requireNotBlank("lockName")

        val lock: RLock = redissonClient.getLock(lockName)
        var result: T = uninitialized()

        try {
            log.debug { "Leader 승격을 요청합니다 ..." }

            // Thread Id 기반으로 Lock 을 걸게 되므로, Coroutines 환경에서는 사용할 수 없다.
            // 고유의 Id 값을 제공해야 하므로 [RAtomicLong] 을 사용한다.
            val lockId = redissonClient.getLockId(lockName)

            // Redis IO 를 줄이기 위해 Default Snowflake 를 사용합니다.
            // val lockId = Snowflakers.Default.nextId()

            val acquired = lock.tryLockAsync(
                waitTimeMills,
                leaseTimeMills,
                TimeUnit.MILLISECONDS,
                lockId
            ).suspendAwait()

            if (acquired) {
                log.debug { "Leader로 승격되어 작업을 수행합니다. lock=$lockName, lockId=$lockId" }
                try {
                    result = action()
                } finally {
                    if (lock.isHeldByThread(lockId)) {
                        lock.unlockAsync(lockId).suspendAwait()
                        log.debug { "작업이 완료되어 Leader 권한을 반납했습니다. lock=$lockName, lockId=$lockId" }
                    }
                }
            }
        } catch (e: InterruptedException) {
            log.warn(e) { "Interrupt to run action as leader. lockName=$lockName" }
        }
        result
    }
}
