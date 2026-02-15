package io.bluetape4k.tokenizer.korean.tokenizer

import io.bluetape4k.collections.eclipse.fastListOf
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
 * 한글 형대소 분석 결과를 다시 문장으로 복원합니다.
 */
object KoreanDetokenizer: KLogging() {

    @JvmField
    val SuffixPos: Set<KoreanPos> = setOf(Josa, Eomi, PreEomi, Suffix, Punctuation)

    @JvmField
    val PrefixPos: Set<KoreanPos> = setOf(Modifier, VerbPrefix)

    /**
     * 한글 형태소 분석기로 분석된 단어들을 하나의 문장으로 만듭니다.
     *
     * ```
     * var actual = detokenize(listOf("연세", "대학교", "보건", "대학원", "에", "오신", "것", "을", "환영", "합니다", "!"))
     * actual shouldBeEqualTo "연세대학교 보건 대학원에 오신것을 환영합니다!"
     *
     * actual = detokenize(listOf("뭐", "완벽", "하진", "않", "지만", "그럭저럭", "쓸", "만", "하군", "..."))
     * actual shouldBeEqualTo "뭐 완벽하진 않지만 그럭저럭 쓸 만하군..."
     * ```
     *
     * @param input 분석된 단어들
     * @return 복원된 문장
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
        val output = fastListOf<String>()
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
