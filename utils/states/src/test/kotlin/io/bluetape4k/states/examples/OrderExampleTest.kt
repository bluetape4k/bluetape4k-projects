package io.bluetape4k.states.examples

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.states.core.on
import io.bluetape4k.states.core.stateMachine
import io.bluetape4k.states.testing.arrives
import io.bluetape4k.states.testing.assertRejects
import io.bluetape4k.states.testing.verifyPath
import io.bluetape4k.states.testing.via
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class OrderExampleTest {
    companion object: KLogging()

    enum class S { CREATED, PAID, SHIPPED, DELIVERED, CANCELLED }

    sealed class E {
        data object Pay: E()
        data object Ship: E()
        data object Deliver: E()
        data class Cancel(val reason: String = ""): E()
    }

    private fun fsm() = stateMachine<S, E> {
        initialState = S.CREATED
        finalStates = setOf(S.DELIVERED, S.CANCELLED)
        transition(S.CREATED, on<E.Pay>(), to = S.PAID)
        transition(S.PAID, on<E.Ship>(), to = S.SHIPPED)
        transition(S.SHIPPED, on<E.Deliver>(), to = S.DELIVERED)
        transition(S.CREATED, on<E.Cancel>(), to = S.CANCELLED)
        transition(S.PAID, on<E.Cancel>(), to = S.CANCELLED)
        onTransition { p, ev, n -> log.debug { "$p --[${ev::class.simpleName}]--> $n" } }
    }

    @Test fun `주문 정상 흐름 - 생성에서 배송완료까지`() {
        val m = fsm()
        m.verifyPath(
            S.CREATED via E.Pay arrives S.PAID,
            S.PAID via E.Ship arrives S.SHIPPED,
            S.SHIPPED via E.Deliver arrives S.DELIVERED,
        )
        m.isInFinalState().shouldBeTrue()
    }

    @Test fun `주문 취소 - 생성 상태에서 취소`() {
        val m = fsm()
        m.transition(E.Cancel("고객 요청")).currentState shouldBeEqualTo S.CANCELLED
        m.isInFinalState().shouldBeTrue()
    }

    @Test fun `주문 취소 - 결제 후 취소`() {
        val m = fsm()
        m.transition(E.Pay)
        m.transition(E.Cancel("환불 요청")).currentState shouldBeEqualTo S.CANCELLED
    }

    @Test fun `배송 중 취소 불가`() {
        val m = fsm()
        m.transition(E.Pay); m.transition(E.Ship)
        m.assertRejects(E.Cancel("너무 늦음"))
    }
}
