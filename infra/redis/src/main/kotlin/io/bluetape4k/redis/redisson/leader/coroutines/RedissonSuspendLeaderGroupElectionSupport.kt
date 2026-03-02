package io.bluetape4k.redis.redisson.leader.coroutines

import io.bluetape4k.redis.redisson.leader.RedissonLeaderElectionOptions
import io.bluetape4k.support.requireNotBlank
import org.redisson.api.RedissonClient

/**
 * Redisson 분산 Semaphore를 이용하여 복수 리더 선출을 통한 suspend 작업을 수행합니다.
 *
 * ```kotlin
 * val client: RedissonClient = ...
 * val result: Int = client.runSuspendIfLeaderGroup("batch-job", maxLeaders = 3) {
 *     // 최대 3개 프로세스가 동시에 실행
 *     delay(100)
 *     42
 * }
 * ```
 *
 * @param lockName 락 이름
 * @param maxLeaders 허용하는 최대 동시 리더 수. 기본값 2
 * @param options 리더 선출 옵션
 * @param action 리더 그룹 슬롯 획득 시 수행할 suspend 작업
 * @return 작업 결과
 */
suspend fun <T> RedissonClient.runSuspendIfLeaderGroup(
    lockName: String,
    maxLeaders: Int = 2,
    options: RedissonLeaderElectionOptions = RedissonLeaderElectionOptions.Default,
    action: suspend () -> T,
): T {
    lockName.requireNotBlank("lockName")
    return RedissonSuspendLeaderGroupElection(this, maxLeaders, options).runIfLeader(lockName, action)
}
