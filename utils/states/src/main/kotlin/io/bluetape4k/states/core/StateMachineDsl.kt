package io.bluetape4k.states.core

import io.bluetape4k.states.api.StateMachine
import io.bluetape4k.states.api.SuspendStateMachineInterface
import io.bluetape4k.states.coroutines.SuspendStateMachine

/**
 * 상태 머신 DSL 마커 어노테이션입니다.
 *
 * DSL 빌더 내에서 외부 리시버 접근을 방지합니다.
 */
@DslMarker
annotation class StateMachineDsl

/**
 * 상태 전이에 guard 조건을 설정하는 빌더입니다.
 *
 * ```kotlin
 * transition(State.PENDING, on<ApproveEvent>(), to = State.APPROVED) {
 *     guard { state, event -> (event as ApproveEvent).approvedBy != null }
 * }
 * ```
 *
 * @param S 상태 타입
 * @param E 이벤트 타입
 */
@StateMachineDsl
class TransitionBuilder<S : Any, E : Any> {
    /**
     * Guard 조건 람다를 설정합니다.
     */
    var guardFunction: ((S, E) -> Boolean)? = null
        private set

    /**
     * Guard 조건을 설정합니다.
     *
     * @param predicate 상태와 이벤트를 받아 전이 허용 여부를 반환하는 함수
     */
    fun guard(predicate: (S, E) -> Boolean) {
        guardFunction = predicate
    }
}

/**
 * 동기 상태 머신 DSL 빌더입니다.
 *
 * ```kotlin
 * val fsm = stateMachine<OrderState, OrderEvent> {
 *     initialState = OrderState.CREATED
 *     finalStates = setOf(OrderState.DELIVERED, OrderState.CANCELLED)
 *
 *     transition(OrderState.CREATED, on<OrderEvent.Pay>(), to = OrderState.PAID)
 *     transition(OrderState.PAID, on<OrderEvent.Ship>(), to = OrderState.SHIPPED)
 *
 *     onTransition { prev, event, next ->
 *         println("$prev --[$event]--> $next")
 *     }
 * }
 * ```
 *
 * @param S 상태 타입
 * @param E 이벤트 타입
 */
@StateMachineDsl
class StateMachineBuilder<S : Any, E : Any> {
    /**
     * 초기 상태를 설정합니다.
     */
    lateinit var initialState: S

    /**
     * 종료 상태 집합을 설정합니다.
     */
    var finalStates: Set<S> = emptySet()

    private val transitions = mutableMapOf<TransitionKey<S, E>, TransitionTarget<S, E>>()
    private var onTransitionCallback: ((S, E, S) -> Unit)? = null

    /**
     * 상태 전이를 등록합니다.
     *
     * @param from 시작 상태
     * @param eventType 이벤트 클래스 타입
     * @param to 대상 상태
     * @param block guard 조건 설정 (선택 사항)
     */
    fun transition(
        from: S,
        eventType: Class<out E>,
        to: S,
        block: TransitionBuilder<S, E>.() -> Unit = {},
    ) {
        val builder = TransitionBuilder<S, E>().apply(block)
        val key = TransitionKey(from, eventType)
        val target = TransitionTarget(to, builder.guardFunction)
        transitions[key] = target
    }

    /**
     * 전이 콜백을 설정합니다.
     *
     * @param callback 이전 상태, 이벤트, 다음 상태를 받는 콜백
     */
    fun onTransition(callback: (S, E, S) -> Unit) {
        onTransitionCallback = callback
    }

    /**
     * [DefaultStateMachine]을 생성합니다.
     */
    fun build(): StateMachine<S, E> = DefaultStateMachine(
        initialState = initialState,
        finalStates = finalStates,
        transitions = transitions.toMap(),
        onTransitionCallback = onTransitionCallback,
    )
}

/**
 * 코루틴 상태 머신 DSL 빌더입니다.
 *
 * ```kotlin
 * val fsm = suspendStateMachine<AppointmentState, AppointmentEvent> {
 *     initialState = AppointmentState.PENDING
 *     finalStates = setOf(AppointmentState.COMPLETED, AppointmentState.CANCELLED)
 *
 *     transition(AppointmentState.PENDING, on<AppointmentEvent.Request>(), to = AppointmentState.REQUESTED)
 *
 *     onTransition { prev, event, next ->
 *         println("$prev --> $next")
 *     }
 * }
 * ```
 *
 * @param S 상태 타입
 * @param E 이벤트 타입
 */
