package io.bluetape4k.leader

/**
 * 리더 그룹의 현재 상태 정보를 담는 불변 데이터 클래스입니다.
 *
 * [LeaderGroupElection] 및 [SuspendLeaderGroupElection] 구현체에서 공통으로 사용합니다.
 *
 * ```kotlin
 * val state = election.state("batch-lock")
 * println("활성 리더: ${state.activeCount}/${state.maxLeaders}")
 * if (state.isFull) println("슬롯이 가득 참")
 * ```
 *
 * @property lockName 리더 그룹 식별에 사용하는 락 이름
 * @property maxLeaders 허용하는 최대 동시 리더 수
 * @property activeCount 현재 활성(실행 중인) 리더 수
 */
data class LeaderGroupState(
    val lockName: String,
    val maxLeaders: Int,
    val activeCount: Int,
) {
    /** 새 리더를 수용할 수 있는 남은 슬롯 수 */
    val availableSlots: Int get() = maxLeaders - activeCount

    /** 최대 리더 수에 도달하여 추가 선출이 불가한지 여부 */
    val isFull: Boolean get() = activeCount >= maxLeaders

    /** 현재 활성 리더가 없는지 여부 */
    val isEmpty: Boolean get() = activeCount == 0
}
