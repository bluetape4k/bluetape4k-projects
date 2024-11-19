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
 * 일본어 문장중에 금칙어가 있는 경우 Masking 처리를 수행합니다.
 */
object JapaneseBlockwordProcessor: KLogging() {

    /**
     * 금칙어에 해당하는 단어를 찾습니다.
     *
     * ```
     * val text = "ホモの男性を理解できない"
     * val tokens = findBlockwords(text)  // [ホモ]
     * ```
     *
     * @param text 일본어 문장
     * @return 금칙어에 해당하는 [Token] 리스트
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
     * 복합명사, 명사+동사 에 대한 금칙어 처리를 수행합니다.
     *
     * 예:
     *  覚せい剤 : 覚せい(각성) + 剤(제)
     *  盗撮す: 盗(명사) + 撮す(동사), 도찰하다
     *
     * ```
     * val text = "覚せい剤を使用するな"
     * val tokens = JapaneseTokenizer.tokenize(text)
     * val blockwords = processCompositBlockWords(tokens)  // [覚せい]
     * ```
     *
     * @param tokens [Token] 리스트
     * @return 복합명사로서 금칙어에 해댱하는 [Token] 리스트
     */
    private fun processCompositBlockWords(tokens: List<Token>): List<Token> {
        if (tokens.size < 2) {
            return emptyList()
        }
        return tokens.zipWithNext { t1, t2 ->
            if (t1.isNoun() && t2.isNounOrVerb()) {
                val composite = t1.surface + t2.surface
                log.debug { "check blockword for composite=$composite" }
                if (isBlockword(composite)) {
                    return listOf(t1)
                }
            }
            null
        }
            .mapNotNull { it }
    }

    /**
     * 일본어 문장에서 금칙어를 마스킹 합니다. 금칙어가 없는 경우 원본 문장을 반환합니다.
     *
     * ```
     * val request = BlockwordRequest("ホモの男性を理解できない")
     * val response = maskBlockwords(request)
     * println(response.maskedText) // ***の男性を理解できない
     * ```
     *
     * @param request 금칙어 처리 요청 정보 [BlockwordRequest]
     * @return 금칙어를 처리한 결과 [BlockwordResponse]
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
        return token.isNounOrVerb() && isBlockword(token.surface)
    }

    private fun isBlockword(text: String): Boolean {
        return JapaneseDictionaryProvider.blockWordDictionary.contains(text)
    }
}
