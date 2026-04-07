package io.bluetape4k.states.api

/**
 * 상태 머신에서 발생하는 예외의 기본 클래스입니다.
 *
 * 잘못된 상태 전이, guard 조건 실패 등의 오류 상황에서 발생합니다.
 *
 * ```kotlin
 * throw StateMachineException("허용되지 않은 전이: $currentState + $event")
 * ```
 *
 * @param message 예외 메시지
 * @param cause 원인 예외
 */
open class StateMachineException(
    message: String? = null,
    cause: Throwable? = null,
): RuntimeException(message, cause)
