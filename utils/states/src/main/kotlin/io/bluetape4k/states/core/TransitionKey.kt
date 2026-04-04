package io.bluetape4k.states.core

import java.io.Serializable

/**
 * 상태 전이의 키를 나타내는 데이터 클래스입니다.
 *
 * 현재 상태와 이벤트 타입의 조합으로 전이 맵에서 대상 상태를 조회하는 데 사용됩니다.
 *
 * ```kotlin
 * val key = TransitionKey(OrderState.CREATED, OrderEvent.Pay::class.java)
 * val target = transitions[key]
 * ```
 *
 * @param S 상태 타입
 * @param E 이벤트 타입
 * @property state 현재 상태
 * @property eventType 이벤트 클래스 타입
 */
data class TransitionKey<S : Any, E : Any>(
    val state: S,
    val eventType: Class<out E>,
) : Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }
}
