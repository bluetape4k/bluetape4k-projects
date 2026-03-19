package io.bluetape4k.captcha

import io.bluetape4k.captcha.config.CaptchaConfig

interface CaptchaGenerator<T> {

    /** CAPTCHA 생성 설정입니다. */
    val config: CaptchaConfig

    /** CAPTCHA 코드 생성기입니다. */
    val codeGenerator: CaptchaCodeGenerator

    /**
     * CAPTCHA 코드를 생성해 최종 콘텐츠를 렌더링합니다.
     *
     * ## 동작/계약
     * - 코드 생성은 [codeGenerator] 규칙을 따릅니다.
     * - 반환 객체는 [Captcha.code]와 [Captcha.content]를 함께 제공합니다.
     *
     * ```kotlin
     * val captcha = generator.generate()
     * // captcha.code.length == generator.config.length
     * ```
     */
    fun generate(): Captcha<T>
}
