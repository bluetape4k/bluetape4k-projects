package io.bluetape4k.captcha.exceptions

/**
 * CAPTCHA 폰트 로딩 실패를 나타내는 예외입니다.
 *
 * ## 동작/계약
 * - 폰트 파일 미존재/파싱 실패 시 래핑 예외로 사용됩니다.
 * - [CaptchaException] 계층으로 전파됩니다.
 *
 * ```kotlin
 * val ex = FontLoadException("font not found")
 * // ex.message == "font not found"
 * ```
 */
open class FontLoadException: CaptchaException {
    constructor(message: String): super(message)
    constructor(message: String, cause: Throwable): super(message, cause)
    constructor(cause: Throwable): super(cause)
}
