package io.bluetape4k.tokenizer.model

import io.bluetape4k.support.requireNotBlank

/**
 * 형태소 분석 대상 텍스트와 옵션을 전달하는 요청 모델이다.
 *
 * ## 동작/계약
 * - 생성 시 `text.requireNotBlank("text")`를 수행해 공백 입력을 거부한다.
 * - `options`를 생략하면 `TokenizeOptions.DEFAULT`를 사용한다.
 * - `AbstractMessage`의 `timestamp`가 함께 생성되어 요청 시각을 기록한다.
 *
 * ```kotlin
 * val request = tokenizeRequestOf("코틀린 코루틴")
 * // request.text == "코틀린 코루틴"
 * // request.options == TokenizeOptions.DEFAULT
 * ```
 */
data class TokenizeRequest(
    val text: String,
    val options: TokenizeOptions = TokenizeOptions.DEFAULT,
): AbstractMessage() {
    init {
        text.requireNotBlank("text")
    }
}

/**
 * 형태소 분석 요청 객체를 생성한다.
 *
 * ## 동작/계약
 * - `text.requireNotBlank("text")` 검증 후 `TokenizeRequest`를 생성한다.
 * - 텍스트가 공백이면 검증 예외가 발생하고 인스턴스는 만들어지지 않는다.
 *
 * ```kotlin
 * val request = tokenizeRequestOf("비동기 처리", TokenizeOptions())
 * // request.text == "비동기 처리"
 * // request.options.locale == Locale.KOREAN
 * ```
 */
fun tokenizeRequestOf(
    text: String,
    options: TokenizeOptions = TokenizeOptions.DEFAULT,
): TokenizeRequest {
    text.requireNotBlank("text")
    return TokenizeRequest(text, options)
}
