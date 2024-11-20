package io.bluetape4k.captcha

import io.bluetape4k.captcha.config.CaptchaConfig

interface CaptchaGenerator<T> {

    /**
     * Captcha 생성 설정 정보
     */
    val config: CaptchaConfig

    /**
     * Captcha에 쓰일 문자열 생성기
     */
    val codeGenerator: CaptchaCodeGenerator

    /**
     * `captchaCodeGenerator`로 부터 생성된 Captcha 코드를 컨텐츠로 생성하는 함수
     */
    fun generate(): Captcha<T>
}
