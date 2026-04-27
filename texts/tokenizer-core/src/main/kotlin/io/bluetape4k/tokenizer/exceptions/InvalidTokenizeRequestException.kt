package io.bluetape4k.tokenizer.exceptions

/**
 * 토크나이즈 요청의 입력 형식이나 값이 유효하지 않을 때 사용하는 예외 타입이다.
 *
 * ## 동작/계약
 * - `TokenizerException`을 상속해 토크나이저 예외 계층에 포함된다.
 * - 메시지/원인 조합 생성자를 제공해 검증 실패 맥락을 전달할 수 있다.
 * - 요청 검증 계층에서 도메인 예외로 분리할 때 사용한다.
 *
 * ```kotlin
 * val ex = InvalidTokenizeRequestException("text는 비어 있을 수 없습니다.")
 * // ex is TokenizerException
 * ```
 */
open class InvalidTokenizeRequestException: TokenizerException {
    /**
     * 메시지와 원인 없이 예외를 생성한다.
     *
     * ## 동작/계약
     * - 상위 기본 생성자를 호출한다.
     * - 진단 정보가 없는 기본 인스턴스를 만든다.
     *
     * ```kotlin
     * val ex = InvalidTokenizeRequestException()
     * // ex.message == null
     * ```
     */
    constructor(): super()

    /**
     * 요청 검증 실패 메시지를 포함해 예외를 생성한다.
     *
     * ## 동작/계약
     * - 전달한 메시지를 그대로 보관한다.
     * - 원인 예외는 비워 둔다.
     *
     * ```kotlin
     * val ex = InvalidTokenizeRequestException("locale 값이 잘못되었습니다.")
     * // ex.message?.contains("locale") == true
     * ```
     */
    constructor(message: String): super(message)

    /**
     * 요청 검증 실패 메시지와 원인 예외를 함께 포함해 생성한다.
     *
     * ## 동작/계약
     * - `message`, `cause`를 상위 생성자에 전달한다.
     * - 파싱/검증 단계에서 발생한 원인 예외를 체인으로 유지한다.
     *
     * ```kotlin
     * val cause = NumberFormatException("NaN")
     * val ex = InvalidTokenizeRequestException("토큰 길이 파싱 실패", cause)
     * // ex.cause == cause
     * ```
     */
    constructor(message: String, cause: Throwable?): super(message, cause)

    /**
     * 원인 예외만 포함해 예외를 생성한다.
     *
     * ## 동작/계약
     * - 원인 예외를 상위 생성자에 그대로 전달한다.
     * - 메시지는 원인 예외의 구현에 따라 결정된다.
     *
     * ```kotlin
     * val cause = IllegalStateException("invalid request")
     * val ex = InvalidTokenizeRequestException(cause)
     * // ex.cause == cause
     * ```
     */
    constructor(cause: Throwable?): super(cause)
}
