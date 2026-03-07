package io.bluetape4k.leader

/**
 * 리더 그룹 선출의 상태 조회 메서드를 정의하는 공통 인터페이스입니다.
 *
 * [AsyncLeaderGroupElection], [LeaderGroupElection], [VirtualThreadLeaderGroupElection] 등
 * 모든 리더 그룹 선출 인터페이스가 이 인터페이스를 상속하여 상태 조회 메서드를 공유합니다.
 *
 * ```kotlin
 * val election: LeaderGroupElectionState = LocalLeaderGroupElection(maxLeaders = 3)
 * val state = election.state("batch-job")  // LeaderGroupState
 * ```
 */
interface LeaderGroupElectionState {

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
}
