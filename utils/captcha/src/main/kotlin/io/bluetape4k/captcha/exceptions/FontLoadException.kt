package io.bluetape4k.captcha.exceptions

/**
 * Captcha 의 폰트를 로드하는 도중 발생하는 예외
 */
open class FontLoadException: CaptchaException {
    constructor(message: String): super(message)
    constructor(message: String, cause: Throwable): super(message, cause)
    constructor(cause: Throwable): super(cause)
}
