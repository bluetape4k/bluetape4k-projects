package io.bluetape4k.states.reactive

import io.bluetape4k.states.api.StateMachineException
import io.bluetape4k.states.core.ParentTransitionKey
import io.bluetape4k.states.core.StateMachineDsl
import io.bluetape4k.states.core.TransitionKey
import io.bluetape4k.states.core.TransitionRegistry
import io.bluetape4k.states.core.TransitionTarget
import kotlinx.coroutines.CoroutineScope

/**
 * Configures a reactive transition.
 *
 * @param S state type
 * @param E event type
 * @param F effect type
 */
@StateMachineDsl
class ReactiveTransitionBuilder<S: Any, E: Any, F: Any> {

    internal var guardFunction: ((S, E) -> Boolean)? = null
        private set

    internal var effectHandler: (suspend ReactiveEffectScope<E, F>.(S, E, S) -> Unit)? = null
        private set

    /**
     * Sets a guard condition for this transition.
     */
    fun guard(predicate: (S, E) -> Boolean) {
        guardFunction = predicate
    }

    /**
     * Sets an effect handler that runs after a successful transition.
     */
    fun effect(handler: suspend ReactiveEffectScope<E, F>.(S, E, S) -> Unit) {
        effectHandler = handler
    }
}

/**
 * Configures lifecycle side effects for a state or state family.
 */
@StateMachineDsl
class ReactiveStateBuilder<S: Any, E: Any, F: Any> internal constructor(
    private val register: (ReactiveSideEffect<S, E, F>) -> Unit,
    private val matcher: ReactiveStateMatcher<S>,
) {

    /**
     * Starts [block] whenever the matching state is active.
     */
    fun sideEffect(block: suspend ReactiveEffectScope<E, F>.(S) -> Unit) {
        sideEffect(key = { it }, block = block)
    }

    /**
     * Starts [block] whenever the matching state is active and restarts it only
     * when [key] changes.
     */
    fun sideEffect(
        key: (S) -> Any?,
        block: suspend ReactiveEffectScope<E, F>.(S) -> Unit,
    ) {
        register(ReactiveSideEffect(matcher, key, block))
    }
}

/**
 * Builder for [ReactiveStateMachine].
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
     * Registers an exact state transition.
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
     * Registers an inherited transition for a state family.
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
     * Configures side effects for one exact state value.
     */
    fun onState(
        state: S,
        block: ReactiveStateBuilder<S, E, F>.() -> Unit,
    ) {
        ReactiveStateBuilder(::registerSideEffect, ReactiveStateMatcher.Exact(state)).apply(block)
    }

    /**
     * Configures side effects for every state matching [stateType].
     */
    fun onState(
        stateType: Class<out S>,
        block: ReactiveStateBuilder<S, E, F>.() -> Unit,
    ) {
        ReactiveStateBuilder(::registerSideEffect, ReactiveStateMatcher.Parent(stateType)).apply(block)
    }

    /**
     * Creates a reactive state machine.
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
 * Creates a [ReactiveStateMachine].
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
