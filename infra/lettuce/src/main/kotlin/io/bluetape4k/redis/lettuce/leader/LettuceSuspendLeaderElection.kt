package io.bluetape4k.redis.lettuce.leader

import io.bluetape4k.leader.LeaderElectionOptions
import io.bluetape4k.leader.coroutines.SuspendLeaderElection
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.lettuce.lock.LettuceSuspendLock
import io.bluetape4k.support.requireNotBlank
import io.lettuce.core.api.StatefulRedisConnection

/**
 * [StatefulRedisConnection]에서 [LettuceSuspendLeaderElection] 인스턴스를 생성합니다.
 *
 * ```kotlin
 * val election = connection.suspendLeaderElection()
 * val result = election.runIfLeader("daily-job") { "done" }
 * ```
 *
 * @param options 리더 선출 옵션 (기본값: [LeaderElectionOptions.Default])
 * @return [LettuceSuspendLeaderElection] 인스턴스
 */
fun StatefulRedisConnection<String, String>.suspendLeaderElection(
    options: LeaderElectionOptions = LeaderElectionOptions.Default,
): LettuceSuspendLeaderElection =
    LettuceSuspendLeaderElection(this, options)


/**
 * Lettuce Redis 클라이언트를 이용한 코루틴 기반 리더 선출 구현체입니다.
 *
 * [LettuceSuspendLock]을 사용하여 비동기적으로 리더를 선출합니다.
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
    val options: LeaderElectionOptions = LeaderElectionOptions.Default,
): SuspendLeaderElection {

    companion object: KLogging()

    override suspend fun <T> runIfLeader(lockName: String, action: suspend () -> T): T {
        lockName.requireNotBlank("lockName")

        val lock = LettuceSuspendLock(connection, lockName, options.leaseTime)
        val acquired = lock.tryLock(options.waitTime, options.leaseTime)
        if (!acquired) {
            throw IllegalStateException("리더 선출 실패 (suspend): lockName=$lockName")
        }
        log.debug { "리더 선출 성공 (suspend): lockName=$lockName" }
        try {
            return action()
        } finally {
            if (lock.isHeldByCurrentInstance()) {
                runCatching { lock.unlock() }
            }
        }
    }
}
