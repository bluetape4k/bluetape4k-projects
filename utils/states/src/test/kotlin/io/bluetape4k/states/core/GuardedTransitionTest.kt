package io.bluetape4k.states.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.states.api.StateMachineException
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GuardedTransitionTest {

    companion object: KLogging()

    enum class State {
        PENDING,
        APPROVED,
        REJECTED
    }

    sealed class Event {
        data class Approve(val approvedBy: String?): Event()
        data object Reject: Event()
    }

    @Test
    fun `guard 조건이 true이면 전이가 성공한다`() {
        val fsm = stateMachine<State, Event> {
            initialState = State.PENDING
            finalStates = setOf(State.APPROVED, State.REJECTED)

            transition(State.PENDING, on<Event.Approve>(), to = State.APPROVED) {
                guard { _, event -> (event as Event.Approve).approvedBy != null }
            }
            transition(State.PENDING, on<Event.Reject>(), to = State.REJECTED)
        }

        val result = fsm.transition(Event.Approve(approvedBy = "admin"))
        result.currentState shouldBeEqualTo State.APPROVED
    }

    @Test
    fun `guard 조건이 false이면 예외가 발생한다`() {
        val fsm = stateMachine<State, Event> {
            initialState = State.PENDING
            finalStates = setOf(State.APPROVED, State.REJECTED)

            transition(State.PENDING, on<Event.Approve>(), to = State.APPROVED) {
                guard { _, event -> (event as Event.Approve).approvedBy != null }
            }
        }

        assertThrows<StateMachineException> {
            fsm.transition(Event.Approve(approvedBy = null))
        }
    }

    @Test
    fun `canTransition이 guard 조건을 반영한다`() {
        val fsm = stateMachine<State, Event> {
            initialState = State.PENDING
            finalStates = setOf(State.APPROVED)

            transition(State.PENDING, on<Event.Approve>(), to = State.APPROVED) {
                guard { _, event -> (event as Event.Approve).approvedBy != null }
            }
        }

        fsm.canTransition(Event.Approve(approvedBy = "admin")).shouldBeTrue()
        fsm.canTransition(Event.Approve(approvedBy = null)).shouldBeFalse()
    }

    @Test
    fun `guard 없는 전이는 항상 통과한다`() {
        val fsm = stateMachine<State, Event> {
            initialState = State.PENDING
            finalStates = setOf(State.REJECTED)

            transition(State.PENDING, on<Event.Reject>(), to = State.REJECTED)
        }

        val result = fsm.transition(Event.Reject)
        result.currentState shouldBeEqualTo State.REJECTED
    }
}
