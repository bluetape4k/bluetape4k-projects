package io.bluetape4k.tokenizer.korean.block

import io.bluetape4k.collections.sliding
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanChunker
import io.bluetape4k.tokenizer.korean.tokenizer.KoreanToken
import io.bluetape4k.tokenizer.korean.utils.KoreanPos


/**
 * 금칙어를 회피하기 위해 구두점(Punctuation)을 단어 중간에 추가하는 회피방식에 대응하기 위해, 구두점(Punctuation) 을 제거하도록 합니다.
 *
 * ```
 * 섹.스 -> 섹스
 * 찌~~~찌~뽕 -> 찌찌뽕
 * ```
 */
class PunctuationProcessor {

    companion object: KLogging() {
        private val normalPos = arrayOf(
            KoreanPos.Korean,
            KoreanPos.KoreanParticle,
            KoreanPos.Foreign,
            KoreanPos.Number,
            KoreanPos.Alpha,
            KoreanPos.Adjective
        )
        private val punctuationPos = arrayOf(
            KoreanPos.Punctuation,
            KoreanPos.Email,
            KoreanPos.Hashtag,
            KoreanPos.CashTag,
            KoreanPos.URL,
        )
    }

    /**
     * [text] 중간에 들어간 구두점(Punctuation)을 제거합니다.
     *
     * ```
     * 섹.스 -> 섹스
     * 찌~~~찌~뽕 -> 찌찌뽕
     * ```
     *
     * @param text 구두점이 포함된 문장
     * @return 구두점이 제거된 문장
     */
    fun removePunctuation(text: String): String {
        val tokens = findPunctuation(text)
        var result = text
        tokens.reversed()
            .forEach {
                val token = it.first
                log.trace { "remove token. $it" }
                if (it.second) {
                    result = result.removeRange(token.offset, token.offset + token.length)
                }
            }
        log.trace { "chunk removed text=$result" }
        return result
    }

    /**
     * [text] 중간에 들어간 구두점(Punctuation)을 찾아 [KoreanToken]과 구두점 여부를 반환합니다.
     *
     * ```
     * val list = findPunctuation("섹.스")  // [(섹스, true)]
     * val list = findPunctuation("찌~~~찌~뽕")  // [(찌찌뽕, true)]
     * ```
     *
     * @param text 구두점이 포함된 문장
     * @return 구두점이 포함된 [KoreanToken] 리스트
     */
    fun findPunctuation(text: String): List<Pair<KoreanToken, Boolean>> {
        val chunks = KoreanChunker.chunk(text)

        return chunks
            // .filter { it.pos != KoreanPos.Space }
            .sliding(3, false)
            .onEach { log.trace { "sliding tokens=$it" } }
            .mapIndexed { index, tokens -> (index + 1) to canRemovePunctuation(tokens) }
            .map { chunks[it.first] to it.second }
            .onEach { log.trace { "can remove punctuation=$it" } }
    }


    private fun canRemovePunctuation(tokens: List<KoreanToken>): Boolean {
        if (tokens.size < 3) {
            return false
        }
        val prev = tokens[0]
        val current = tokens[1]
        val next = tokens[2]

        // 중간에 있는 token이 Punctuation이고, 앞뒤로 있는 token이 일반 token이면 Puctuation을 제거할 수 있다고 판단합니다.
        return current.pos in punctuationPos &&
                prev.pos in normalPos &&
                next.pos in normalPos
    }
}
