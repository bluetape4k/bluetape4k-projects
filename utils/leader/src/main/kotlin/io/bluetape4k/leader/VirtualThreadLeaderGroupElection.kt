package io.bluetape4k.leader

import io.bluetape4k.concurrent.virtualthread.VirtualFuture

/**
 * Virtual Thread 기반 복수 리더 비동기 선출 계약을 정의합니다.
 *
 * ## [VirtualThreadLeaderElection] 과의 차이
 * - [VirtualThreadLeaderElection]은 `lockName`당 리더를 1개로 제한합니다.
 * - [VirtualThreadLeaderGroupElection]은 [maxLeaders]개까지 동시에 리더를 허용합니다.
 *
 * ## [LeaderGroupElection] 과의 차이
 * - [LeaderGroupElection]의 [LeaderGroupElection.runAsyncIfLeader]는 [java.util.concurrent.CompletableFuture]를 반환합니다.
 * - 이 인터페이스의 [runAsyncIfLeader]는 [VirtualFuture]를 반환하며, `action`이 `() -> T` 람다로 단순합니다.
 * - Virtual Thread 기반이므로 I/O 블로킹 작업에 carrier thread를 소모하지 않습니다.
 *
 * ## 동작/계약
 * - 구현체는 `lockName` 기준으로 최대 [maxLeaders]개의 `action`을 동시에 실행합니다.
 * - 슬롯이 가득 찬 경우, 빈 슬롯이 생길 때까지 Virtual Thread가 블로킹됩니다.
 * - `action` 예외 발생 시에도 슬롯이 반드시 반환됩니다.
 * - 상태 조회 메서드([state], [activeCount], [availableSlots])는 [LeaderGroupElectionState]에서 상속합니다.
 *
 * ```kotlin
 * val election = LocalVirtualThreadLeaderGroupElection(maxLeaders = 3)
 * val result = election.runAsyncIfLeader("batch-job") { processChunk() }.await()
 * ```
 */
interface VirtualThreadLeaderGroupElection: LeaderGroupElectionState {

    /**
     * 슬롯을 획득하여 리더로 선출되면 [action]을 Virtual Thread에서 비동기로 실행합니다.
     *
     * ## 동작/계약
     * - 슬롯이 가득 찬 경우 빈 슬롯이 생길 때까지 Virtual Thread가 블로킹됩니다.
     * - [action] 예외 발생 시에도 슬롯은 반드시 반환됩니다.
     * - [VirtualFuture.await]로 결과를 동기 대기하거나 [VirtualFuture.toCompletableFuture]로 변환할 수 있습니다.
     *
     * ```kotlin
     * val result = election.runAsyncIfLeader("job-lock") { computeResult() }.await()
     * ```
     *
     * @param lockName 리더 그룹 선출에 사용할 락 이름
     * @param action 리더 선출 성공 시 실행할 작업. 결과를 직접 반환합니다.
     * @return [action] 실행 결과를 담은 [VirtualFuture]
     */
    fun <T> runAsyncIfLeader(
        lockName: String,
        action: () -> T,
    ): VirtualFuture<T>
}
