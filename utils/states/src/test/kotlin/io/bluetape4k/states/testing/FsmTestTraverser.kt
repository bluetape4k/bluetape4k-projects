package io.bluetape4k.states.testing

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.states.api.StateMachine
import io.bluetape4k.states.api.StateMachineException
import io.bluetape4k.states.api.SuspendStateMachineInterface

class TransitionExpectation<out S: Any, out E: Any>(val from: S, val event: E, val to: S)
class PartialExpectation<out S: Any, out E: Any>(val from: S, val event: E)

infix fun <S: Any, E: Any> S.via(event: E): PartialExpectation<S, E> = PartialExpectation(this, event)
infix fun <S: Any, E: Any> PartialExpectation<S, E>.arrives(to: S): TransitionExpectation<S, E> =
    TransitionExpectation(from, event, to)

fun <S: Any, E: Any> StateMachine<S, E>.verifyPath(vararg ex: TransitionExpectation<S, E>) {
    ex.forEach { e -> currentState shouldBeEqualTo e.from; transition(e.event); currentState shouldBeEqualTo e.to }
}

fun <S: Any, E: Any> StateMachine<S, E>.assertReaches(target: S, vararg events: E) {
    events.forEach { transition(it) }; currentState shouldBeEqualTo target
}

fun <S: Any, E: Any> StateMachine<S, E>.assertRejects(event: E) {
    assertFailsWith<StateMachineException> { transition(event) }
}

suspend fun <S: Any, E: Any> SuspendStateMachineInterface<S, E>.verifyPath(vararg ex: TransitionExpectation<S, E>) {
    ex.forEach { e -> currentState shouldBeEqualTo e.from; transition(e.event); currentState shouldBeEqualTo e.to }
}

suspend fun <S: Any, E: Any> SuspendStateMachineInterface<S, E>.assertReaches(target: S, vararg events: E) {
    events.forEach { transition(it) }; currentState shouldBeEqualTo target
}

suspend fun <S: Any, E: Any> SuspendStateMachineInterface<S, E>.assertRejects(event: E) {
    assertFailsWith<StateMachineException> { transition(event) }
}
