package io.bluetape4k.leader.coroutines

/**
 * 코루틴 기반 리더 선출 실행 계약을 정의합니다.
 *
 * ## 동작/계약
 * - 구현체는 동일 [lockName]에 대해 리더 획득 성공 호출만 [action]을 실행합니다.
 * - [action]은 suspend 함수이며 호출 컨텍스트/디스패처는 구현체 정책을 따릅니다.
 * - 리더 선출 실패/취소/예외 전파 규칙은 구현체에 위임됩니다.
 *
 * ```kotlin
 * val result = election.runIfLeader("sync-job") { "ok" }
 * // result == "ok"
 * ```
 */
interface SuspendLeaderElection {

    /**
     * 리더 획득 성공 시 suspend [action]을 실행합니다.
     *
     * ## 동작/계약
     * - [lockName] 기준 리더 획득 성공 시 [action]을 1회 실행합니다.
     * - [action] 예외는 호출자에게 전파됩니다.
     * - [lockName] 검증 규칙은 구현체 정책을 따릅니다.
     *
     * ```kotlin
     * val value = election.runIfLeader("job-lock") { 7 }
     * // value == 7
     * ```
     *
     * @param lockName 리더 선출에 사용할 락 이름
     * @param action 리더 획득 성공 시 실행할 suspend 작업
     * @return [action] 실행 결과
     */
    suspend fun <T> runIfLeader(
        lockName: String,
        action: suspend () -> T,
    ): T
}
