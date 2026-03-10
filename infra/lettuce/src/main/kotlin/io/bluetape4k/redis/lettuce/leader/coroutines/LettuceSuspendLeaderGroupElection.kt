package io.bluetape4k.redis.lettuce.leader.coroutines

import io.bluetape4k.leader.LeaderGroupState
import io.bluetape4k.leader.coroutines.SuspendLeaderGroupElection
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.lettuce.leader.LettuceLeaderElectionOptions
import io.bluetape4k.redis.lettuce.semaphore.RedisSemaphore
import io.lettuce.core.api.StatefulRedisConnection

/**
 * Lettuce Redis 클라이언트를 이용한 코루틴 기반 복수 리더 선출 구현체입니다.
 *
 * [RedisSemaphore]의 suspend 메서드를 사용하여 최대 [maxLeaders]개의 리더를 동시에 허용합니다.
 *
 * ```kotlin
 * val election = LettuceSuspendLeaderGroupElection(connection, maxLeaders = 3)
 * val result = election.runIfLeader("batch-job") { processChunk() }
 * println(election.state("batch-job"))
 * ```
 *
 * @param connection Lettuce [StatefulRedisConnection] (StringCodec 기반)
 * @param maxLeaders 최대 동시 리더 수
 * @param options    리더 선출 옵션 (waitTime, leaseTime)
 */
class LettuceSuspendLeaderGroupElection(
    private val connection: StatefulRedisConnection<String, String>,
    override val maxLeaders: Int,
    private val options: LettuceLeaderElectionOptions = LettuceLeaderElectionOptions(),
) : SuspendLeaderGroupElection {

    companion object : KLogging()

    private fun getSemaphore(lockName: String): RedisSemaphore {
        val semaphore = RedisSemaphore(connection, lockName, maxLeaders)
        semaphore.initialize()
        return semaphore
    }

    override fun activeCount(lockName: String): Int {
        val semaphore = getSemaphore(lockName)
        return maxLeaders - semaphore.availablePermits()
    }

    override fun availableSlots(lockName: String): Int {
        val semaphore = getSemaphore(lockName)
        return semaphore.availablePermits()
    }

    override fun state(lockName: String): LeaderGroupState {
        val active = activeCount(lockName)
        return LeaderGroupState(lockName, maxLeaders, active)
    }

    override suspend fun <T> runIfLeader(lockName: String, action: suspend () -> T): T {
        require(lockName.isNotBlank()) { "lockName은 공백이 아니어야 합니다." }
        val semaphore = getSemaphore(lockName)
        semaphore.acquireSuspending(waitTime = options.waitTime)
        log.debug { "리더 선출 성공 (suspend): lockName=$lockName" }
        try {
            return action()
        } finally {
            runCatching { semaphore.releaseSuspending() }
        }
    }
}
