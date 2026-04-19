package io.bluetape4k.redis.lettuce.leader

import io.bluetape4k.leader.LeaderGroupElection
import io.bluetape4k.leader.LeaderGroupElectionOptions
import io.bluetape4k.leader.LeaderGroupState
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.lettuce.semaphore.LettuceSemaphore
import io.bluetape4k.support.requireNotBlank
import io.lettuce.core.api.StatefulRedisConnection
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor

/**
 * [StatefulRedisConnection]에서 [LettuceLeaderGroupElection] 인스턴스를 생성합니다.
 *
 * ```kotlin
 * val options = LeaderGroupElectionOptions(maxLeaders = 3)
 * val election = connection.leaderGroupElection(options)
 * val result = election.runIfLeader("batch-job") { processChunk() }
 * ```
 *
 * @param options    리더 선출 옵션 (기본값: [LeaderElectionOptions.Default])
 * @return [LettuceLeaderGroupElection] 인스턴스
 */
fun StatefulRedisConnection<String, String>.leaderGroupElection(
    options: LeaderGroupElectionOptions = LeaderGroupElectionOptions.Default,
): LettuceLeaderGroupElection =
    LettuceLeaderGroupElection(this, options)


/**
 * Lettuce Redis 클라이언트를 이용한 복수 리더 선출 구현체입니다.
 *
 * [LettuceSemaphore]를 사용하여 분산 환경에서 최대 [maxLeaders]개의 리더를 동시에 허용합니다.
 * 동기([runIfLeader])와 비동기([runAsyncIfLeader]) 방식을 모두 지원합니다.
 *
 * ```kotlin
 * val options = LeaderGroupElectionOptions(maxLeaders = 3)
 * val election = LettuceLeaderGroupElection(connection, options)
 * val result = election.runIfLeader("batch-job") { processChunk() }
 * println(election.state("batch-job"))
 * ```
 *
 * @param connection Lettuce [StatefulRedisConnection] (StringCodec 기반)
 * @param options    리더 선출 옵션 (maxLeaders, waitTime, leaseTime)
 */
class LettuceLeaderGroupElection(
    private val connection: StatefulRedisConnection<String, String>,
    val options: LeaderGroupElectionOptions = LeaderGroupElectionOptions.Default,
): LeaderGroupElection {

    companion object: KLogging()

    // 개선: LettuceSemaphore 인스턴스를 lockName 별로 1회만 생성/초기화 하여 재사용합니다.
    //       이전엔 매 호출마다 `new + initialize()` 로 SET NX 라운드트립을 반복했습니다.
    //       ConcurrentHashMap.computeIfAbsent 로 원자적 캐싱을 보장합니다.
    private val semaphores = ConcurrentHashMap<String, LettuceSemaphore>()

    private fun getSemaphore(lockName: String): LettuceSemaphore {
        lockName.requireNotBlank("lockName")
        return semaphores.computeIfAbsent(lockName) {
            LettuceSemaphore(connection, it, maxLeaders).apply { initialize() }
        }
    }

    override val maxLeaders: Int = options.maxLeaders

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

    override fun <T> runIfLeader(lockName: String, action: () -> T): T {
        val semaphore = getSemaphore(lockName)

        semaphore.acquire(waitTime = options.waitTime)
        log.debug { "리더 선출 성공: lockName=$lockName" }
        try {
            return action()
        } finally {
            runCatching { semaphore.release() }
        }
    }

    override fun <T> runAsyncIfLeader(
        lockName: String,
        executor: Executor,
        action: () -> CompletableFuture<T>,
    ): CompletableFuture<T> {
        val semaphore = getSemaphore(lockName)

        return CompletableFuture.supplyAsync({
            semaphore.acquire(waitTime = options.waitTime)
            log.debug { "리더 선출 성공 (async): lockName=$lockName" }
        }, executor).thenCompose {
            try {
                action().whenComplete { _, _ ->
                    runCatching { semaphore.release() }
                }
            } catch (e: Throwable) {
                runCatching { semaphore.release() }
                CompletableFuture.failedFuture(e)
            }
        }
    }
}
