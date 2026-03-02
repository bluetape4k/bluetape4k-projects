package io.bluetape4k.redis.redisson.leader

import io.bluetape4k.support.requireNotBlank
import org.redisson.api.RedissonClient

/**
 * Redisson 분산 Semaphore를 이용하여 복수 리더 선출을 통한 작업을 수행합니다.
 *
 * ```kotlin
 * val client: RedissonClient = ...
 * val result: Int = client.runIfLeaderGroup("batch-job", maxLeaders = 3) {
 *     // 최대 3개 프로세스가 동시에 실행
 *     42
 * }
 * ```
 *
 * @param lockName 락 이름
 * @param maxLeaders 허용하는 최대 동시 리더 수. 기본값 2
 * @param options 리더 선출 옵션
 * @param action 리더 그룹 슬롯 획득 시 수행할 작업
 * @return 작업 결과
 */
inline fun <T> RedissonClient.runIfLeaderGroup(
    lockName: String,
    maxLeaders: Int = 2,
    options: RedissonLeaderElectionOptions = RedissonLeaderElectionOptions.Default,
    crossinline action: () -> T,
): T {
    lockName.requireNotBlank("lockName")
    return RedissonLeaderGroupElection(this, maxLeaders, options).runIfLeader(lockName) { action() }
}
