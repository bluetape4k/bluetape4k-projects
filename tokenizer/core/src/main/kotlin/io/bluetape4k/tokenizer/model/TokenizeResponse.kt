package io.bluetape4k.tokenizer.model

/**
 * 형태소 분석 결과 토큰 목록을 반환하는 응답 모델이다.
 *
 * ## 동작/계약
 * - `text`는 분석에 사용한 원문을 그대로 담는다.
 * - `tokens` 기본값은 빈 목록이며 토큰 추출 결과가 없음을 의미한다.
 * - `AbstractMessage`를 상속해 응답 생성 시각을 함께 기록한다.
 *
 * ```kotlin
 * val response = tokenizeResponseOf("코틀린 코루틴", listOf("코틀린", "코루틴"))
 * // response.tokens.size == 2
 * // response.text == "코틀린 코루틴"
 * ```
 */
data class TokenizeResponse(
    val text: String,
    val tokens: List<String> = emptyList(),
): AbstractMessage()

/**
 * 형태소 분석 응답 객체를 생성한다.
 *
 * ## 동작/계약
 * - 전달한 원문과 토큰 목록을 그대로 `TokenizeResponse`에 매핑한다.
 * - `tokens`를 생략하면 빈 목록이 사용된다.
 *
 * ```kotlin
 * val response = tokenizeResponseOf("문장")
 * // response.tokens == emptyList<String>()
 * ```
 */
fun tokenizeResponseOf(
    text: String,
    tokens: List<String> = emptyList(),
): TokenizeResponse =
    TokenizeResponse(text, tokens)
