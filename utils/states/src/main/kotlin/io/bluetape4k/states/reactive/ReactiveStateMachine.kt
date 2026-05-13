package io.bluetape4k.states.reactive

import io.bluetape4k.states.api.BaseStateMachine
import io.bluetape4k.states.api.TransitionResult
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Coroutine-first state machine with queued events and one-time effects.
 *
 * This API is optional and separate from the core synchronous and suspending
 * FSM APIs. It is useful when callers need a long-lived runtime that exposes
 * current state, accepts events, emits one-time effects, and owns lifecycle
 * side effects.
 *
 * @param S state type
 * @param E event type
 * @param F one-time effect type
 */
@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
interface ReactiveStateMachine<S: Any, E: Any, F: Any>: BaseStateMachine<S, E>, StateFlow<S>, AutoCloseable {

    /**
     * State stream backed by this machine.
     */
    val stateFlow: StateFlow<S>

    /**
     * One-time effects emitted by transition and state side-effect handlers.
     */
    val effects: Flow<F>

    /**
     * Sends [event] through the sequential event processor.
     *
     * @throws io.bluetape4k.states.api.StateMachineException when the machine is closed
     */
    suspend fun send(event: E): TransitionResult<S, E>

    /**
     * Cancels active state side effects and rejects later [send] calls.
     */
    override fun close()
}
