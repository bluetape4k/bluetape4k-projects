package io.bluetape4k.leader

/**
 * 동기 방식 리더 선출 실행 계약을 정의합니다.
 *
 * ## 동작/계약
 * - 동일 [lockName]에 대해 구현체가 리더 획득에 성공한 호출만 [action]을 실행합니다.
 * - 리더 획득/해제 전략과 실패 처리(예외/재시도)는 구현체 정책을 따릅니다.
 * - [action] 실행 시점의 스레드와 컨텍스트는 구현체에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val leaderElection = DefaultLeaderElection()
 * val result = leaderElection.runIfLeader("daily-job") {
 *     "done"
 * }
 * // result == "done" (리더 획득 성공 경로)
 */
interface LeaderElection: AsyncLeaderElection {

    /**
     * 리더로 선출된 경우에만 동기 [action]을 실행합니다.
     *
     * ## 동작/계약
     * - [lockName]에 대한 리더 획득 성공 시 [action]을 1회 실행합니다.
     * - [action]에서 발생한 예외는 호출자에게 전파됩니다.
     * - [lockName] 유효성(blank 허용 여부)은 구현체의 입력 검증 규칙을 따릅니다.
     *
     * ```kotlin
     * val value = leaderElection.runIfLeader("job-lock") { 42 }
     * // value == 42
     * ```
     *
     * @param lockName 리더 선출에 사용할 락 이름
     * @param action 리더 획득 성공 시 실행할 동기 작업
     * @return [action] 실행 결과
     */
    fun <T> runIfLeader(lockName: String, action: () -> T): T

}
