package io.bluetape4k.states.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.states.api.TransitionResult
import io.bluetape4k.states.testing.assertReaches
import io.bluetape4k.states.testing.assertRejects
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class DefaultStateMachineTest {
    companion object: KLogging()

    enum class S { A, B, C, FINAL }
    sealed class E { data object GoB: E(); data object GoC: E(); data object GoFinal: E() }

    private fun fsm() = stateMachine<S, E> {
        initialState = S.A
        finalStates = setOf(S.FINAL)
        transition(S.A, on<E.GoB>(), to = S.B)
        transition(S.B, on<E.GoC>(), to = S.C)
        transition(S.C, on<E.GoFinal>(), to = S.FINAL)
    }

    @Test fun `초기 상태가 올바르게 설정된다`() {
        val m = fsm()
        m.currentState shouldBeEqualTo S.A
        m.initialState shouldBeEqualTo S.A
    }

    @Test fun `정상적인 상태 전이가 성공한다`() {
        val m = fsm()
        m.transition(E.GoB) shouldBeEqualTo TransitionResult(S.A, E.GoB, S.B)
        m.currentState shouldBeEqualTo S.B
    }

    @Test fun `연속 전이가 성공한다`() {
        val m = fsm()
        m.assertReaches(S.FINAL, E.GoB, E.GoC, E.GoFinal)
        m.isInFinalState().shouldBeTrue()
    }

    @Test fun `허용되지 않은 전이에서 예외가 발생한다`() {
        fsm().assertRejects(E.GoC)
    }

    @Test fun `종료 상태에서 전이 시 예외가 발생한다`() {
        val m = fsm()
        m.assertReaches(S.FINAL, E.GoB, E.GoC, E.GoFinal)
        m.assertRejects(E.GoB)
    }

    @Test fun `canTransition이 올바르게 동작한다`() {
        val m = fsm()
        m.canTransition(E.GoB).shouldBeTrue()
        m.canTransition(E.GoC).shouldBeFalse()
    }

    @Test fun `allowedEvents가 올바르게 동작한다`() {
        fsm().allowedEvents() shouldBeEqualTo setOf(E.GoB::class.java)
    }

    @Test fun `isInFinalState가 올바르게 동작한다`() {
        val m = fsm()
        m.isInFinalState().shouldBeFalse()
        m.assertReaches(S.FINAL, E.GoB, E.GoC, E.GoFinal)
        m.isInFinalState().shouldBeTrue()
    }

    @Test fun `종료 상태에서는 canTransition이 false를 반환한다`() {
        val m = stateMachine<S, E> {
            initialState = S.A
            finalStates = setOf(S.FINAL)
            transition(S.A, on<E.GoFinal>(), to = S.FINAL)
            transition(S.FINAL, on<E.GoB>(), to = S.B)
        }
        m.transition(E.GoFinal)
        m.canTransition(E.GoB).shouldBeFalse()
    }

    @Test fun `종료 상태에서는 allowedEvents가 비어있다`() {
        val m = stateMachine<S, E> {
            initialState = S.A
            finalStates = setOf(S.FINAL)
            transition(S.A, on<E.GoFinal>(), to = S.FINAL)
            transition(S.FINAL, on<E.GoB>(), to = S.B)
        }
        m.transition(E.GoFinal)
        m.allowedEvents().shouldBeEmpty()
    }
}
