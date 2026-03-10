package io.bluetape4k.leader.coroutines

import io.bluetape4k.leader.LeaderElectionOptions
import io.bluetape4k.support.requireNotBlank
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Coroutines [Mutex]를 이용한 로컬(단일 JVM) suspend 리더 선출 구현체입니다.
 *
 * ## 동작
 * - 동일 [lockName]에 대해 코루틴 간 상호 배제(mutual exclusion)로 직렬 실행을 보장합니다.
 * - [Mutex]를 획득한 코루틴이 리더로서 [action]을 실행하며, 다른 코루틴은 해제될 때까지 suspend됩니다.
 * - 분산 환경이 아닌 단일 JVM 프로세스 내 코루틴 동시 실행 직렬화에 적합합니다.
 *
 * ## 주의
 * - [Mutex]는 재진입(re-entrancy)을 지원하지 않습니다.
 *   동일 코루틴에서 동일 [lockName]으로 중첩 호출하면 데드락이 발생합니다.
 *   재진입이 필요한 경우 [LocalLeaderElection] ([ReentrantLock] 기반)을 사용하세요.
 *
 * ```kotlin
 * val election = LocalSuspendLeaderElection()
 * val result = election.runIfLeader("job-lock") { "done" }
 * // result == "done"
 * ```
 */
class LocalSuspendLeaderElection(
    private val options: LeaderElectionOptions = LeaderElectionOptions.Default,
): SuspendLeaderElection {

    private val mutexes = ConcurrentHashMap<String, Mutex>()

    private fun getMutex(lockName: String): Mutex {
        lockName.requireNotBlank("lockName")
        return mutexes.computeIfAbsent(lockName) { Mutex() }
    }

    /**
     * [lockName]에 대한 [Mutex]를 획득하고 [action]을 직렬로 실행합니다.
     *
     * 다른 코루틴이 동일 [lockName]의 [Mutex]를 보유 중이면 해제될 때까지 suspend됩니다.
     *
     * @param lockName 리더 선출에 사용할 락 이름
     * @param action 리더 획득 성공 시 실행할 suspend 작업
     * @return [action] 실행 결과
     */
    override suspend fun <T> runIfLeader(lockName: String, action: suspend () -> T): T =
        getMutex(lockName).withLock { action() }
}
