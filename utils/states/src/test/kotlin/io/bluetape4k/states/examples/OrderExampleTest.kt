package io.bluetape4k.states.examples

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.states.api.StateMachineException
import io.bluetape4k.states.core.on
import io.bluetape4k.states.core.stateMachine
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * 주문(Order) 상태 머신 예제 테스트입니다.
 *
 * 상태: CREATED, PAID, SHIPPED, DELIVERED, CANCELLED
 * 이벤트: Pay, Ship, Deliver, Cancel
 */
class OrderExampleTest {

    companion object: KLogging()

    enum class OrderState {
        CREATED,
        PAID,
        SHIPPED,
        DELIVERED,
        CANCELLED
    }

    sealed class OrderEvent {
        data object Pay: OrderEvent()
        data object Ship: OrderEvent()
        data object Deliver: OrderEvent()
        data class Cancel(val reason: String = ""): OrderEvent()
    }

    private fun createOrderFsm() = stateMachine<OrderState, OrderEvent> {
        initialState = OrderState.CREATED
        finalStates = setOf(OrderState.DELIVERED, OrderState.CANCELLED)

        transition(OrderState.CREATED, on<OrderEvent.Pay>(), to = OrderState.PAID)
        transition(OrderState.PAID, on<OrderEvent.Ship>(), to = OrderState.SHIPPED)
        transition(OrderState.SHIPPED, on<OrderEvent.Deliver>(), to = OrderState.DELIVERED)
        transition(OrderState.CREATED, on<OrderEvent.Cancel>(), to = OrderState.CANCELLED)
        transition(OrderState.PAID, on<OrderEvent.Cancel>(), to = OrderState.CANCELLED)

        onTransition { prev, event, next ->
            log.debug { "$prev --[${event::class.simpleName}]--> $next" }
        }
    }

    @Test
    fun `주문 정상 흐름 - 생성에서 배송완료까지`() {
        val fsm = createOrderFsm()

        fsm.transition(OrderEvent.Pay)
        fsm.currentState shouldBeEqualTo OrderState.PAID

        fsm.transition(OrderEvent.Ship)
        fsm.currentState shouldBeEqualTo OrderState.SHIPPED

        fsm.transition(OrderEvent.Deliver)
        fsm.currentState shouldBeEqualTo OrderState.DELIVERED

        fsm.isInFinalState().shouldBeTrue()
    }

    @Test
    fun `주문 취소 - 생성 상태에서 취소`() {
        val fsm = createOrderFsm()

        val result = fsm.transition(OrderEvent.Cancel("고객 요청"))
        result.currentState shouldBeEqualTo OrderState.CANCELLED
        fsm.isInFinalState().shouldBeTrue()
    }

    @Test
    fun `주문 취소 - 결제 후 취소`() {
        val fsm = createOrderFsm()

        fsm.transition(OrderEvent.Pay)
        val result = fsm.transition(OrderEvent.Cancel("환불 요청"))
        result.currentState shouldBeEqualTo OrderState.CANCELLED
    }

    @Test
    fun `배송 중 취소 불가`() {
        val fsm = createOrderFsm()

        fsm.transition(OrderEvent.Pay)
        fsm.transition(OrderEvent.Ship)

        assertThrows<StateMachineException> {
            fsm.transition(OrderEvent.Cancel("너무 늦음"))
        }
    }
}
