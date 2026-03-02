package io.bluetape4k.tokenizer.exceptions

import io.bluetape4k.exceptions.BluetapeException

/**
 * 토크나이저 모듈 전반에서 공통 기반으로 사용하는 예외 타입이다.
 *
 * ## 동작/계약
 * - `BluetapeException`을 상속해 상위 예외 처리 정책을 따른다.
 * - 기본 생성자와 메시지/원인 조합 생성자를 모두 제공한다.
 * - 모듈별 세부 예외는 이 타입을 상속해 분류한다.
 *
 * ```kotlin
 * val ex = TokenizerException("tokenize 실패")
 * // ex.message == "tokenize 실패"
 * ```
 */
open class TokenizerException: BluetapeException {
    /**
     * 메시지와 원인 없이 예외를 생성한다.
     *
     * ## 동작/계약
     * - 상위 기본 생성자를 호출한다.
     * - `message`와 `cause`는 `null` 상태다.
     *
     * ```kotlin
     * val ex = TokenizerException()
     * // ex.message == null
     * ```
     */
    constructor(): super()

    /**
     * 설명 메시지를 포함한 예외를 생성한다.
     *
     * ## 동작/계약
     * - 전달한 `message`를 상위 예외에 그대로 저장한다.
     * - 원인 예외는 설정하지 않는다.
     *
     * ```kotlin
     * val ex = TokenizerException("잘못된 입력")
     * // ex.message == "잘못된 입력"
     * ```
     */
    constructor(message: String): super(message)

    /**
     * 설명 메시지와 원인 예외를 함께 포함해 생성한다.
     *
     * ## 동작/계약
     * - `message`와 `cause`를 상위 생성자에 그대로 전달한다.
     * - 예외 체인 추적에 사용된다.
     *
     * ```kotlin
     * val cause = IllegalArgumentException("invalid")
     * val ex = TokenizerException("요청 오류", cause)
     * // ex.cause == cause
     * ```
     */
    constructor(message: String, cause: Throwable?): super(message, cause)

    /**
     * 원인 예외만 포함해 생성한다.
     *
     * ## 동작/계약
     * - 상위 `cause` 기반 생성자를 호출한다.
     * - `message`는 원인 예외 정보에 따라 결정된다.
     *
     * ```kotlin
     * val cause = RuntimeException("boom")
     * val ex = TokenizerException(cause)
     * // ex.cause == cause
     * ```
     */
    constructor(cause: Throwable?): super(cause)
}
