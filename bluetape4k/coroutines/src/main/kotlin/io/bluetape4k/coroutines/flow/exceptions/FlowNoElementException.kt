package io.bluetape4k.coroutines.flow.exceptions

/**
 * Flow에서 기대한 요소가 없을 때 사용하는 예외입니다.
 *
 * ## 동작/계약
 * - 요소 부재 상황을 [FlowOperationException] 하위 타입으로 구분합니다.
 * - 상태를 변경하지 않는 불변 예외 객체입니다.
 * - `message`/`cause` 전달 규칙은 표준 [Throwable] 생성자 계약을 따릅니다.
 *
 * ```kotlin
 * val ex = FlowNoElementException("no element")
 * // ex.message == "no element"
 * ```
 */
open class FlowNoElementException: FlowOperationException {
    /** 기본 메시지 없이 예외를 생성합니다. */
    constructor(): super()

    /**
     * 메시지만 지정해 예외를 생성합니다.
     * @param message 예외 설명 메시지입니다.
     */
    constructor(message: String): super(message)

    /**
     * 메시지와 원인 예외를 함께 지정해 예외를 생성합니다.
     * @param message 예외 설명 메시지입니다.
     * @param cause 원인 예외입니다.
     */
    constructor(message: String, cause: Throwable?): super(message, cause)

    /**
     * 원인 예외만 지정해 예외를 생성합니다.
     * @param cause 원인 예외입니다.
     */
    constructor(cause: Throwable?): super(cause)
}
