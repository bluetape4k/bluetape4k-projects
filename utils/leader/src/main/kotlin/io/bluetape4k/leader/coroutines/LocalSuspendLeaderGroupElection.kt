package io.bluetape4k.leader.coroutines

import io.bluetape4k.leader.LeaderGroupState
import io.bluetape4k.support.requireNotBlank
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap

/**
 * 코루틴 [Semaphore]를 이용한 로컬(단일 JVM) suspend 복수 리더 선출 구현체입니다.
 *
 * ## 동작
 * - `lockName`별로 `kotlinx.coroutines.sync.Semaphore(maxLeaders)`를 생성하여 동시 실행 수를 제한합니다.
 * - 슬롯이 가득 찬 경우 빈 슬롯이 생길 때까지 호출 코루틴이 suspend됩니다.
 * - [Semaphore.withPermit]으로 슬롯을 관리하며, 예외 시에도 반드시 반환됩니다.
 * - 분산 환경이 아닌 단일 JVM 프로세스 내 코루틴 동시 실행 제한에 적합합니다.
 *
 * ## [LocalLeaderGroupElection] 과의 차이
 * - [LocalLeaderGroupElection]은 `java.util.concurrent.Semaphore`(스레드 블로킹)를 사용합니다.
 * - 이 구현체는 `kotlinx.coroutines.sync.Semaphore`(코루틴 suspend)를 사용합니다.
 *
 * ```kotlin
 * val election = LocalSuspendLeaderGroupElection(maxLeaders = 3)
 *
 * // 최대 3개 코루틴이 동시에 실행
 * val result = election.runIfLeader("batch-job") { processChunkSuspend() }
 *
 * // 상태 조회
 * println(election.state("batch-job"))
 * ```
 *
 * @param maxLeaders 허용하는 최대 동시 리더 수. 기본값 2
 */
class LocalSuspendLeaderGroupElection(override val maxLeaders: Int = 2) : SuspendLeaderGroupElection {

    init {
        require(maxLeaders > 0) { "maxLeaders 는 1 이상이어야 합니다. maxLeaders=$maxLeaders" }
    }

    private val semaphores = ConcurrentHashMap<String, Semaphore>()

    private fun getSemaphore(lockName: String): Semaphore {
        lockName.requireNotBlank("lockName")
        return semaphores.computeIfAbsent(lockName) { Semaphore(maxLeaders) }
    }

    /**
     * [lockName]에 대해 현재 활성(실행 중인) 리더 수를 반환합니다.
     *
     * `maxLeaders - availablePermits`로 계산하므로 근사값입니다.
     */
    override fun activeCount(lockName: String): Int =
        maxLeaders - getSemaphore(lockName).availablePermits

    /**
     * [lockName]에 대해 새 리더를 수용할 수 있는 남은 슬롯 수를 반환합니다.
     */
    override fun availableSlots(lockName: String): Int =
        getSemaphore(lockName).availablePermits

    /**
     * [lockName]에 대한 현재 [LeaderGroupState] 스냅샷을 반환합니다.
     */
    override fun state(lockName: String): LeaderGroupState =
        LeaderGroupState(lockName, maxLeaders, activeCount(lockName))

    /**
     * [lockName]의 [Semaphore] 슬롯을 획득하고 suspend [action]을 실행합니다.
     *
     * - 슬롯이 가득 찬 경우 빈 슬롯이 생길 때까지 코루틴이 suspend됩니다.
     * - [action] 예외 발생 시에도 슬롯은 반드시 반환됩니다.
     *
     * @param lockName 리더 그룹 선출에 사용할 락 이름
     * @param action 슬롯 획득 성공 시 실행할 suspend 작업
     * @return [action] 실행 결과
     */
    override suspend fun <T> runIfLeader(lockName: String, action: suspend () -> T): T =
        getSemaphore(lockName).withPermit { action() }
}
