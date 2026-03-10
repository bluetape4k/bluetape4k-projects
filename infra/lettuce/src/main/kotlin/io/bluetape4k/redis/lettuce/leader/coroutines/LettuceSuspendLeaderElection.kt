package io.bluetape4k.redis.lettuce.leader.coroutines

import io.bluetape4k.leader.coroutines.SuspendLeaderElection
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.lettuce.leader.LettuceLeaderElectionOptions
import io.bluetape4k.redis.lettuce.lock.RedisLock
import io.lettuce.core.api.StatefulRedisConnection

/**
 * Lettuce Redis 클라이언트를 이용한 코루틴 기반 리더 선출 구현체입니다.
 *
 * [RedisLock]의 suspend 메서드를 사용하여 비동기적으로 리더를 선출합니다.
 *
 * ```kotlin
 * val election = LettuceSuspendLeaderElection(connection)
 * val result = election.runIfLeader("daily-job") { "done" }
 * ```
 *
 * @param connection Lettuce [StatefulRedisConnection] (StringCodec 기반)
 * @param options    리더 선출 옵션 (waitTime, leaseTime)
 */
class LettuceSuspendLeaderElection(
    private val connection: StatefulRedisConnection<String, String>,
    private val options: LettuceLeaderElectionOptions = LettuceLeaderElectionOptions(),
) : SuspendLeaderElection {

    companion object : KLogging()

    override suspend fun <T> runIfLeader(lockName: String, action: suspend () -> T): T {
        require(lockName.isNotBlank()) { "lockName은 공백이 아니어야 합니다." }
        val lock = RedisLock(connection, lockName, options.leaseTime)
        val acquired = lock.tryLockSuspending(options.waitTime, options.leaseTime)
        if (!acquired) {
            throw IllegalStateException("리더 선출 실패 (suspend): lockName=$lockName")
        }
        log.debug { "리더 선출 성공 (suspend): lockName=$lockName" }
        try {
            return action()
        } finally {
            if (lock.isHeldByCurrentInstance()) {
                runCatching { lock.unlockSuspending() }
            }
        }
    }
}
