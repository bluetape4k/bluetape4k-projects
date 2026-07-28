package io.bluetape4k.states.core

import java.io.Serializable

/**
 * state family에 등록된 inherited transition을 식별합니다.
 *
 * Exact state transitions still have precedence. Parent transitions are used
 * when the current state is an instance of [stateType] and no exact transition
 * matches the same event type.
 *
 * @param S state type
 * @param E event type
 * @property stateType parent state class or interface
 * @property eventType event class
 */
data class ParentTransitionKey<S: Any, E: Any>(
    val stateType: Class<out S>,
    val eventType: Class<out E>,
): Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }
}

