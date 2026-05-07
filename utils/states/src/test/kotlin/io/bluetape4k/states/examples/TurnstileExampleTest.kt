package io.bluetape4k.states.examples

import io.bluetape4k.logging.KLogging
import io.bluetape4k.states.core.on
import io.bluetape4k.states.core.stateMachine
import io.bluetape4k.states.testing.arrives
import io.bluetape4k.states.testing.verifyPath
import io.bluetape4k.states.testing.via
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import org.junit.jupiter.api.Test

class TurnstileExampleTest {
    companion object: KLogging()

    enum class S { LOCKED, UNLOCKED }
    sealed class E { data object Coin: E(); data object Push: E() }

    private fun fsm() = stateMachine<S, E> {
        initialState = S.LOCKED
        finalStates = emptySet()
        transition(S.LOCKED, on<E.Coin>(), to = S.UNLOCKED)
        transition(S.UNLOCKED, on<E.Push>(), to = S.LOCKED)
        transition(S.UNLOCKED, on<E.Coin>(), to = S.UNLOCKED)
    }

    @Test fun `동전 투입으로 회전문이 열린다`() {
        val m = fsm()
        m.currentState shouldBeEqualTo S.LOCKED
        m.transition(E.Coin).currentState shouldBeEqualTo S.UNLOCKED
    }

    @Test fun `밀기로 회전문이 잠긴다`() {
        fsm().verifyPath(
            S.LOCKED via E.Coin arrives S.UNLOCKED,
            S.UNLOCKED via E.Push arrives S.LOCKED,
        )
    }

    @Test fun `회전문은 종료 상태가 없다`() {
        val m = fsm()
        m.isInFinalState().shouldBeFalse()
        m.transition(E.Coin); m.isInFinalState().shouldBeFalse()
    }
}
