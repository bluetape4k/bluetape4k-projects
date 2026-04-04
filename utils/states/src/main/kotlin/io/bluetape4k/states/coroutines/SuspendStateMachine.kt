package io.bluetape4k.states.coroutines

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.states.api.StateMachineException
import io.bluetape4k.states.api.SuspendStateMachineInterface
import io.bluetape4k.states.api.TransitionResult
import io.bluetape4k.states.core.TransitionKey
import io.bluetape4k.states.core.TransitionTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * [SuspendStateMachineInterface]의 기본 구현체입니다.
 *
 * `Mutex`를 사용하여 동시 전이를 직렬화하고,
 * `MutableStateFlow`를 통해 상태 변경을 관찰할 수 있습니다.
 *
 * ```kotlin
 * val fsm = suspendStateMachine<AppointmentState, AppointmentEvent> {
 *     initialState = AppointmentState.PENDING
 *     finalStates = setOf(AppointmentState.COMPLETED, AppointmentState.CANCELLED)
 *     transition(AppointmentState.PENDING, on<AppointmentEvent.Request>(), to = AppointmentState.REQUESTED)
 * }
 *
 * // 상태 관찰
 * launch { fsm.stateFlow.collect { state -> println(state) } }
 *
 * // 전이
 * val result = fsm.transition(AppointmentEvent.Request())
 * ```
 *
 * @param S 상태 타입
 * @param E 이벤트 타입
 * @property initialState 초기 상태
 * @property finalStates 종료 상태 집합
 * @property transitions 전이 맵
 * @property onTransitionCallback suspend 전이 콜백
 */
class SuspendStateMachine<S : Any, E : Any>(
    override val initialState: S,
    override val finalStates: Set<S>,
    private val transitions: Map<TransitionKey<S, E>, TransitionTarget<S, E>>,
    private val onTransitionCallback: (suspend (S, E, S) -> Unit)? = null,
) : SuspendStateMachineInterface<S, E> {

    companion object : KLogging()

    private val mutex = Mutex()
    private val _stateFlow = MutableStateFlow(initialState)

    override val stateFlow: StateFlow<S> = _stateFlow.asStateFlow()

    override val currentState: S
        get() = _stateFlow.value

    /**
     * 주어진 이벤트로 상태 전이를 수행합니다 (suspend).
     *
     * ```kotlin
     * val fsm = suspendStateMachine<AppointmentState, AppointmentEvent> {
     *     initialState = AppointmentState.PENDING
     *     finalStates = setOf(AppointmentState.COMPLETED)
     *     transition(AppointmentState.PENDING, on<AppointmentEvent.Request>(), to = AppointmentState.REQUESTED)
     * }
     * val result = fsm.transition(AppointmentEvent.Request())
     * // result.previousState == AppointmentState.PENDING
     * // result.currentState == AppointmentState.REQUESTED
     * ```
     *
     * @param event 전이를 발생시킬 이벤트
     * @return 전이 결과 ([TransitionResult])
     * @throws StateMachineException 허용되지 않는 전이이거나 guard 조건 실패 시
     */
    override suspend fun transition(event: E): TransitionResult<S, E> = mutex.withLock {
        val previous = _stateFlow.value

        if (previous in finalStates) {
            throw StateMachineException("이미 종료 상태입니다: $previous")
        }

        val key = TransitionKey(previous, event::class.java)
        val target = transitions[key]
            ?: throw StateMachineException(
                "허용되지 않은 전이: $previous + ${event::class.simpleName}. " +
                    "허용된 이벤트: ${allowedEvents().map { it.simpleName }}"
            )

        // Guard 조건 평가
        val guardResult = target.guard?.invoke(previous, event) ?: true
        if (!guardResult) {
            throw StateMachineException(
                "Guard 조건 실패: $previous + ${event::class.simpleName}"
            )
        }

        _stateFlow.value = target.state

        log.debug { "$previous --[${event::class.simpleName}]--> ${target.state}" }
        onTransitionCallback?.invoke(previous, event, target.state)

        TransitionResult(
            previousState = previous,
            event = event,
            currentState = target.state,
        )
    }

    /**
     * 현재 상태에서 주어진 이벤트로 전이 가능한지 확인합니다.
     *
     * ```kotlin
     * val fsm = suspendStateMachine<AppointmentState, AppointmentEvent> {
     *     initialState = AppointmentState.PENDING
     *     finalStates = setOf(AppointmentState.COMPLETED)
     *     transition(AppointmentState.PENDING, on<AppointmentEvent.Request>(), to = AppointmentState.REQUESTED)
     * }
     * fsm.canTransition(AppointmentEvent.Request())  // true
     * fsm.canTransition(AppointmentEvent.Cancel())   // false
     * ```
     *
     * @param event 확인할 이벤트
     * @return 전이 가능하면 true, 등록되지 않은 전이이거나 guard 조건 실패 시 false
     */
    override fun canTransition(event: E): Boolean {
        val key = TransitionKey(currentState, event::class.java)
        val target = transitions[key] ?: return false
        return target.guard?.invoke(currentState, event) ?: true
    }

    /**
     * 현재 상태에서 허용된 이벤트 클래스 목록을 반환합니다.
     *
     * ```kotlin
     * val fsm = suspendStateMachine<AppointmentState, AppointmentEvent> {
     *     initialState = AppointmentState.PENDING
     *     finalStates = setOf(AppointmentState.COMPLETED)
     *     transition(AppointmentState.PENDING, on<AppointmentEvent.Request>(), to = AppointmentState.REQUESTED)
     *     transition(AppointmentState.PENDING, on<AppointmentEvent.Cancel>(), to = AppointmentState.CANCELLED)
     * }
     * val events = fsm.allowedEvents()
     * // events == setOf(AppointmentEvent.Request::class.java, AppointmentEvent.Cancel::class.java)
     * ```
     *
     * @return 현재 상태에서 허용된 이벤트 클래스 집합
     */
    override fun allowedEvents(): Set<Class<out E>> {
        return transitions.keys
            .filter { it.state == currentState }
            .map { it.eventType }
            .toSet()
    }
}
