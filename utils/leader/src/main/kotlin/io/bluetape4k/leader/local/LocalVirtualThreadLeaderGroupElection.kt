package io.bluetape4k.leader.local

import io.bluetape4k.concurrent.virtualthread.VirtualFuture
import io.bluetape4k.concurrent.virtualthread.virtualFuture
import io.bluetape4k.leader.LeaderGroupState
import io.bluetape4k.leader.VirtualThreadLeaderGroupElection
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireNotBlank
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore

/**
 * [Semaphore]와 Virtual Thread를 이용한 로컬(단일 JVM) 복수 리더 비동기 선출 구현체입니다.
 *
 * ## 동작
 * - `lockName`별로 `Semaphore(maxLeaders, fair=true)`를 생성하여 동시 실행 수를 제한합니다.
 * - 슬롯이 가득 찬 경우 빈 슬롯이 생길 때까지 Virtual Thread가 블로킹됩니다.
 *   Virtual Thread는 [Semaphore.acquire] 블로킹 시 carrier thread를 반납합니다.
 * - [Semaphore.acquire]/[Semaphore.release] 쌍으로 슬롯을 관리하며, 예외 시에도 반드시 반환됩니다.
 * - `fair=true`로 생성하여 대기 Virtual Thread의 공정한 순서를 보장합니다.
 * - 분산 환경이 아닌 단일 JVM 프로세스 내 동시 실행 제한에 적합합니다.
 *
 * ## [LocalAsyncLeaderGroupElection] 과의 차이
 * - `action`이 `() -> T`로 단순하며, [java.util.concurrent.CompletableFuture] 래핑이 불필요합니다.
 * - 반환이 [VirtualFuture]로 `await()` API가 명시적입니다.
 * - Virtual Thread 기반이므로 I/O 블로킹 작업에 carrier thread를 소모하지 않습니다.
 *
 * ```kotlin
 * val election = LocalVirtualThreadLeaderGroupElection(maxLeaders = 3)
 *
 * // 최대 3개 Virtual Thread 가 동시에 실행
 * val result = election.runAsyncIfLeader("batch-job") { processChunk() }.await()
 *
 * // 상태 조회
 * println(election.state("batch-job"))
 * ```
 *
 * @param maxLeaders 허용하는 최대 동시 리더 수. 기본값 2
 */
class LocalVirtualThreadLeaderGroupElection private constructor(
    override val maxLeaders: Int,
) : VirtualThreadLeaderGroupElection {

    companion object : KLogging() {

        operator fun invoke(maxLeaders: Int = 2): VirtualThreadLeaderGroupElection {
            require(maxLeaders > 0) { "maxLeaders 는 1 이상이어야 합니다. maxLeaders=$maxLeaders" }
            return LocalVirtualThreadLeaderGroupElection(maxLeaders)
        }
    }

    private val semaphores = ConcurrentHashMap<String, Semaphore>()

    private fun getSemaphore(lockName: String): Semaphore {
        lockName.requireNotBlank("lockName")
        return semaphores.computeIfAbsent(lockName) { Semaphore(maxLeaders, true) }
    }

    /**
     * [lockName]에 대해 현재 활성(실행 중인) 리더 수를 반환합니다.
     *
     * `maxLeaders - availablePermits()`로 계산하므로 근사값입니다.
     */
    override fun activeCount(lockName: String): Int =
        maxLeaders - getSemaphore(lockName).availablePermits()

    /**
     * [lockName]에 대해 새 리더를 수용할 수 있는 남은 슬롯 수를 반환합니다.
     */
    override fun availableSlots(lockName: String): Int =
        getSemaphore(lockName).availablePermits()

    /**
     * [lockName]에 대한 현재 [LeaderGroupState] 스냅샷을 반환합니다.
     */
    override fun state(lockName: String): LeaderGroupState =
        LeaderGroupState(lockName, maxLeaders, activeCount(lockName))

    /**
     * [lockName]의 [Semaphore] 슬롯을 획득하고 [action]을 동기로 실행합니다.
     *
     * - 슬롯이 가득 찬 경우 빈 슬롯이 생길 때까지 블로킹됩니다.
     * - [action] 예외 발생 시에도 슬롯은 반드시 반환됩니다.
     *
     * @param lockName 리더 그룹 선출에 사용할 락 이름
     * @param action 슬롯 획득 성공 시 실행할 동기 작업
     * @return [action] 실행 결과
     */
    override fun <T> runIfLeader(lockName: String, action: () -> T): T {
        val semaphore = getSemaphore(lockName)
        semaphore.acquire()
        try {
            return action()
        } finally {
            semaphore.release()
        }
    }

    /**
     * [lockName]의 [Semaphore] 슬롯을 Virtual Thread에서 획득하고 [action]을 실행합니다.
     *
     * - Virtual Thread는 [Semaphore.acquire] 블로킹 시 carrier thread를 반납합니다.
     * - 슬롯이 가득 찬 경우 빈 슬롯이 생길 때까지 Virtual Thread가 블로킹됩니다.
     * - [action] 예외 발생 시에도 슬롯은 반드시 반환됩니다.
     *
     * @param lockName 리더 그룹 선출에 사용할 락 이름
     * @param action 슬롯 획득 성공 시 실행할 작업
     * @return [action] 실행 결과를 담은 [VirtualFuture]
     */
    override fun <T> runAsyncIfLeader(lockName: String, action: () -> T): VirtualFuture<T> =
        virtualFuture {
            val semaphore = getSemaphore(lockName)
            semaphore.acquire()
            try {
                action()
            } finally {
                semaphore.release()
            }
        }
}
