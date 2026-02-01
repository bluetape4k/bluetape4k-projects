package io.bluetape4k.tokenizer.korean.block

import io.bluetape4k.collections.eclipse.fastListOf
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
 * 한국어 문장 중에 금칙어를 찾고, 마스킹 처리를 수행합니다.
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
     * 금칙어에 해당하는 단어를 찾습니다.
     *
     * ```
     * val blockWords = findBlockwords("미니미와 니미")
     * blockWords.forEach { println(it) }   // `니미`
     * ```
     *
     * @param text 한국어 문장
     * @return 금칙어에 해당하는 [KoreanToken] 리스트
     */
    fun findBlockwords(text: String): List<KoreanToken> {
        if (text.isBlank()) {
            return emptyList()
        }
        try {
            val punctuationRemoved = punctuationProcessor.removePunctuation(text)
            val tokens = KoreanTokenizer.tokenize(punctuationRemoved)
            val blockWords = fastListOf<KoreanToken>()
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
     * 금칙어 (Block words) 를 masking 합니다.
     *
     * 예:
     * - 미니미와 니미 -> 미니미와 **     // `니미` 는 속어
     * - ㅆ.ㅂ 웃기네 -> *** 웃기네      // ㅆㅂ, ㅆ.ㅂ, ㅆ~ㅂ 등을 처리
     *
     * ```
     * val request = BlockwordRequest("미니미와 니미", BlockwordOptions())
     * val response = maskBlockwords(request)
     * println(response.text)  // 미니미와 **
     * ```
     *
     * @param request 금칙어 처리 요청 정보 [BlockwordRequest]
     * @return 금칙어를 처리한 결과 [BlockwordResponse]
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
            val blockWords = fastListOf<String>()

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
