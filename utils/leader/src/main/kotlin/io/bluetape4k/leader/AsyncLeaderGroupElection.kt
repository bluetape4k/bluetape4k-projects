package io.bluetape4k.leader

import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

/**
 * Semaphore 기반 복수 리더 비동기 선출 계약을 정의합니다.
 *
 * ## [LeaderGroupElection] 과의 차이
 * - [LeaderGroupElection]은 `action`이 `() -> T` 동기 람다입니다.
 * - [AsyncLeaderGroupElection]은 `action`이 `() -> CompletableFuture<T>` 비동기 람다로,
 *   [CompletableFuture]를 반환합니다.
 *
 * ## [AsyncLeaderElection] 과의 차이
 * - [AsyncLeaderElection]은 `lockName`당 리더를 1개로 제한합니다.
 * - [AsyncLeaderGroupElection]은 [maxLeaders]개까지 동시에 리더를 허용합니다.
 * - 내부적으로 `Semaphore(maxLeaders)`를 이용하여 동시 실행 수를 제한합니다.
 *
 * ## 동작/계약
 * - 구현체는 `lockName` 기준으로 최대 [maxLeaders]개의 `action`을 동시에 실행합니다.
 * - 슬롯이 가득 찬 경우, 빈 슬롯이 생길 때까지 [Executor] 스레드가 블로킹됩니다.
 * - `action` 예외 발생 시에도 슬롯이 반드시 반환됩니다.
 * - 상태 조회 메서드([state], [activeCount], [availableSlots])는 근사값을 반환할 수 있습니다.
 *
 * ```kotlin
 * val election = LocalAsyncLeaderGroupElection(maxLeaders = 3)
 * val result = election.runAsyncIfLeader("batch-job") {
 *     CompletableFuture.completedFuture(processChunk())
 * }.join()
 * ```
 */
interface AsyncLeaderGroupElection : LeaderGroupElection {

    /**
     * 슬롯을 획득하여 리더로 선출되면 비동기 [action]을 실행합니다.
     *
     * ## 동작/계약
     * - 슬롯이 가득 찬 경우 빈 슬롯이 생길 때까지 [executor] 스레드가 블로킹됩니다.
     * - [action]이 반환하는 [CompletableFuture]가 완료될 때까지 슬롯을 보유합니다.
     * - [action] 실패(예외 또는 future 실패) 시에도 슬롯은 반드시 반환됩니다.
     * - 기본 [executor]는 [VirtualThreadExecutor] 싱글턴으로, 블로킹 작업에 적합합니다.
     *
     * ```kotlin
     * val result = election.runAsyncIfLeader("job-lock") {
     *     CompletableFuture.completedFuture(42)
     * }.join()
     * // result == 42
     * ```
     *
     * @param lockName 리더 그룹 선출에 사용할 락 이름
     * @param executor 비동기 실행에 사용할 [Executor]. 기본값은 [VirtualThreadExecutor] 싱글턴
     * @param action 리더 선출 성공 시 실행할 비동기 작업
     * @return [action] 실행 결과를 담은 [CompletableFuture]
     */
    fun <T> runAsyncIfLeader(
        lockName: String,
        executor: Executor = VirtualThreadExecutor,
        action: () -> CompletableFuture<T>,
    ): CompletableFuture<T>
}
