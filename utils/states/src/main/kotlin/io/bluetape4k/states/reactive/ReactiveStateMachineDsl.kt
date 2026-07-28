package io.bluetape4k.states.reactive

import io.bluetape4k.states.api.StateMachineException
import io.bluetape4k.states.core.ParentTransitionKey
import io.bluetape4k.states.core.StateMachineDsl
import io.bluetape4k.states.core.TransitionKey
import io.bluetape4k.states.core.TransitionRegistry
import io.bluetape4k.states.core.TransitionTarget
import kotlinx.coroutines.CoroutineScope

/**
 * reactive transition을 설정합니다.
 *
 * @param S state 타입입니다.
 * @param E event 타입입니다.
 * @param F effect 타입입니다.
 */
@StateMachineDsl
class ReactiveTransitionBuilder<S: Any, E: Any, F: Any> {

    internal var guardFunction: ((S, E) -> Boolean)? = null
        private set

    internal var effectHandler: (suspend ReactiveEffectScope<E, F>.(S, E, S) -> Unit)? = null
        private set

    /**
     * 이 transition의 guard 조건을 설정합니다.
     */
    fun guard(predicate: (S, E) -> Boolean) {
        guardFunction = predicate
    }

    /**
     * transition 성공 후 실행할 effect handler를 설정합니다.
     */
    fun effect(handler: suspend ReactiveEffectScope<E, F>.(S, E, S) -> Unit) {
        effectHandler = handler
    }
}

/**
 * state 또는 state family에 대한 lifecycle side effect를 설정합니다.
 */
@StateMachineDsl
class ReactiveStateBuilder<S: Any, E: Any, F: Any> internal constructor(
    private val register: (ReactiveSideEffect<S, E, F>) -> Unit,
    private val matcher: ReactiveStateMatcher<S>,
) {

    /**
     * matching state가 active일 때마다 [block]을 시작합니다.
     */
    fun sideEffect(block: suspend ReactiveEffectScope<E, F>.(S) -> Unit) {
        sideEffect(key = { it }, block = block)
    }

    /**
     * matching state가 active일 때마다 [block]을 시작하고 [key]가 바뀔 때만 다시 시작합니다.
     */
    fun sideEffect(
        key: (S) -> Any?,
        block: suspend ReactiveEffectScope<E, F>.(S) -> Unit,
    ) {
        register(ReactiveSideEffect(matcher, key, block))
    }
}

/**
 * [ReactiveStateMachine]을 구성하는 builder입니다.
 */
@StateMachineDsl
class ReactiveStateMachineBuilder<S: Any, E: Any, F: Any> internal constructor(
    private val scope: CoroutineScope,
) {

    lateinit var initialState: S
    var finalStates: Set<S> = emptySet()

    private val transitions = mutableMapOf<TransitionKey<S, E>, TransitionTarget<S, E>>()
    private val parentTransitions = mutableMapOf<ParentTransitionKey<S, E>, TransitionTarget<S, E>>()
    private val transitionEffects =
        mutableMapOf<TransitionKey<S, E>, suspend ReactiveEffectScope<E, F>.(S, E, S) -> Unit>()
    private val parentTransitionEffects =
        mutableMapOf<ParentTransitionKey<S, E>, suspend ReactiveEffectScope<E, F>.(S, E, S) -> Unit>()
    private val sideEffects = mutableListOf<ReactiveSideEffect<S, E, F>>()

    /**
     * 정확히 일치하는 state transition을 등록합니다.
     */
    fun transition(
        from: S,
        eventType: Class<out E>,
        to: S,
        block: ReactiveTransitionBuilder<S, E, F>.() -> Unit = {},
    ) {
        val builder = ReactiveTransitionBuilder<S, E, F>().apply(block)
        val key = TransitionKey(from, eventType)
        transitions[key] = TransitionTarget(to, builder.guardFunction)
        builder.effectHandler?.let { transitionEffects[key] = it }
    }

    /**
     * state family에 대한 inherited transition을 등록합니다.
     */
    fun transition(
        from: Class<out S>,
        eventType: Class<out E>,
        to: S,
        block: ReactiveTransitionBuilder<S, E, F>.() -> Unit = {},
    ) {
        val builder = ReactiveTransitionBuilder<S, E, F>().apply(block)
        val key = ParentTransitionKey(from, eventType)
        parentTransitions[key] = TransitionTarget(to, builder.guardFunction)
        builder.effectHandler?.let { parentTransitionEffects[key] = it }
    }

    /**
     * 하나의 정확한 state 값에 대한 side effect를 설정합니다.
     */
    fun onState(
        state: S,
        block: ReactiveStateBuilder<S, E, F>.() -> Unit,
    ) {
        ReactiveStateBuilder(::registerSideEffect, ReactiveStateMatcher.Exact(state)).apply(block)
    }

    /**
     * [stateType]과 일치하는 모든 state에 대한 side effect를 설정합니다.
     */
    fun onState(
        stateType: Class<out S>,
        block: ReactiveStateBuilder<S, E, F>.() -> Unit,
    ) {
        ReactiveStateBuilder(::registerSideEffect, ReactiveStateMatcher.Parent(stateType)).apply(block)
    }

    /**
     * reactive state machine을 생성합니다.
     */
    fun build(): ReactiveStateMachine<S, E, F> {
        val exactTransitions = transitions.toMap()
        val inheritedTransitions = parentTransitions.toMap()
        val knownStates = buildSet {
            add(initialState)
            addAll(finalStates)
            addAll(exactTransitions.keys.map { it.state })
            addAll(exactTransitions.values.map { it.state })
            addAll(inheritedTransitions.values.map { it.state })
        }
        TransitionRegistry(exactTransitions, inheritedTransitions).validateKnownStates(knownStates)

        if (exactTransitions.isEmpty() && inheritedTransitions.isEmpty()) {
            throw StateMachineException("Reactive state machine requires at least one transition")
        }

        return DefaultReactiveStateMachine(
            initialState = initialState,
            finalStates = finalStates,
            transitions = exactTransitions,
            parentTransitions = inheritedTransitions,
            transitionEffects = transitionEffects.toMap(),
            parentTransitionEffects = parentTransitionEffects.toMap(),
            sideEffects = sideEffects.toList(),
            parentScope = scope,
        )
    }

    private fun registerSideEffect(sideEffect: ReactiveSideEffect<S, E, F>) {
        sideEffects += sideEffect
    }
}

/**
 * [ReactiveStateMachine]을 생성합니다.
 */
fun <S: Any, E: Any, F: Any> reactiveStateMachine(
    scope: CoroutineScope,
    block: ReactiveStateMachineBuilder<S, E, F>.() -> Unit,
): ReactiveStateMachine<S, E, F> =
    ReactiveStateMachineBuilder<S, E, F>(scope).apply(block).build()

internal sealed class ReactiveStateMatcher<S: Any> {

    abstract fun matches(state: S): Boolean

    class Exact<S: Any>(private val expectedState: S): ReactiveStateMatcher<S>() {
        override fun matches(state: S): Boolean = state == expectedState
    }

    class Parent<S: Any>(private val expectedType: Class<out S>): ReactiveStateMatcher<S>() {
        override fun matches(state: S): Boolean = expectedType.isInstance(state)
    }
}

internal class ReactiveSideEffect<S: Any, E: Any, F: Any>(
    private val matcher: ReactiveStateMatcher<S>,
    val key: (S) -> Any?,
    val block: suspend ReactiveEffectScope<E, F>.(S) -> Unit,
) {

    fun matches(state: S): Boolean = matcher.matches(state)
}
