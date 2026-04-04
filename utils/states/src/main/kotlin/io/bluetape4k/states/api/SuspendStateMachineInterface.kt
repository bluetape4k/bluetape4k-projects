package io.bluetape4k.states.api

import kotlinx.coroutines.flow.StateFlow

/**
 * 코루틴 기반 유한 상태 머신(FSM) 인터페이스입니다.
 *
 * [BaseStateMachine]을 확장하여 suspend 상태 전이와 [StateFlow] 기반 상태 관찰 기능을 제공합니다.
 * 내부적으로 `Mutex`를 사용하여 동시성을 직렬화합니다.
 *
 * **주의**: 이 인터페이스는 [StateMachine]을 구현하지 않습니다.
 * `suspend fun transition()`과 `fun transition()`의 시그니처 충돌을 방지하기 위함입니다.
 *
 * ```kotlin
 * val fsm: SuspendStateMachineInterface<State, Event> = suspendStateMachine {
 *     initialState = State.PENDING
 *     finalStates = setOf(State.COMPLETED)
 *     transition(State.PENDING, on<Event.Start>(), to = State.ACTIVE)
 * }
 *
 * // 상태 관찰
 * launch { fsm.stateFlow.collect { state -> println(state) } }
 *
 * // 전이
 * val result = fsm.transition(Event.Start())
 * ```
 *
 * @param S 상태 타입
 * @param E 이벤트 타입
 */
interface SuspendStateMachineInterface<S : Any, E : Any> : BaseStateMachine<S, E> {

    /**
     * 주어진 이벤트로 상태 전이를 수행합니다 (suspend).
     *
     * @param event 전이를 발생시킬 이벤트
     * @return 전이 결과
     * @throws StateMachineException 전이가 허용되지 않는 경우
     */
    suspend fun transition(event: E): TransitionResult<S, E>

    /**
     * 현재 상태를 관찰할 수 있는 [StateFlow]입니다.
     *
     * 상태 전이가 발생할 때마다 새로운 상태가 emit됩니다.
     */
    val stateFlow: StateFlow<S>
}
