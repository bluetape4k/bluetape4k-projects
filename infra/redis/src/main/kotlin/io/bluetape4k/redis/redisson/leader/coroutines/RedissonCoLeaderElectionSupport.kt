package io.bluetape4k.redis.redisson.leader.coroutines

import io.bluetape4k.redis.redisson.leader.RedissonLeaderElectionOptions
import io.bluetape4k.support.requireNotBlank
import org.redisson.api.RedissonClient

/**
 * Redisson 분산 락을 이용하여 리더 선출을 통한 작업을 Coroutine 환경에서 사용할 수 있도록 지원합니다.
 */
@Deprecated(
    message = "Use runSuspendIfLeader instead.",
    replaceWith = ReplaceWith("runSuspendIfLeader(jobName, options, action)")
)
suspend inline fun <T> RedissonClient.runIfLeaderSuspending(
    jobName: String,
    options: RedissonLeaderElectionOptions = RedissonLeaderElectionOptions.Default,
    crossinline action: suspend () -> T,
): T {
    jobName.requireNotBlank("jobName")

    val leaderElection = RedissonCoLeaderElection(this, options)
    return leaderElection.runIfLeader(jobName) { action() }
}
