package io.bluetape4k.leader.local

import io.bluetape4k.concurrent.virtualthread.VirtualFuture
import io.bluetape4k.concurrent.virtualthread.virtualFuture
import io.bluetape4k.leader.VirtualThreadLeaderGroupElection
import io.bluetape4k.logging.KLogging

/**
 * [AbstractLocalLeaderGroupElection]을 상속한 로컬(단일 JVM) 복수 리더 Virtual Thread 비동기 선출 구현체입니다.
 *
 * ## 동작
 * - [VirtualFuture] 기반의 [runAsyncIfLeader]를 지원합니다.
 * - 슬롯 관리(Semaphore 풀, 상태 조회)는 [AbstractLocalLeaderGroupElection]에서 처리합니다.
 * - Virtual Thread는 [java.util.concurrent.Semaphore.acquire] 블로킹 시 carrier thread를 반납합니다.
 * - 분산 환경이 아닌 단일 JVM 프로세스 내 동시 실행 제한에 적합합니다.
 *
 * ## [LocalAsyncLeaderGroupElection] 과의 차이
 * - `action`이 `() -> T`로 단순하며, [java.util.concurrent.CompletableFuture] 래핑이 불필요합니다.
 * - 반환이 [VirtualFuture]로 `await()` API가 명시적입니다.
 *
 * ```kotlin
 * val election = LocalVirtualThreadLeaderGroupElection(maxLeaders = 3)
 *
 * // 최대 3개 Virtual Thread 가 동시에 실행
 * val result = election.runAsyncIfLeader("batch-job") { processChunk() }.await()
 * ```
 *
 * @param maxLeaders 허용하는 최대 동시 리더 수. 기본값 2
 */
class LocalVirtualThreadLeaderGroupElection private constructor(maxLeaders: Int) :
    AbstractLocalLeaderGroupElection(maxLeaders), VirtualThreadLeaderGroupElection {

    companion object : KLogging() {

        operator fun invoke(maxLeaders: Int = 2): VirtualThreadLeaderGroupElection =
            LocalVirtualThreadLeaderGroupElection(maxLeaders)
    }

    /**
     * [lockName]의 슬롯을 Virtual Thread에서 획득하고 [action]을 실행합니다.
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
