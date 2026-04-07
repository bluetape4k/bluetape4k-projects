package io.bluetape4k.states.api

/**
 * 상태 머신의 읽기 전용 공통 인터페이스입니다.
 *
 * 현재 상태 조회, 전이 가능 여부 확인, 허용된 이벤트 목록 조회 등
 * 동기/코루틴 상태 머신이 공통으로 제공하는 기능을 정의합니다.
 *
 * ```kotlin
 * val fsm: BaseStateMachine<OrderState, OrderEvent> = ...
 * println("현재 상태: ${fsm.currentState}")
 * println("전이 가능: ${fsm.canTransition(OrderEvent.Pay())}")
 * println("종료 상태: ${fsm.isInFinalState()}")
 * ```
 *
 * @param S 상태 타입
 * @param E 이벤트 타입
 */
interface BaseStateMachine<S: Any, E: Any> {

    /**
     * 현재 상태를 반환합니다.
     */
    val currentState: S

    /**
     * 초기 상태를 반환합니다.
     */
    val initialState: S

    /**
     * 종료 상태 집합을 반환합니다.
     */
    val finalStates: Set<S>

    /**
     * 현재 상태에서 주어진 이벤트로 전이 가능한지 확인합니다.
     *
     * @param event 확인할 이벤트
     * @return 전이 가능하면 true
     */
    fun canTransition(event: E): Boolean

    /**
     * 현재 상태에서 허용된 이벤트 클래스 목록을 반환합니다.
     *
     * @return 허용된 이벤트 클래스 집합
     */
    fun allowedEvents(): Set<Class<out E>>

    /**
     * 현재 상태가 종료 상태인지 확인합니다.
     *
     * @return 종료 상태이면 true
     */
    fun isInFinalState(): Boolean = currentState in finalStates
}
