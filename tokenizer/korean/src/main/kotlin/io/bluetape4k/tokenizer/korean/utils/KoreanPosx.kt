package io.bluetape4k.tokenizer.korean.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Adjective
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Adverb
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Alpha
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.CashTag
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Conjunction
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Determiner
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Email
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Eomi
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Exclamation
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Foreign
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Hashtag
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Josa
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Korean
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.KoreanParticle
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Modifier
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Noun
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Number
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Others
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.PreEomi
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Punctuation
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.ScreenName
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Suffix
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.URL
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.Verb
import io.bluetape4k.tokenizer.korean.utils.KoreanPos.VerbPrefix

/**
 * 한글 품사에 대한 유용한 함수를 제공합니다.
 */
object KoreanPosx: KLogging() {

    val OtherPoses = hashSetOf(
        Korean,
        Foreign,
        Number,
        KoreanParticle,
        Alpha,
        Punctuation,
        Hashtag,
        ScreenName,
        Email,
        URL,
        CashTag
    )

    val shortCut = hashMapOf(
        'N' to Noun,
        'V' to Verb,
        'J' to Adjective,
        'A' to Adverb,
        'D' to Determiner,
        'E' to Exclamation,
        'C' to Conjunction,

        'j' to Josa,
        'e' to Eomi,
        'r' to PreEomi,
        'm' to Modifier,
        'v' to VerbPrefix,
        's' to Suffix,

        'a' to Alpha,
        'n' to Number,

        'o' to Others
    )

    private const val PLUS = '+'
    private const val ANY = '*'
    private const val ONE = '1'
    private const val ZERO = '0'

    val SelfNode = KoreanPosTrie(curPos = null, nextTrie = null, ending = null)

    fun buildTrie(s: String, endingPos: KoreanPos): List<KoreanPosTrie> {

        fun isFinal(rest: String): Boolean {
            return rest.isEmpty() ||
                    rest.fold(true) { output, c ->
                        if (c == '+' || c == '1') false
                        else output
                    }
        }

        // 한자라면 Trie 를 빌드하지 않습니다.
        if (s.length < 2) {
            return emptyList()
        }

        val pos = shortCut[s[0]]
        val rule = s[1]
        val rest = s.slice(2 until s.length)
        val end = if (isFinal(rest)) endingPos else null

        return when (rule) {
            PLUS -> mutableListOf(
                KoreanPosTrie(
                    pos,
                    mutableListOf<KoreanPosTrie>().apply {
                        add(SelfNode)
                        addAll(buildTrie(rest, endingPos))
                    },
                    end
                )
            )

            ANY  -> mutableListOf<KoreanPosTrie>().apply {
                add(KoreanPosTrie(pos, mutableListOf(SelfNode) + buildTrie(rest, endingPos), end))
                addAll(buildTrie(rest, endingPos))
            }

            ONE  -> mutableListOf(KoreanPosTrie(pos, buildTrie(rest, endingPos), end))
            ZERO -> mutableListOf<KoreanPosTrie>().apply {
                add(KoreanPosTrie(pos, buildTrie(rest, endingPos), end))
                addAll(buildTrie(rest, endingPos))
            }

            else -> error("Not supported rule. only support [$PLUS, $ANY, $ONE, $ZERO]")
        }
    }

    internal fun getTrie(
        sequences: Map<String, KoreanPos>,
        destination: MutableList<KoreanPosTrie> = mutableListOf(),
    ): List<KoreanPosTrie> {
        sequences.forEach { (key, value) ->
            destination.addAll(0, buildTrie(key, value))
        }
        return destination
    }

    @JvmField
    val Predicates = setOf(Verb, Adjective)
}
