package io.bluetape4k.states.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.states.testing.assertRejects
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class GuardedTransitionTest {
    companion object: KLogging()

    enum class S { PENDING, APPROVED, REJECTED }
    sealed class E {
        data class Approve(val approvedBy: String?): E()
        data object Reject: E()
    }

    private fun guarded() = stateMachine<S, E> {
        initialState = S.PENDING
        finalStates = setOf(S.APPROVED, S.REJECTED)
        transition(S.PENDING, on<E.Approve>(), to = S.APPROVED) {
            guard { _, ev -> (ev as E.Approve).approvedBy != null }
        }
        transition(S.PENDING, on<E.Reject>(), to = S.REJECTED)
    }

    @Test fun `guard 조건이 true이면 전이가 성공한다`() {
        guarded().transition(E.Approve("admin")).currentState shouldBeEqualTo S.APPROVED
    }

    @Test fun `guard 조건이 false이면 예외가 발생한다`() {
        guarded().assertRejects(E.Approve(null))
    }

    @Test fun `canTransition이 guard 조건을 반영한다`() {
        val m = guarded()
        m.canTransition(E.Approve("admin")).shouldBeTrue()
        m.canTransition(E.Approve(null)).shouldBeFalse()
    }

    @Test fun `guard 없는 전이는 항상 통과한다`() {
        val m = stateMachine<S, E> {
            initialState = S.PENDING
            finalStates = setOf(S.REJECTED)
            transition(S.PENDING, on<E.Reject>(), to = S.REJECTED)
        }
        m.transition(E.Reject).currentState shouldBeEqualTo S.REJECTED
    }
}