@StateMachineDsl
class SuspendStateMachineBuilder<S : Any, E : Any> {
    /**
     * 초기 상태를 설정합니다.
     */
    lateinit var initialState: S

    /**
     * 종료 상태 집합을 설정합니다.
     */
    var finalStates: Set<S> = emptySet()

    private val transitions = mutableMapOf<TransitionKey<S, E>, TransitionTarget<S, E>>()
    private var onTransitionCallback: (suspend (S, E, S) -> Unit)? = null

    /**
     * 상태 전이를 등록합니다.
     *
     * @param from 시작 상태
     * @param eventType 이벤트 클래스 타입
     * @param to 대상 상태
     * @param block guard 조건 설정 (선택 사항)
     */
    fun transition(
        from: S,
        eventType: Class<out E>,
        to: S,
        block: TransitionBuilder<S, E>.() -> Unit = {},
    ) {
        val builder = TransitionBuilder<S, E>().apply(block)
        val key = TransitionKey(from, eventType)
        val target = TransitionTarget(to, builder.guardFunction)
        transitions[key] = target
    }

    /**
     * suspend 전이 콜백을 설정합니다.
     *
     * @param callback 이전 상태, 이벤트, 다음 상태를 받는 suspend 콜백
     */
    fun onTransition(callback: suspend (S, E, S) -> Unit) {
        onTransitionCallback = callback
    }

    /**
     * [SuspendStateMachine]을 생성합니다.
     */
    fun build(): SuspendStateMachineInterface<S, E> = SuspendStateMachine(
        initialState = initialState,
        finalStates = finalStates,
        transitions = transitions.toMap(),
        onTransitionCallback = onTransitionCallback,
    )
}

/**
 * 동기 상태 머신을 DSL로 생성합니다.
 *
 * ```kotlin
 * val fsm = stateMachine<OrderState, OrderEvent> {
 *     initialState = OrderState.CREATED
 *     finalStates = setOf(OrderState.DELIVERED)
 *     transition(OrderState.CREATED, on<OrderEvent.Pay>(), to = OrderState.PAID)
 * }
 * ```
 *
 * @param S 상태 타입
 * @param E 이벤트 타입
 * @param block DSL 빌더 블록
 * @return 생성된 [StateMachine]
 */
fun <S : Any, E : Any> stateMachine(
    block: StateMachineBuilder<S, E>.() -> Unit,
): StateMachine<S, E> = StateMachineBuilder<S, E>().apply(block).build()

/**
 * 코루틴 상태 머신을 DSL로 생성합니다.
 *
 * ```kotlin
 * val fsm = suspendStateMachine<AppointmentState, AppointmentEvent> {
 *     initialState = AppointmentState.PENDING
 *     finalStates = setOf(AppointmentState.COMPLETED)
 *     transition(AppointmentState.PENDING, on<AppointmentEvent.Request>(), to = AppointmentState.REQUESTED)
 * }
 * ```
 *
 * @param S 상태 타입
 * @param E 이벤트 타입
 * @param block DSL 빌더 블록
 * @return 생성된 [SuspendStateMachineInterface]
 */
fun <S : Any, E : Any> suspendStateMachine(
    block: SuspendStateMachineBuilder<S, E>.() -> Unit,
): SuspendStateMachineInterface<S, E> = SuspendStateMachineBuilder<S, E>().apply(block).build()

/**
 * 이벤트 타입을 Class 객체로 반환하는 헬퍼 함수입니다.
 *
 * DSL에서 전이 등록 시 이벤트 타입을 지정하는 데 사용됩니다.
 *
 * ```kotlin
 * transition(State.A, on<MyEvent>(), to = State.B)
 * ```
 *
 * @param E 이벤트 타입
 * @return 이벤트 클래스 객체
 */
inline fun <reified E : Any> on(): Class<E> = E::class.java
