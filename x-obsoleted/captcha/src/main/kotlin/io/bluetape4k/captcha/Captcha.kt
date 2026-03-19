package io.bluetape4k.captcha

import java.io.Serializable

/**
 * CAPTCHA 코드와 렌더링 결과를 함께 보관하는 계약입니다.
 *
 * ## 동작/계약
 * - [code]는 사용자가 입력해야 하는 정답 문자열입니다.
 * - [content]는 타입 [T]로 표현된 렌더링 결과(이미지 등)입니다.
 * - 구현체는 직렬화 가능해야 합니다.
 *
 * ```kotlin
 * val captcha: Captcha<ByteArray> = ...
 * // captcha.code.isNotBlank() == true
 * ```
 */
interface Captcha<T>: Serializable {

    /** CAPTCHA 정답 코드입니다. */
    val code: String

    /** CAPTCHA 렌더링 결과입니다. */
    val content: T
}
