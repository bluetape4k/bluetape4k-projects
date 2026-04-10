package io.bluetape4k.states.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.states.api.StateMachine
import io.bluetape4k.states.api.StateMachineException
import io.bluetape4k.states.api.TransitionResult
import java.util.concurrent.atomic.AtomicReference

/**
 * [StateMachine]의 기본 구현체입니다.
 *
 * `AtomicReference`와 CAS(Compare-And-Swap) 연산을 사용하여
 * Thread Safety를 보장합니다. 동시 전이가 발생하면 CAS 실패 시
 * [StateMachineException]이 발생합니다.
 *
 * **동시성 정책**: 동시 전이가 필요한 경우 [io.bluetape4k.states.coroutines.SuspendStateMachine] 사용을 권장합니다.
 *
 * ```kotlin
 * val fsm = stateMachine<OrderState, OrderEvent> {
 *     initialState = OrderState.CREATED
 *     finalStates = setOf(OrderState.DELIVERED, OrderState.CANCELLED)
 *     transition(OrderState.CREATED, on<OrderEvent.Pay>(), to = OrderState.PAID)
 * }
 * val result = fsm.transition(OrderEvent.Pay())
 * ```
 *
 * @param S 상태 타입
 * @param E 이벤트 타입
 * @property initialState 초기 상태
 * @property finalStates 종료 상태 집합
 * @property transitions 전이 맵
 * @property onTransitionCallback 전이 콜백
 */
class DefaultStateMachine<S: Any, E: Any>(
    override val initialState: S,
    override val finalStates: Set<S>,
    private val transitions: Map<TransitionKey<S, E>, TransitionTarget<S, E>>,
    private val onTransitionCallback: ((S, E, S) -> Unit)? = null,
): StateMachine<S, E> {

    companion object: KLogging()

    private val _currentState = AtomicReference(initialState)

    override val currentState: S
        get() = _currentState.get()

    /**
     * 주어진 이벤트로 상태 전이를 수행합니다.
     *
     * ```kotlin
     * val fsm = stateMachine<OrderState, OrderEvent> {
     *     initialState = OrderState.CREATED
     *     finalStates = setOf(OrderState.DELIVERED)
     *     transition(OrderState.CREATED, on<OrderEvent.Pay>(), to = OrderState.PAID)
     * }
     * val result = fsm.transition(OrderEvent.Pay())
     * // result.previousState == OrderState.CREATED
     * // result.currentState == OrderState.PAID
     * ```
     *
     * @param event 전이를 발생시킬 이벤트
     * @return 전이 결과 ([TransitionResult])
     * @throws StateMachineException 허용되지 않는 전이이거나 guard 조건 실패, 또는 동시 CAS 충돌 시
     */
    override fun transition(event: E): TransitionResult<S, E> {
        val previous = _currentState.get()

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

        // CAS 전이
        if (!_currentState.compareAndSet(previous, target.state)) {
            throw StateMachineException(
                "동시 전이 충돌: 예상 상태=$previous, 실제 상태=${_currentState.get()}"
            )
        }

        log.debug { "$previous --[${event::class.simpleName}]--> ${target.state}" }
        onTransitionCallback?.invoke(previous, event, target.state)

        return TransitionResult(
            previousState = previous,
            event = event,
            currentState = target.state,
        )
    }

    /**
     * 현재 상태에서 주어진 이벤트로 전이 가능한지 확인합니다.
     *
     * ```kotlin
     * val fsm = stateMachine<OrderState, OrderEvent> {
     *     initialState = OrderState.CREATED
     *     finalStates = setOf(OrderState.DELIVERED)
     *     transition(OrderState.CREATED, on<OrderEvent.Pay>(), to = OrderState.PAID)
     * }
     * fsm.canTransition(OrderEvent.Pay())   // true
     * fsm.canTransition(OrderEvent.Ship())  // false
     * ```
     *
     * @param event 확인할 이벤트
     * @return 전이 가능하면 true, 등록되지 않은 전이이거나 guard 조건 실패 시 false
     */
    override fun canTransition(event: E): Boolean {
        val state = currentState
        if (state in finalStates) return false

        val key = TransitionKey(state, event::class.java)
        val target = transitions[key] ?: return false
        return target.guard?.invoke(state, event) ?: true
    }

    /**
     * 현재 상태에서 허용된 이벤트 클래스 목록을 반환합니다.
     *
     * ```kotlin
     * val fsm = stateMachine<OrderState, OrderEvent> {
     *     initialState = OrderState.CREATED
     *     finalStates = setOf(OrderState.DELIVERED)
     *     transition(OrderState.CREATED, on<OrderEvent.Pay>(), to = OrderState.PAID)
     *     transition(OrderState.CREATED, on<OrderEvent.Cancel>(), to = OrderState.CANCELLED)
     * }
     * val events = fsm.allowedEvents()
     * // events == setOf(OrderEvent.Pay::class.java, OrderEvent.Cancel::class.java)
     * ```
     *
     * @return 현재 상태에서 허용된 이벤트 클래스 집합
     */
    override fun allowedEvents(): Set<Class<out E>> {
        val state = currentState
        if (state in finalStates) return emptySet()

        return transitions.keys
            .filter { it.state == state }
            .map { it.eventType }
            .toSet()
    }
}
