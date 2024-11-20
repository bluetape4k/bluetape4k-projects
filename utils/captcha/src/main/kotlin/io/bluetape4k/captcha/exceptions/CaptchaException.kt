package io.bluetape4k.captcha.exceptions

import io.bluetape4k.exceptions.BluetapeException

/**
 * Captcha 에서 발생하는 예외의 최상위 클래스
 */
open class CaptchaException: BluetapeException {
    constructor(message: String): super(message)
    constructor(message: String, cause: Throwable): super(message, cause)
    constructor(cause: Throwable): super(cause)
}
