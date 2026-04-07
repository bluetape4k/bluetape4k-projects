package io.bluetape4k.states.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.states.api.StateMachineException
import io.bluetape4k.states.api.TransitionResult
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DefaultStateMachineTest {

    companion object: KLogging()

    enum class State {
        A,
        B,
        C,
        FINAL
    }

    sealed class Event {
        data object GoB: Event()
        data object GoC: Event()
        data object GoFinal: Event()
    }

    private fun createFsm() = stateMachine<State, Event> {
        initialState = State.A
        finalStates = setOf(State.FINAL)
        transition(State.A, on<Event.GoB>(), to = State.B)
        transition(State.B, on<Event.GoC>(), to = State.C)
        transition(State.C, on<Event.GoFinal>(), to = State.FINAL)
    }

    @Test
    fun `초기 상태가 올바르게 설정된다`() {
        val fsm = createFsm()
        fsm.currentState shouldBeEqualTo State.A
        fsm.initialState shouldBeEqualTo State.A
    }

    @Test
    fun `정상적인 상태 전이가 성공한다`() {
        val fsm = createFsm()

        val result = fsm.transition(Event.GoB)

        result shouldBeEqualTo TransitionResult(State.A, Event.GoB, State.B)
        fsm.currentState shouldBeEqualTo State.B
    }

    @Test
    fun `연속 전이가 성공한다`() {
        val fsm = createFsm()

        fsm.transition(Event.GoB)
        fsm.transition(Event.GoC)
        val result = fsm.transition(Event.GoFinal)

        result.currentState shouldBeEqualTo State.FINAL
        fsm.isInFinalState().shouldBeTrue()
    }

    @Test
    fun `허용되지 않은 전이에서 예외가 발생한다`() {
        val fsm = createFsm()

        assertThrows<StateMachineException> {
            fsm.transition(Event.GoC) // A에서 GoC는 허용되지 않음
        }
    }

    @Test
    fun `종료 상태에서 전이 시 예외가 발생한다`() {
        val fsm = createFsm()
        fsm.transition(Event.GoB)
        fsm.transition(Event.GoC)
        fsm.transition(Event.GoFinal)

        assertThrows<StateMachineException> {
            fsm.transition(Event.GoB)
        }
    }

    @Test
    fun `canTransition이 올바르게 동작한다`() {
        val fsm = createFsm()

        fsm.canTransition(Event.GoB).shouldBeTrue()
        fsm.canTransition(Event.GoC).shouldBeFalse()
    }

    @Test
    fun `allowedEvents가 올바르게 동작한다`() {
        val fsm = createFsm()

        val allowed = fsm.allowedEvents()
        allowed shouldBeEqualTo setOf(Event.GoB::class.java)
    }

    @Test
    fun `isInFinalState가 올바르게 동작한다`() {
        val fsm = createFsm()

        fsm.isInFinalState().shouldBeFalse()

        fsm.transition(Event.GoB)
        fsm.transition(Event.GoC)
        fsm.transition(Event.GoFinal)

        fsm.isInFinalState().shouldBeTrue()
    }
}
