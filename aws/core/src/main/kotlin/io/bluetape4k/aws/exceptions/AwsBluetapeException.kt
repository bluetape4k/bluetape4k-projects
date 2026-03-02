package io.bluetape4k.aws.exceptions

import io.bluetape4k.exceptions.BluetapeException

/**
 * AWS 관련 예외를 표현하는 모듈 공통 상위 예외 클래스입니다.
 *
 * ## 동작/계약
 * - [BluetapeException]을 상속하며 메시지/원인 생성자 오버로드를 그대로 노출한다.
 * - 호출 상황에 따라 기본 생성자, 메시지 전용, 메시지+원인, 원인 전용 생성자를 선택할 수 있다.
 *
 * ```kotlin
 * val ex = AwsBluetapeException("aws failure")
 * // ex.message == "aws failure"
 * ```
 */
open class AwsBluetapeException: BluetapeException {
    /**
     * 메시지와 원인 없이 예외 인스턴스를 생성합니다.
     *
     * ## 동작/계약
     * - 상위 기본 생성자를 호출한다.
     *
     * ```kotlin
     * val ex = AwsBluetapeException()
     * // ex.message == null
     * ```
     */
    constructor(): super()

    /**
     * 메시지만 포함한 예외 인스턴스를 생성합니다.
     *
     * ## 동작/계약
     * - 전달한 [message]를 상위 예외 메시지로 저장한다.
     *
     * ```kotlin
     * val ex = AwsBluetapeException("error")
     * // ex.message == "error"
     * ```
     */
    constructor(message: String): super(message)

    /**
     * 메시지와 원인을 함께 포함한 예외 인스턴스를 생성합니다.
     *
     * ## 동작/계약
     * - [message]와 [cause]를 상위 예외 생성자로 전달한다.
     *
     * ```kotlin
     * val cause = IllegalStateException("boom")
     * val ex = AwsBluetapeException("wrapped", cause)
     * // ex.cause === cause
     * ```
     */
    constructor(message: String, cause: Throwable): super(message, cause)

    /**
     * 원인만 포함한 예외 인스턴스를 생성합니다.
     *
     * ## 동작/계약
     * - [cause]를 상위 예외 생성자로 전달한다.
     *
     * ```kotlin
     * val cause = IllegalArgumentException("invalid")
     * val ex = AwsBluetapeException(cause)
     * // ex.cause === cause
     * ```
     */
    constructor(cause: Throwable): super(cause)
}
