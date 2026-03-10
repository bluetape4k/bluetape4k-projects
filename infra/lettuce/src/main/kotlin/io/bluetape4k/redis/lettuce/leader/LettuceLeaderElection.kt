package io.bluetape4k.redis.lettuce.leader

import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import io.bluetape4k.leader.LeaderElection
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.lettuce.lock.RedisLock
import io.lettuce.core.api.StatefulRedisConnection
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

/**
 * Lettuce Redis 클라이언트를 이용한 리더 선출 구현체입니다.
 *
 * [RedisLock]을 사용하여 분산 환경에서 단일 리더를 선출합니다.
 * 동기([runIfLeader])와 비동기([runAsyncIfLeader]) 방식을 모두 지원합니다.
 *
 * ```kotlin
 * val election = LettuceLeaderElection(connection)
 * val result = election.runIfLeader("daily-job") { "done" }
 * ```
 *
 * @param connection Lettuce [StatefulRedisConnection] (StringCodec 기반)
 * @param options    리더 선출 옵션 (waitTime, leaseTime)
 */
class LettuceLeaderElection(
    private val connection: StatefulRedisConnection<String, String>,
    private val options: LettuceLeaderElectionOptions = LettuceLeaderElectionOptions(),
) : LeaderElection {

    companion object : KLogging()

    override fun <T> runIfLeader(lockName: String, action: () -> T): T {
        require(lockName.isNotBlank()) { "lockName은 공백이 아니어야 합니다." }
        val lock = RedisLock(connection, lockName, options.leaseTime)
        val acquired = lock.tryLock(options.waitTime, options.leaseTime)
        if (!acquired) {
            throw IllegalStateException("리더 선출 실패: lockName=$lockName")
        }
        log.debug { "리더 선출 성공: lockName=$lockName" }
        try {
            return action()
        } finally {
            if (lock.isHeldByCurrentInstance()) {
                runCatching { lock.unlock() }
            }
        }
    }

    override fun <T> runAsyncIfLeader(
        lockName: String,
        executor: Executor,
        action: () -> CompletableFuture<T>,
    ): CompletableFuture<T> {
        require(lockName.isNotBlank()) { "lockName은 공백이 아니어야 합니다." }
        val lock = RedisLock(connection, lockName, options.leaseTime)
        return CompletableFuture.supplyAsync({
            val acquired = lock.tryLock(options.waitTime, options.leaseTime)
            if (!acquired) {
                throw IllegalStateException("리더 선출 실패 (async): lockName=$lockName")
            }
            log.debug { "리더 선출 성공 (async): lockName=$lockName" }
        }, executor).thenCompose {
            try {
                action().whenComplete { _, _ ->
                    if (lock.isHeldByCurrentInstance()) {
                        runCatching { lock.unlock() }
                    }
                }
            } catch (e: Throwable) {
                if (lock.isHeldByCurrentInstance()) {
                    runCatching { lock.unlock() }
                }
                CompletableFuture.failedFuture(e)
            }
        }
    }
}
