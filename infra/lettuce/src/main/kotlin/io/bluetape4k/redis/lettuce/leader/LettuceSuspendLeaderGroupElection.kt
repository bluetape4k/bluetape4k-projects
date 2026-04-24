package io.bluetape4k.redis.lettuce.leader

import io.bluetape4k.leader.LeaderGroupElectionOptions
import io.bluetape4k.leader.LeaderGroupState
import io.bluetape4k.leader.coroutines.SuspendLeaderGroupElection
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.lettuce.semaphore.LettuceSemaphore
import io.bluetape4k.support.requireNotBlank
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.coroutines.future.await
import java.util.concurrent.ConcurrentHashMap

/**
 * [StatefulRedisConnection]에서 [LettuceSuspendLeaderGroupElection] 인스턴스를 생성합니다.
 *
 * ```kotlin
 * val election = connection.suspendLeaderGroupElection(maxLeaders = 3)
 * val result = election.runIfLeader("batch-job") { processChunkSuspend() }
 * ```
 *
 * @param options    리더 선출 옵션 (기본값: [LeaderGroupElectionOptions.Default])
 * @return [LettuceSuspendLeaderGroupElection] 인스턴스
 */
fun StatefulRedisConnection<String, String>.suspendLeaderGroupElection(
    options: LeaderGroupElectionOptions = LeaderGroupElectionOptions.Default,
): LettuceSuspendLeaderGroupElection =
    LettuceSuspendLeaderGroupElection(this, options)


/**
 * Lettuce Redis 클라이언트를 이용한 코루틴 기반 복수 리더 선출 구현체입니다.
 *
 * [LettuceSemaphore]의 suspend 메서드를 사용하여 최대 [maxLeaders]개의 리더를 동시에 허용합니다.
 *
 * ```kotlin
 * val options = LeaderGroupElectionOptions(maxLeaders = 3)
 * val election = LettuceSuspendLeaderGroupElection(connection, options)
 * val result = election.runIfLeader("batch-job") { processChunk() }
 * println(election.state("batch-job"))
 * ```
 *
 * @param connection Lettuce [StatefulRedisConnection] (StringCodec 기반)
 * @param options    리더 선출 옵션 (maxLeaders, waitTime, leaseTime)
 */
class LettuceSuspendLeaderGroupElection(
    private val connection: StatefulRedisConnection<String, String>,
    val options: LeaderGroupElectionOptions = LeaderGroupElectionOptions.Default,
): SuspendLeaderGroupElection {

    companion object: KLogging()

    override val maxLeaders: Int = options.maxLeaders

    // 개선: LettuceSemaphore 인스턴스를 lockName 별로 1회만 생성/초기화 하여 재사용합니다.
    //       ConcurrentHashMap.computeIfAbsent 로 원자적 캐싱을 보장합니다.
    private val semaphores = ConcurrentHashMap<String, LettuceSemaphore>()

    private fun getSemaphore(lockName: String): LettuceSemaphore {
        lockName.requireNotBlank("lockName")
        return semaphores.computeIfAbsent(lockName) {
            LettuceSemaphore(connection, it, maxLeaders).apply { initialize() }
        }
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
        val semaphore = getSemaphore(lockName)
        semaphore.acquireAsync(waitTime = options.waitTime).await()
        log.debug { "리더 선출 성공 (suspend): lockName=$lockName" }
        try {
            return action()
        } finally {
            runCatching { semaphore.releaseAsync().await() }
        }
    }
}
