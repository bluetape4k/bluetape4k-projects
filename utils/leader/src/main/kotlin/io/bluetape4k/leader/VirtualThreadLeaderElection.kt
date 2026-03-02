package io.bluetape4k.leader

import io.bluetape4k.concurrent.virtualthread.VirtualFuture

/**
 * Virtual Thread 기반 리더 선출 실행 계약을 정의합니다.
 *
 * ## [AsyncLeaderElection] 과의 차이
 * - `action`이 `() -> T` 람다로, [java.util.concurrent.CompletableFuture] 래핑 없이 결과를 직접 반환합니다.
 * - 반환 타입이 [VirtualFuture]로, `await()` 또는 `toCompletableFuture()`로 결과를 소비합니다.
 * - 내부적으로 Virtual Thread를 사용하므로 락 대기·I/O 블로킹 시 carrier thread를 반납합니다.
 * - Java 21 이상 환경을 전제합니다.
 *
 * ## 동작/계약
 * - 구현체는 `lockName` 기준으로 리더 획득 성공 시에만 `action`을 실행합니다.
 * - `action` 예외는 [VirtualFuture.await] 호출 시 호출자에게 전파됩니다.
 * - 리더 선출 실패/실행 오류의 예외 모델은 구현체 정책을 따릅니다.
 *
 * ```kotlin
 * val future = election.runAsyncIfLeader("batch-lock") { "ok" }
 * val result = future.await()  // "ok"
 * ```
 */
interface VirtualThreadLeaderElection {

    /**
     * 리더 획득 성공 시 [action]을 Virtual Thread에서 비동기로 실행합니다.
     *
     * ## 동작/계약
     * - `action`은 결과를 직접 반환하며 [java.util.concurrent.CompletableFuture] 래핑이 불필요합니다.
     * - [VirtualFuture.await]로 결과를 동기 대기하거나 [VirtualFuture.toCompletableFuture]로 변환할 수 있습니다.
     * - `action` 실행 중 예외 발생 시에도 락이 안전하게 해제됩니다.
     * - `lockName` 검증 규칙은 구현체 정책을 따릅니다.
     *
     * ```kotlin
     * val result = election.runAsyncIfLeader("job-lock") { computeResult() }.await()
     * ```
     *
     * @param lockName 리더 선출에 사용할 락 이름
     * @param action 리더 획득 성공 시 실행할 작업. 결과를 직접 반환합니다.
     * @return [action] 실행 결과를 담은 [VirtualFuture]
     */
    fun <T> runAsyncIfLeader(
        lockName: String,
        action: () -> T,
    ): VirtualFuture<T>
}
