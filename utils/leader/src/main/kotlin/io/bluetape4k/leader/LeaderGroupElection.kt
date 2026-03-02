package io.bluetape4k.leader

/**
 * Semaphore 기반 복수 리더 선출 계약을 정의합니다.
 *
 * ## [LeaderElection] 과의 차이
 * - [LeaderElection]은 `lockName`당 리더를 1개로 제한합니다.
 * - [LeaderGroupElection]은 [maxLeaders]개까지 동시에 리더를 허용합니다.
 * - 내부적으로 `Semaphore(maxLeaders)`를 이용하여 동시 실행 수를 제한합니다.
 *
 * ## 동작/계약
 * - 구현체는 `lockName` 기준으로 최대 [maxLeaders]개의 `action`을 동시에 실행합니다.
 * - 슬롯이 가득 찬 경우, 빈 슬롯이 생길 때까지 호출 스레드가 블로킹됩니다.
 * - `action` 예외 발생 시에도 슬롯이 반드시 반환됩니다.
 * - 상태 조회 메서드([state], [activeCount], [availableSlots])는 근사값을 반환할 수 있습니다.
 *
 * ```kotlin
 * val election = LocalLeaderGroupElection(maxLeaders = 3)
 * val result = election.runIfLeader("batch-job") { processChunk() }
 *
 * println(election.state("batch-job"))  // LeaderGroupState(activeCount=2, ...)
 * ```
 */
interface LeaderGroupElection {

    /** 허용하는 최대 동시 리더 수 */
    val maxLeaders: Int

    /**
     * [lockName]에 대해 현재 활성(실행 중인) 리더 수를 반환합니다.
     *
     * @param lockName 조회할 락 이름
     * @return 현재 활성 리더 수 (근사값)
     */
    fun activeCount(lockName: String): Int

    /**
     * [lockName]에 대해 새 리더를 수용할 수 있는 남은 슬롯 수를 반환합니다.
     *
     * @param lockName 조회할 락 이름
     * @return 사용 가능한 슬롯 수 (근사값)
     */
    fun availableSlots(lockName: String): Int

    /**
     * [lockName]에 대한 현재 [LeaderGroupState]를 반환합니다.
     *
     * @param lockName 조회할 락 이름
     * @return 현재 리더 그룹 상태 스냅샷
     */
    fun state(lockName: String): LeaderGroupState

    /**
     * 슬롯을 획득하여 리더로 선출되면 [action]을 실행합니다.
     *
     * ## 동작/계약
     * - 슬롯이 가득 찬 경우 빈 슬롯이 생길 때까지 블로킹됩니다.
     * - [action] 예외 발생 시에도 슬롯은 반드시 반환됩니다.
     * - [action] 실행 중 [activeCount]가 증가하고, 완료 시 감소합니다.
     *
     * ```kotlin
     * val result = election.runIfLeader("job-lock") { compute() }
     * ```
     *
     * @param lockName 리더 그룹 선출에 사용할 락 이름
     * @param action 리더 선출 성공 시 실행할 동기 작업
     * @return [action] 실행 결과
     */
    fun <T> runIfLeader(lockName: String, action: () -> T): T
}
