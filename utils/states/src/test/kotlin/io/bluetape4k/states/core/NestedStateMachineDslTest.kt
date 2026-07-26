package io.bluetape4k.states.core

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.states.api.StateMachineException
import org.junit.jupiter.api.Test

class NestedStateMachineDslTest {

    private sealed interface Step
    private data object Draft: Step
    private sealed interface Active: Step
    private data object Review: Active
    private data object Approved: Active
    private data object Archived: Step
    private data object Cancelled: Step

    private sealed class Event {
        data object Submit: Event()
        data object Approve: Event()
        data object Cancel: Event()
        data object Archive: Event()
    }

    private sealed interface LeftBranch: Step
    private sealed interface RightBranch: Step
    private data object SharedState: LeftBranch, RightBranch

    @Test
    fun `nested transition applies to matching state family`() {
        val machine = stateMachine<Step, Event> {
            initialState = Review
            transition(state<Active>(), on<Event.Cancel>(), to = Cancelled)
        }

        machine.canTransition(Event.Cancel).shouldBeTrue()
        machine.allowedEvents() shouldBeEqualTo setOf(Event.Cancel::class.java)

        val result = machine.transition(Event.Cancel)

        result.previousState shouldBeEqualTo Review
        result.currentState shouldBeEqualTo Cancelled
    }

    @Test
    fun `exact transition overrides nested transition`() {
        val machine = stateMachine<Step, Event> {
            initialState = Review
            transition(state<Active>(), on<Event.Cancel>(), to = Cancelled)
            transition(Review, on<Event.Cancel>(), to = Archived)
        }

        machine.transition(Event.Cancel).currentState shouldBeEqualTo Archived
    }

    @Test
    fun `nested guard participates in canTransition`() {
        val machine = stateMachine<Step, Event> {
            initialState = Approved
            transition(state<Active>(), on<Event.Archive>(), to = Archived) {
                guard { state, _ -> state == Approved }
            }
        }

        machine.canTransition(Event.Archive).shouldBeTrue()
        machine.transition(Event.Archive).currentState shouldBeEqualTo Archived
    }

    @Test
    fun `nested guard rejection returns false from canTransition`() {
        val machine = stateMachine<Step, Event> {
            initialState = Review
            transition(state<Active>(), on<Event.Archive>(), to = Archived) {
                guard { state, _ -> state == Approved }
            }
        }

        machine.canTransition(Event.Archive).shouldBeFalse()
    }

    @Test
    fun `ambiguous nested transitions fail during build`() {
        assertFailsWith<StateMachineException> {
            stateMachine<Step, Event> {
                initialState = SharedState
                transition(state<LeftBranch>(), on<Event.Cancel>(), to = Cancelled)
                transition(state<RightBranch>(), on<Event.Cancel>(), to = Archived)
            }
        }
    }

    @Test
    fun `final state suppresses inherited transitions`() {
        val machine = stateMachine<Step, Event> {
            initialState = Review
            finalStates = setOf(Review)
            transition(state<Active>(), on<Event.Cancel>(), to = Cancelled)
        }

        machine.isInFinalState().shouldBeTrue()
        machine.canTransition(Event.Cancel).shouldBeFalse()
        machine.allowedEvents() shouldBeEqualTo emptySet()
    }
}
