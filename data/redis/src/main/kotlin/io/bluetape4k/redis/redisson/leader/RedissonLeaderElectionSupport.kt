package io.bluetape4k.redis.redisson.leader

import io.bluetape4k.support.requireNotBlank
import org.redisson.api.RedissonClient
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.ForkJoinPool

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
    options: RedissonLeaderElectionOptions = RedissonLeaderElectionOptions.Default,
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
    options: RedissonLeaderElectionOptions = RedissonLeaderElectionOptions.Default,
    crossinline action: () -> CompletableFuture<T>,
): CompletableFuture<T> {
    jobName.requireNotBlank("jobName")
    val leaderElection = RedissonLeaderElection(this, options)
    return leaderElection.runAsyncIfLeader(jobName, executor) { action() }
}
