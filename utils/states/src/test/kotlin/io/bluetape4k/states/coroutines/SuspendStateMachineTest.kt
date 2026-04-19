package io.bluetape4k.states.coroutines

import io.bluetape4k.logging.KLogging
import io.bluetape4k.states.api.TransitionResult
import io.bluetape4k.states.core.on
import io.bluetape4k.states.core.suspendStateMachine
import io.bluetape4k.states.testing.assertReaches
import io.bluetape4k.states.testing.assertRejects
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class SuspendStateMachineTest {
    companion object: KLogging()

    enum class S { IDLE, RUNNING, DONE }
    sealed class E { data object Start: E(); data object Finish: E() }

    private fun fsm() = suspendStateMachine<S, E> {
        initialState = S.IDLE
        finalStates = setOf(S.DONE)
        transition(S.IDLE, on<E.Start>(), to = S.RUNNING)
        transition(S.RUNNING, on<E.Finish>(), to = S.DONE)
    }

    @Test fun `suspend 전이가 성공한다`() = runTest {
        val m = fsm()
        m.transition(E.Start) shouldBeEqualTo TransitionResult(S.IDLE, E.Start, S.RUNNING)
        m.currentState shouldBeEqualTo S.RUNNING
    }

    @Test fun `연속 suspend 전이가 성공한다`() = runTest {
        val m = fsm()
        m.assertReaches(S.DONE, E.Start, E.Finish)
        m.isInFinalState().shouldBeTrue()
    }

    @Test fun `허용되지 않은 전이에서 예외가 발생한다`() = runTest {
        fsm().assertRejects(E.Finish)
    }

    @Test fun `StateFlow로 상태를 관찰할 수 있다`() = runTest {
        val m = fsm()
        m.stateFlow.value shouldBeEqualTo S.IDLE
        m.transition(E.Start); m.stateFlow.value shouldBeEqualTo S.RUNNING
    }

    @Test fun `Mutex로 동시 전이가 직렬화된다`() = runTest {
        val m = fsm()
        val r = async { m.transition(E.Start) }.await()
        r.currentState shouldBeEqualTo S.RUNNING
        m.currentState shouldBeEqualTo S.RUNNING
    }

    @Test fun `종료 상태에서 전이 시 예외가 발생한다`() = runTest {
        val m = fsm()
        m.assertReaches(S.DONE, E.Start, E.Finish)
        m.isInFinalState().shouldBeTrue()
        m.assertRejects(E.Start)
    }

    @Test fun `종료 상태에서는 canTransition이 false를 반환한다`() = runTest {
        val m = suspendStateMachine<S, E> {
            initialState = S.IDLE
            finalStates = setOf(S.DONE)
            transition(S.IDLE, on<E.Finish>(), to = S.DONE)
            transition(S.DONE, on<E.Start>(), to = S.RUNNING)
        }
        m.transition(E.Finish)
        m.canTransition(E.Start).shouldBeFalse()
    }

    @Test fun `종료 상태에서는 allowedEvents가 비어있다`() = runTest {
        val m = suspendStateMachine<S, E> {
            initialState = S.IDLE
            finalStates = setOf(S.DONE)
            transition(S.IDLE, on<E.Finish>(), to = S.DONE)
            transition(S.DONE, on<E.Start>(), to = S.RUNNING)
        }
        m.transition(E.Finish)
        m.allowedEvents().shouldBeEmpty()
    }
}
