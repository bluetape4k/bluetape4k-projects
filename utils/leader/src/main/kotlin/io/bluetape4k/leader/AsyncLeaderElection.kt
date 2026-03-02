package io.bluetape4k.leader

import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

/**
 * 비동기 리더 선출 실행 계약을 정의합니다.
 *
 * ## 동작/계약
 * - 구현체는 `lockName` 기준으로 리더 획득 성공 시에만 `action`을 실행합니다.
 * - 반환 [CompletableFuture] 완료 시점은 리더 획득 및 `action` 완료 시점에 의존합니다.
 * - 리더 선출 실패/실행 오류의 예외 모델은 구현체 정책을 따릅니다.
 * - 기본 [Executor]는 [VirtualThreadExecutor] 싱글턴을 사용합니다.
 *   Virtual Thread는 락 대기·future 블로킹 시 carrier thread를 반납하므로 비동기 선출에 적합합니다.
 *   Java 21 이상 환경을 전제합니다.
 *
 * ```kotlin
 * val future = election.runAsyncIfLeader("batch-lock") { CompletableFuture.completedFuture("ok") }
 * // future.join() == "ok"
 * ```
 */
interface AsyncLeaderElection {

    /**
     * 리더 획득 성공 시 비동기 [action]을 실행합니다.
     *
     * ## 동작/계약
     * - [executor]는 리더 선출/작업 실행 경로에서 구현체가 사용할 수 있습니다.
     * - [action]이 반환한 future의 성공/실패 상태가 결과 future로 반영됩니다.
     * - [lockName] 검증 규칙은 구현체 정책을 따릅니다.
     * - 기본 [executor]는 [VirtualThreadExecutor] 싱글턴으로, 블로킹 작업에 적합합니다.
     *
     * ```kotlin
     * val result = election.runAsyncIfLeader("job", action = { CompletableFuture.completedFuture(1) }).join()
     * // result == 1
     * ```
     *
     * @param lockName 리더 선출에 사용할 락 이름
     * @param executor 비동기 실행에 사용할 [Executor]. 기본값은 [VirtualThreadExecutor] 싱글턴
     * @param action 리더 획득 성공 시 실행할 비동기 작업
     * @return 리더 실행 결과를 담은 [CompletableFuture]
     */
    fun <T> runAsyncIfLeader(
        lockName: String,
        executor: Executor = VirtualThreadExecutor,
        action: () -> CompletableFuture<T>,
    ): CompletableFuture<T>
}
