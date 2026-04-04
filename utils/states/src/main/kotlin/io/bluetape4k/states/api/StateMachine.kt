package io.bluetape4k.states.api

/**
 * 동기 유한 상태 머신(FSM) 인터페이스입니다.
 *
 * [BaseStateMachine]을 확장하여 동기적 상태 전이 기능을 제공합니다.
 * Thread Safety는 내부적으로 `AtomicReference` CAS 연산을 통해 보장됩니다.
 *
 * ```kotlin
 * val fsm: StateMachine<OrderState, OrderEvent> = stateMachine {
 *     initialState = OrderState.CREATED
 *     finalStates = setOf(OrderState.DELIVERED)
 *     transition(OrderState.CREATED, on<OrderEvent.Pay>(), to = OrderState.PAID)
 * }
 * val result = fsm.transition(OrderEvent.Pay())
 * ```
 *
 * @param S 상태 타입
 * @param E 이벤트 타입
 */
interface StateMachine<S : Any, E : Any> : BaseStateMachine<S, E> {

    /**
     * 주어진 이벤트로 상태 전이를 수행합니다.
     *
     * @param event 전이를 발생시킬 이벤트
     * @return 전이 결과
     * @throws StateMachineException 전이가 허용되지 않는 경우
     */
    fun transition(event: E): TransitionResult<S, E>
}
