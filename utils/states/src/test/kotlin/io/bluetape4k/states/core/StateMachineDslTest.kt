package io.bluetape4k.states.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.states.api.StateMachineException
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StateMachineDslTest {

    companion object: KLogging()

    enum class Light {
        RED,
        YELLOW,
        GREEN
    }

    sealed class LightEvent {
        data object Next: LightEvent()
    }

    @Test
    fun `DSL로 상태 머신을 생성할 수 있다`() {
        val fsm = stateMachine<Light, LightEvent> {
            initialState = Light.RED
            finalStates = emptySet()
            transition(Light.RED, on<LightEvent.Next>(), to = Light.GREEN)
            transition(Light.GREEN, on<LightEvent.Next>(), to = Light.YELLOW)
            transition(Light.YELLOW, on<LightEvent.Next>(), to = Light.RED)
        }

        fsm.currentState shouldBeEqualTo Light.RED
        fsm.transition(LightEvent.Next)
        fsm.currentState shouldBeEqualTo Light.GREEN
    }

    @Test
    fun `onTransition 콜백이 호출된다`() {
        val transitions = mutableListOf<String>()

        val fsm = stateMachine<Light, LightEvent> {
            initialState = Light.RED
            transition(Light.RED, on<LightEvent.Next>(), to = Light.GREEN)
            onTransition { prev, _, next ->
                transitions.add("$prev -> $next")
            }
        }

        fsm.transition(LightEvent.Next)
        transitions shouldContain "RED -> GREEN"
    }

    @Test
    fun `finalStates 없이 상태 머신을 생성할 수 있다`() {
        val fsm = stateMachine<Light, LightEvent> {
            initialState = Light.RED
            transition(Light.RED, on<LightEvent.Next>(), to = Light.GREEN)
        }

        fsm.finalStates shouldBeEqualTo emptySet()
        fsm.isInFinalState().shouldBeFalse()
    }

    @Test
    fun `여러 전이를 등록할 수 있다`() {
        val fsm = stateMachine<Light, LightEvent> {
            initialState = Light.RED
            transition(Light.RED, on<LightEvent.Next>(), to = Light.GREEN)
            transition(Light.GREEN, on<LightEvent.Next>(), to = Light.YELLOW)
            transition(Light.YELLOW, on<LightEvent.Next>(), to = Light.RED)
        }

        fsm.transition(LightEvent.Next)
        fsm.currentState shouldBeEqualTo Light.GREEN
        fsm.transition(LightEvent.Next)
        fsm.currentState shouldBeEqualTo Light.YELLOW
        fsm.transition(LightEvent.Next)
        fsm.currentState shouldBeEqualTo Light.RED
    }

    @Test
    fun `등록되지 않은 전이에서 예외가 발생한다`() {
        val fsm = stateMachine<Light, LightEvent> {
            initialState = Light.RED
            finalStates = setOf(Light.GREEN)
            transition(Light.RED, on<LightEvent.Next>(), to = Light.GREEN)
        }

        fsm.transition(LightEvent.Next)
        fsm.isInFinalState().shouldBeTrue()

        assertThrows<StateMachineException> {
            fsm.transition(LightEvent.Next)
        }
    }
}
