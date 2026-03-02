package io.bluetape4k.captcha.exceptions

import io.bluetape4k.exceptions.BluetapeException

/**
 * CAPTCHA 처리 중 발생하는 예외의 기본 타입입니다.
 *
 * ## 동작/계약
 * - [BluetapeException]을 상속한 런타임 예외 계열입니다.
 * - 메시지/원인(cause) 조합 생성자를 제공합니다.
 *
 * ```kotlin
 * val ex = CaptchaException("captcha failed")
 * // ex.message == "captcha failed"
 * ```
 */
open class CaptchaException: BluetapeException {
    constructor(message: String): super(message)
    constructor(message: String, cause: Throwable): super(message, cause)
    constructor(cause: Throwable): super(cause)
}
