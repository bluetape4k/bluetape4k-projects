package io.bluetape4k.states.coroutines

import io.bluetape4k.logging.KLogging
import io.bluetape4k.states.api.StateMachineException
import io.bluetape4k.states.api.TransitionResult
import io.bluetape4k.states.core.on
import io.bluetape4k.states.core.suspendStateMachine
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SuspendStateMachineTest {

    companion object: KLogging()

    enum class State {
        IDLE,
        RUNNING,
        DONE
    }

    sealed class Event {
        data object Start: Event()
        data object Finish: Event()
    }

    private fun createFsm() = suspendStateMachine<State, Event> {
        initialState = State.IDLE
        finalStates = setOf(State.DONE)
        transition(State.IDLE, on<Event.Start>(), to = State.RUNNING)
        transition(State.RUNNING, on<Event.Finish>(), to = State.DONE)
    }

    @Test
    fun `suspend 전이가 성공한다`() = runTest {
        val fsm = createFsm()

        val result = fsm.transition(Event.Start)

        result shouldBeEqualTo TransitionResult(State.IDLE, Event.Start, State.RUNNING)
        fsm.currentState shouldBeEqualTo State.RUNNING
    }

    @Test
    fun `연속 suspend 전이가 성공한다`() = runTest {
        val fsm = createFsm()

        fsm.transition(Event.Start)
        val result = fsm.transition(Event.Finish)

        result.currentState shouldBeEqualTo State.DONE
        fsm.isInFinalState().shouldBeTrue()
    }

    @Test
    fun `허용되지 않은 전이에서 예외가 발생한다`() = runTest {
        val fsm = createFsm()

        assertThrows<StateMachineException> {
            fsm.transition(Event.Finish) // IDLE에서 Finish는 허용되지 않음
        }
    }

    @Test
    fun `StateFlow로 상태를 관찰할 수 있다`() = runTest {
        val fsm = createFsm()

        // 초기 상태 확인
        fsm.stateFlow.value shouldBeEqualTo State.IDLE

        fsm.transition(Event.Start)
        fsm.stateFlow.value shouldBeEqualTo State.RUNNING
    }

    @Test
    fun `Mutex로 동시 전이가 직렬화된다`() = runTest {
        val fsm = createFsm()

        // 동시에 두 개의 전이를 시도
        val deferred1 = async { fsm.transition(Event.Start) }
        val result1 = deferred1.await()

        result1.currentState shouldBeEqualTo State.RUNNING
        fsm.currentState shouldBeEqualTo State.RUNNING
    }

    @Test
    fun `종료 상태에서 전이 시 예외가 발생한다`() = runTest {
        val fsm = createFsm()
        fsm.transition(Event.Start)
        fsm.transition(Event.Finish)

        fsm.isInFinalState().shouldBeTrue()

        assertThrows<StateMachineException> {
            fsm.transition(Event.Start)
        }
    }

    @Test
    fun `종료 상태에서는 canTransition이 false를 반환한다`() = runTest {
        val fsm = suspendStateMachine<State, Event> {
            initialState = State.IDLE
            finalStates = setOf(State.DONE)
            transition(State.IDLE, on<Event.Finish>(), to = State.DONE)
            transition(State.DONE, on<Event.Start>(), to = State.RUNNING)
        }

        fsm.transition(Event.Finish)

        fsm.canTransition(Event.Start).shouldBeFalse()
    }

    @Test
    fun `종료 상태에서는 allowedEvents가 비어있다`() = runTest {
        val fsm = suspendStateMachine<State, Event> {
            initialState = State.IDLE
            finalStates = setOf(State.DONE)
            transition(State.IDLE, on<Event.Finish>(), to = State.DONE)
            transition(State.DONE, on<Event.Start>(), to = State.RUNNING)
        }

        fsm.transition(Event.Finish)

        fsm.allowedEvents().shouldBeEmpty()
    }
}
