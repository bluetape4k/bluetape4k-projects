package io.bluetape4k.states.api

import java.io.Serializable

/**
 * 상태 전이 결과를 나타내는 데이터 클래스입니다.
 *
 * 이전 상태, 발생한 이벤트, 전이 후 현재 상태를 포함합니다.
 *
 * ```kotlin
 * val result: TransitionResult<OrderState, OrderEvent> = fsm.transition(OrderEvent.Pay())
 * println("${result.previousState} --[${result.event}]--> ${result.currentState}")
 * ```
 *
 * @param S 상태 타입
 * @param E 이벤트 타입
 * @property previousState 전이 이전 상태
 * @property event 전이를 발생시킨 이벤트
 * @property currentState 전이 이후 현재 상태
 */
data class TransitionResult<S: Any, E: Any>(
    val previousState: S,
    val event: E,
    val currentState: S,
): Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }
}
