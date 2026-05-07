package io.bluetape4k.states.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.states.testing.arrives
import io.bluetape4k.states.testing.assertRejects
import io.bluetape4k.states.testing.verifyPath
import io.bluetape4k.states.testing.via
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import org.junit.jupiter.api.Test

class StateMachineDslTest {
    companion object: KLogging()

    enum class L { RED, YELLOW, GREEN }
    sealed class E { data object Next: E() }

    @Test fun `DSL로 상태 머신을 생성할 수 있다`() {
        val m = stateMachine<L, E> {
            initialState = L.RED
            finalStates = emptySet()
            transition(L.RED, on<E.Next>(), to = L.GREEN)
            transition(L.GREEN, on<E.Next>(), to = L.YELLOW)
            transition(L.YELLOW, on<E.Next>(), to = L.RED)
        }
        m.verifyPath(L.RED via E.Next arrives L.GREEN)
    }

    @Test fun `onTransition 콜백이 호출된다`() {
        val log = mutableListOf<String>()
        val m = stateMachine<L, E> {
            initialState = L.RED
            transition(L.RED, on<E.Next>(), to = L.GREEN)
            onTransition { p, _, n -> log.add("$p -> $n") }
        }
        m.transition(E.Next)
        log shouldContain "RED -> GREEN"
    }

    @Test fun `finalStates 없이 상태 머신을 생성할 수 있다`() {
        val m = stateMachine<L, E> {
            initialState = L.RED
            transition(L.RED, on<E.Next>(), to = L.GREEN)
        }
        m.finalStates shouldBeEqualTo emptySet()
        m.isInFinalState().shouldBeFalse()
    }

    @Test fun `여러 전이를 등록할 수 있다`() {
        val m = stateMachine<L, E> {
            initialState = L.RED
            transition(L.RED, on<E.Next>(), to = L.GREEN)
            transition(L.GREEN, on<E.Next>(), to = L.YELLOW)
            transition(L.YELLOW, on<E.Next>(), to = L.RED)
        }
        m.verifyPath(
            L.RED via E.Next arrives L.GREEN,
            L.GREEN via E.Next arrives L.YELLOW,
            L.YELLOW via E.Next arrives L.RED,
        )
    }

    @Test fun `등록되지 않은 전이에서 예외가 발생한다`() {
        val m = stateMachine<L, E> {
            initialState = L.RED
            finalStates = setOf(L.GREEN)
            transition(L.RED, on<E.Next>(), to = L.GREEN)
        }
        m.transition(E.Next)
        m.isInFinalState().shouldBeTrue()
        m.assertRejects(E.Next)
    }
}
