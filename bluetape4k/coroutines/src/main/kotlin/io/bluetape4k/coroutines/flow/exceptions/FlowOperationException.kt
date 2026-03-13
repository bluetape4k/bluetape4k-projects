package io.bluetape4k.coroutines.flow.exceptions

import io.bluetape4k.exceptions.BluetapeException
import java.io.Serializable

/**
 * Flow 연산 단계에서 발생한 오류를 표현하는 기본 예외입니다.
 *
 * ## 동작/계약
 * - Flow 확장에서 연산 실패를 의미적으로 구분하기 위한 기반 타입입니다.
 * - 상태를 변경하지 않는 불변 예외 객체입니다.
 * - `message`/`cause` 전달 규칙은 표준 [Throwable] 생성자 계약을 따릅니다.
 *
 * ```kotlin
 * val ex = FlowOperationException("throttle failed")
 * // ex.message == "throttle failed"
 * ```
 */
open class FlowOperationException :
    BluetapeException,
    Serializable {
    companion object {
        @JvmStatic
        private val serialVersionUID: Long = 1L
    }

    /** 기본 메시지 없이 예외를 생성합니다. */
    constructor() : super()

    /**
     * 메시지만 지정해 예외를 생성합니다.
     * @param message 예외 설명 메시지입니다.
     */
    constructor(message: String) : super(message)

    /**
     * 메시지와 원인 예외를 함께 지정해 예외를 생성합니다.
     * @param message 예외 설명 메시지입니다.
     * @param cause 원인 예외입니다.
     */
    constructor(message: String, cause: Throwable?) : super(message, cause)

    /**
     * 원인 예외만 지정해 예외를 생성합니다.
     * @param cause 원인 예외입니다.
     */
    constructor(cause: Throwable?) : super(cause)
}
