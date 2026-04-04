package io.bluetape4k.states.examples

import io.bluetape4k.logging.KLogging
import io.bluetape4k.states.core.on
import io.bluetape4k.states.core.stateMachine
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.junit.jupiter.api.Test

/**
 * 회전문(Turnstile) 상태 머신 예제 테스트입니다.
 *
 * 상태: Locked, Unlocked
 * 이벤트: Coin(동전 투입), Push(밀기)
 */
class TurnstileExampleTest {

    companion object : KLogging()

    enum class TurnstileState { LOCKED, UNLOCKED }

    sealed class TurnstileEvent {
        data object Coin : TurnstileEvent()
        data object Push : TurnstileEvent()
    }

    private fun createTurnstile() = stateMachine<TurnstileState, TurnstileEvent> {
        initialState = TurnstileState.LOCKED
        finalStates = emptySet()

        transition(TurnstileState.LOCKED, on<TurnstileEvent.Coin>(), to = TurnstileState.UNLOCKED)
        transition(TurnstileState.UNLOCKED, on<TurnstileEvent.Push>(), to = TurnstileState.LOCKED)
        // 동전 투입 시에도 Unlocked 유지
        transition(TurnstileState.UNLOCKED, on<TurnstileEvent.Coin>(), to = TurnstileState.UNLOCKED)
    }

    @Test
    fun `동전 투입으로 회전문이 열린다`() {
        val fsm = createTurnstile()

        fsm.currentState shouldBeEqualTo TurnstileState.LOCKED

        val result = fsm.transition(TurnstileEvent.Coin)
        result.currentState shouldBeEqualTo TurnstileState.UNLOCKED
    }

    @Test
    fun `밀기로 회전문이 잠긴다`() {
        val fsm = createTurnstile()

        fsm.transition(TurnstileEvent.Coin)
        val result = fsm.transition(TurnstileEvent.Push)

        result.currentState shouldBeEqualTo TurnstileState.LOCKED
    }

    @Test
    fun `회전문은 종료 상태가 없다`() {
        val fsm = createTurnstile()
        fsm.isInFinalState().shouldBeFalse()

        fsm.transition(TurnstileEvent.Coin)
        fsm.isInFinalState().shouldBeFalse()
    }
}
