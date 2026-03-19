package io.bluetape4k.tokenizer.korean.tokenizer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.korean.utils.KoreanPos
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Eomi
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Josa
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Modifier
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Noun
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.PreEomi
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Punctuation
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Suffix
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Verb
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.VerbPrefix


/**
 * 토큰 문자열 목록을 자연스러운 문장 형태로 재조합합니다.
 *
 * ## 동작/계약
 * - 입력 단어 경계로 `spaceGuide`를 만든 뒤 다시 토크나이즈하여 품사 결합 규칙을 적용한다.
 * - 조사/어미/접미사와 접두사는 앞뒤 토큰에 붙여 공백을 줄인다.
 * - 빈 입력이면 빈 문자열을 반환한다.
 *
 * ```kotlin
 * val sentence = KoreanDetokenizer.detokenize(listOf("연세", "대학교", "에", "오신", "것", "을"))
 * // sentence.contains("연세대학교") == true
 * ```
 */
object KoreanDetokenizer: KLogging() {

    /**
     * 앞 토큰에 결합하는 후행 품사 집합입니다.
     *
     * ## 동작/계약
     * - 토큰 결합 시 현재 토큰 품사가 이 집합에 포함되면 직전 문자열에 붙인다.
     *
     * ```kotlin
     * val join = KoreanDetokenizer.SuffixPos.contains(KoreanPos.Josa)
     * // join == true
     * ```
     */
    @JvmField
    val SuffixPos: Set<KoreanPos> = setOf(Josa, Eomi, PreEomi, Suffix, Punctuation)

    /**
     * 다음 토큰과 결합을 유도하는 선행 품사 집합입니다.
     *
     * ## 동작/계약
     * - 토큰 결합 시 현재 토큰 품사가 이 집합이면 다음 토큰을 이어 붙인다.
     *
     * ```kotlin
     * val prefix = KoreanDetokenizer.PrefixPos.contains(KoreanPos.Modifier)
     * // prefix == true
     * ```
     */
    @JvmField
    val PrefixPos: Set<KoreanPos> = setOf(Modifier, VerbPrefix)

    /**
     * 형태소 단어 목록을 재결합해 문장을 만듭니다.
     *
     * ## 동작/계약
     * - `input.joinToString("")` 결과를 `TokenizerProfile(spaceGuide=...)`로 재분석한다.
     * - `Noun + Verb` 조합은 붙여 쓰기(`사랑` + `해`)로 결합한다.
     * - `KoreanDetokenizerTest` 기준으로 `"연세","대학교",...` 입력을 `"연세대학교 ..."` 형태로 복원한다.
     *
     * ```kotlin
     * val text = KoreanDetokenizer.detokenize(listOf("뭐", "완벽", "하진", "않", "지만"))
     * // text == "뭐 완벽하진 않지만"
     * ```
     */
    fun detokenize(input: Collection<String>): String {
        // Space guide prevents tokenizing a word that was not tokenized in the input.
        val spaceGuide = getSpaceGuide(input)

        // Tokenize a merged text with the space guide.
        val tokenized = KoreanTokenizer.tokenize(
            input.joinToString(""),
            TokenizerProfile(spaceGuide = spaceGuide)
        )

        // Attach suffixes and prefixes.
        // Attach Noun + Verb
        if (tokenized.isEmpty()) {
            return ""
        }
        return collapseTokens(tokenized).joinToString(" ")
    }

    private fun collapseTokens(tokenized: List<KoreanToken>): List<String> {
        val output = mutableListOf<String>()
        var isPrefix = false
        var prevToken: KoreanToken? = null

        tokenized
            .onEach { token ->
                if (output.isNotEmpty() && (isPrefix || token.pos in SuffixPos)) {
                    val attached = output.last() + token.text
                    output[output.lastIndex] = attached
                    isPrefix = false
                    prevToken = token
                } else if (prevToken?.pos == Noun && token.pos == Verb) {
                    val attached = (output.lastOrNull().orEmpty()) + token.text
                    output[output.lastIndex] = attached
                    isPrefix = false
                    prevToken = token
                } else if (token.pos in PrefixPos) {
                    output.add(token.text)
                    isPrefix = true
                    prevToken = token
                } else {
                    output.add(token.text)
                    isPrefix = false
                    prevToken = token
                }
            }

        return output
    }

    private fun getSpaceGuide(input: Collection<String>): IntArray {
        val spaceGuid = IntArray(input.size)
        var len = 0
        input.forEachIndexed { index, word ->
            val length = len + word.length
            spaceGuid[index] = length
            len = length
        }
        return spaceGuid
    }
}
