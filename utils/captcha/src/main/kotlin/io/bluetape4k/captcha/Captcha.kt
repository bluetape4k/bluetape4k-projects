package io.bluetape4k.captcha

import java.io.Serializable

/**
 * Captcha 를 표현하는 인터페이스
 */
interface Captcha<T>: Serializable {

    /**
     * Captcha 의 코드
     */
    val code: String

    /**
     * Captcha 의 이미지
     */
    val content: T
}
