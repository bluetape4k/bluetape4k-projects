package io.bluetape4k.tokenizer.model

import io.bluetape4k.support.requireNotBlank

/**
 * 형태소 분석 요청 정보
 *
 * @property text 형태소 분석 대상 문자열
 * @property options 형태소 분석을 위한 옵션 정보
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
 * 형태소 분석 요청 정보를 생성한다.
 *
 * @param text    형태소 분석 대상 문자열
 * @param options 형태소 분석을 위한 옵션 정보
 */
fun tokenizeRequestOf(
    text: String,
    options: TokenizeOptions = TokenizeOptions.DEFAULT,
): TokenizeRequest {
    text.requireNotBlank("text")
    return TokenizeRequest(text, options)
}
