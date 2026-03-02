package io.bluetape4k.tokenizer.japanese.block

import com.atilika.kuromoji.ipadic.Token
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.logging.trace
import io.bluetape4k.support.EMPTY_STRING
import io.bluetape4k.tokenizer.exceptions.TokenizerException
import io.bluetape4k.tokenizer.japanese.tokenizer.JapaneseTokenizer
import io.bluetape4k.tokenizer.japanese.tokenizer.isNoun
import io.bluetape4k.tokenizer.japanese.tokenizer.isNounOrVerb
import io.bluetape4k.tokenizer.japanese.utils.JapaneseDictionaryProvider
import io.bluetape4k.tokenizer.model.BlockwordRequest
import io.bluetape4k.tokenizer.model.BlockwordResponse
import io.bluetape4k.tokenizer.model.blockwordResponseOf

/**
 * 일본어 문장에서 금칙어를 탐지하고 마스킹 결과를 생성하는 처리기입니다.
 *
 * ## 동작/계약
 * - 금칙어 판단은 `JapaneseDictionaryProvider.blockWordDictionary` 포함 여부를 기준으로 합니다.
 * - 탐지/마스킹 대상 품사는 명사 또는 동사(`isNounOrVerb`)로 제한됩니다.
 * - 단일 토큰 탐지 실패 시 복합어(앞 토큰 명사 + 뒤 토큰 명사/동사) 조합을 추가 검사합니다.
 *
 * ```kotlin
 * val blockwords = JapaneseBlockwordProcessor
 *     .findBlockwords("ホモの男性を理解できない")
 *     .map { it.surface }
 *
 * // result == ["ホモ"]
 * ```
 */
object JapaneseBlockwordProcessor: KLogging() {

    /**
     * 입력 문장에서 금칙어에 해당하는 토큰 목록을 반환합니다.
     *
     * ## 동작/계약
     * - `text`가 blank이면 즉시 빈 리스트를 반환합니다.
     * - 토큰을 명사/동사로 1차 필터링한 뒤 사전 포함 여부를 검사합니다.
     * - 1차 결과가 비어 있고 토큰이 2개 이상이면 복합어 검사 결과를 추가합니다.
     *
     * ```kotlin
     * val found = JapaneseBlockwordProcessor.findBlockwords("覚せい剤を注文できるサイトはありますか？")
     *     .map { it.surface }
     *
     * // result == ["覚せい"]
     * ```
     */
    fun findBlockwords(text: String): List<Token> {
        if (text.isBlank()) {
            return emptyList()
        }
        val tokens = JapaneseTokenizer.tokenize(text)
        val blockwords = tokens
            .onEach { token -> log.trace { "token=${token.surface}, ${token.allFeatures}" } }
            .filter { it.isNounOrVerb() }
            .filter { isBlockword(it.surface) }
            .toMutableList()

        if (blockwords.isEmpty() && tokens.size > 1) {
            blockwords.addAll(processCompositBlockWords(tokens))
        }

        return blockwords
    }

    /**
     * 복합 명사 또는 명사+동사 조합을 금칙어 사전으로 추가 검사합니다.
     *
     * 예:
     *  覚せい剤 : 覚せい(각성) + 剤(제)
     *  盗撮す: 盗(명사) + 撮す(동사), 도찰하다
     *
     * ```kotlin
     * val request = io.bluetape4k.tokenizer.model.blockwordRequestOf("覚せい剤を注文できるサイトはありますか？")
     * val response = JapaneseBlockwordProcessor.maskBlockwords(request)
     *
     * // response.blockwordExists == true
     * ```
     */
    private fun processCompositBlockWords(tokens: List<Token>): List<Token> {
        if (tokens.size < 2) {
            return emptyList()
        }
        return tokens.zipWithNext { t1, t2 ->
            if (t1.isNoun() && t2.isNounOrVerb()) {
                val composite = t1.surface + t2.surface
                log.debug { "check blockword for composite=$composite" }
                if (isBlockword(composite)) t1 else null
            } else {
                null
            }
        }.filterNotNull()
    }

    /**
     * 요청 텍스트에서 금칙어를 마스크 문자열로 치환한 응답을 반환합니다.
     *
     * ## 동작/계약
     * - 요청 텍스트가 blank이면 `maskedText`가 빈 문자열인 응답을 반환합니다.
     * - 명사/동사 토큰 중 사전에 존재하는 표면형만 치환하고, 치환 길이는 토큰 길이와 동일합니다.
     * - 처리 중 예외가 발생하면 `TokenizerException`으로 감싸 재전파합니다.
     *
     * ```kotlin
     * val request = io.bluetape4k.tokenizer.model.blockwordRequestOf("ホモの男性を理解できない")
     * val response = JapaneseBlockwordProcessor.maskBlockwords(request)
     *
     * // response.maskedText == "**の男性を理解できない"
     * ```
     */
    fun maskBlockwords(request: BlockwordRequest): BlockwordResponse {
        if (request.text.isBlank()) {
            return BlockwordResponse(request, EMPTY_STRING)
        }

        try {

            val tokens = JapaneseTokenizer.tokenize(request.text)
            var maskedText = request.text
            val maskStr = request.options.mask
            val blockwords = mutableListOf<String>()

            tokens
                .onEach { token -> log.trace { "token=${token.surface}, ${token.allFeatures}" } }
                .filter { it.isNounOrVerb() }
                .forEach { token ->
                    if (canMask(token)) {
                        log.trace { "mask token=$token" }
                        maskedText = maskedText.replaceRange(
                            token.position,
                            token.position + token.surface.length,
                            maskStr.repeat(token.surface.length)
                        )
                        blockwords.add(token.surface)
                    }
                }
            return blockwordResponseOf(request, maskedText, blockwords)
        } catch (e: Throwable) {
            log.error(e) { "Fail to mask block words. request=$request" }
            throw TokenizerException("Fail to mask block words. request=$request", e)
        }
    }

    private fun canMask(token: Token): Boolean {
        return isBlockword(token.surface)
    }

    private fun isBlockword(text: String): Boolean {
        return JapaneseDictionaryProvider.blockWordDictionary.contains(text)
    }
}
