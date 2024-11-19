package io.bluetape4k.tokenizer.model

/**
 * 형태소 분석 결과 응답
 *
 * @property text 형태소 분석 대상 문자열
 * @property tokens 형태소 분석 결과 토큰 목록
 */
data class TokenizeResponse(
    val text: String,
    val tokens: List<String> = emptyList(),
): AbstractMessage()

/**
 * 형태소 분석 결과 응답을 생성한다.
 *
 * @param text   형태소 분석 대상 문자열
 * @param tokens 형태소 분석 결과 토큰 목록
 */
fun tokenizeResponseOf(
    text: String,
    tokens: List<String> = emptyList(),
): TokenizeResponse =
    TokenizeResponse(text, tokens)
