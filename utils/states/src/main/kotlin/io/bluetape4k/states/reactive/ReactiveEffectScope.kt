package io.bluetape4k.states.reactive

/**
 * Scope available to reactive transition and state side-effect handlers.
 *
 * Effects are one-time outputs and are not part of persistent state. Handlers
 * may also enqueue a follow-up event through [send]. Follow-up events are
 * scheduled on the owning machine and are processed after the current handler
 * returns.
 *
 * @param E event type
 * @param F effect type
 */
class ReactiveEffectScope<E: Any, F: Any> internal constructor(
    private val emitEffect: suspend (F) -> Unit,
    private val sendEvent: suspend (E) -> Unit,
) {

    /**
     * Emits a one-time effect.
     */
    suspend fun emit(effect: F) {
        emitEffect(effect)
    }

    /**
     * Enqueues a follow-up event to the owning reactive state machine.
     */
    suspend fun send(event: E) {
        sendEvent(event)
    }
}
