package io.bluetape4k.tokenizer.korean.block

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.bluetape4k.logging.trace
import io.bluetape4k.support.EMPTY_STRING
import io.bluetape4k.tokenizer.exceptions.InvalidTokenizeRequestException
import io.bluetape4k.tokenizer.exceptions.TokenizerException
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanToken
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanTokenizer
import io.bluetape4k.tokenizer.korean.utils.KoreanDictionaryProvider
import io.bluetape4k.tokenizer.korean.utils.KoreanPos
import io.bluetape4k.tokenizer.model.BlockwordRequest
import io.bluetape4k.tokenizer.model.BlockwordResponse
import io.bluetape4k.tokenizer.model.Severity
import io.bluetape4k.tokenizer.model.blockwordResponseOf
import java.util.*

/**
 * 문장에서 금칙어를 탐지하고 마스킹 결과를 생성합니다.
 *
 * ## 동작/계약
 * - 구두점 우회 패턴을 제거한 뒤 토크나이즈 결과로 금칙어를 판정한다.
 * - 마스킹 대상은 길이 2 이상, 지정 품사(`blockedPos`)이며 금칙어 사전에 존재하는 토큰이다.
 * - 처리 중 예외는 `TokenizerException`으로 감싸 재던진다.
 *
 * ```kotlin
 * val found = KoreanBlockwordProcessor.findBlockwords("미니미와 니미")
 * // found.any { it.text == "니미" } == true
 * ```
 */
object KoreanBlockwordProcessor: KLogging() {

    /**
     * 금칙어로서 마스킹 처리할 수 있는 품사
     */
    private val blockedPos = listOf(
        KoreanPos.Noun,
        KoreanPos.Adjective,
        KoreanPos.Verb,
        KoreanPos.Adverb,
        KoreanPos.Korean,
        KoreanPos.KoreanParticle,
        KoreanPos.Foreign,
        KoreanPos.Number,
        KoreanPos.Alpha,        // 영어 금칙어도 적용한다
    )

    private val punctuationProcessor = PunctuationProcessor()

    /**
     * 입력 문장에서 금칙어 토큰 목록을 반환합니다.
     *
     * ## 동작/계약
     * - 공백/빈 문자열 입력이면 빈 리스트를 반환한다.
     * - 구두점 제거 후 토큰화한 결과에서 길이 2 이상 토큰만 검사한다.
     *
     * ```kotlin
     * val tokens = KoreanBlockwordProcessor.findBlockwords("미니미와 니미")
     * // tokens.map { it.text } == ["니미"]
     * ```
     */
    fun findBlockwords(text: String): List<KoreanToken> {
        if (text.isBlank()) {
            return emptyList()
        }
        try {
            val punctuationRemoved = punctuationProcessor.removePunctuation(text)
            val tokens = KoreanTokenizer.tokenize(punctuationRemoved)
            val blockWords = mutableListOf<KoreanToken>()
            tokens
                .onEach { log.trace { "token=$it" } }
                .filter { it.length > 1 }
                .onEach { log.trace { "try to mask block word... token=$it" } }
                .forEach { token ->
                    if (canMask(token)) {
                        log.trace { "mask token=$token" }
                        blockWords.add(token)
                    }
                }
            return blockWords
        } catch (e: Throwable) {
            log.error(e) { "Fail to mask block word. text=[$text]" }
            throw TokenizerException("Fail to mask block word. text=[$text]", e)
        }
    }

    /**
     * 요청 옵션에 따라 금칙어를 마스킹한 응답을 반환합니다.
     *
     * ## 동작/계약
     * - 입력 텍스트가 비어 있으면 빈 문자열 응답을 반환한다.
     * - 요청 언어가 한국어가 아니면 `InvalidTokenizeRequestException`을 던진다.
     * - severity 조건을 만족하는 토큰 구간을 `mask` 문자열 반복값으로 치환한다.
     *
     * ```kotlin
     * val response = KoreanBlockwordProcessor.maskBlockwords(BlockwordRequest("미니미와 니미"))
     * // response.text.contains("**") == true
     * ```
     */
    fun maskBlockwords(request: BlockwordRequest): BlockwordResponse {
        if (request.text.isBlank()) {
            return BlockwordResponse(request, EMPTY_STRING)
        }
        if (request.options.locale.language != Locale.KOREAN.language) {
            throw InvalidTokenizeRequestException("Invalid Language[${request.options.locale.language}], Only support Korean")
        }
        try {
            val punctuationRemoved = punctuationProcessor.removePunctuation(request.text)
            val tokens = KoreanTokenizer.tokenize(punctuationRemoved)

            var result = punctuationRemoved
            val maskStr = request.options.mask
            val blockWords = mutableListOf<String>()

            tokens
                .filter { !it.unknown && it.length > 1 }
                .onEach { log.trace { "try to mask block word... token=$it" } }
                .forEach { token ->
                    if (canMask(token, request.options.severity)) {
                        log.trace { "mask token=$token" }
                        result = result.replaceRange(
                            token.offset,
                            token.offset + token.length,
                            maskStr.repeat(token.length)
                        )
                        blockWords.add(token.text)
                    }
                }
            return blockwordResponseOf(request, result, blockWords)
        } catch (e: Throwable) {
            log.error(e) { "Fail to mask block word. request=$request" }
            throw TokenizerException("Fail to mask block word. request=$request", e)
        }
    }

    /**
     * [token]이 금칙어로서 mask 되어야 할 것인지 판단합니다.
     *
     * 단어 또는 동사의 기본형이 금칙어에 포함되어 있는지 검사한다
     */
    private fun canMask(
        token: KoreanToken,
        severity: Severity = Severity.DEFAULT,
    ): Boolean {
        return token.pos in blockedPos &&
                (containsBlockWord(token.text, severity) || containsBlockWord(token.stem, severity))
    }

    private fun containsBlockWord(
        text: String?,
        severity: Severity = Severity.DEFAULT,
    ): Boolean {
        return text?.run { KoreanDictionaryProvider.blockWords[severity]?.contains(this) } ?: false
    }
}
