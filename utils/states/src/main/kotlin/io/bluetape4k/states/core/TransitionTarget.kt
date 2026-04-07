package io.bluetape4k.states.core

/**
 * 상태 전이의 대상을 나타내는 데이터 클래스입니다.
 *
 * 전이 대상 상태와 선택적 guard 조건을 포함합니다.
 * Guard 조건은 람다 함수이므로 `Serializable`을 구현하지 않습니다.
 *
 * ```kotlin
 * // guard 없는 전이
 * val target = TransitionTarget<OrderState, OrderEvent>(OrderState.PAID)
 *
 * // guard 조건 포함
 * val guardedTarget = TransitionTarget<OrderState, OrderEvent>(
 *     state = OrderState.APPROVED,
 *     guard = { state, event -> (event as ApproveEvent).approvedBy != null }
 * )
 * ```
 *
 * @param S 상태 타입
 * @param E 이벤트 타입
 * @property state 전이 대상 상태
 * @property guard 전이 전에 평가되는 guard 조건 (null이면 항상 통과)
 */
data class TransitionTarget<S: Any, E: Any>(
    val state: S,
    val guard: ((S, E) -> Boolean)? = null,
)
